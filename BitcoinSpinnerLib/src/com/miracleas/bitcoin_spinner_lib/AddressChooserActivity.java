package com.miracleas.bitcoin_spinner_lib;

import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.miracleas.bitcoin_spinner_lib.AddressBookManager.Entry;
import com.miracleas.bitcoin_spinner_lib.SimpleGestureFilter.SimpleGestureListener;

public class AddressChooserActivity extends ListActivity implements SimpleGestureListener {

	public static final String ADDRESS_RESULT_NAME = "address result";
	private Activity mActivity;
	private SharedPreferences preferences;
	private ListView lvAdressList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.address_chooser);
		preferences = getSharedPreferences(Consts.PREFS_NAME, MODE_PRIVATE);
		mActivity = this;
		lvAdressList = (ListView) findViewById(android.R.id.list);
		lvAdressList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
				String value = (String)view.getTag();
				Intent result = new Intent();
				result.putExtra(ADDRESS_RESULT_NAME, value);
				setResult(RESULT_OK, result);
				finish();
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		// Check whether we need to do account initialization
		if (!SpinnerContext.isInitialized()) {
			SpinnerContext.initialize(this, getWindowManager().getDefaultDisplay());
		}

		if (!preferences.getString(Consts.LOCALE, "").matches("")) {
			Locale locale = new Locale(preferences.getString(Consts.LOCALE, "en"));
			Locale.setDefault(locale);
			Configuration config = new Configuration();
			config.locale = locale;
			getBaseContext().getResources().updateConfiguration(config,
					getBaseContext().getResources().getDisplayMetrics());
		}
		AddressBookManager addressBook = AddressBookManager.getInstance();
		List<Entry> entries = addressBook.getEntries();
		setListAdapter(new AddressBookAdapter(this, R.layout.address_book_row, entries));
	}

	@Override
	public void onDoubleTap() {

	}

	private class AddressBookAdapter extends ArrayAdapter<Entry> {

		public AddressBookAdapter(Context context, int textViewResourceId, List<Entry> objects) {
			super(context, textViewResourceId, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;

			if (v == null) {
				LayoutInflater vi = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.address_book_row, null);
			}
			TextView tvName = (TextView) v.findViewById(R.id.address_book_name);
			TextView tvAddress = (TextView) v.findViewById(R.id.address_book_address);
			Entry e = getItem(position);
			tvName.setText(e.getName());
			tvAddress.setText(formatAddress(e.getAddress()));
			v.setTag(e.getAddress());
			return v;
		}

	}

	private static final int ADDRESS_SPLIT_LENGTH = 12;

	private static String formatAddress(String address) {
		StringBuilder sb = new StringBuilder();
		int remaining = address.length();
		int pos = 0;
		while (remaining > ADDRESS_SPLIT_LENGTH) {
			sb.append(address.substring(pos, pos + ADDRESS_SPLIT_LENGTH));
			sb.append('\n');
			pos += ADDRESS_SPLIT_LENGTH;
			remaining -= ADDRESS_SPLIT_LENGTH;
		}
		if (remaining > 0) {
			sb.append(address.substring(pos));
		}
		return sb.toString();
	}

	@Override
	public void onSwipe(int direction) {
		// TODO Auto-generated method stub

	}

}
