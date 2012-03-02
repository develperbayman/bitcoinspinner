package com.miracleas.bitcoin_spinner_lib;

import android.content.Intent;

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
	public static final int    DEFAULT_TRANSACTION_HISTORY_SIZE = 15;

	public static final String PACKAGE_NAME_PROD = "com.miracleas.bitcoin_spinner";
	public static final String PACKAGE_NAME_TEST = "com.miracleas.bitcoin_spinner" + '_' + "test";
	public static final String PACKAGE_NAME_CLOSEDTEST = "com.miracleas.bitcoin_spinner" + '_' + "closedtest";

	/**
	 * Set this to true if you want to use closed test net when on test network
	 */
	public static final boolean USE_CLOSED_TESTNET = false;  
	public static final String EXTRA_NETWORK = "Extra network";
	public static final String BTC_ADDRESS_KEY = "BTC address key";
	public static final String BTC_AMOUNT_KEY = "BTC amount key";
	public static final String DONATION_ADDRESS = "14VWYvbHd4R7oTFS8kEfoWZFTzbedDgwKg";
	public static final String TESTNET_DONATION_ADDRESS = "mnXc9S1HqLiG8n7N5fi6R6RRF7oBR5FfbN";

	public static final String PRODNET_FILE = "seed.bin";
	public static final String TESTNET_FILE = "test-seed.bin";
	public static final String CLOSED_TESTNET_FILE = "closed-test-seed.bin";

	public static final int SEED_SIZE = 32;
	public static final int SEED_GEN_SIZE = 64;
	
	public static final int SATOSHIS_PER_BITCOIN = 100000000;
	public static final long MILLISECOND_IN_NANOSECONDS = 1000000;
	public static final long SECOND_IN_NANOSECONDS = MILLISECOND_IN_NANOSECONDS * 1000;
	public static final long MINUTE_IN_NANOSECONDS = SECOND_IN_NANOSECONDS * 60;
	
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
