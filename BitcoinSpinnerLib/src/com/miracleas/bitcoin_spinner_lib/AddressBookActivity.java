package com.miracleas.bitcoin_spinner_lib;

import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.miracleas.bitcoin_spinner_lib.AddressBookManager.Entry;
import com.miracleas.bitcoin_spinner_lib.SimpleGestureFilter.SimpleGestureListener;

public class AddressBookActivity extends ListActivity implements SimpleGestureListener {

	private Activity mActivity;
	private SharedPreferences preferences;

	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.address_book);
		preferences = getSharedPreferences(Consts.PREFS_NAME, MODE_PRIVATE);
		mActivity = this;
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
			tvAddress.setText(e.getAddress());
			return v;
		}
	}

	@Override
	public void onSwipe(int direction) {
		// TODO Auto-generated method stub

	}

}
