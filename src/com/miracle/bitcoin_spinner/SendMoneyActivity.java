package com.miracle.bitcoin_spinner;

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
import com.bccapi.api.SendCoinForm;
import com.miracle.bitcoin_spinner.SimpleGestureFilter.SimpleGestureListener;
import com.bccapi.core.AddressUtil;
import com.bccapi.core.CoinUtils;
import com.bccapi.core.SendCoinFormValidator;

public class SendMoneyActivity extends Activity implements
		SimpleGestureListener {

	private Context mContext;

	private int currentapiVersion;
	
	private TextView tvValidAdress, tvAvailSpend, tvFee;
	private EditText etAddress, etSpend;
	private Button btnQRScan, btnSpend, btnCancel;

	private String mAvailableBitcoins = "";

	private static final int REQUEST_CODE_SCAN = 0;

	private ProgressDialog mProgressDialog;

	private static final Pattern P_AMOUNT = Pattern
			.compile("([\\d.]+)(?:X(\\d+))?");

	private SimpleGestureFilter detector;

	private SharedPreferences preferences;

	private static final int ABOUT_DIALOG = 1001;

	private static final int UPDATE_AVAILABLE_SPEND_MESSAGE = 101;
	private static final int CONNECTION_MESSAGE = 102;

	private static final int REQUEST_CODE_SETTINGS = 10002;

	private boolean mValidAdress = false;
	private boolean mValidAmount = false;

	private String feeText;
	private InputMethodManager imm;

	ProgressBar pbAvailableSpendUpdateProgress;
	RelativeLayout rlAvailableSpend;

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

		pbAvailableSpendUpdateProgress = (ProgressBar) findViewById(R.id.pb_available_spend_update);
		rlAvailableSpend = (RelativeLayout) findViewById(R.id.rl_available_spend_update);

		detector = new SimpleGestureFilter(this, this);

		tvValidAdress = (TextView) findViewById(R.id.tv_validation_of_adress);
		tvAvailSpend = (TextView) findViewById(R.id.tv_available_spend_balance);
		tvFee = (TextView) findViewById(R.id.tv_fee_spend_val);

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

	}

	@Override
	protected void onResume() {
		super.onResume();
		feeText = preferences.getString(Consts.NETWORKFEESIZE, "0.0005");
		tvFee.setText(feeText + " BTC");
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
				Consts.isConnected(mContext);
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
		switch (item.getItemId()) {
		case R.id.about:
			showDialog(ABOUT_DIALOG);
			return true;
		case R.id.settings:
			startActivityForResult(new Intent(this, SettingsActivity.class),
					REQUEST_CODE_SETTINGS);
		default:
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
			if (currentapiVersion <= android.os.Build.VERSION_CODES.FROYO){
				if (event.getAction() == KeyEvent.ACTION_UP) {
					return v.onKeyDown(keyCode, event);
				} else if (keyCode == KeyEvent.KEYCODE_BACK) {
					return v.onKeyDown(keyCode, event);
				}
			} else{
				return v.onKeyDown(keyCode, event);
			};
			return true;
		}
	};

	private final TextWatcher btcAddressValidatorOnTextChanged = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable e) {
			String tempString = e.toString();

			if (tempString.matches(".*\\s.*")) {
				tempString = tempString.replace(" ", "");
				tempString = tempString.replace("\t", "");
				tempString = tempString.replace("\n", "");
				tempString = tempString.replace("\r", "");
				etAddress.setText("");
				etAddress.append(tempString);
			} else {
				if (tempString.matches(""))
					tvValidAdress.setText("");
				else if (!AddressUtil.validateAddress(tempString,
						Consts.network)) {
					if (Consts.network == Network.testNetwork)
						tvValidAdress
								.setText("Invalid address for testnetwork");
					else if (Consts.network == Network.productionNetwork)
						tvValidAdress.setText("Invalid address");
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
			double spend = -1, fee = -1;
			String spendText = etSpend.getText().toString();

			
			if (spendText.contains(" BTC"))
				spendText = spendText.substring(0, spendText.length()-5);
			else if (spendText.matches("\\.")) {
				spendText = "0.";
			}
			
			if (spendText.matches(""))
				mValidAmount = false;
			else {
				spend = Double.parseDouble(spendText);
				fee = Double.parseDouble(feeText);
				if (spend > 0
						&& fee >= 0
						&& (spend + fee <= Double
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
						"Please install ZXing QR-code scanner or Goggles!",
						Toast.LENGTH_LONG).show();
			}
		}
	};

	private final OnClickListener spendMoneyClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			new SpendMoneyTask().execute((Void[]) null);
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
						etSpend.setText(CoinUtils.valueString(Long.parseLong(m
								.group(1))));
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

	private class SpendMoneyTask extends AsyncTask<Void, Void, Long> {
		BigDecimal amount;
		Long toSend, fee;
		SendCoinForm form;
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

			amount = new BigDecimal(feeText).multiply(new BigDecimal(
					Consts.BTC_IN_SATOSHI));

			fee = amount.longValue();

			try {
				form = Consts.account.getSendCoinForm(address, toSend, fee);
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (!SendCoinFormValidator.validate(form, Consts.account, toSend,
					fee, address))
				return 0L;

			try {
				Consts.account.signAndSubmitSendCoinForm(form);
			} catch (APIException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return 1L;
		}

		protected void onPreExecute() {
			mProgressDialog = ProgressDialog.show(mContext,
					getString(R.string.sending_bitcoins),
					getString(R.string.sending_bitcoins_wait), true);
		}

		protected void onPostExecute(Long result) {
			mProgressDialog.dismiss();
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
		Dialog dialog;
		switch (id) {
		case ABOUT_DIALOG:
			dialog = new Dialog(mContext);
			dialog.setTitle(R.string.about_title);
			dialog.setContentView(R.layout.dialog_about);
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	private void readyAvailableSpend(final boolean updateAvailableSpend) {
		Date thisLogin = new Date();
		long loginDiff = thisLogin.getTime() - Consts.lastLogin.getTime();

		pbAvailableSpendUpdateProgress.setVisibility(View.VISIBLE);
		rlAvailableSpend.setVisibility(View.VISIBLE);

		if (loginDiff > 1140000) {
			Consts.lastLogin = new Date();
			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						Consts.account.login();
						Consts.info = Consts.account.getInfo();
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
		} else {
			Message message = handler.obtainMessage();
			message.arg1 = UPDATE_AVAILABLE_SPEND_MESSAGE;
			handler.sendMessage(message);
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
