package com.miracleas.bitcoin_spinner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
import com.miracleas.bitcoin_spinner.SimpleGestureFilter.SimpleGestureListener;

public class SendBitcoinsActivity extends Activity implements SimpleGestureListener, GetSendCoinFormCallbackHandler,
    TransactionSubmissionCallbackHandler {

  private final int STANDARD_FEE = 50000;
  private Context mContext;

  private TextView tvValidAdress, tvAvailSpend, tvFeeInfo, tvValidAmount;
  private EditText etAddress, etSpend;
  private Button btnQRScan, btnAddressBook, btnSpend, btnCancel;

  private static final int REQUEST_CODE_SCAN = 0;
  private static final int REQUEST_CODE_ADDRESS_BOOK = 1;

  private ProgressDialog mCalcFeeProgressDialog;
  private ProgressDialog mSendCoinsProgressDialog;

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
    if (!SpinnerContext.isInitialized()) {
      SpinnerContext.initialize(this, getWindowManager().getDefaultDisplay());
    }

  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!preferences.getString(Consts.LOCALE, "").matches("")) {
      Locale locale = new Locale(preferences.getString(Consts.LOCALE, "en"));
      Locale.setDefault(locale);
      Configuration config = new Configuration();
      config.locale = locale;
      getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    detector = new SimpleGestureFilter(this, this);
    tvValidAdress = (TextView) findViewById(R.id.tv_validation_of_adress);
    tvValidAmount = (TextView) findViewById(R.id.tv_validation_of_amount);
    tvAvailSpend = (TextView) findViewById(R.id.tv_available_spend_balance);
    tvFeeInfo = (TextView) findViewById(R.id.tv_fee_info);
    tvFeeInfo.setText(Html.fromHtml(String
        .format(getString(R.string.transaction_fee_text), "<br /><a href=''>", "</a>")));
    tvFeeInfo.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(R.string.transaction_fee_text_info).setCancelable(false)
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
    btnAddressBook = (Button) findViewById(R.id.btn_address_book);
    btnSpend = (Button) findViewById(R.id.btn_spend);
    btnCancel = (Button) findViewById(R.id.btn_spend_cancel);
    etSpend.setOnKeyListener(handleReturn);
    etAddress.addTextChangedListener(btcAddressValidatorOnTextChanged);
    etSpend.addTextChangedListener(btcBitcoinValidatorOnTextChanged);
    etSpend.setOnFocusChangeListener(btcFocusChangeListenerAddExtend);
    btnQRScan.setOnClickListener(qrScanClickListener);
    btnAddressBook.setOnClickListener(addressBookClickListener);
    btnSpend.setOnClickListener(spendMoneyClickListener);
    btnCancel.setOnClickListener(cancelClickListener);

    // Enable the address book if we have at least one address in it
    btnAddressBook.setEnabled(AddressBookManager.getInstance().numEntries() > 0);

    // If an address has been added pre-populate it. This allows for the
    // Donation feature.
    Bundle extras = getIntent().getExtras();
    if (extras != null) {
      String address = getIntent().getExtras().getString(Consts.BTC_ADDRESS_KEY);
      if (address != null) {
        etAddress.setText(address.trim());
      }
      long amount = getIntent().getExtras().getLong(Consts.BTC_AMOUNT_KEY);
      if (amount != 0) {
        etSpend.setText(CoinUtils.valueString(amount));
      }
    }

    long balance = SpinnerContext.getInstance().getAccount().getCachedBalance();
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
        if (keyCode == KeyEvent.KEYCODE_ENTER && !(event.getAction() == KeyEvent.ACTION_UP)) {
          if (null != v && v == etSpend) {
            imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(etSpend.getWindowToken(), 0);
            v.clearFocus();
            return true;
          }
        }
      }
      return false;
    }
  };

  private final TextWatcher btcAddressValidatorOnTextChanged = new TextWatcher() {

    @Override
    public void afterTextChanged(Editable e) {
      String tempString = e.toString().trim();
      Network network = SpinnerContext.getInstance().getNetwork();
      if (tempString.matches("")) {
        tvValidAdress.setText("");
        tvValidAdress.setError(null);
        mValidAdress = false;
      } else if (!AddressUtil.validateAddress(tempString, network)) {
        if (network.equals(Network.testNetwork)) {
          tvValidAdress.setText(R.string.invalid_address_for_testnet);
        } else if (network.equals(Network.productionNetwork)) {
          tvValidAdress.setText(R.string.invalid_address_for_prodnet);
        }
        mValidAdress = false;
        tvValidAdress.setError("");
      } else {
        tvValidAdress.setText("");
        tvValidAdress.setError(null);
        mValidAdress = true;
        etSpend.requestFocus();
      }
      enableSendButton();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

  };

  private final TextWatcher btcBitcoinValidatorOnTextChanged = new TextWatcher() {

    @Override
    public void afterTextChanged(Editable e) {
      String spendText = etSpend.getText().toString().trim();

      if (spendText.equals("")) {
        mValidAmount = false;
        tvValidAmount.setText("");
        tvValidAmount.setError(null);
        enableSendButton();
        return;
      }

      long spend = getSatoshisToSend();
      long available = SpinnerContext.getInstance().getAccount().getCachedBalance();
      if (spend <= 0) {
        mValidAmount = false;
        tvValidAmount.setText(R.string.invalid_amount);
        tvValidAmount.setError("");
      } else if (spend + STANDARD_FEE > available) {
        mValidAmount = false;
        tvValidAmount.setText(R.string.amount_too_large);
        tvValidAmount.setError("");
      } else {
        mValidAmount = true;
        tvValidAmount.setText("");
        tvValidAmount.setError(null);
      }
      enableSendButton();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

  };

  private final OnFocusChangeListener btcFocusChangeListenerAddExtend = new OnFocusChangeListener() {

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
      if (hasFocus) {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
      }
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
        Toast.makeText(mContext, getString(R.string.install_qr_scanner), Toast.LENGTH_LONG).show();
      }
    }
  };

  private final OnClickListener addressBookClickListener = new OnClickListener() {

    @Override
    public void onClick(View v) {
      Intent intent = new Intent();
      intent.setClass(SendBitcoinsActivity.this, AddressChooserActivity.class);
      startActivityForResult(intent, REQUEST_CODE_ADDRESS_BOOK);
    }
  };

  private final OnClickListener spendMoneyClickListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      // Make sure that we do not show the keyboard if the PIN is wrong
      getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
      Utils.runPinProtectedFunction(mContext, new Runnable() {
        @Override
        public void run() {
          requestSendCoinForm();
        }
      });
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
    final Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(Consts.MARKET_APP_URL,
        packageName)));
    if (getPackageManager().resolveActivity(marketIntent, 0) != null)
      startActivity(marketIntent);
    else
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(Consts.WEBMARKET_APP_URL, packageName))));
  }

  @Override
  public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
    if (requestCode == REQUEST_CODE_SCAN && resultCode == RESULT_OK
        && "QR_CODE".equals(intent.getStringExtra("SCAN_RESULT_FORMAT"))) {
      final String contents = intent.getStringExtra("SCAN_RESULT");
      if (contents.matches("[a-zA-Z0-9]*")) {
        etAddress.setText(contents.trim());
      } else {
        BitcoinUri b = BitcoinUri.parse(contents);
        if (b == null) {
          // not a bitcoin URI
          tvValidAdress.setText(R.string.invalid_address_for_prodnet);
          return;
        }
        etAddress.setText(b.getAddress().trim());
        if (b.getAmount() > 0) {
          etSpend.setText(CoinUtils.valueString(b.getAmount()));
        } else {
          etSpend.setText("");
        }
      }
    } else if (requestCode == REQUEST_CODE_ADDRESS_BOOK && resultCode == RESULT_OK) {
      String address = intent.getStringExtra(AddressChooserActivity.ADDRESS_RESULT_NAME);
      if (address != null) {
        etAddress.setText(address.trim());
      }
    }
  }

  private void requestSendCoinForm() {
    mCalcFeeProgressDialog = ProgressDialog.show(mContext, getString(R.string.calculating_fee),
        getString(R.string.calculating_fee_wait), true);

    String address = getReceivingAddress();
    Long satoshisToSend = getSatoshisToSend();
    mGetFormTask = SpinnerContext.getInstance().getAccount().requestSendCoinForm(address, satoshisToSend, -1, this);
  }

  @Override
  public void handleGetSendCoinFormCallback(SendCoinForm form, String errorMessage) {
    mFormToSend = form;
    mGetFormTask = null;
    mCalcFeeProgressDialog.dismiss();
    if (form == null) {
      Utils.showConnectionAlert(this);
      return;
    }

    long fee = SendCoinFormValidator.calculateFee(mFormToSend);

    String address = getReceivingAddress();
    Long satoshisToSend = getSatoshisToSend();

    // Validate that the server is not cheating us
    List<String> addresses = new ArrayList<String>();
    addresses.add(SpinnerContext.getInstance().getAccount().getPrimaryBitcoinAddress());
    if (!SendCoinFormValidator.validate(mFormToSend, addresses, SpinnerContext.getInstance().getNetwork(),
        satoshisToSend, fee, address)) {
      Utils.showAlert(this, R.string.unexpected_error);
      return;
    }

    if (fee > STANDARD_FEE) {
      // If the fee is larger than 0.0005 we ask the user for confirmation
      AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
      builder.setMessage(String.format(getString(R.string.calculating_fee_done), CoinUtils.valueString(fee)))
          .setCancelable(false).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              sendCoins();
            }
          }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
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
    AsynchronousAccount account = SpinnerContext.getInstance().getAccount();
    Tx tx = AsynchronousAccount.signSendCoinForm(mFormToSend, account);
    mSubmitTask = account.requestTransactionSubmission(tx, this);
  }

  @Override
  public void handleTransactionSubmission(Tx transaction, String errorMessage) {
    mSendCoinsProgressDialog.dismiss();
    mSubmitTask = null;
    if (transaction == null) {
      Utils.showConnectionAlert(this);
    } else {
      finish();
    }
  }

  private String getReceivingAddress() {
    return etAddress.getText().toString();
  }

  private long getSatoshisToSend() {
    String spendText = etSpend.getText().toString().trim();
    if (spendText.equals("")) {
      return -1;
    }
    if (spendText.charAt(0) == '.') {
      spendText = "0" + spendText;
    }

    long spend;
    try {
      BigDecimal amount = new BigDecimal(spendText);
      amount = amount.multiply(new BigDecimal(Consts.SATOSHIS_PER_BITCOIN));
      spend = amount.longValue();
    } catch (NumberFormatException ex) {
      return -1;
    }
    return spend;
  }

  @Override
  public void onSwipe(int direction) {
    switch (direction) {
    // Swiping disabled for now, not sure I like it here
    // case SimpleGestureFilter.SWIPE_RIGHT:
    // finish();
    // break;
    }
  }

  @Override
  public void onDoubleTap() {
  }
}
