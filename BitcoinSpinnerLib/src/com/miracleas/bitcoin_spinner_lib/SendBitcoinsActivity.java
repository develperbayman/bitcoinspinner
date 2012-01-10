package com.miracleas.bitcoin_spinner_lib;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
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
import android.widget.TextView;
import android.widget.Toast;

import com.bccapi.api.Network;
import com.bccapi.api.SendCoinForm;
import com.bccapi.api.Tx;
import com.bccapi.core.AddressUtil;
import com.bccapi.core.CoinUtils;
import com.bccapi.core.SendCoinFormValidator;
import com.bccapi.core.Asynchronous.AccountTask;
import com.bccapi.core.Asynchronous.AsynchronousAccount;
import com.bccapi.core.Asynchronous.GetSendCoinFormCallbackHandler;
import com.bccapi.core.Asynchronous.TransactionSubmissionCallbackHandler;
import com.miracleas.bitcoin_spinner_lib.SimpleGestureFilter.SimpleGestureListener;

public class SendBitcoinsActivity extends Activity implements
		SimpleGestureListener, GetSendCoinFormCallbackHandler, TransactionSubmissionCallbackHandler {

	private final int STANDARD_FEE = 50000;
	private Context mContext;

	private TextView tvValidAdress, tvAvailSpend, tvFeeInfo;
	private EditText etAddress, etSpend;
	private Button btnQRScan, btnSpend, btnCancel;

	private static final int REQUEST_CODE_SCAN = 0;

	private ProgressDialog mCalcFeeProgressDialog;
	private ProgressDialog mSendCoinsProgressDialog;

	private static final Pattern P_AMOUNT = Pattern
			.compile("([\\d.]+)(?:X(\\d+))?");

	private SimpleGestureFilter detector;

	private static final int ABOUT_DIALOG = 1001;

	private boolean mValidAdress = false;
	private boolean mValidAmount = false;

	private InputMethodManager imm;

	private SharedPreferences preferences;
	private AccountTask mGetFormTask;
	private AccountTask mSubmitTask;
	private SendCoinForm mFormToSend;
	
	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.send_money);

		mContext = this;

		preferences = getSharedPreferences(Consts.PREFS_NAME, MODE_PRIVATE);

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
		etSpend.addTextChangedListener(btcBitcoinValidatorOnTextChanged);

		etSpend.setOnFocusChangeListener(btcFocusChangeListenerAddExtend);

		btnQRScan.setOnClickListener(qrScanClickListener);
		btnSpend.setOnClickListener(spendMoneyClickListener);
		btnCancel.setOnClickListener(cancelClickListener);
		
		// If an address has been added pre-populate it. This allows for the Donation feature.
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			String address = getIntent().getExtras().getString(Consts.BTC_ADDRESS_KEY);
			if (address != null) {
				etAddress.setText(address);
			}
		}
		
	}

	@Override
	protected void onResume() {
		super.onResume();

		if(!preferences.getString(Consts.LOCALE, "").matches("")) {
			Locale locale = new Locale(preferences.getString(Consts.LOCALE, "en"));
			Locale.setDefault(locale);
			Configuration config = new Configuration();
			config.locale = locale;
			getBaseContext().getResources().updateConfiguration(config,
			      getBaseContext().getResources().getDisplayMetrics());
		}

		long balance = Consts.account.getCachedBalance();
		if (balance == -1) {
			tvAvailSpend.setText(R.string.unknown);
		} else {
			tvAvailSpend.setText(CoinUtils.valueString(balance) + " BTC");
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		if (mGetFormTask != null) {
			mGetFormTask.cancel();
		}
		if (mSubmitTask != null) {
			mSubmitTask.cancel();
		}
		super.onDestroy();
	};
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent me) {
		this.detector.onTouchEvent(me);
		return super.dispatchTouchEvent(me);
	}

	/** Called when menu button is pressed. */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	/** Called when menu item is selected */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		if (item.getItemId() == R.id.about) {
			showDialog(ABOUT_DIALOG);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	private final OnKeyListener handleReturn = new OnKeyListener() {

		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				if (keyCode == KeyEvent.KEYCODE_ENTER
						&& !(event.getAction() == KeyEvent.ACTION_UP)) {
					if (null != v && v == etSpend) {
						imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(etSpend.getWindowToken(), 0);
						v.clearFocus();
						return true;
					}
				}
			}
			return false;
			// Handle all other keys in the default way
//			if (currentapiVersion <= android.os.Build.VERSION_CODES.FROYO) {
//				if (event.getAction() == KeyEvent.ACTION_UP) {
//					return v.onKeyDown(keyCode, event);
//				} else if (keyCode == KeyEvent.KEYCODE_BACK) {
//					return v.onKeyDown(keyCode, event);
//				}
//			} else {
//				return v.onKeyDown(keyCode, event);
//			}
//			return true;
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
				else if (!AddressUtil.validateAddress(tempString, Consts.account.getNetwork())) {
					if (Consts.account.getNetwork() == Network.testNetwork)
						tvValidAdress.setText(R.string.invalid_address_for_testnet);
					else if (Consts.account.getNetwork() == Network.productionNetwork)
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

	private final TextWatcher btcBitcoinValidatorOnTextChanged = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable e) {
			String spendText = etSpend.getText().toString();

			if (spendText.contains(" BTC"))
				spendText = spendText.substring(0, spendText.length() - 4);
			else if (spendText.matches("\\.")) {
				spendText = "0.";
			}

			if (spendText.matches(""))
				mValidAmount = false;
			else {
				long spend = (long) (Double.parseDouble(spendText) * Consts.SATOSHIS_PER_BITCOIN);
				long available = Consts.account.getCachedBalance();
				if (spend > 0 && (spend + STANDARD_FEE <= available)) {
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
			requestSendCoinForm();
		}
	};

	private final OnClickListener cancelClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			finish();
		}
	};

	private void enableSendButton() {
		btnSpend.setEnabled(mValidAdress && mValidAmount);
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
		}
	}

	private void requestSendCoinForm() {
		mCalcFeeProgressDialog = ProgressDialog.show(mContext, getString(R.string.calculating_fee),
				getString(R.string.calculating_fee_wait), true);

		String address = getReceivingAddress();
		Long satoshisToSend = getSatoshisToSend();
		mGetFormTask = Consts.account.requestSendCoinForm(address, satoshisToSend, -1, this);
	}
	
	@Override
	public void handleGetSendCoinFormCallback(SendCoinForm form, String errorMessage) {
		mFormToSend = form;
		mGetFormTask = null;
		mCalcFeeProgressDialog.dismiss();
		if(form == null) {
			Utils.showConnectionAlert(this);
			return;
		}
		
		long fee = SendCoinFormValidator.calculateFee(mFormToSend);
		
		String address = getReceivingAddress();
		Long satoshisToSend = getSatoshisToSend();
		
		// Validate that the server is not cheating us
		List<String> addresses = new ArrayList<String>();
		addresses.add(Consts.account.getPrimaryBitcoinAddress());
		if (!SendCoinFormValidator.validate(mFormToSend, addresses, Consts.account.getNetwork(), satoshisToSend,
				fee, address)){
			Utils.showAlert(this,R.string.unexpected_error);
			return;
		}

		if (fee != STANDARD_FEE) {
			// If the fee is not 0.0005 we ask the user for confirmation
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setMessage(String.format(getString(R.string.calculating_fee_done), CoinUtils.valueString(fee)))
					.setCancelable(false)
					.setPositiveButton(R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									sendCoins();
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
			sendCoins();
		}
	}
	
	private void sendCoins() {
		mSendCoinsProgressDialog = ProgressDialog.show(mContext, getString(R.string.sending_bitcoins),
				getString(R.string.sending_bitcoins_wait), true);
		Tx tx = AsynchronousAccount.signSendCoinForm(mFormToSend, Consts.account);
		mSubmitTask = Consts.account.requestTransactionSubmission(tx, this);
	}
	
	@Override
	public void handleTransactionSubmission(Tx transaction, String errorMessage) {
		mSendCoinsProgressDialog.dismiss();
		mSubmitTask = null;
		if(transaction == null) {
			Utils.showConnectionAlert(this);
		}else {
			finish();
		}
		
	}
	
	private String getReceivingAddress(){
		return etAddress.getText().toString();
	}
	
	private long getSatoshisToSend() {
		BigDecimal amount;
		if (etSpend.getText().toString().matches(".* BTC"))
			amount = new BigDecimal(etSpend.getText().toString()
					.substring(0, etSpend.getText().toString().length() - 4));
		else {
			amount = new BigDecimal(etSpend.getText().toString());
		}
		amount = amount.multiply(new BigDecimal(Consts.SATOSHIS_PER_BITCOIN));
		return amount.longValue();
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
}
