package com.miracleas.bitcoin_spinner_lib;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

import com.bccapi.api.AccountStatement;
import com.bccapi.api.AccountStatement.Record;
import com.bccapi.api.AccountStatement.Record.Type;
import com.bccapi.core.CoinUtils;
import com.bccapi.core.Asynchronous.AccountTask;
import com.bccapi.core.Asynchronous.RecentStatementsCallbackHandler;
import com.miracleas.bitcoin_spinner_lib.SimpleGestureFilter.SimpleGestureListener;

public class TransactionHistoryActivity extends ListActivity implements SimpleGestureListener, RecentStatementsCallbackHandler {

	private Activity mActivity;
	private SimpleGestureFilter detector;
	private SharedPreferences preferences;
	private AccountTask mTask;
	
	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.transaction_history);
		preferences = getSharedPreferences(Consts.PREFS_NAME, MODE_PRIVATE);
		mActivity = this;
		detector = new SimpleGestureFilter(this, this);
		int size = preferences.getInt(Consts.TRANSACTION_HISTORY_SIZE, 15);
		mTask = Consts.account.requestRecentStatements(size, this);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if(!preferences.getString(Consts.LOCALE, "").matches("")) {
			Locale locale = new Locale(preferences.getString(Consts.LOCALE, "en"));
			Locale.setDefault(locale);
			Configuration config = new Configuration();
			config.locale = locale;
			getBaseContext().getResources().updateConfiguration(config,
			      getBaseContext().getResources().getDisplayMetrics());
		}
	}
	
	@Override
	protected void onDestroy() {
		if(mTask != null){
			mTask.cancel();
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

	private class transactionHistoryAdapter extends ArrayAdapter<Record> {

		public transactionHistoryAdapter(Context context, int textViewResourceId,
				List<Record> objects) {
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
			Record r = getItem(position);
			if(r == null) {
				tvDescription.setText("No Records");
			}
            String description;
			if (r.getType() == Type.Sent) {
				if (r.getConfirmations() == 0) {
					description = getContext().getString(R.string.unconfirmed_to);
				} else {
					description = getContext().getString(R.string.sent_to);
				}
			} else if (r.getType() == Type.Received) {
				if (r.getConfirmations() == 0) {
					description = getContext().getString(R.string.unconfirmed_from);
				} else {
					description = getContext().getString(R.string.received_from);
				}
			} else {
				description = getContext().getString(R.string.sent_to_yourself);
			}

            String valueString = CoinUtils.valueString(r.getAmount());
            
			Date midnight = getMidnight();
			DateFormat hourFormat = DateFormat
					.getDateInstance(DateFormat.SHORT);
			DateFormat dayFormat = DateFormat
					.getTimeInstance(DateFormat.SHORT);
            Date date = new Date(r.getDate());
            DateFormat dateFormat = date.before(midnight) ? hourFormat : dayFormat;
			
			tvDescription.setText(description);
			tvDate.setText(dateFormat.format(date));
			tvAddress.setText(r.getAddresses());
			tvCredits.setText(valueString);
			if(r.getConfirmations() == 0) {
				tvDescription.setTextColor(Color.GRAY);
				tvDate.setTextColor(Color.GRAY);
				tvAddress.setTextColor(Color.GRAY);
				tvCredits.setTextColor(Color.GRAY);
			} else if(r.getConfirmations() < 5) {
				tvDescription.setTextColor(Color.GRAY);
				tvDate.setTextColor(Color.GRAY);
				tvAddress.setTextColor(Color.GRAY);
				tvCredits.setTextColor(Color.GRAY);
			}
			return v;
		}
}

	private static Date getMidnight() {
		Calendar midnight = Calendar.getInstance();
		midnight.set(midnight.get(Calendar.YEAR), midnight.get(Calendar.MONTH),
				midnight.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
		return midnight.getTime();
	}

	@Override
	public void handleRecentStatementsCallback(AccountStatement statements, String errorMessage) {
		if(statements == null ){
			Utils.showConnectionAlert(this);
			return;
		}
		if (statements.getRecords().isEmpty()) {
			Toast.makeText(this, R.string.no_transactions,
					Toast.LENGTH_SHORT).show();
		} else {
			List<Record> records = statements.getRecords();
			Collections.reverse(records);
			setListAdapter(new transactionHistoryAdapter(this, R.layout.transaction_history_row, records));
		}
	}

}
