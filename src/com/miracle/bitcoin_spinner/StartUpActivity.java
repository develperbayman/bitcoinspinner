package com.miracle.bitcoin_spinner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ProgressBar;

import com.bccapi.api.APIException;
import com.bccapi.api.Network;
import com.bccapi.core.Account;
import com.bccapi.core.BitcoinClientApiImpl;
import com.bccapi.core.DeterministicECKeyManager;
import com.bccapi.core.ECKeyManager;
import com.bccapi.core.HashUtils;

public class StartUpActivity extends Activity {

	private SharedPreferences preferences;
	private SharedPreferences.Editor editor;

	private static final int STARTUP_PROGRESS_MESSAGE = 1;

	private static final int SETUP_MESSAGE = 100;
	private static final int SAVE_SEED_MESSAGE = 101;
	private static final int STARTUP_MESSAGE = 102;
	private static final int KEY_MANAGER_MESSAGE = 103;
	private static final int BIT_COIN_CLIENT_API_INSTANCE_MESSAGE = 104;
	private static final int ACCOUNT_MESSAGE = 105;
	private static final int LOGIN_MESSAGE = 106;
	private static final int GET_ACCOUNT_INFO_MESSAGE = 107;

	private static final int STARTUP_DIALOG = 1;

	ProgressBar progressbarShake, progressbarStartup;
	ProgressThread progressThread;

	ECKeyManager keyManager;
	BitcoinClientApiImpl api;

	String seedFile;
	byte[] seed = new byte[Consts.SEED_SIZE];
	int seedNbr = 1;

	Context context;

	boolean firstTime;
	AlertDialog noConnDialog;

	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		preferences = getSharedPreferences(Consts.PREFS_NAME, MODE_PRIVATE);
		editor = preferences.edit();

		context = this;
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (preferences.getBoolean("ShowQrCode", false) && null != noConnDialog) {
			noConnDialog.show();
		} else {

			firstTime = preferences.getBoolean(Consts.FIRSTTIME_PREFS, true);

			try {
				if (Consts.USE_TEST_NETWORK) {
					Consts.network = Network.testNetwork;
					seedFile = Consts.TESTNET_FILE;
					Consts.url = new URL("https://testnet.bccapi.com:444");
				} else if (Consts.USE_CLOSED_TEST_NETWORK) {
					Consts.network = Network.testNetwork;
					seedFile = Consts.CLOSED_TESTNET_FILE;
					Consts.url = new URL("https://testnet.bccapi.com:445");
				} else {
					Consts.network = Network.productionNetwork;
					seedFile = Consts.PRODNET_FILE;
					Consts.url = new URL("https://prodnet.bccapi.com:443");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			Message message = handler.obtainMessage();
			message.arg1 = SETUP_MESSAGE;
			handler.sendMessage(message);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (null != noConnDialog) {
			editor.putBoolean("ShowNoConn", noConnDialog.isShowing());
			editor.commit();
			noConnDialog.dismiss();
		} else {
			editor.putBoolean("ShowNoConn", false);
			editor.commit();
		}
	}

	// Define the Handler that receives messages from the thread and update UI
	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			int total;
			Intent intent = new Intent();
			Message message;
			switch (msg.arg1) {
			case STARTUP_PROGRESS_MESSAGE:
				total = msg.arg2;
				progressbarStartup.setProgress(total);
				break;
			case SETUP_MESSAGE:
				Consts.isConnected(context);
				if (isConnected()) {
					if (firstTime) {
						message = handler.obtainMessage();
						message.arg1 = SAVE_SEED_MESSAGE;
						handler.sendMessage(message);
					} else {
						message = handler.obtainMessage();
						message.arg1 = STARTUP_MESSAGE;
						handler.sendMessage(message);
					}
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							context);
					builder.setMessage(
							"You need to be connected to the internet.")
							.setCancelable(false)
							.setPositiveButton("Try again?",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {
											Message message = handler
													.obtainMessage();
											message.arg1 = SETUP_MESSAGE;
											handler.sendMessage(message);
											dialog.cancel();
										}
									})
							.setNegativeButton("Exit",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {
											finish();
											dialog.cancel();
										}
									});
					noConnDialog = builder.create();
					noConnDialog.show();
				}
				break;
			case SAVE_SEED_MESSAGE:
				FileOutputStream fos = null;
				try {
					SecureRandom random = new SecureRandom();
					byte genseed[] = random.generateSeed(Consts.SEED_GEN_SIZE);
					seed = HashUtils.sha256(genseed);
					fos = openFileOutput(seedFile, MODE_PRIVATE);
					fos.write(seed);

				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				message = handler.obtainMessage();
				message.arg1 = STARTUP_MESSAGE;
				handler.sendMessage(message);

				break;
			case STARTUP_MESSAGE:
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
				showDialog(STARTUP_DIALOG);
				break;
			case KEY_MANAGER_MESSAGE:
				keyManager = new DeterministicECKeyManager(seed);
				message = handler.obtainMessage();
				message.arg1 = BIT_COIN_CLIENT_API_INSTANCE_MESSAGE;
				handler.sendMessage(message);
				break;
			case BIT_COIN_CLIENT_API_INSTANCE_MESSAGE:
				api = new BitcoinClientApiImpl(Consts.url, Consts.network);
				Consts.account = new Account(keyManager, api);
				message = handler.obtainMessage();
				message.arg1 = LOGIN_MESSAGE;
				handler.sendMessage(message);
				break;
			case ACCOUNT_MESSAGE:
				Consts.account = new Account(keyManager, api);
				message = handler.obtainMessage();
				message.arg1 = LOGIN_MESSAGE;
				handler.sendMessage(message);
				break;
			case LOGIN_MESSAGE:
				new AsyncLogin().execute(Consts.account);
				break;
			case GET_ACCOUNT_INFO_MESSAGE:
				try {
					Consts.info = Consts.account.getInfo();
					if (Consts.info.getKeys() == 0) {
						// upload first wallet public key
						Consts.account.addKey();
					}
				} catch (APIException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				progressThread.mState = ProgressThread.STOP;
				intent.setClass(context, MainActivity.class);
				startActivity(intent);
				finish();
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
			dialog.setTitle(R.string.startup_dialog_title);
			progressbarStartup = (ProgressBar) dialog
					.findViewById(R.id.pb_startup);

			editor.putBoolean(Consts.FIRSTTIME_PREFS, false);
			editor.commit();

			Message msg = handler.obtainMessage();
			msg.arg1 = KEY_MANAGER_MESSAGE;
			handler.sendMessage(msg);

			break;
		}
		return dialog;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case STARTUP_DIALOG:
			progressbarStartup = (ProgressBar) dialog
					.findViewById(R.id.pb_startup);
			progressbarStartup.setProgress(0);
			progressThread = new ProgressThread(ProgressThread.STARTUP);
			progressThread.start();
			break;
		}
	}

	private class AsyncLogin extends AsyncTask<Account, Integer, Long> {

		@Override
		protected Long doInBackground(Account... params) {
			try {
				Consts.account.login();
				Consts.lastLogin = new Date();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (APIException e) {
				e.printStackTrace();
			}
			Message msg = handler.obtainMessage();
			msg.arg1 = STARTUP_PROGRESS_MESSAGE;
			msg.arg2 = 100;
			handler.sendMessage(msg);
			return null;
		}

		protected void onPostExecute(Long result) {
			progressThread.mState = ProgressThread.STOP;
			Message message = handler.obtainMessage();
			message.arg1 = GET_ACCOUNT_INFO_MESSAGE;
			handler.sendMessage(message);
		}
	}

	private class ProgressThread extends Thread {
		private static final int STOP = 0;
		private static final int STARTUP = 1;
		private int mState;

		public ProgressThread(int state) {
			mState = state;
		}

		public void run() {
			Message msg;
			switch (mState) {
			case STARTUP:
				msg = handler.obtainMessage();
				msg.arg1 = STARTUP_PROGRESS_MESSAGE;
				msg.arg2 = 1;
				handler.sendMessage(msg);
				break;
			}
		}
	}

	private boolean isConnected() {
		return Consts.isConnected(context);
	}
}
