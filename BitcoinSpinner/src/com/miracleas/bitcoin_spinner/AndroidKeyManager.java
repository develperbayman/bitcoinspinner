package com.miracleas.bitcoin_spinner;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.bccapi.bitlib.crypto.BitcoinSigner;
import com.bccapi.bitlib.crypto.InMemoryPrivateKey;
import com.bccapi.bitlib.crypto.PrivateKey;
import com.bccapi.bitlib.crypto.PublicKey;
import com.bccapi.bitlib.model.NetworkParameters;
import com.bccapi.bitlib.util.Base58;
import com.bccapi.bitlib.util.BitUtils;
import com.bccapi.legacy.DeterministicECKeyManager;
import com.bccapi.legacy.ECKeyManager;

public class AndroidKeyManager implements ECKeyManager {

  private static final String PREFS_NAME = "KeyManagerCache";
  private static final String SEED_NAME = "seed";
  private static final String NUM_KEYS_NAME = "num keys";
  private static final String ACCOUNT_PRIVATE_KEY_NAME = "account pri key";
  private static final String ACCOUNT_PUBLIC_KEY_NAME = "account pub key";
  private static final String BITCOIN_PRIVATE_KEY_NAME = "bitcoin pri key 1";
  private static final String BITCOIN_PUBLIC_KEY_NAME = "bitcoin pub key 1";
  private SharedPreferences mPreferences;
  List<InMemoryPrivateKey> _keys;

  /**
   * Used when restoring a wallet
   * 
   * @param context
   *          Application context
   * @param network
   *          Network to use
   * @param seed
   *          The seed from which to recover
   */
  public AndroidKeyManager(Context context, NetworkParameters network, byte[] seed) {
    mPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    Utils.writeSeed(context, network, seed);
    String seed_string = mPreferences.getString(SEED_NAME, "");
    byte[] cached_seed = Base58.decode(seed_string);
    if (BitUtils.areEqual(seed, cached_seed)) {
      if (initFromPrefs()) {
        return;
      }

    }
    initFromSeed(seed);
  }

  /**
   * Normal initialization
   * 
   * @param context
   *          Application context
   * @param network
   *          Network to use
   */
  public AndroidKeyManager(Context context, NetworkParameters network) {
    mPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    byte[] seed = Utils.readSeed(context, network);
    if (seed == null) {
      seed = Utils.createAndWriteSeed(context, network);
    }
    String seed_string = mPreferences.getString(SEED_NAME, "");
    byte[] cached_seed = Base58.decode(seed_string);
    if (BitUtils.areEqual(seed, cached_seed)) {
      if (initFromPrefs()) {
        return;
      }

    }
    initFromSeed(seed);
  }

  private boolean initFromPrefs() {
    int numKeys = mPreferences.getInt(NUM_KEYS_NAME, 0);
    if (numKeys != 2) {
      return false;
    }
    _keys = new ArrayList<InMemoryPrivateKey>();
    InMemoryPrivateKey accountKey = getKeyFromPrefs(ACCOUNT_PRIVATE_KEY_NAME, ACCOUNT_PUBLIC_KEY_NAME);
    if (accountKey == null) {
      return false;
    }
    _keys.add(accountKey);

    InMemoryPrivateKey bitcoinKey = getKeyFromPrefs(BITCOIN_PRIVATE_KEY_NAME, BITCOIN_PUBLIC_KEY_NAME);
    if (bitcoinKey == null) {
      return false;
    }
    _keys.add(bitcoinKey);

    return true;
  }

  private InMemoryPrivateKey getKeyFromPrefs(String privName, String pubName) {
    String privString = mPreferences.getString(privName, "");
    byte[] privBytes = Base58.decode(privString);
    if (privBytes == null) {
      return null;
    }
    String pubString = mPreferences.getString(pubName, "");
    byte[] pubBytes = Base58.decode(pubString);
    if (pubBytes == null) {
      return null;
    }
    InMemoryPrivateKey priv = new InMemoryPrivateKey(privBytes, pubBytes);
    return priv;
  }

  private void initFromSeed(byte[] seed) {
    Editor editor = mPreferences.edit();
    String seed_string = Base58.encode(seed);
    editor.putString(SEED_NAME, seed_string);
    _keys = new ArrayList<InMemoryPrivateKey>();
    KeyGenerator generator = new KeyGenerator(seed);
    InMemoryPrivateKey account_key = generator.getPrivateKey(0);
    _keys.add(account_key);
    setKeyInPrefs(editor, account_key, ACCOUNT_PRIVATE_KEY_NAME, ACCOUNT_PUBLIC_KEY_NAME);
    InMemoryPrivateKey bitcoin_key = generator.getPrivateKey(1);
    _keys.add(bitcoin_key);
    setKeyInPrefs(editor, bitcoin_key, BITCOIN_PRIVATE_KEY_NAME, BITCOIN_PUBLIC_KEY_NAME);
    editor.putInt(NUM_KEYS_NAME, _keys.size());
    editor.commit();
  }

  private void setKeyInPrefs(Editor editor, InMemoryPrivateKey key, String privName, String pubName) {
    String privKeyString = Base58.encode(key.getPrivateKeyBytes());
    editor.putString(privName, privKeyString);
    String pubKeyString = Base58.encode(key.getPublicKey().getPublicKeyBytes());
    editor.putString(pubName, pubKeyString);
  }

  @Override
  public PublicKey getPublicKey(int keyIndex) {
    if (keyIndex < 0 || keyIndex > _keys.size()) {
      throw new RuntimeException("Invalid key index: " + keyIndex);
    }
    return _keys.get(keyIndex).getPublicKey();
  }

  @Override
  public List<PublicKey> getPublicKeys() {
    List<PublicKey> keys = new ArrayList<PublicKey>(_keys.size());
    for (InMemoryPrivateKey privateKey : _keys) {
      keys.add(privateKey.getPublicKey());
    }
    return keys;
  }

  @Override
  public BitcoinSigner getSigner(int keyIndex) {
    return this.getPrivateKey(keyIndex);
  }

  private static class KeyGenerator extends DeterministicECKeyManager {

    private KeyGenerator(byte[] seed) {
      super(seed);
    }

    public InMemoryPrivateKey getPrivateKey(int index) {
      if (index > _privateKeys.size() - 1) {
        generateKeysForIndex(index);
      }
      return _privateKeys.get(index);
    }

  }

  @Override
  public PrivateKey getPrivateKey(int keyIndex) {
    if (keyIndex < 0 || keyIndex > _keys.size()) {
      throw new RuntimeException("Invalid key index: " + keyIndex);
    }
    return _keys.get(keyIndex);
  }
}
