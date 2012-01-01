package com.miracleas.bitcoin_spinner_lib;

import java.net.URL;

import android.content.Context;
import android.content.Intent;

import com.bccapi.core.Asynchronous.AsynchronousAccount;

public final class Consts {
	public static final String TAG = "Bitcoin Spinner";
	
	public static final String PREFS_NAME = "BitcoinSpinnerPrefs";
	public static final String SEEDSTRING = "SeedString";
	public static final String NETWORKFEESIZE = "NetworkFeeSize";
	public static final String AVAIABLE_BITCOINS = "AvailableBitcoins";
	public static final String BITCOINS_ON_THE_WAY = "BitcoinsOnTheWay";
	public static final String NETWORK = "BitcoinNetwork";
	public static final String LOCALE = "Locale";
	public static final String TRANSACTION_HISTORY_SIZE = "TransactionHistorySize";

	public static final String PACKAGE_NAME_PROD = "com.miracleas.bitcoin_spinner";
	public static final String PACKAGE_NAME_TEST = "com.miracleas.bitcoin_spinner" + '_' + "test";
	public static final String PACKAGE_NAME_CLOSEDTEST = "com.miracleas.bitcoin_spinner" + '_' + "closedtest";

	public static final int PRODNET = 0;
	public static final int TESTNET = 1;
	public static final int CLOSEDTESTNET = 2;
	public static final String EXTRA_NETWORK = "Extra network";

	public static final String PRODNET_FILE = "seed.bin";
	public static final String TESTNET_FILE = "test-seed.bin";
	public static final String CLOSED_TESTNET_FILE = "closed-test-seed.bin";

	public static final int SEED_SIZE = 32;
	public static final int SEED_GEN_SIZE = 64;
	
	public static final int SATOSHIS_PER_BITCOIN = 100000000;
	
	public static AsynchronousAccount account;
	public static URL url;
	public static Context applicationContext;
	
	public static final String PACKAGE_NAME_ZXING = "com.google.zxing.client.android";
	public static final String MARKET_APP_URL = "market://details?id=%s";
	public static final String WEBMARKET_APP_URL = "https://market.android.com/details?id=%s";
	
	public static final Intent zxingIntent = new Intent(
			"com.google.zxing.client.android.SCAN").putExtra("SCAN_MODE",
			"QR_CODE_MODE");
	public static final Intent gogglesIntent = new Intent().setClassName(
			"com.google.android.apps.unveil",
			"com.google.android.apps.unveil.CaptureActivity");
	
}
