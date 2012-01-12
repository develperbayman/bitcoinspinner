package com.miracleas.bitcoin_spinner_lib;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
import com.bccapi.core.BitcoinClientApiImpl;
import com.bccapi.core.DeterministicECKeyExporter;
import com.bccapi.core.DeterministicECKeyManager;
import com.bccapi.core.ECKeyManager;
import com.bccapi.core.Asynchronous.AsynchronousAccount;

public class SettingsActivity extends PreferenceActivity {

	private SharedPreferences preferences;
	private SharedPreferences.Editor editor;

	private AlertDialog backupWalletDialog, exportKeyDialog;
	private String qrString, keyString;

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

									qrString = "bsb:" + getSeedAsBase58()
											+ "?net=";

									qrString += preferences.getInt(
											Consts.NETWORK, Consts.PRODNET);

									qrAdress.setImageBitmap(Utils.getLargeQRCodeBitmap(
											qrString));
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
											clipboard.setText(qrString);
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

			AsynchronousAccount accountManager = Consts.account;
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
									
									byte[] seed = new byte[Consts.SEED_SIZE];
									String seedFile = null;
									switch (preferences.getInt(Consts.NETWORK, 0)) {
									case Consts.PRODNET:
										seedFile = Consts.PRODNET_FILE;
										break;
							
									case Consts.TESTNET:
										seedFile = Consts.TESTNET_FILE;
										break;
										
									case Consts.CLOSEDTESTNET:
										seedFile = Consts.CLOSED_TESTNET_FILE;
										break;
									}

									FileInputStream fis;
									try {
										fis = openFileInput(seedFile);
										fis.read(seed);
										fis.close();
									} catch (FileNotFoundException e1) {
										e1.printStackTrace();
									} catch (IOException e) {
										e.printStackTrace();
									}

									TextView text =(TextView ) layout.findViewById(R.id.tv_title_text); 
									text.setText(R.string.private_key);
									ImageView qrAdress = (ImageView) layout
											.findViewById(R.id.iv_qr_Address);
									
									DeterministicECKeyExporter exporter = new DeterministicECKeyExporter(seed);
									keyString = exporter.getPrivateKeyExporter(1).getBase58EncodedKey(Consts.account.getNetwork());
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
	public void onActivityResult(final int requestCode, final int resultCode,
			final Intent intent) {
		if (requestCode != REQUEST_CODE_SCAN || 
		    resultCode != RESULT_OK ||
		    !("QR_CODE".equals(intent.getStringExtra("SCAN_RESULT_FORMAT")))) {
			return;
		}
		final String contents = intent.getStringExtra("SCAN_RESULT");
		if (contents.matches("[a-zA-Z0-9]*")) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.restore_invalid_qr_code)
					.setCancelable(false)
					.setNeutralButton(R.string.ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			AlertDialog alertDialog = builder.create();
			alertDialog.show();
			return;
		} 
		
		restoreDialog = ProgressDialog.show(this,
				getString(R.string.restore_dialog_title),
				getString(R.string.please_wait), true);
		new AsyncInit().execute(contents);
	}

	private class AsyncInit extends AsyncTask<String, Integer, Long> {

		@Override
		protected Long doInBackground(String... params) {
			Uri uri = Uri.parse(params[0]);
			final Uri u = Uri.parse("bsb://" + uri.getSchemeSpecificPart());

			final int networkId = Integer
					.parseInt(u.getQueryParameter("net"));

			String seedFile = null;
			Network network = Network.productionNetwork;
			switch (networkId) {
			case Consts.PRODNET:
				seedFile = Consts.PRODNET_FILE;
				network = Network.productionNetwork;
				break;
			case Consts.TESTNET:
				seedFile = Consts.TESTNET_FILE;
				network = Network.testNetwork;
				break;
			case Consts.CLOSEDTESTNET:
				seedFile = Consts.CLOSED_TESTNET_FILE;
				network = Network.testNetwork;
				break;
			}

			FileOutputStream fos = null;
			byte seed[] = new byte[Consts.SEED_SIZE];
			try {
				String host = u.getHost();
				seed = Base58.decode(host);
				fos = openFileOutput(seedFile, MODE_PRIVATE);
				fos.write(seed);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			ECKeyManager keyManager = new DeterministicECKeyManager(seed);
			BitcoinClientApiImpl api = new BitcoinClientApiImpl(Consts.url,
					network);
			Consts.account = new AndroidAccount(keyManager, api, Consts.applicationContext);
			// Force deterministic key manager to calculate its keys, this is CPU intensive
			Consts.account.getPrimaryBitcoinAddress();
			return null;
		}

		protected void onPostExecute(Long result) {
			restoreDialog.dismiss();
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setMessage(R.string.restore_complete_dialog_text)
					.setCancelable(false)
					.setNeutralButton(R.string.ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			AlertDialog alertDialog = builder.create();
			alertDialog.show();
		}
	}

	private String getSeedAsBase58() {
		String seedFile;
		byte[] seed = new byte[Consts.SEED_SIZE];

		if (Consts.TESTNET == preferences
				.getInt(Consts.NETWORK, Consts.PRODNET)) {
			seedFile = Consts.TESTNET_FILE;
		} else if (Consts.CLOSEDTESTNET == preferences.getInt(Consts.NETWORK,
				Consts.PRODNET)) {
			seedFile = Consts.CLOSED_TESTNET_FILE;
		} else {
			seedFile = Consts.PRODNET_FILE;
		}

		FileInputStream fis;
		try {
			fis = openFileInput(seedFile);
			fis.read(seed);
			fis.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return Base58.encode(seed);
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

}
