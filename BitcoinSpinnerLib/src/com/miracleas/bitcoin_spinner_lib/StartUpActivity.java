package com.miracleas.bitcoin_spinner_lib;

import java.util.Locale;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ProgressBar;

import com.bccapi.api.Network;

public class StartUpActivity extends Activity {

	private static final int SET_PROGRESS_MESSAGE = 1;
	private static final int INITIALIZE_MESSAGE = 100;
	private static final int START_MESSAGE = 101;

	private static final int STARTUP_DIALOG = 1;

	private ProgressBar mProgressbarStartup;
	private SharedPreferences mPreferences;
	private Context mContext;
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
		mPreferences = getSharedPreferences(Consts.PREFS_NAME, MODE_PRIVATE);
		mContext = this;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(!mPreferences.getString(Consts.LOCALE, "").matches("")) {
			Locale locale = new Locale(mPreferences.getString(Consts.LOCALE, "en"));
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
				mProgressbarStartup.setProgress(total);
				break;
			case START_MESSAGE:
				Intent intent = new Intent();
				intent.setClass(mContext, MainActivity.class);
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
			dialog.setTitle(R.string.initializing_bitcoinspinner);
			mProgressbarStartup = (ProgressBar) dialog
					.findViewById(R.id.pb_startup);
			break;
		}
		return dialog;
	}


	private class AsyncInit extends AsyncTask<Void, Integer, Long> {

		@Override
		protected Long doInBackground(Void... params) {
			Intent i = getIntent();
			Network network = (Network) i.getExtras().getSerializable(Consts.EXTRA_NETWORK);
			SpinnerContext.initialize(mContext, getWindowManager().getDefaultDisplay(), network);
			return null;
		}

		protected void onPostExecute(Long result) {
			Message message = handler.obtainMessage();
			message.arg1 = START_MESSAGE;
			handler.sendMessage(message);
		}
	}
}
