package com.miracleas.bitcoin_spinner;

import java.net.URL;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.view.Display;

import com.bccapi.bitlib.crypto.PrivateKeyRing;
import com.bccapi.bitlib.crypto.PublicKeyRing;
import com.bccapi.bitlib.model.NetworkParameters;
import com.bccapi.ng.async.AsynchronousApi;

public class SpinnerContext {

  private static final String NETWORK_USED = "network used";
  private static final String PIN = "pin";
  private static final String PUBLIC_KEY_REGISTERED = "pub key registered";
  private static final String BACKUP_WARNING_DISABLED = "backup warning disabled";

  private static SpinnerContext _spinnerContext;

  private NetworkParameters _network;
  private Context _applicationContext;
  private AsynchronousApi _asyncApi;
  private PublicKeyRing _publicKeyRing;
  private PrivateKeyRing _privateKeyRing;
  private int _displayWidth; // Total width of the device display in pixels
  private int _displayHeight; // Total height of the device display in pixels

  public static boolean isInitialized() {
    return _spinnerContext != null;
  }

  public static void initialize(Context context, Display display, NetworkParameters network) {
    context = context.getApplicationContext();
    SharedPreferences preferences = context.getSharedPreferences(Consts.PREFS_NAME, Context.MODE_PRIVATE);
    preferences.edit().putInt(NETWORK_USED, network.getStandardAddressHeader()).commit();
    _spinnerContext = new SpinnerContext(context, display, network);
    Utils.getPrimaryAddressAsSmallQrCode(_spinnerContext.getAsyncApi());
  }

  public static void initialize(Context context, Display display) {
    context = context.getApplicationContext();
    SharedPreferences preferences = context.getSharedPreferences(Consts.PREFS_NAME, Context.MODE_PRIVATE);
    int net = preferences.getInt(NETWORK_USED, -1);
    NetworkParameters network;
    if (net == NetworkParameters.productionNetwork.getStandardAddressHeader()) {
      network = NetworkParameters.productionNetwork;
    } else if (net == NetworkParameters.testNetwork.getStandardAddressHeader()) {
      network = NetworkParameters.testNetwork;
    } else {
      throw new RuntimeException("Network never initialized");
    }
    _spinnerContext = new SpinnerContext(context, display, network);
    Utils.getPrimaryAddressAsSmallQrCode(_spinnerContext.getAsyncApi());
  }

  public static SpinnerContext getInstance() {
    if (!isInitialized()) {
      throw new RuntimeException("BitcoinSpinner not initialized");
    }
    return _spinnerContext;
  }

  private SpinnerContext(Context context, Display display, NetworkParameters network) {
    _applicationContext = context.getApplicationContext();
    _network = network;
    DisplayMetrics dm = new DisplayMetrics();
    display.getMetrics(dm);
    _displayWidth = dm.widthPixels;
    _displayHeight = dm.heightPixels;
    URL url = Utils.getBccapiUrl(_network);
    com.bccapi.ng.impl.BitcoinClientApiImpl newApi = new com.bccapi.ng.impl.BitcoinClientApiImpl(url, _network);
    NewAndroidKeyManager newKeyManager = new NewAndroidKeyManager(_applicationContext, _network);
    _publicKeyRing = new PublicKeyRing();
    _publicKeyRing.addPublicKey(newKeyManager.getPublicKey(1), _network);
    _asyncApi = new AndroidAsyncApi(_publicKeyRing, newApi, _applicationContext);
    _privateKeyRing = new PrivateKeyRing();
    _privateKeyRing.addPrivateKey(newKeyManager.getPrivateKey(1), _network);
  }

  public void recoverWallet(byte[] seed) {
    URL url = Utils.getBccapiUrl(_network);
    com.bccapi.ng.impl.BitcoinClientApiImpl newApi = new com.bccapi.ng.impl.BitcoinClientApiImpl(url, _network);
    NewAndroidKeyManager newKeyManager = new NewAndroidKeyManager(_applicationContext, _network, seed);
    _publicKeyRing = new PublicKeyRing();
    _publicKeyRing.addPublicKey(newKeyManager.getPublicKey(1), _network);
    _asyncApi = new AndroidAsyncApi(_publicKeyRing, newApi, _applicationContext);
    _privateKeyRing = new PrivateKeyRing();
    _privateKeyRing.addPrivateKey(newKeyManager.getPrivateKey(1), _network);
  }

  public NetworkParameters getNetwork() {
    return _network;
  }

  public boolean isPinProtected() {
    return getPin().length() > 0;
  }

  public boolean isPublicKeyRegistered() {
    SharedPreferences preferences = getApplicationContext().getSharedPreferences(Consts.PREFS_NAME,
        Context.MODE_PRIVATE);
    return preferences.getBoolean(PUBLIC_KEY_REGISTERED, false);
  }

  public void markPublicKeyRegistered() {
    SharedPreferences preferences = getApplicationContext().getSharedPreferences(Consts.PREFS_NAME,
        Context.MODE_PRIVATE);
    if (!preferences.getBoolean(PUBLIC_KEY_REGISTERED, false)) {
      preferences.edit().putBoolean(PUBLIC_KEY_REGISTERED, true).commit();
    }
  }

  public boolean isBackupWarningDisabled() {
    SharedPreferences preferences = getApplicationContext().getSharedPreferences(Consts.PREFS_NAME,
        Context.MODE_PRIVATE);
    return preferences.getBoolean(BACKUP_WARNING_DISABLED, false);
  }

  public void markBackupWarningDisabled() {
    SharedPreferences preferences = getApplicationContext().getSharedPreferences(Consts.PREFS_NAME,
        Context.MODE_PRIVATE);
    if (!preferences.getBoolean(BACKUP_WARNING_DISABLED, false)) {
      preferences.edit().putBoolean(BACKUP_WARNING_DISABLED, true).commit();
    }
  }

  public String getPin() {
    SharedPreferences preferences = getApplicationContext().getSharedPreferences(Consts.PREFS_NAME,
        Context.MODE_PRIVATE);
    return preferences.getString(PIN, "");
  }

  public void setPin(String pin) {
    SharedPreferences preferences = getApplicationContext().getSharedPreferences(Consts.PREFS_NAME,
        Context.MODE_PRIVATE);
    preferences.edit().putString(PIN, pin).commit();
  }

  public Context getApplicationContext() {
    return _applicationContext;
  }

  public AsynchronousApi getAsyncApi() {
    return _asyncApi;
  }

  public PublicKeyRing getPublicKeyRing() {
    return _publicKeyRing;
  }

  public PrivateKeyRing getPrivateKeyRing() {
    return _privateKeyRing;
  }

  public int getDisplayWidth() {
    return _displayWidth;
  }

  public int getDisplayHeight() {
    return _displayHeight;
  }

}
