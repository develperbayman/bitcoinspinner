package com.miracleas.bitcoin_spinner;

import java.math.BigDecimal;
import java.util.LinkedList;
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

import com.bccapi.bitlib.StandardTransactionBuilder;
import com.bccapi.bitlib.StandardTransactionBuilder.InsufficientFundsException;
import com.bccapi.bitlib.StandardTransactionBuilder.UnsignedTransaction;
import com.bccapi.bitlib.crypto.PrivateKeyRing;
import com.bccapi.bitlib.crypto.PublicKeyRing;
import com.bccapi.bitlib.model.Address;
import com.bccapi.bitlib.model.NetworkParameters;
import com.bccapi.bitlib.model.Transaction;
import com.bccapi.bitlib.model.UnspentTransactionOutput;
import com.bccapi.bitlib.util.CoinUtil;
import com.bccapi.ng.api.ApiError;
import com.bccapi.ng.api.Balance;
import com.bccapi.ng.api.BroadcastTransactionResponse;
import com.bccapi.ng.api.QueryUnspentOutputsResponse;
import com.bccapi.ng.async.AbstractCallbackHandler;
import com.bccapi.ng.async.AsyncTask;
import com.miracleas.bitcoin_spinner.SimpleGestureFilter.SimpleGestureListener;

public class SendBitcoinsActivity extends Activity implements SimpleGestureListener,
    AbstractCallbackHandler<QueryUnspentOutputsResponse> {

  private static final int REQUEST_CODE_SCAN = 0;
  private static final int REQUEST_CODE_ADDRESS_BOOK = 1;
  private static final int STANDARD_FEE = 50000;
  private Context mContext;

  private TextView tvValidAdress, tvAvailSpend, tvFeeInfo, tvValidAmount;
  private EditText etAddress, etSpend;
  private Button btnQRScan, btnAddressBook, btnSpend, btnCancel;

  private Address mReceivingAddress;

  private ProgressDialog mCalcFeeProgressDialog;
  private ProgressDialog mSendCoinsProgressDialog;

  private SimpleGestureFilter detector;

  private static final int ABOUT_DIALOG = 1001;

  private boolean mValidAmount = false;

  private InputMethodManager imm;

  private SharedPreferences preferences;
  private AsyncTask mGetUnspentTask;
  private AsyncTask mSubmitTask;
  private UnsignedTransaction mUnsignedTransaction;

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
        etSpend.setText(CoinUtil.valueString(amount));
      }
    }

    Balance balance = SpinnerContext.getInstance().getAsyncApi().getCachedBalance();
    if (balance == null) {
      tvAvailSpend.setText(R.string.unknown);
    } else {
      tvAvailSpend.setText(CoinUtil.valueString(balance.unspent + balance.pendingChange) + " BTC");
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    if (mGetUnspentTask != null) {
      mGetUnspentTask.cancel();
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
      NetworkParameters network = SpinnerContext.getInstance().getNewNetwork();
      if (tempString.matches("")) {
        tvValidAdress.setText("");
        tvValidAdress.setError(null);
        mReceivingAddress = null;
      } else {
        Address address = Address.fromString(tempString, network);
        if (address == null) {
          if (network.isTestnet()) {
            tvValidAdress.setText(R.string.invalid_address_for_testnet);
          } else if (network.isProdnet()) {
            tvValidAdress.setText(R.string.invalid_address_for_prodnet);
          }
          tvValidAdress.setError("");
          mReceivingAddress = null;
        } else {
          tvValidAdress.setText("");
          tvValidAdress.setError(null);
          etSpend.requestFocus();
          mReceivingAddress = address;
        }

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
      Balance balance = SpinnerContext.getInstance().getAsyncApi().getCachedBalance();
      long available = balance.unspent + balance.pendingChange;
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
          requestUnspentOutputs();
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
    btnSpend.setEnabled(mReceivingAddress != null && mValidAmount);
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
          etSpend.setText(CoinUtil.valueString(b.getAmount()));
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

  private void requestUnspentOutputs() {
    mCalcFeeProgressDialog = ProgressDialog.show(mContext, getString(R.string.calculating_fee),
        getString(R.string.calculating_fee_wait), true);
    mGetUnspentTask = SpinnerContext.getInstance().getAsyncApi().queryUnspentOutputs(this);
  }

  @Override
  public void handleCallback(QueryUnspentOutputsResponse response, ApiError error) {
    mGetUnspentTask = null;
    mCalcFeeProgressDialog.dismiss();
    if (error != null) {
      Utils.showConnectionAlert(this);
    }

    // Get change address
    Address changeAddress = SpinnerContext.getInstance().getAsyncApi().getPrimaryBitcoinAddress();

    // Get receiving address
    Address receivingAddress = getReceivingAddress();
    if (receivingAddress == null) {
      Utils.showAlert(this, R.string.unexpected_error);
      return;
    }

    // Get amount to send
    long satoshisToSend = getSatoshisToSend();
    if (satoshisToSend <= 0) {
      Utils.showAlert(this, R.string.unexpected_error);
      return;
    }

    // Create transaction builder
    NetworkParameters network = SpinnerContext.getInstance().getNewNetwork();
    StandardTransactionBuilder txBuilder = new StandardTransactionBuilder(network);
    txBuilder.addOutput(receivingAddress, satoshisToSend);

    // Build list of unspent transaction outputs
    List<UnspentTransactionOutput> unspent = new LinkedList<UnspentTransactionOutput>();
    unspent.addAll(response.unspent);
    unspent.addAll(response.change);

    // Create an unsigned transaction with automatic fee calculation
    PublicKeyRing keyRing = SpinnerContext.getInstance().getPublicKeyRing();
    try {
      mUnsignedTransaction = txBuilder.createUnsignedTransaction(unspent, changeAddress, keyRing, network);
    } catch (InsufficientFundsException e) {
      if (e.fee > STANDARD_FEE) {
        // This trsnasaction has a high fee which we cannot afford
        String msg = String.format(getString(R.string.insufficient_funds_for_fee), CoinUtil.valueString(e.fee));
        Utils.showAlert(mContext, msg);
        return;
      } else {
        // This transaction has a standard fee, but we cannot afford it
        String msg = String.format(getString(R.string.insufficient_funds), CoinUtil.valueString(e.fee));
        Utils.showAlert(mContext, msg);
        return;
      }
    }

    // We can afford the transaction. Verify that the user is OK with the fee if
    // it is larger than normal
    long fee = mUnsignedTransaction.calculateFee();

    if (fee > STANDARD_FEE) {
      // If the fee is larger than 0.0005 we ask the user for confirmation
      AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
      builder.setMessage(String.format(getString(R.string.calculating_fee_done), CoinUtil.valueString(fee)))
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

    // Sign all inputs
    PrivateKeyRing keyRing = SpinnerContext.getInstance().getPrivateKeyRing();
    List<byte[]> signatures = StandardTransactionBuilder.generateSignatures(mUnsignedTransaction.getSignatureInfo(),
        keyRing);

    // Apply signatures to unsigned transaction, and create final transaction
    Transaction transaction = StandardTransactionBuilder.finalizeTransaction(mUnsignedTransaction, signatures);

    mSubmitTask = SpinnerContext.getInstance().getAsyncApi()
        .broadcastTransaction(transaction, new BroadcastCompletionHandler());
  }

  private class BroadcastCompletionHandler implements AbstractCallbackHandler<BroadcastTransactionResponse> {

    @Override
    public void handleCallback(BroadcastTransactionResponse response, ApiError error) {
      mSendCoinsProgressDialog.dismiss();
      mSubmitTask = null;
      if (response == null) {
        Utils.showConnectionAlert(SendBitcoinsActivity.this);
      } else {
        finish();
      }
    }

  }

  private Address getReceivingAddress() {
    return mReceivingAddress;
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
