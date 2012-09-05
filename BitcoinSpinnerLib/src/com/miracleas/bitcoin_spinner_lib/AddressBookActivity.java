package com.miracleas.bitcoin_spinner_lib;

import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.miracleas.bitcoin_spinner_lib.AddressBookManager.Entry;
import com.miracleas.bitcoin_spinner_lib.SimpleGestureFilter.SimpleGestureListener;

public class AddressBookActivity extends ListActivity implements SimpleGestureListener {

	private Activity mActivity;
	private SharedPreferences preferences;
	private String mSelectedAddress;
	private Context mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.address_book);
		preferences = getSharedPreferences(Consts.PREFS_NAME, MODE_PRIVATE);
		mActivity = this;
		registerForContextMenu(getListView());
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
		if (entries.isEmpty()) {
			// Show a toast and open options menu
			Toast.makeText(this, R.string.address_book_empty, Toast.LENGTH_SHORT).show();
		}
		// Always show the Add Address option when resuming the activity
		new Handler().postDelayed(new Runnable() {
			public void run() {
				openOptionsMenu();
			}
		}, 100);
		setListAdapter(new AddressBookAdapter(this, R.layout.address_book_row, entries));

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		mSelectedAddress = (String) v.getTag();
		l.showContextMenuForChild(v);
	};

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.addressbook_context_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		if (item.getItemId() == R.id.delete_address) {
			doDeleteEntry();
			return true;
		} else if (item.getItemId() == R.id.edit_address) {
			doEditEntry();
			return true;
		} else if (item.getItemId() == R.id.send_bitcoins) {
			doSendBitcoins();
			return true;
		} else if (item.getItemId() == R.id.show_qr_code) {
			doShowQrCode();
			return true;
		} else {
			return false;
		}
	}

	private void doEditEntry() {
		final Dialog dialog = new Dialog(mContext);
		dialog.setTitle(R.string.set_name_title);
		dialog.setContentView(R.layout.dialog_address_name);
		final EditText et = (EditText) dialog.findViewById(R.id.et_name);
		dialog.findViewById(R.id.btn_ok).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				EditText et = (EditText) dialog.findViewById(R.id.et_name);
				String name = et.getText().toString();
				dialog.dismiss();
				AddressBookManager addressBook = AddressBookManager.getInstance();
				addressBook.addEntry(mSelectedAddress, name);
				List<Entry> entries = addressBook.getEntries();
				setListAdapter(new AddressBookAdapter(mContext, R.layout.address_book_row, entries));
			}
		});
		dialog.findViewById(R.id.btn_cancel).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		String name = AddressBookManager.getInstance().getNameByAddress(mSelectedAddress);
		name = name == null ? "" : name;
		et.setText(name);
		et.selectAll();
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
		dialog.show();
	}

	private void doSendBitcoins() {
		Intent i = new Intent(this, SendBitcoinsActivity.class);
		i.putExtra(Consts.BTC_ADDRESS_KEY, mSelectedAddress);
		startActivity(i);
	}

	private void doShowQrCode() {
		Bitmap qrCode = Utils.getLargeQRCodeBitmap("bitcoin:" + mSelectedAddress);
		Utils.showQrCode(mContext, R.string.bitcoin_address, qrCode, mSelectedAddress);
	}

	private void doDeleteEntry() {
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setMessage(R.string.delete_address_confirmation).setCancelable(false)
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
						AddressBookManager addressBook = AddressBookManager.getInstance();
						addressBook.deleteEntry(mSelectedAddress);
						List<Entry> entries = addressBook.getEntries();
						setListAdapter(new AddressBookAdapter(mContext, R.layout.address_book_row, entries));
					}
				}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
					}
				});
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}

	@Override
	public void onDoubleTap() {

	}

	/** Called when menu button is pressed. */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.addressbook_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		if (item.getItemId() == R.id.add_address) {
			startActivity(new Intent(this, AddAddressActivity.class));
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
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
