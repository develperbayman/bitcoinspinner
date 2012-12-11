package com.miracleas.bitcoin_spinner;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bccapi.bitlib.model.Address;
import com.bccapi.bitlib.model.NetworkParameters;
import com.bccapi.bitlib.util.CoinUtil;
import com.bccapi.bitlib.util.StringUtils;
import com.bccapi.ng.api.ApiError;
import com.bccapi.ng.api.QueryTransactionSummaryResponse;
import com.bccapi.ng.api.TransactionSummary;
import com.bccapi.ng.async.AbstractCallbackHandler;
import com.bccapi.ng.async.AsyncTask;
import com.bccapi.ng.util.TransactionSummaryUtils;
import com.bccapi.ng.util.TransactionSummaryUtils.TransactionType;
import com.miracleas.bitcoin_spinner.SimpleGestureFilter.SimpleGestureListener;

public class AddAddressActivity extends ListActivity implements SimpleGestureListener,
    AbstractCallbackHandler<QueryTransactionSummaryResponse> {

  public static final String ADD_ADDRESS_RESULT = "address";
  public static final String ADD_NAME_RESULT = "NAME";
  private static final int REQUEST_CODE_SCAN = 100;

  private Activity mActivity;
  private SimpleGestureFilter detector;
  private SharedPreferences preferences;
  private ListView lvAdressList;
  private Context mContext;
  private String mAddress;
  private Button btnQRScan;
  private AsyncTask mApiTask;
  private Set<Address> mAddressSet;
  private int mChainHeight;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mContext = this;
    setContentView(R.layout.add_address);
    if (!SpinnerContext.isInitialized()) {
      SpinnerContext.initialize(this, getWindowManager().getDefaultDisplay());
    }
    preferences = getSharedPreferences(Consts.PREFS_NAME, MODE_PRIVATE);
    mActivity = this;
    detector = new SimpleGestureFilter(this, this);
    int size = preferences.getInt(Consts.TRANSACTION_HISTORY_SIZE, 15);
    mAddressSet = SpinnerContext.getInstance().getAsyncApi().getBitcoinAddressSet();
    mApiTask = SpinnerContext.getInstance().getAsyncApi().queryRecentTransactionSummary(size, this);
    btnQRScan = (Button) findViewById(R.id.btn_qr_scan);
    btnQRScan.setOnClickListener(qrScanClickListener);
    lvAdressList = (ListView) findViewById(android.R.id.list);
    lvAdressList.setOnItemClickListener(new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
        mAddress = (String) view.getTag();
        showSetNameDialog();
      }
    });

  }

  private void showSetNameDialog() {
    final Dialog dialog = new Dialog(mContext);
    dialog.setTitle(R.string.set_name_title);
    dialog.setContentView(R.layout.dialog_address_name);
    final EditText et = (EditText) dialog.findViewById(R.id.et_name);
    dialog.findViewById(R.id.btn_ok).setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        EditText et = (EditText) dialog.findViewById(R.id.et_name);
        String name = et.getText().toString();
        AddressBookManager addressBook = AddressBookManager.getInstance();
        addressBook.addEntry(mAddress, name);
        dialog.dismiss();
        finish();

      }
    });
    dialog.findViewById(R.id.btn_cancel).setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        dialog.dismiss();
      }
    });
    String name = AddressBookManager.getInstance().getNameByAddress(mAddress);
    name = name == null ? "" : name;
    et.setText(name);
    et.selectAll();
    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
    dialog.show();
  }

  @Override
  public void onResume() {
    super.onResume();

    if (!preferences.getString(Consts.LOCALE, "").matches("")) {
      Locale locale = new Locale(preferences.getString(Consts.LOCALE, "en"));
      Locale.setDefault(locale);
      Configuration config = new Configuration();
      config.locale = locale;
      getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }
    mAddressSet = SpinnerContext.getInstance().getAsyncApi().getBitcoinAddressSet();
  }

  @Override
  protected void onDestroy() {
    if (mApiTask != null) {
      mApiTask.cancel();
    }
    super.onDestroy();
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent me) {
    this.detector.onTouchEvent(me);
    return super.dispatchTouchEvent(me);
  }

  @Override
  public void onSwipe(int direction) {
    switch (direction) {
    case SimpleGestureFilter.SWIPE_LEFT:
      finish();
      break;
    }
  }

  private final OnClickListener qrScanClickListener = new OnClickListener() {

    @Override
    public void onClick(View v) {
      Utils.startScannerActivity(mActivity, REQUEST_CODE_SCAN);
    }
  };

  @Override
  public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
    if (requestCode == REQUEST_CODE_SCAN && resultCode == RESULT_OK
        && "QR_CODE".equals(intent.getStringExtra("SCAN_RESULT_FORMAT"))) {
      final String contents = intent.getStringExtra("SCAN_RESULT");
      String address;
      if (contents.matches("[a-zA-Z0-9]*")) {
        address = contents;
      } else {
        try {
          BitcoinUri b = BitcoinUri.parse(contents);
          if (b == null) {
            address = "";
          } else {
            address = b.getAddress();
          }
        } catch (Exception e) {
          address = "";
        }
      }
      NetworkParameters network = SpinnerContext.getInstance().getNetwork();
      Address parsedAddress = Address.fromString(address, network);
      if (parsedAddress != null) {
        mAddress = address;
        showSetNameDialog();
      } else {
        if (network.isTestnet()) {
          Toast.makeText(mContext, getString(R.string.invalid_address_for_testnet), Toast.LENGTH_LONG).show();
        } else if (network.isProdnet()) {
          Toast.makeText(mContext, getString(R.string.invalid_address_for_prodnet), Toast.LENGTH_LONG).show();
        }
      }
    }
  }

  @Override
  public void onDoubleTap() {

  }

  private static Date getMidnight() {
    Calendar midnight = Calendar.getInstance();
    midnight.set(midnight.get(Calendar.YEAR), midnight.get(Calendar.MONTH), midnight.get(Calendar.DAY_OF_MONTH), 0, 0,
        0);
    return midnight.getTime();
  }

  private class transactionHistoryAdapter2 extends ArrayAdapter<TransactionSummary> {

    public transactionHistoryAdapter2(Context context, int textViewResourceId, List<TransactionSummary> objects) {
      super(context, textViewResourceId, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View v = convertView;

      if (v == null) {
        LayoutInflater vi = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = vi.inflate(R.layout.transaction_history_row, null);
      }
      TextView tvDescription = (TextView) v.findViewById(R.id.tv_description);
      TextView tvDate = (TextView) v.findViewById(R.id.tv_date);
      TextView tvAddress = (TextView) v.findViewById(R.id.tv_address);
      TextView tvCredits = (TextView) v.findViewById(R.id.tv_credits);
      TransactionSummary r = getItem(position);
      if (r == null) {
        tvDescription.setText(R.string.none);
      }
      String description;
      String[] addresses = new String[0];
      int confirmations = r.calculateConfirmatons(mChainHeight);
      TransactionSummaryUtils.TransactionType type = TransactionSummaryUtils.getTransactionType(r, mAddressSet);
      if (type == TransactionType.SentToOthers) {
        if (confirmations == 0) {
          description = getContext().getString(R.string.unconfirmed_to);
        } else {
          description = getContext().getString(R.string.sent_to);
        }
        addresses = TransactionSummaryUtils.getReceiversNotMe(r, mAddressSet);
      } else if (type == TransactionType.ReceivedFromOthers) {
        if (confirmations == 0) {
          description = getContext().getString(R.string.unconfirmed_from);
        } else {
          description = getContext().getString(R.string.received_from);
        }
        addresses = TransactionSummaryUtils.getSenders(r);
      } else {
        description = getContext().getString(R.string.sent_to_yourself);
      }

      long value = TransactionSummaryUtils.calculateBalanceChange(r, mAddressSet);
      String valueString = CoinUtil.valueString(value) + " BTC";

      Date midnight = getMidnight();
      DateFormat hourFormat = DateFormat.getDateInstance(DateFormat.SHORT);
      DateFormat dayFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
      Date date = new Date(r.time * 1000L);
      DateFormat dateFormat = date.before(midnight) ? hourFormat : dayFormat;

      tvDescription.setText(description);
      tvDate.setText(dateFormat.format(date));

      // It may happen that this record was sent to two addresses, we ignore
      // anything but the first address as this only happens if the key was
      // exported to a more advanced wallet.
      if (addresses.length > 0) {
        v.setTag(addresses[0]);
      }

      // Replace known addresses from the address book
      for (int i = 0; i < addresses.length; i++) {
        String name = AddressBookManager.getInstance().getNameByAddress(addresses[i]);
        if (name != null && name.length() != 0) {
          // Was sent to one in our address book
          addresses[i] = name;
        }
      }

      String addressText = StringUtils.join(addresses, ", ");

      tvAddress.setText(addressText);

      tvCredits.setText(valueString);
      return v;
    }
  }

  @Override
  public void handleCallback(QueryTransactionSummaryResponse response, ApiError error) {
    if (response == null) {
      Utils.showConnectionAlert(this);
      return;
    }
    mChainHeight = response.chainHeight;

    if (response.transactions.isEmpty()) {
      Toast.makeText(this, R.string.no_transactions, Toast.LENGTH_SHORT).show();
    } else {
      List<TransactionSummary> sending = new LinkedList<TransactionSummary>();
      for (TransactionSummary item : response.transactions) {
        if (TransactionSummaryUtils.getTransactionType(item, mAddressSet) == TransactionType.SentToOthers) {
          sending.add(item);
        }
      }
      setListAdapter(new transactionHistoryAdapter2(this, R.layout.transaction_history_row, sending));
    }
  }

}
