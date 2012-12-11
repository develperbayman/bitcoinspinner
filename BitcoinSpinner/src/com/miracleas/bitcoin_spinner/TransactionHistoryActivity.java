package com.miracleas.bitcoin_spinner;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.bccapi.bitlib.model.Address;
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

public class TransactionHistoryActivity extends ListActivity implements SimpleGestureListener,
    AbstractCallbackHandler<QueryTransactionSummaryResponse> {

  private static final int RECEIVED_COLOR = Color.rgb(128, 255, 128);
  private static final int RECEIVED_COLOR_DIM = Color.rgb(128 / 2, 255 / 2, 128 / 2);
  private static final int SENT_COLOR = Color.rgb(255, 128, 128);
  private static final int SENT_COLOR_DIM = Color.rgb(255 / 2, 128 / 2, 128 / 2);

  private Activity mActivity;
  private SimpleGestureFilter detector;
  private SharedPreferences preferences;
  private AsyncTask mApiTask;
  private Set<Address> mAddressSet;
  private int mChainHeight;

  /**
   * @see android.app.Activity#onCreate(Bundle)
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.transaction_history);
    if (!SpinnerContext.isInitialized()) {
      SpinnerContext.initialize(this, getWindowManager().getDefaultDisplay());
    }
    preferences = getSharedPreferences(Consts.PREFS_NAME, MODE_PRIVATE);
    mActivity = this;
    detector = new SimpleGestureFilter(this, this);
    int size = preferences.getInt(Consts.TRANSACTION_HISTORY_SIZE, Consts.DEFAULT_TRANSACTION_HISTORY_SIZE);
    mAddressSet = SpinnerContext.getInstance().getAsyncApi().getBitcoinAddressSet();
    mApiTask = SpinnerContext.getInstance().getAsyncApi().queryRecentTransactionSummary(size, this);
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

  @Override
  public void onDoubleTap() {

  }

  private static Date getMidnight() {
    Calendar midnight = Calendar.getInstance();
    midnight.set(midnight.get(Calendar.YEAR), midnight.get(Calendar.MONTH), midnight.get(Calendar.DAY_OF_MONTH), 0, 0,
        0);
    return midnight.getTime();
  }

  private class transactionHistoryAdapter extends ArrayAdapter<TransactionSummary> {

    public transactionHistoryAdapter(Context context, int textViewResourceId, List<TransactionSummary> objects) {
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
      int color;
      if (confirmations < 5) {
        if (value < 0) {
          color = SENT_COLOR_DIM;
        } else {
          color = RECEIVED_COLOR_DIM;
        }
      } else {
        if (value < 0) {
          color = SENT_COLOR;
        } else {
          color = RECEIVED_COLOR;
        }
      }

      tvDescription.setTextColor(color);
      tvDate.setTextColor(color);
      tvAddress.setTextColor(color);
      tvCredits.setTextColor(color);

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
      setListAdapter(new transactionHistoryAdapter(this, R.layout.transaction_history_row, response.transactions));
    }
  }

}
