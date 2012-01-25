package com.miracleas.bitcoin_spinner_lib;

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
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.text.ClipboardManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bccapi.api.Network;
import com.bccapi.core.Base58;
import com.bccapi.core.DeterministicECKeyExporter;
import com.bccapi.core.Asynchronous.AsynchronousAccount;

public class SettingsActivity extends PreferenceActivity {

	private SharedPreferences preferences;
	private SharedPreferences.Editor editor;

	private AlertDialog backupWalletDialog, exportKeyDialog;

	private static final int REQUEST_CODE_SCAN = 0;

	private Context mContext;

	private Preference backupWalletPref, restoreWalletPref,
			ExportPrivateKeyPref;
	private EditTextPreference transactionHistorySizePref;
	private ListPreference useLocalePref;

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

		transactionHistorySizePref = (EditTextPreference) findPreference("transactionHistorySize");
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

	private final OnPreferenceChangeListener TransactionHistorySizeChangeListener = new OnPreferenceChangeListener() {

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			String value = ((String) newValue).replaceAll("\\D", "");
			if (value != "") {
				editor.putInt(Consts.TRANSACTION_HISTORY_SIZE,
						Integer.parseInt(value));
				editor.commit();
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

									LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
									View layout = inflater.inflate(
											R.layout.dialog_qr_address, null);
									AlertDialog.Builder builder = new AlertDialog.Builder(
											mContext).setView(layout);
									backupWalletDialog = builder.create();
									backupWalletDialog
											.setCanceledOnTouchOutside(true);
									TextView text =(TextView ) layout.findViewById(R.id.tv_title_text); 
									text.setText(R.string.bitcoinspinner_backup);
									ImageView qrAdress = (ImageView) layout
											.findViewById(R.id.iv_qr_Address);
									Network net = SpinnerContext.getInstance().getNetwork();
									final BackupInfo info = new BackupInfo(Utils.readSeed(SpinnerContext.getInstance().getApplicationContext(),net),net);
									info.getBackupUrl();
									qrAdress.setImageBitmap(Utils.getLargeQRCodeBitmap(
											info.getBackupUrl()));
									qrAdress.setOnClickListener(new OnClickListener() {

										@Override
										public void onClick(View v) {
											backupWalletDialog.dismiss();
										}
									});

									Button copy = (Button) layout
											.findViewById(R.id.btn_copy_to_clip);
									copy.setOnClickListener(new OnClickListener() {

										@Override
										public void onClick(View v) {
											ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
											clipboard.setText(info.getBackupUrl());
											Toast.makeText(mContext,
													R.string.clipboard_copy,
													Toast.LENGTH_SHORT).show();
										}
									});

									backupWalletDialog.show();
								}
							})
					.setNegativeButton(R.string.no,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									// put your code here
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
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setMessage(R.string.restore_dialog_no_coins).setCancelable(false)
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {

						final PackageManager pm = getPackageManager();
						if (pm.resolveActivity(Consts.zxingIntent, 0) != null) {
							startActivityForResult(Consts.zxingIntent, REQUEST_CODE_SCAN);
						} else if (pm.resolveActivity(Consts.gogglesIntent, 0) != null) {
							startActivity(Consts.gogglesIntent);
						} else {
							showMarketPage(Consts.PACKAGE_NAME_ZXING);
							Toast.makeText(mContext, R.string.install_qr_scanner, Toast.LENGTH_LONG).show();
						}
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
			builder.setMessage(R.string.export_private_key_dialog_text)
					.setCancelable(false)
					.setPositiveButton(R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();

									LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
									View layout = inflater.inflate(
											R.layout.dialog_qr_address, null);
									AlertDialog.Builder builder = new AlertDialog.Builder(
											mContext).setView(layout);
									exportKeyDialog = builder.create();
									exportKeyDialog
											.setCanceledOnTouchOutside(true);
									TextView text =(TextView ) layout.findViewById(R.id.tv_title_text); 
									text.setText(R.string.private_key);
									ImageView qrAdress = (ImageView) layout
											.findViewById(R.id.iv_qr_Address);
									
									Network net = SpinnerContext.getInstance().getNetwork();
									byte[] seed = Utils.readSeed(SpinnerContext.getInstance().getApplicationContext(), net);
									DeterministicECKeyExporter exporter = new DeterministicECKeyExporter(seed);
									final String keyString;
									keyString = exporter.getPrivateKeyExporter(1).getBase58EncodedKey(SpinnerContext.getInstance().getNetwork());
									qrAdress.setImageBitmap(Utils.getLargeQRCodeBitmap(
											keyString));
									
									qrAdress.setOnClickListener(new OnClickListener() {

										@Override
										public void onClick(View v) {
											exportKeyDialog.dismiss();
										}
									});

									Button copy = (Button) layout
											.findViewById(R.id.btn_copy_to_clip);
									copy.setOnClickListener(new OnClickListener() {

										@Override
										public void onClick(View v) {
											ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
											clipboard.setText(keyString);
											Toast.makeText(mContext,
													R.string.clipboard_copy,
													Toast.LENGTH_SHORT).show();
										}
									});

									exportKeyDialog.show();
								}
							})
					.setNegativeButton(R.string.no,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
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


	private void showMarketPage(final String packageName) {
		final Intent marketIntent = new Intent(Intent.ACTION_VIEW,
				Uri.parse(String.format(Consts.MARKET_APP_URL, packageName)));
		if (getPackageManager().resolveActivity(marketIntent, 0) != null)
			startActivity(marketIntent);
		else
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(String
					.format(Consts.WEBMARKET_APP_URL, packageName))));
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
