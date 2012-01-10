package com.miracleas.bitcoin_spinner_lib;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ProgressBar;

import com.bccapi.api.Network;
import com.bccapi.core.BitcoinClientApiImpl;
import com.bccapi.core.DeterministicECKeyManager;
import com.bccapi.core.HashUtils;

public class StartUpActivity extends Activity {

	private SharedPreferences preferences;

	private static final int SET_PROGRESS_MESSAGE = 1;
	private static final int INITIALIZE_MESSAGE = 100;
	private static final int START_MESSAGE = 101;
	private static final int ERROR_MESSAGE = 102;
	

	private static final int STARTUP_DIALOG = 1;

	private ProgressBar progressbarStartup;

	private String seedFile;
	private byte[] seed = new byte[Consts.SEED_SIZE];

	private Context context;
	private String mInitializationError;
	private AsyncInit mAsyncInit;
	
	
	// Guard to avoid that we get two StartupActivities competing to initialize.
	// For some reason that I cannot understand we some times experience that two StartupActivity instances are created.
	// This crude code makes sure that only one of them gets to initialize
	private static boolean wasStarted = false;
	private static synchronized boolean isFirst(){
		if(!wasStarted) {
			wasStarted = true;
			return true;
		}
		return false;
	}
	
	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		preferences = getSharedPreferences(Consts.PREFS_NAME, MODE_PRIVATE);
		context = this;
		Consts.applicationContext = getApplicationContext();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(!preferences.getString(Consts.LOCALE, "").matches("")) {
			Locale locale = new Locale(preferences.getString(Consts.LOCALE, "en"));
			Locale.setDefault(locale);
			Configuration config = new Configuration();
			config.locale = locale;
			getBaseContext().getResources().updateConfiguration(config,
			      getBaseContext().getResources().getDisplayMetrics());
		}
		if (isFirst()) {
			Message message = handler.obtainMessage();
			message.arg1 = INITIALIZE_MESSAGE;
			handler.sendMessage(message);
		}
		
	}
	
	private byte[] createSeed(String seedFileName) {
		FileOutputStream fos = null;
		try {
			SecureRandom random = new SecureRandom();
			byte genseed[] = random.generateSeed(Consts.SEED_GEN_SIZE);
			seed = HashUtils.sha256(genseed);
			fos = openFileOutput(seedFileName, MODE_PRIVATE);
			fos.write(seed);
			fos.close();
			return seed;
		} catch (IOException e) {
			return null;
		}
	}
	
	private byte[] readSeed(String seedFileName) {
		FileInputStream fis;
		byte[] seed = new byte[Consts.SEED_SIZE];
		try {
			fis = openFileInput(seedFile);
			fis.read(seed);
			fis.close();
			return seed;
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	// Define the Handler that receives messages from the thread and update UI
	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.arg1) {
			case INITIALIZE_MESSAGE:
				if(mAsyncInit == null) {
					mAsyncInit = new AsyncInit();
					mAsyncInit.execute();
					showDialog(STARTUP_DIALOG);
				}
				break;
			case SET_PROGRESS_MESSAGE:
				int total = msg.arg2;
				progressbarStartup.setProgress(total);
				break;
			case START_MESSAGE:
				Intent intent = new Intent();
				intent.setClass(context, MainActivity.class);
				startActivity(intent);
				finish();
				break;
			case ERROR_MESSAGE:
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setMessage(mInitializationError).setCancelable(false)
						.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								finish();
							}
						});
				AlertDialog alertDialog = builder.create();
				alertDialog.show();
				break;
			}
		}
	};

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = new Dialog(this);
		dialog.setCancelable(false);
		switch (id) {
		case STARTUP_DIALOG:
			dialog.setContentView(R.layout.dialog_startup);
			dialog.setTitle(R.string.initializing_bitcoinspinner);
			progressbarStartup = (ProgressBar) dialog
					.findViewById(R.id.pb_startup);
			break;
		}
		return dialog;
	}

	private void initialize(){
		Intent i = getIntent();
		Editor editor = preferences.edit();
		Network network = Network.productionNetwork;
		try {
			switch (i.getExtras().getInt(Consts.EXTRA_NETWORK)) {
			case Consts.PRODNET:
				network = Network.productionNetwork;
				seedFile = Consts.PRODNET_FILE;
				Consts.url = new URL("https://prodnet.bccapi.com:443");
				editor.putInt(Consts.NETWORK, Consts.PRODNET);
				break;
	
			case Consts.TESTNET:
				network = Network.testNetwork;
				seedFile = Consts.TESTNET_FILE;
				Consts.url = new URL("https://testnet.bccapi.com:444");
				editor.putInt(Consts.NETWORK, Consts.TESTNET);
				break;
				
			case Consts.CLOSEDTESTNET:
				network = Network.testNetwork;
				seedFile = Consts.CLOSED_TESTNET_FILE;
				Consts.url = new URL("https://testnet.bccapi.com:445");
				editor.putInt(Consts.NETWORK, Consts.CLOSEDTESTNET);
				break;
			}
			editor.commit();
			
			seed = readSeed(seedFile);
			if (seed == null) {
				seed = createSeed(seedFile);
			}
		} catch (Exception e) {
			mInitializationError = e.getMessage();
			return;
		}

		DeterministicECKeyManager keyManager = new DeterministicECKeyManager(seed);
		BitcoinClientApiImpl api = new BitcoinClientApiImpl(Consts.url, network);
		AndroidAccount account = new AndroidAccount(keyManager, api, Consts.applicationContext);
		// Force deterministic key manager to calculate its keys, this is CPU intensive
		account.getPrimaryBitcoinAddress();
		// Force the QR code to get generated (it is cached)
		Utils.getPrimaryAddressAsSmallQrCode(account);
		Consts.account = account;
	}

	private class AsyncInit extends AsyncTask<Void, Integer, Long> {

		@Override
		protected Long doInBackground(Void... params) {
			initialize();
			return null;
		}

		protected void onPostExecute(Long result) {
			if (mInitializationError != null) {
				Message message = handler.obtainMessage();
				message.arg1 = ERROR_MESSAGE;
				handler.sendMessage(message);
			}else{
				Message message = handler.obtainMessage();
				message.arg1 = START_MESSAGE;
				handler.sendMessage(message);
			}
		}
	}
}
