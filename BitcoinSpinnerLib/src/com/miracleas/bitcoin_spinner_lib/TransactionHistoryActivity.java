package com.miracleas.bitcoin_spinner_lib;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bccapi.api.APIException;
import com.bccapi.api.AccountStatement;
import com.bccapi.api.AccountStatement.Record;
import com.bccapi.api.AccountStatement.Record.Type;
import com.bccapi.core.Account;
import com.bccapi.core.CoinUtils;
import com.miracleas.bitcoin_spinner_lib.SimpleGestureFilter.SimpleGestureListener;

public class TransactionHistoryActivity extends ListActivity implements SimpleGestureListener {

	private Account account;
	private Activity mActivity;
	private List<Record> records;

	private SimpleGestureFilter detector;

	private SharedPreferences preferences;

	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.transaction_history);
		
		preferences = getSharedPreferences(Consts.PREFS_NAME, MODE_PRIVATE);

		mActivity = this;
		account = Consts.account;
		
		detector = new SimpleGestureFilter(this, this);

		try {
			AccountStatement statement = account.getStatement(0, 0);
			int totalRecords = statement.getTotalRecordCount();
			if (totalRecords != 0) {
				statement = account.getStatement(
						Math.max(0, totalRecords - 15), 15);
			}
			if (!statement.getRecords().isEmpty()) {
				records = statement.getRecords();
				setListAdapter(new transactionHistoryAdapter(this, R.layout.transaction_history_row, records));
			}
		} catch (APIException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
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

			Record r = (Record) records.get(position);

            String description;
            if (r.getType() == Type.Sent) {
               description = getString(R.string.sent_to);
            } else if (r.getType() == Type.Received) {
               description = getString(R.string.received_from);
            } else {
               description = getString(R.string.sent_to_yourself);
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
			return v;
		}
}

	private static Date getMidnight() {
		Calendar midnight = Calendar.getInstance();
		midnight.set(midnight.get(Calendar.YEAR), midnight.get(Calendar.MONTH),
				midnight.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
		return midnight.getTime();
	}

}
