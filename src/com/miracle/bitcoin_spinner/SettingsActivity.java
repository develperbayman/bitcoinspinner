package com.miracle.bitcoin_spinner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.bccapi.api.APIException;
import com.bccapi.api.AccountInfo;
import com.bccapi.core.Account;
import com.bccapi.core.BitcoinClientApiImpl;
import com.bccapi.core.DeterministicECKeyManager;
import com.bccapi.core.ECKeyManager;
import com.bccapi.core.HexUtils;

public class SettingsActivity extends PreferenceActivity {

	private SharedPreferences preferences;
	private SharedPreferences.Editor editor;

	private AlertDialog backupWalletDialog;
	private static final QRCodeWriter QR_CODE_WRITER = new QRCodeWriter();

	private static final int REQUEST_CODE_SCAN = 0;

	private Context mContext;

	ProgressDialog restoreDialog;

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

		Preference networkFeeSizePref = (Preference) findPreference("networkFeeSizePref");
		networkFeeSizePref
				.setOnPreferenceChangeListener(networkFeeSizeChangeListener);
		networkFeeSizePref.setSummary(preferences.getString(
				Consts.NETWORKFEESIZE, "0.0005") + " BTC");

		Preference backupWalletPref = (Preference) findPreference("backupSeed");
		backupWalletPref
				.setOnPreferenceClickListener(backupWalletClickListener);

		Preference restoreWalletPref = (Preference) findPreference("restoreSeed");
		restoreWalletPref
				.setOnPreferenceClickListener(restoreWalletClickListener);

	}

	private final OnPreferenceChangeListener networkFeeSizeChangeListener = new OnPreferenceChangeListener() {

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			String fee = (String) newValue;
			fee = fee.replaceAll("[^.0-9]", "");
			if (fee.matches("[0-9]+\\.[0-9]+")) {
				editor.putString(Consts.NETWORKFEESIZE, fee);
				editor.commit();
				preference.setSummary(fee + " BTC");
				return true;
			}
			return false;
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
									View layout = inflater
											.inflate(
													R.layout.dialog_backup_wallet,
													null);
									AlertDialog.Builder builder = new AlertDialog.Builder(
											mContext).setView(layout);
									backupWalletDialog = builder.create();
									backupWalletDialog
											.setCanceledOnTouchOutside(true);
									ImageView qrAdress = (ImageView) layout
											.findViewById(R.id.iv_backup_Address);

									String qrString = "bclbackup:"
											+ getSeedAsHex() + "?network=";
									if (Consts.USE_TEST_NETWORK)
										qrString += Consts.TESTNET;
									else if (Consts.USE_CLOSED_TEST_NETWORK)
										qrString += Consts.CLOSEDTESTNET;
									else
										qrString += Consts.PRODNET;

									qrAdress.setImageBitmap(getQRCodeBitmap(
											qrString, 320));
									qrAdress.setOnClickListener(new OnClickListener() {

										@Override
										public void onClick(View v) {
											backupWalletDialog.dismiss();
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

			if (isConnected()) {
				AccountInfo info = null;
				try {
					info = Consts.account.getInfo();
				} catch (APIException e) {
					if (e.getMessage().matches("Invalid session")) {
						try {
							Consts.account.login();
							info = Consts.account.getInfo();
						} catch (IOException e1) {
							e1.printStackTrace();
						} catch (APIException e1) {
							e1.printStackTrace();
						}
					}
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				if (info.getAvailableBalance() > 0
						|| info.getEstimatedBalance() > 0) {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							mContext);
					builder.setMessage(R.string.restore_dialog_coins)
							.setCancelable(false)
							.setNeutralButton(R.string.ok,
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {
											dialog.cancel();
										}
									});
					AlertDialog alertDialog = builder.create();
					alertDialog.show();
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							mContext);
					builder.setMessage(R.string.restore_dialog_no_coins)
							.setCancelable(false)
							.setPositiveButton(R.string.yes,
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {

											final PackageManager pm = getPackageManager();
											if (pm.resolveActivity(
													Consts.zxingIntent, 0) != null) {
												startActivityForResult(
														Consts.zxingIntent,
														REQUEST_CODE_SCAN);
											} else if (pm.resolveActivity(
													Consts.gogglesIntent, 0) != null) {
												startActivity(Consts.gogglesIntent);
											} else {
												showMarketPage(Consts.PACKAGE_NAME_ZXING);
												Toast.makeText(
														mContext,
														"Please install ZXing QR-code scanner or Goggles!",
														Toast.LENGTH_LONG)
														.show();
											}
										}
									})
							.setNegativeButton(R.string.no,
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {
											dialog.cancel();
										}
									});
					AlertDialog alertDialog = builder.create();
					alertDialog.show();
				}
			} else {
				Toast.makeText(mContext, "You need to be connected", Toast.LENGTH_LONG).show();
			}
			return true;
		}
	};

	@Override
	public void onActivityResult(final int requestCode, final int resultCode,
			final Intent intent) {
		if (requestCode == REQUEST_CODE_SCAN
				&& resultCode == RESULT_OK
				&& "QR_CODE"
						.equals(intent.getStringExtra("SCAN_RESULT_FORMAT"))) {
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
			} else {
				Uri uri = Uri.parse(contents);
				final Uri u = Uri.parse("bclbackup://"
						+ uri.getSchemeSpecificPart());

				final int network = Integer.parseInt(u
						.getQueryParameter("network"));

				String seedFile = null;
				switch (network) {
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

				FileOutputStream fos = null;
				byte seed[] = new byte[Consts.SEED_SIZE];
				try {
					seed = HexUtils.toBytes(u.getHost());
					fos = openFileOutput(seedFile, MODE_PRIVATE);
					fos.write(seed);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				ECKeyManager keyManager = new DeterministicECKeyManager(seed);
				BitcoinClientApiImpl api = new BitcoinClientApiImpl(Consts.url,
						Consts.network);
				Consts.account = new Account(keyManager, api);
				restoreDialog = ProgressDialog.show(this, "Restoring",
						"Please wiat...", true);
				new AsyncLogin().execute(Consts.account);
			}
		}
	}

	private class AsyncLogin extends AsyncTask<Account, Integer, Long> {

		@Override
		protected Long doInBackground(Account... params) {
			try {
				Consts.lastLogin = new Date();
				Consts.account.login();
				Consts.info = Consts.account.getInfo();
				for (String address : Consts.account.getAddresses()) {
					editor.putString(Consts.BITCOIN_ADDRESS, address);
					break;
				}
				editor.commit();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (APIException e) {
				e.printStackTrace();
			}
			
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

	private static Bitmap getQRCodeBitmap(final String url, final int size) {
		try {
			final Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
			hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
			final BitMatrix result = QR_CODE_WRITER.encode(url,
					BarcodeFormat.QR_CODE, size, size, hints);

			final int width = result.getWidth();
			final int height = result.getHeight();
			final int[] pixels = new int[width * height];

			for (int y = 0; y < height; y++) {
				final int offset = y * width;
				for (int x = 0; x < width; x++) {
					pixels[offset + x] = result.get(x, y) ? Color.BLACK
							: Color.WHITE;
				}
			}

			final Bitmap bitmap = Bitmap.createBitmap(width, height,
					Bitmap.Config.ARGB_8888);
			bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
			return bitmap;
		} catch (final WriterException x) {
			x.printStackTrace();
			return null;
		}
	}

	private String getSeedAsHex() {
		String seedFile;
		byte[] seed = new byte[Consts.SEED_SIZE];

		if (Consts.USE_TEST_NETWORK) {
			seedFile = "test-seed.bin";
		} else if (Consts.USE_CLOSED_TEST_NETWORK) {
			seedFile = "closed-test-seed.bin";
		} else {
			seedFile = "seed.bin";
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

		return HexUtils.toHex(seed);
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
	
	private boolean isConnected() {
		return Consts.isConnected(mContext);
	}
}
