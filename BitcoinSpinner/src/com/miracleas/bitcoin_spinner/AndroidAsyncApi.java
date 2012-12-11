package com.miracleas.bitcoin_spinner;

import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;

import com.bccapi.bitlib.crypto.PublicKeyRing;
import com.bccapi.bitlib.model.Address;
import com.bccapi.bitlib.util.ByteReader;
import com.bccapi.bitlib.util.ByteWriter;
import com.bccapi.bitlib.util.HashUtils;
import com.bccapi.bitlib.util.HexUtils;
import com.bccapi.bitlib.util.Sha256Hash;
import com.bccapi.ng.api.ApiException;
import com.bccapi.ng.api.ApiObject;
import com.bccapi.ng.api.Balance;
import com.bccapi.ng.api.BitcoinClientApi;
import com.bccapi.ng.api.TransactionSummary;
import com.bccapi.ng.async.ApiCache;
import com.bccapi.ng.async.AsynchronousApi;
import com.bccapi.ng.async.CallbackRunnerInvoker;

public class AndroidAsyncApi extends AsynchronousApi {

  public AndroidAsyncApi(PublicKeyRing keyRing, BitcoinClientApi api, Context applicationContext) {
    super(keyRing, api, new AndroidApiCache(applicationContext));
  }

  public static class HandlerRunnerPoster implements CallbackRunnerInvoker {

    Handler _handler;

    private HandlerRunnerPoster() {
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

  private static class AndroidApiCache implements ApiCache {

    private static final String BALANCE = "Balance";
    private static final String ADDRESS_IDENTIFIER = "AddressIdentifier";
    private static final String TRANSACTION_KEY_PREFIX = "tx:";

    private SharedPreferences _preferences;

    public AndroidApiCache(Context applicationContext) {
      _preferences = applicationContext.getSharedPreferences("ApiCache", Context.MODE_PRIVATE);
    }

    @Override
    public void cacheBalance(List<Address> addresses, Balance balance) {
      String identifier = getAddressListIdentifier(addresses);
      String serializedBalance = serialize(balance);
      Editor editor = _preferences.edit();
      editor.putString(ADDRESS_IDENTIFIER, identifier);
      editor.putString(BALANCE, serializedBalance);
      editor.commit();
    }

    @Override
    public Balance getBalance(List<Address> addresses) {
      String identifier = getAddressListIdentifier(addresses);
      String storedIdentifier = _preferences.getString(ADDRESS_IDENTIFIER, "");
      if (!identifier.equals(storedIdentifier)) {
        return null;
      }
      return deserialize(Balance.class, _preferences.getString(BALANCE, ""));
    }

    @Override
    public void cacheTransactionSummary(TransactionSummary transaction) {
      String key = getTransactionSummaryKey(transaction.hash);
      String value = serialize(transaction);
      Editor editor = _preferences.edit();
      editor.putString(key, value);
      editor.commit();
    }

    @Override
    public boolean hasTransactionSummary(Sha256Hash hash) {
      String key = getTransactionSummaryKey(hash);
      return _preferences.contains(key);
    }

    @Override
    public TransactionSummary getTransactionSummary(Sha256Hash hash) {
      String key = getTransactionSummaryKey(hash);
      String value = _preferences.getString(key, "");
      return deserialize(TransactionSummary.class, value);
    }

    private String getTransactionSummaryKey(Sha256Hash hash) {
      return new StringBuilder().append(TRANSACTION_KEY_PREFIX).append(hash).toString();
    }

    /**
     * Get a string that uniquely identifies this list of addresses
     */
    private String getAddressListIdentifier(List<Address> addresses) {
      ByteWriter writer = new ByteWriter(1024);
      for (Address address : addresses) {
        writer.putBytes(address.getAllAddressBytes());
      }
      return new Sha256Hash(HashUtils.sha256(writer.toBytes())).toString();
    }

    private <T> T deserialize(Class<T> klass, String serialized) {
      byte[] bytes;
      try {
        bytes = HexUtils.toBytes(serialized);
        if (bytes == null) {
          return null;
        }
      } catch (Exception e) {
        return null;
      }
      try {
        return (T) ApiObject.deserialize(klass, new ByteReader(bytes));
      } catch (ApiException e) {
        return null;
      }
    }

    private String serialize(ApiObject obj) {
      ByteWriter writer = new ByteWriter(1024);
      obj.serialize(writer);
      return HexUtils.toHex(writer.toBytes());
    }

  }

}
