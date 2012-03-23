package com.miracleas.bitcoin_spinner_lib;

import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import com.bccapi.api.Network;
import com.bccapi.core.Base58;
import com.bccapi.core.DeterministicECKeyExporter;
import com.bccapi.core.Asynchronous.AsynchronousAccount;

public class SettingsActivity extends PreferenceActivity {

	private SharedPreferences preferences;
	private SharedPreferences.Editor editor;

	private static final int REQUEST_CODE_SCAN = 0;

	private Context mContext;

	private Preference backupWalletPref, restoreWalletPref,
			ExportPrivateKeyPref;
	private EditTextPreference transactionHistorySizePref;
	private ListPreference useLocalePref;
	private ListPreference usedCurrencyPref;

	private ProgressDialog restoreDialog;
	
	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!SpinnerContext.isInitialized()) {
			SpinnerContext.initialize(this, getWindowManager().getDefaultDisplay());
		}

		preferences = getSharedPreferences(Consts.PREFS_NAME,
				Activity.MODE_PRIVATE);
		editor = preferences.edit();

		mContext = this;

		addPreferencesFromResource(R.xml.preferences);

		useLocalePref = (ListPreference) findPreference("useLocale");
		useLocalePref.setTitle(R.string.prefs_choose_default_locale);
		useLocalePref.setOnPreferenceChangeListener(useLocalChangeListener);

		usedCurrencyPref = (ListPreference) findPreference("usedCurrency");
		String localCurrency = preferences.getString(Consts.LOCAL_CURRENCY, Consts.DEFAULT_CURRENCY);
		usedCurrencyPref.setValue(localCurrency);
		usedCurrencyPref.setTitle(R.string.prefs_currency);
		usedCurrencyPref.setOnPreferenceChangeListener(usedCurrency);

		transactionHistorySizePref = (EditTextPreference) findPreference("transactionHistorySize");
		int transactionSize = preferences.getInt(Consts.TRANSACTION_HISTORY_SIZE,
				Consts.DEFAULT_TRANSACTION_HISTORY_SIZE);
		transactionHistorySizePref.setText(String.valueOf(transactionSize));
		transactionHistorySizePref
				.setOnPreferenceChangeListener(TransactionHistorySizeChangeListener);

		backupWalletPref = (Preference) findPreference("backupSeed");
		backupWalletPref
				.setOnPreferenceClickListener(backupWalletClickListener);

		restoreWalletPref = (Preference) findPreference("restoreSeed");
		restoreWalletPref
				.setOnPreferenceClickListener(restoreWalletClickListener);

		ExportPrivateKeyPref = (Preference) findPreference("exportPrivateKey");
		ExportPrivateKeyPref
				.setOnPreferenceClickListener(exportPrivateKeyClickListener);

	}

	@Override
	public void onResume() {
		super.onResume();

		if (!preferences.getString(Consts.LOCALE, "").matches("")) {
			Locale locale = new Locale(preferences.getString(Consts.LOCALE,
					"en"));
			Locale.setDefault(locale);
			Configuration config = new Configuration();
			config.locale = locale;
			getBaseContext().getResources().updateConfiguration(config,
					getBaseContext().getResources().getDisplayMetrics());
		}
	}

	private final OnPreferenceChangeListener useLocalChangeListener = new OnPreferenceChangeListener() {

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			editor.putString(Consts.LOCALE, (String) newValue);
			editor.commit();
			return true;
		}
	};

	private final OnPreferenceChangeListener usedCurrency = new OnPreferenceChangeListener() {

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			editor.putString(Consts.LOCAL_CURRENCY, (String) newValue);
			editor.commit();
			return true;
		}
	};
	
	private final OnPreferenceChangeListener TransactionHistorySizeChangeListener = new OnPreferenceChangeListener() {

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			try {
				int newSize = Integer.parseInt((String) newValue);
				if(newSize < 1){
					newSize = Consts.DEFAULT_TRANSACTION_HISTORY_SIZE;
				}
				editor.putInt(Consts.TRANSACTION_HISTORY_SIZE, newSize);
				editor.commit();
			} catch (NumberFormatException e) {
				Toast.makeText(mContext, R.string.invalid_value, Toast.LENGTH_SHORT).show();
			}
			return true;
		}
	};

	private final OnPreferenceClickListener backupWalletClickListener = new OnPreferenceClickListener() {

		public boolean onPreferenceClick(Preference preference) {
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setMessage(R.string.backup_dialog_text)
					.setCancelable(false)
					.setPositiveButton(R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();

									Network net = SpinnerContext.getInstance().getNetwork();
									final BackupInfo info = new BackupInfo(Utils.readSeed(SpinnerContext.getInstance().getApplicationContext(),net),net);
									info.getBackupUrl();
									Bitmap qrCode = Utils.getLargeQRCodeBitmap(info.getBackupUrl());
									Utils.showQrCode(mContext, R.string.bitcoinspinner_backup, qrCode);
								}
							})
					.setNegativeButton(R.string.no,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									// do nothing
								}
							});
			AlertDialog alertDialog = builder.create();
			alertDialog.show();
			return true;
		}
	};

	private final OnPreferenceClickListener restoreWalletClickListener = new OnPreferenceClickListener() {

		public boolean onPreferenceClick(Preference preference) {

			AsynchronousAccount accountManager = SpinnerContext.getInstance().getAccount();
			if (accountManager.getCachedBalance() > 0 || accountManager.getCachedCoinsOnTheWay() > 0) {
				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				builder.setMessage(R.string.restore_dialog_coins).setCancelable(false)
						.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
								ShowRestoreWalletAlert();
							}
						}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
							}
						});
				AlertDialog alertDialog = builder.create();
				alertDialog.show();
			} else {
				ShowRestoreWalletAlert();
			}
			return true;
		}
	};
	
	private void ShowRestoreWalletAlert(){
		final Activity myself = this;
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setMessage(R.string.restore_dialog_no_coins).setCancelable(false)
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Utils.startScannerActivity(myself, REQUEST_CODE_SCAN);
					}
				}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}

	private final OnPreferenceClickListener exportPrivateKeyClickListener = new OnPreferenceClickListener() {

		public boolean onPreferenceClick(Preference preference) {

			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setMessage(R.string.export_private_key_dialog_text).setCancelable(false)
					.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();

							Network net = SpinnerContext.getInstance().getNetwork();
							byte[] seed = Utils.readSeed(SpinnerContext.getInstance().getApplicationContext(), net);
							DeterministicECKeyExporter exporter = new DeterministicECKeyExporter(seed);
							final String keyString;
							keyString = exporter.getPrivateKeyExporter(1).getBase58EncodedKey(
									SpinnerContext.getInstance().getNetwork());
							Bitmap qrCode = Utils.getLargeQRCodeBitmap(keyString);
							Utils.showQrCode(mContext, R.string.private_key, qrCode);
						}
					}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							// put your code here
						}
					});
			AlertDialog alertDialog = builder.create();
			alertDialog.show();
			return true;
		}
	};

	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
		if (requestCode != REQUEST_CODE_SCAN || resultCode != RESULT_OK
				|| !("QR_CODE".equals(intent.getStringExtra("SCAN_RESULT_FORMAT")))) {
			return;
		}
		String contents = intent.getStringExtra("SCAN_RESULT");
		BackupInfo info = BackupInfo.fromString(contents);
		if (info == null || !info.getNetwork().equals(SpinnerContext.getInstance().getNetwork())) {
			Utils.showAlert(this, R.string.restore_invalid_qr_code);
			return;
		}
		restoreDialog = ProgressDialog.show(this, getString(R.string.restore_dialog_title),
				getString(R.string.please_wait), true);
		new AsyncInit().execute(info);
	}

	private class AsyncInit extends AsyncTask<BackupInfo, Integer, Long> {

		@Override
		protected Long doInBackground(BackupInfo... params) {
			BackupInfo info = params[0];
			SpinnerContext.getInstance().recoverWallet(info.getSeed());
			return null;
		}

		protected void onPostExecute(Long result) {
			restoreDialog.dismiss();
			Utils.showAlert(mContext, R.string.restore_complete_dialog_text);
		}
	}

	public static class BackupInfo {
		private byte[] _seed;
		private Network _network;

		public static BackupInfo fromString(String string) {
			try {
				Uri temp = Uri.parse(string);
				final Uri uri = Uri.parse("bsb://" + temp.getSchemeSpecificPart());
				int netId = Integer.parseInt(uri.getQueryParameter("net"));
				Network network;
				if (netId == 0) {
					network = Network.productionNetwork;
				} else if (netId == 1) {
					network = Network.testNetwork;
				} else {
					return null;
				}
				String host = uri.getHost();
				byte[] seed = Base58.decode(host);
				if (seed != null) {
					return new BackupInfo(seed, network);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		public BackupInfo(byte[] seed, Network network) {
			_seed = seed;
			_network = network;
		}

		public byte[] getSeed() {
			return _seed;
		}

		public Network getNetwork() {
			return _network;
		}

		String getBackupUrl() {
			StringBuilder sb = new StringBuilder();
			sb.append("bsb:");
			sb.append(Base58.encode(_seed));
			sb.append("?net=");
			sb.append(_network.equals(Network.productionNetwork) ? 0 : 1);
			return sb.toString();
		}
	}
	
}
