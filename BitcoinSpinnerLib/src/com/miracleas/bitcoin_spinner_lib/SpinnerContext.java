package com.miracleas.bitcoin_spinner_lib;

import java.net.URL;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.view.Display;

import com.bccapi.api.Network;
import com.bccapi.core.BitcoinClientApiImpl;
import com.bccapi.core.ECKeyManager;
import com.bccapi.core.Asynchronous.AsynchronousAccount;

public class SpinnerContext {

	private static final String NETWORK_USED = "network used";
	private static final String PIN = "pin";

	private static SpinnerContext _spinnerContext;

	private Network _network;
	private Context _applicationContext;
	private AsynchronousAccount _account;
	private int _displayWidth; // Total width of the device display in pixels
	private int _displayHeight; // Total height of the device display in pixels

	public static boolean isInitialized() {
		return _spinnerContext != null;
	}

	public static void initialize(Context context, Display display,
			Network network) {
		context = context.getApplicationContext();
		SharedPreferences preferences = context.getSharedPreferences(
				Consts.PREFS_NAME, Context.MODE_PRIVATE);
		preferences.edit().putInt(NETWORK_USED, network.getAddressHeader())
				.commit();
		_spinnerContext = new SpinnerContext(context, display, network);
		Utils.getPrimaryAddressAsSmallQrCode(_spinnerContext.getAccount());
	}

	public static void initialize(Context context, Display display) {
		context = context.getApplicationContext();
		SharedPreferences preferences = context.getSharedPreferences(
				Consts.PREFS_NAME, Context.MODE_PRIVATE);
		int net = preferences.getInt(NETWORK_USED, -1);
		Network network;
		if (net == Network.productionNetwork.getAddressHeader()) {
			network = Network.productionNetwork;
		} else if (net == Network.testNetwork.getAddressHeader()) {
			network = Network.testNetwork;
		} else {
			throw new RuntimeException("Network never initialized");
		}
		_spinnerContext = new SpinnerContext(context, display, network);
		Utils.getPrimaryAddressAsSmallQrCode(_spinnerContext.getAccount());
	}

	public static SpinnerContext getInstance() {
		if (!isInitialized()) {
			throw new RuntimeException("BitcoinSpinner not initialized");
		}
		return _spinnerContext;
	}

	private SpinnerContext(Context context, Display display, Network network) {
		_applicationContext = context.getApplicationContext();
		_network = network;
		DisplayMetrics dm = new DisplayMetrics();
		display.getMetrics(dm);
		_displayWidth = dm.widthPixels;
		_displayHeight = dm.heightPixels;
		URL url = Utils.getBccapiUrl(_network);
		ECKeyManager keyManager = new AndroidKeyManager(_applicationContext,
				_network);
		BitcoinClientApiImpl api = new BitcoinClientApiImpl(url, _network);
		_account = new AndroidAccount(keyManager, api, _applicationContext);
	}

	public void recoverWallet(byte[] seed) {
		URL url = Utils.getBccapiUrl(_network);
		ECKeyManager keyManager = new AndroidKeyManager(_applicationContext,
				_network, seed);
		BitcoinClientApiImpl api = new BitcoinClientApiImpl(url, _network);
		_account = new AndroidAccount(keyManager, api, _applicationContext);
	}

	public Network getNetwork() {
		return _network;
	}

	public boolean isPinProtected() {
		return getPin().length() > 0;
	}

	public String getPin() {
		SharedPreferences preferences = getApplicationContext()
				.getSharedPreferences(Consts.PREFS_NAME, Context.MODE_PRIVATE);
		return preferences.getString(PIN, "");
	}

	public void setPin(String pin) {
		SharedPreferences preferences = getApplicationContext()
				.getSharedPreferences(Consts.PREFS_NAME, Context.MODE_PRIVATE);
		preferences.edit().putString(PIN, pin).commit();
	}

	public Context getApplicationContext() {
		return _applicationContext;
	}

	public AsynchronousAccount getAccount() {
		return _account;
	}

	public int getDisplayWidth() {
		return _displayWidth;
	}

	public int getDisplayHeight() {
		return _displayHeight;
	}

}
