package com.miracleas.bitcoin_spinner_lib;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
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

import com.bccapi.api.AccountStatement;
import com.bccapi.api.AccountStatement.Record;
import com.bccapi.api.AccountStatement.Record.Type;
import com.bccapi.api.Network;
import com.bccapi.core.AddressUtil;
import com.bccapi.core.CoinUtils;
import com.bccapi.core.Asynchronous.AccountTask;
import com.bccapi.core.Asynchronous.RecentStatementsCallbackHandler;
import com.miracleas.bitcoin_spinner_lib.SimpleGestureFilter.SimpleGestureListener;

public class AddAddressActivity extends ListActivity implements SimpleGestureListener, RecentStatementsCallbackHandler {

	public static final String ADD_ADDRESS_RESULT = "address";
	public static final String ADD_NAME_RESULT = "NAME";
	private static final int SET_NAME_DIALOG = 1001;
	private static final int REQUEST_CODE_SCAN = 100;

	private Activity mActivity;
	private SimpleGestureFilter detector;
	private SharedPreferences preferences;
	private AccountTask mTask;
	private ListView lvAdressList;
	private Context mContext;
	private String mAddress;
	private Button btnQRScan;

	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
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
		mTask = SpinnerContext.getInstance().getAccount().requestRecentStatements(size, this);

		btnQRScan = (Button) findViewById(R.id.btn_qr_scan);
		btnQRScan.setOnClickListener(qrScanClickListener);
		lvAdressList = (ListView) findViewById(android.R.id.list);
		lvAdressList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
				mAddress = (String) view.getTag();
				showDialog(SET_NAME_DIALOG);
			}
		});

	}

	@Override
	public void onResume() {
		super.onResume();

		if (!preferences.getString(Consts.LOCALE, "").matches("")) {
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
		if (mTask != null) {
			mTask.cancel();
		}
		super.onDestroy();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		final Dialog dialog;
		switch (id) {
		case SET_NAME_DIALOG:
			dialog = new Dialog(mContext);
			dialog.setTitle(R.string.set_name_title);
			dialog.setContentView(R.layout.dialog_address_name);
			final EditText et = (EditText) dialog.findViewById(R.id.et_name);
			dialog.setOnShowListener(new OnShowListener() {

				@Override
				public void onShow(DialogInterface dialog) {
					String name = AddressBookManager.getInstance().getNameByAddress(mAddress);
					name = name == null ? "" : name;
					et.setText(name);
					et.selectAll();
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
				}
			});
			dialog.findViewById(R.id.btn_ok).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					EditText et = (EditText) dialog.findViewById(R.id.et_name);
					String name = et.getText().toString();
					dialog.dismiss();
					AddressBookManager addressBook = AddressBookManager.getInstance();
					addressBook.addEntry(mAddress, name);
					finish();

				}
			});
			dialog.findViewById(R.id.btn_cancel).setOnClickListener(new OnClickListener() {

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
					Uri uri = Uri.parse(contents);
					final Uri u = Uri.parse("bitcoin://" + uri.getSchemeSpecificPart());
					address = u.getHost();
				} catch (Exception e) {
					address = "";
				}
			}
			Network network = SpinnerContext.getInstance().getNetwork();
			if (AddressUtil.validateAddress(address, network)) {
				mAddress = address;
				showDialog(SET_NAME_DIALOG);
			} else {
				if (network.equals(Network.testNetwork)) {
					Toast.makeText(mContext, getString(R.string.invalid_address_for_testnet), Toast.LENGTH_LONG).show();
				} else if (network.equals(Network.productionNetwork)) {
					Toast.makeText(mContext, getString(R.string.invalid_address_for_prodnet), Toast.LENGTH_LONG).show();
				}
			}
		}
	}

	@Override
	public void onDoubleTap() {

	}

	private class transactionHistoryAdapter extends ArrayAdapter<Record> {

		public transactionHistoryAdapter(Context context, int textViewResourceId, List<Record> objects) {
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
			if (r == null) {
				tvDescription.setText(R.string.none);
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
					description = getContext().getString(R.string.unconfirmed_received_with);
				} else {
					description = getContext().getString(R.string.received_with);
				}
			} else {
				description = getContext().getString(R.string.sent_to_yourself);
			}

			String valueString = CoinUtils.valueString(r.getAmount());

			Date midnight = getMidnight();
			DateFormat hourFormat = DateFormat.getDateInstance(DateFormat.SHORT);
			DateFormat dayFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
			Date date = new Date(r.getDate());
			DateFormat dateFormat = date.before(midnight) ? hourFormat : dayFormat;

			tvDescription.setText(description);
			tvDate.setText(dateFormat.format(date));
			tvAddress.setText(r.getAddresses());
			tvCredits.setText(valueString);
			// There will only ever be one address, as we only
			String address = getfirstAddress(r.getAddresses());
			v.setTag(address);
			return v;
		}
	}

	private String getfirstAddress(String addresses) {
		int commaIndex = addresses.indexOf(',');
		if (commaIndex == -1) {
			return addresses;
		} else {
			return addresses.substring(0, commaIndex - 1);
		}
	}

	private static Date getMidnight() {
		Calendar midnight = Calendar.getInstance();
		midnight.set(midnight.get(Calendar.YEAR), midnight.get(Calendar.MONTH), midnight.get(Calendar.DAY_OF_MONTH), 0,
				0, 0);
		return midnight.getTime();
	}

	@Override
	public void handleRecentStatementsCallback(AccountStatement statements, String errorMessage) {
		if (statements == null) {
			Utils.showConnectionAlert(this);
			return;
		}
		if (statements.getRecords().isEmpty()) {
			Toast.makeText(this, R.string.no_transactions, Toast.LENGTH_SHORT).show();
		} else {
			List<Record> records = statements.getRecords();
			List<Record> sending = new ArrayList<Record>(records.size());
			for (Record r : records) {
				if (r.getType() == Type.Sent) {
					sending.add(r);
				}
			}
			Collections.reverse(sending);
			setListAdapter(new transactionHistoryAdapter(this, R.layout.transaction_history_row, sending));
		}
	}

}
