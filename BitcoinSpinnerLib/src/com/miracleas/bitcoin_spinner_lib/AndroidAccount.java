package com.miracleas.bitcoin_spinner_lib;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;

import com.bccapi.api.AccountInfo;
import com.bccapi.api.BitcoinClientAPI;
import com.bccapi.core.ECKeyManager;
import com.bccapi.core.Asynchronous.AccountCache;
import com.bccapi.core.Asynchronous.AsynchronousAccount;
import com.bccapi.core.Asynchronous.CallbackRunnerInvoker;

public class AndroidAccount extends AsynchronousAccount {

	AndroidAccount(ECKeyManager keyManager, BitcoinClientAPI api, Context applicationContext) {
		super(keyManager, api, new AndroidAccountCache(applicationContext));
	}

	public static class HandlerRunnerPoster implements CallbackRunnerInvoker {

		Handler _handler;
		
		private HandlerRunnerPoster(){
			_handler = new Handler();
		}
		
		@Override
		public void invoke(Runnable runnable) {
			_handler.post(runnable);
			
		}
		
	}
	@Override
	protected CallbackRunnerInvoker createCallbackRunnerInvoker() {
		return new HandlerRunnerPoster();
	}

	private static class AndroidAccountCache implements AccountCache {

		private static final String AVAILABLE_BALANCE = "AvailableBalance";
		private static final String ON_THE_WAY_TO_YOU = "OnTheWayToYou";
		private static final String KEYS = "BitcoinKeys";
		private static final String ACCOUNT_PUB_KEY = "AccountPublicKey";

		private SharedPreferences _preferences;

		public AndroidAccountCache(Context applicationContext){
			_preferences = applicationContext.getSharedPreferences("AccountManagerCache", Context.MODE_PRIVATE);
		}
		
		@Override
		public synchronized String getAccountPublicKey() {
			return _preferences.getString(ACCOUNT_PUB_KEY, "");
		}

		@Override
		public synchronized void setAccountPublicKey(String accountPublicKey) {
			String currentPubKey = getAccountPublicKey();
			Editor editor = _preferences.edit();
			if(!currentPubKey.equals(accountPublicKey)){
				editor.putLong(AVAILABLE_BALANCE, -1);
				editor.putLong(ON_THE_WAY_TO_YOU, -1);
				editor.putInt(KEYS, -1);
			}
			editor.putString(ACCOUNT_PUB_KEY, accountPublicKey);
			editor.commit();
		}

		@Override
		public synchronized void cacheAccountInfo(AccountInfo info) {
			Editor editor = _preferences.edit();
			editor.putLong(AVAILABLE_BALANCE, info.getAvailableBalance());
			editor.putLong(ON_THE_WAY_TO_YOU, info.getEstimatedBalance() - info.getAvailableBalance());
			editor.putInt(KEYS, info.getKeys());
			editor.commit();
		}

		@Override
		public synchronized long getBalance() {
			return _preferences.getLong(AVAILABLE_BALANCE, -1);
		}

		@Override
		public synchronized long getConsOnTheWayToYou() {
			return _preferences.getLong(ON_THE_WAY_TO_YOU, -1);
		}

		@Override
		public synchronized int getBitcoinKeyCount() {
			return _preferences.getInt(KEYS, -1);
		}
		
	}
}
