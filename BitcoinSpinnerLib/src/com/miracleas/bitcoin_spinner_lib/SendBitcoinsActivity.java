package com.miracleas.bitcoin_spinner_lib;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bccapi.api.APIException;
import com.bccapi.api.Network;
import com.bccapi.core.AddressUtil;
import com.bccapi.core.CoinUtils;
import com.bccapi.core.SendCoinFormValidator;
import com.miracleas.bitcoin_spinner_lib.SimpleGestureFilter.SimpleGestureListener;

public class SendBitcoinsActivity extends Activity implements
		SimpleGestureListener {

	private Context mContext;

	private int currentapiVersion;

	private TextView tvValidAdress, tvAvailSpend, tvFeeInfo;
	private EditText etAddress, etSpend;
	private Button btnQRScan, btnSpend, btnCancel;

	private String mAvailableBitcoins = "";

	private static final int REQUEST_CODE_SCAN = 0;

	private ProgressDialog mCalcFeeProgressDialog;
	private ProgressDialog mSendCoinsProgressDialog;

	private static final Pattern P_AMOUNT = Pattern
			.compile("([\\d.]+)(?:X(\\d+))?");

	private SimpleGestureFilter detector;

	private static final int ABOUT_DIALOG = 1001;

	private static final int UPDATE_AVAILABLE_SPEND_MESSAGE = 101;
	private static final int CONNECTION_MESSAGE = 102;

	private static final int REQUEST_CODE_SETTINGS = 10002;

	private boolean mValidAdress = false;
	private boolean mValidAmount = false;

	private InputMethodManager imm;

	ProgressBar pbAvailableSpendUpdateProgress;
	RelativeLayout rlAvailableSpend;

	private BigDecimal amount;
	private Long toSend, fee;
	
	private SharedPreferences preferences;
	private SharedPreferences.Editor editor;
	
	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.send_money);

		currentapiVersion = android.os.Build.VERSION.SDK_INT;

		mContext = this;

		new Thread(ConnectionWatcher).start();

		preferences = getSharedPreferences(Consts.PREFS_NAME, MODE_PRIVATE);
		editor = preferences.edit();

		pbAvailableSpendUpdateProgress = (ProgressBar) findViewById(R.id.pb_available_spend_update);
		rlAvailableSpend = (RelativeLayout) findViewById(R.id.rl_available_spend_update);

		detector = new SimpleGestureFilter(this, this);

		tvValidAdress = (TextView) findViewById(R.id.tv_validation_of_adress);
		tvAvailSpend = (TextView) findViewById(R.id.tv_available_spend_balance);
		
		tvFeeInfo = (TextView) findViewById(R.id.tv_fee_info);
		tvFeeInfo.setText(Html.fromHtml(String.format(getString(R.string.transaction_fee_text), "<br /><a href=''>", "</a>")));
		tvFeeInfo.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				builder.setMessage(R.string.transaction_fee_text_info)
				       .setCancelable(false)
				       .setNeutralButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				           }
				       });
				AlertDialog alertDialog = builder.create();
				alertDialog.show();
			}
		});
		
		etAddress = (EditText) findViewById(R.id.et_address);
		etSpend = (EditText) findViewById(R.id.et_spend);

		btnQRScan = (Button) findViewById(R.id.btn_qr_scan);
		btnSpend = (Button) findViewById(R.id.btn_spend);
		btnCancel = (Button) findViewById(R.id.btn_spend_cancel);

		etSpend.setOnKeyListener(handleReturn);

		etAddress.addTextChangedListener(btcAddressValidatorOnTextChanged);
		etSpend.addTextChangedListener(btcBitCoinValidatorOnTextChanged);

		etSpend.setOnFocusChangeListener(btcFocusChangeListenerAddExtend);

		btnQRScan.setOnClickListener(qrScanClickListener);
		btnSpend.setOnClickListener(spendMoneyClickListener);
		btnCancel.setOnClickListener(cancelClickListener);

		mAvailableBitcoins = preferences.getString(Consts.LASTKNOWNBALANCE, "0");
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (isConnected())
			readyAvailableSpend(true);
		else {
			tvAvailSpend.setText(preferences.getString(
					Consts.AVAIABLE_BITCOINS, "0.00000") + " BTC");
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	private Runnable ConnectionWatcher = new Runnable() {

		@Override
		public void run() {
			while (true) {
				Message message = handler.obtainMessage();
				message.arg1 = CONNECTION_MESSAGE;
				handler.sendMessage(message);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};

	@Override
	public boolean dispatchTouchEvent(MotionEvent me) {
		this.detector.onTouchEvent(me);
		return super.dispatchTouchEvent(me);
	}

	/** Called when menu button is pressed. */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	/** Called when menu item is selected */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		if (item.getItemId() == R.id.about) {
			showDialog(ABOUT_DIALOG);
			return true;
		} else if (item.getItemId() == R.id.settings) {
			startActivityForResult(new Intent(this, SettingsActivity.class),
					REQUEST_CODE_SETTINGS);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	private final OnKeyListener handleReturn = new OnKeyListener() {

		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if (keyCode == KeyEvent.KEYCODE_ENTER
					&& !(event.getAction() == KeyEvent.ACTION_UP)) {
				if (null != v && v == etSpend) {
					imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(etSpend.getWindowToken(), 0);
					v.clearFocus();
					return true;
				}
			}
			// Handle all other keys in the default way
			if (currentapiVersion <= android.os.Build.VERSION_CODES.FROYO) {
				if (event.getAction() == KeyEvent.ACTION_UP) {
					return v.onKeyDown(keyCode, event);
				} else if (keyCode == KeyEvent.KEYCODE_BACK) {
					return v.onKeyDown(keyCode, event);
				}
			} else {
				return v.onKeyDown(keyCode, event);
			}
			;
			return true;
		}
	};

	private final TextWatcher btcAddressValidatorOnTextChanged = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable e) {
			String tempString = e.toString();
		
			if (tempString.matches("\\W")) {
				tempString = tempString.replaceAll("[\\W]", "");
				etAddress.setText("");
				etAddress.append(tempString);
			} else {
				if (tempString.matches(""))
					tvValidAdress.setText("");
				else if (!AddressUtil.validateAddress(tempString,
						Consts.network)) {
					if (Consts.network == Network.testNetwork)
						tvValidAdress
								.setText(R.string.invalid_address_for_testnet);
					else if (Consts.network == Network.productionNetwork)
						tvValidAdress.setText(R.string.invalid_address_for_prodnet);
					mValidAdress = false;
				} else {
					tvValidAdress.setText("");
					mValidAdress = true;
					etSpend.requestFocus();
				}
				enableSendButton();
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
		}

	};

	private final TextWatcher btcBitCoinValidatorOnTextChanged = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable e) {
			double spend = -1;
			String spendText = etSpend.getText().toString();

			if (spendText.contains(" BTC"))
				spendText = spendText.substring(0, spendText.length() - 4);
			else if (spendText.matches("\\.")) {
				spendText = "0.";
			}

			if (spendText.matches(""))
				mValidAmount = false;
			else {
				spend = Double.parseDouble(spendText);
				if (spend > 0
						&& (spend + 0.0005 <= Double
								.parseDouble(mAvailableBitcoins))) {
					mValidAmount = true;
				} else
					mValidAmount = false;
			}
			enableSendButton();
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
		}

	};

	private final OnFocusChangeListener btcFocusChangeListenerAddExtend = new OnFocusChangeListener() {

		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			String text = ((EditText) v).getText().toString();
			if (text.matches("0.0000 BTC")) {
				((EditText) v).setText("");
			} else if (text.matches("")) {
				((EditText) v).setText("0.0000 BTC");
			} else if (text.matches(".* BTC")) {
				((EditText) v).setText("");
				((EditText) v).append(text.substring(0, text.length() - 4));
			} else {
				((EditText) v).setText(text.replaceAll("[^.0-9]", "") + " BTC");
			}
			if (hasFocus)
				getWindow()
						.setSoftInputMode(
								WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		}
	};

	private final OnClickListener qrScanClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			final PackageManager pm = getPackageManager();
			if (pm.resolveActivity(Consts.zxingIntent, 0) != null) {
				startActivityForResult(Consts.zxingIntent, REQUEST_CODE_SCAN);
			} else if (pm.resolveActivity(Consts.gogglesIntent, 0) != null) {
				startActivity(Consts.gogglesIntent);
			} else {
				showMarketPage(Consts.PACKAGE_NAME_ZXING);
				Toast.makeText(mContext,
						getString(R.string.install_qr_scanner),
						Toast.LENGTH_LONG).show();
			}
		}
	};

	private final OnClickListener spendMoneyClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			new CalculateFeeTask().execute((Void[]) null);
		}
	};

	private final OnClickListener cancelClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			finish();
		}
	};

	private void enableSendButton() {
		if (mValidAdress && mValidAmount && isConnected())
			btnSpend.setEnabled(true);
		else
			btnSpend.setEnabled(false);
	}

	private void showMarketPage(final String packageName) {
		final Intent marketIntent = new Intent(Intent.ACTION_VIEW,
				Uri.parse(String.format(Consts.MARKET_APP_URL, packageName)));
		if (getPackageManager().resolveActivity(marketIntent, 0) != null)
			startActivity(marketIntent);
		else
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(String
					.format(Consts.WEBMARKET_APP_URL, packageName))));
	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode,
			final Intent intent) {
		if (requestCode == REQUEST_CODE_SCAN
				&& resultCode == RESULT_OK
				&& "QR_CODE"
						.equals(intent.getStringExtra("SCAN_RESULT_FORMAT"))) {
			final String contents = intent.getStringExtra("SCAN_RESULT");
			if (contents.matches("[a-zA-Z0-9]*")) {
				etAddress.setText(contents);
			} else {
				Uri uri = Uri.parse(contents);
				final Uri u = Uri.parse("bitcoin://"
						+ uri.getSchemeSpecificPart());

				etAddress.setText(u.getHost());

				final String amountStr = u.getQueryParameter("amount");
				if (amountStr != null) {
					final Matcher m = P_AMOUNT.matcher(amountStr);
					if (m.matches()) {
						etSpend.setText(CoinUtils.valueString(new BigDecimal(m
								.group(1)).movePointRight(8).toBigIntegerExact()));
						if (m.group(2) != null)
							;
						// amount.multiply(BigInteger.valueOf(10).pow(Integer.parseInt(m.group(2))
						// - 8));
					}
				}
			}
		} else if (requestCode == REQUEST_CODE_SETTINGS) {
			if (isConnected())
				readyAvailableSpend(true);
		}
	}

	private class CalculateFeeTask extends AsyncTask<Void, Void, Long> {
		String address;

		protected Long doInBackground(Void... voids) {
			address = etAddress.getText().toString();

			if (etSpend.getText().toString().matches(".* BTC"))
				amount = new BigDecimal(
						etSpend.getText()
								.toString()
								.substring(
										0,
										etSpend.getText().toString().length() - 4));
			else
				amount = new BigDecimal(etSpend.getText().toString());

			amount = amount.multiply(new BigDecimal(Consts.BTC_IN_SATOSHI));
			toSend = amount.longValue();

			try {
				Consts.form = Consts.account.getSendCoinForm(address, toSend, -1);
			} catch (Exception e) {
				try {
					Consts.account.login();
					Consts.form = Consts.account.getSendCoinForm(address, toSend, -1);
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (APIException e1) {
					e1.printStackTrace();
				}
			}

			fee = SendCoinFormValidator.calculateFee(Consts.form);

			return 1L;
		}

		protected void onPreExecute() {
			mCalcFeeProgressDialog = ProgressDialog.show(mContext,
					getString(R.string.calculating_fee),
					getString(R.string.calculating_fee_wait), true);
		}

		protected void onPostExecute(Long result) {
			mCalcFeeProgressDialog.dismiss();
			if (result == 1L) {
				if (fee != 50000) {
					AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
					builder.setMessage(String.format(getString(R.string.calculating_fee_done), CoinUtils.valueString(fee)))
							.setCancelable(false)
							.setPositiveButton(R.string.yes,
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog,
												int id) {
											new SpendMoneyTask()
													.execute((Void[]) null);
										}
									})
							.setNegativeButton(R.string.no,
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog,
												int id) {
										}
									});
					AlertDialog alertDialog = builder.create();
					alertDialog.show();
				} else {
					new SpendMoneyTask()
					.execute((Void[]) null);
				}
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				builder.setMessage("Something went wrong! Try again!")
						.setCancelable(false)
						.setNeutralButton("Ok",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
									}
								});
				AlertDialog alertDialog = builder.create();
				alertDialog.show();
			}
		}
	}

	private class SpendMoneyTask extends AsyncTask<Void, Void, Long> {
		String address;

		protected Long doInBackground(Void... voids) {
			address = etAddress.getText().toString();

			if (etSpend.getText().toString().matches(".* BTC"))
				amount = new BigDecimal(
						etSpend.getText()
								.toString()
								.substring(
										0,
										etSpend.getText().toString().length() - 4));
			else
				amount = new BigDecimal(etSpend.getText().toString());

			amount = amount.multiply(new BigDecimal(Consts.BTC_IN_SATOSHI));
			toSend = amount.longValue();

			if (!SendCoinFormValidator.validate(Consts.form, Consts.account, toSend,
					fee, address))
				return 0L;

			try {
				Consts.account.signAndSubmitSendCoinForm(Consts.form);
			} catch (APIException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return 1L;
		}

		protected void onPreExecute() {
			mSendCoinsProgressDialog = ProgressDialog.show(mContext,
					getString(R.string.sending_bitcoins),
					getString(R.string.sending_bitcoins_wait), true);
		}

		protected void onPostExecute(Long result) {
			mSendCoinsProgressDialog.dismiss();
			if (result == 1L) {
				finish();
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				builder.setMessage("Something went wrong! Try again!")
						.setCancelable(false)
						.setNeutralButton("Ok",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
									}
								});
				AlertDialog alertDialog = builder.create();
				alertDialog.show();
			}
		}
	}

	@Override
	public void onSwipe(int direction) {
		switch (direction) {

		case SimpleGestureFilter.SWIPE_RIGHT:
			finish();
			break;
		}
	}

	@Override
	public void onDoubleTap() {
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		final Dialog dialog;
		switch (id) {
		case ABOUT_DIALOG:
			dialog = new Dialog(mContext);
			dialog.setTitle(R.string.about_title);
			dialog.setContentView(R.layout.dialog_about);

			try {
				String VersionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
				TextView tvAboutText = (TextView)dialog.findViewById(R.id.tv_about_text);
				tvAboutText.setText(String.format(getString(R.string.about_text), VersionName));
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}

			dialog.findViewById(R.id.btn_about_ok).setOnClickListener(
					new OnClickListener() {

						@Override
						public void onClick(View v) {
							dialog.dismiss();
						}
					});
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	private void readyAvailableSpend(final boolean updateAvailableSpend) {
		long loginDiff = new Date().getTime() - preferences.getLong(Consts.LASTLOGIN, new Date().getTime());

		pbAvailableSpendUpdateProgress.setVisibility(View.VISIBLE);
		rlAvailableSpend.setVisibility(View.VISIBLE);

		if (loginDiff > 1140000 && isConnected()) {
			editor.putLong(Consts.LASTLOGIN, new Date().getTime());
			editor.commit();

			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						Consts.info = Consts.account.login();
						if (updateAvailableSpend) {
							Message message = handler.obtainMessage();
							message.arg1 = UPDATE_AVAILABLE_SPEND_MESSAGE;
							handler.sendMessage(message);
						} else {
							pbAvailableSpendUpdateProgress
									.setVisibility(View.INVISIBLE);
							rlAvailableSpend.setVisibility(View.INVISIBLE);
						}
					} catch (APIException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			t.start();
		} else if (isConnected()) {
			Message message = handler.obtainMessage();
			message.arg1 = UPDATE_AVAILABLE_SPEND_MESSAGE;
			handler.sendMessage(message);
		} else {
			mAvailableBitcoins = preferences.getString(Consts.LASTKNOWNBALANCE, "0");
			tvAvailSpend.setText(mAvailableBitcoins + " BTC");
		}
	}

	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.arg1) {
			case UPDATE_AVAILABLE_SPEND_MESSAGE:
				mAvailableBitcoins = CoinUtils.valueString(Consts.info
						.getAvailableBalance());
				tvAvailSpend.setText(mAvailableBitcoins + " BTC");

				pbAvailableSpendUpdateProgress.setVisibility(View.INVISIBLE);
				rlAvailableSpend.setVisibility(View.INVISIBLE);
				break;

			case CONNECTION_MESSAGE:
				enableSendButton();
				break;
			}
		}
	};

	private boolean isConnected() {
		return Consts.isConnected(mContext);
	}
}
