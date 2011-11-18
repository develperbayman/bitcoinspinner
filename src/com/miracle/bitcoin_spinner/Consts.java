package com.miracle.bitcoin_spinner;

import java.net.URL;
import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.bccapi.api.AccountInfo;
import com.bccapi.api.Network;
import com.bccapi.core.Account;

public final class Consts {
	public static final String TAG = "BCWLite";
	
	public static final String PREFS_NAME = "BitCoinWalletLitePrefs";
	public static final String FIRSTTIME_PREFS = "FistTimeStart";
	public static final String SEEDSTRING = "SeedString";
	public static final String LASTKNOWNBALANCE = "LastKnownBalance";
	public static final String NETWORKFEESIZE = "NetworkFeeSize";
	public static final String AVAIABLE_BITCOINS = "AvailableBitcoins";
	public static final String BITCOINS_ON_THE_WAY = "BitcoinsOnTheWay";
	public static final String BITCOIN_ADDRESS = "BitcoinAddress";
	
	public static final boolean DEBUG = true;
	public static final boolean USE_TEST_NETWORK = false;
	public static final boolean USE_CLOSED_TEST_NETWORK = true;
	
	public static final int PRODNET = 0;
	public static final int TESTNET = 1;
	public static final int CLOSEDTESTNET = 2;
	
	public static final String PRODNET_FILE = "seed.bin";
	public static final String TESTNET_FILE = "test-seed.bin";
	public static final String CLOSED_TESTNET_FILE = "closed-test-seed.bin";

	public static final int SEED_SIZE = 32;
	public static final int SEED_GEN_SIZE = 64;
	
	public static final int BTC_IN_SATOSHI = 100000000;
	
	public static Account account;
	public static Network network;
	public static URL url;
	public static AccountInfo info;
	
	public static Date lastLogin;
	
	public static final String PACKAGE_NAME_ZXING = "com.google.zxing.client.android";
	public static final String MARKET_APP_URL = "market://details?id=%s";
	public static final String WEBMARKET_APP_URL = "https://market.android.com/details?id=%s";
	
	public static final Intent zxingIntent = new Intent(
			"com.google.zxing.client.android.SCAN").putExtra("SCAN_MODE",
			"QR_CODE_MODE");
	public static final Intent gogglesIntent = new Intent().setClassName(
			"com.google.android.apps.unveil",
			"com.google.android.apps.unveil.CaptureActivity");
	
	static boolean isConnected(Context context) {
		boolean IsConnected = false;
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] NI = cm.getAllNetworkInfo();
		for (int i = 0; i < NI.length; i++) {
			if (NI[i].isConnected()) {
				IsConnected = true;
				break;
			}
		}
		return IsConnected;
	}

}
