package com.bccapi.api;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.bccapi.core.Account;
import com.bccapi.core.BitUtils;
import com.bccapi.core.CoinUtils;

/**
 * AccountInfo holds information about an account such as the current balance
 * and number of keys monitored. Instances are retrieved from the BCCAPI server
 * by calling {@link Account#getInfo}.
 */
public class AccountInfo {

   private int _keys;
   private long _availableBalance;
   private long _estimatedBalance;

   public AccountInfo(int keys, long availableBalance, long estimatedBalance) {
      _keys = keys;
      _availableBalance = availableBalance;
      _estimatedBalance = estimatedBalance;
   }

   /**
    * Get the number of keys being monitored by the BCCAPI server.
    * 
    * @return The number of keys being monitored by the BCCAPI server.
    */
   public int getKeys() {
      return _keys;
   }

   /**
    * Get the balance available to this account. All funds are confirmed by at
    * least one block, and can be used for spending.
    * 
    * @return The balance available to this account.
    */
   public long getAvailableBalance() {
      return _availableBalance;
   }

   /**
    * Get the estimated balance of this account. This value is what you would
    * expect to see in a UI.
    * <p>
    * The estimated balance of an account is the available balance plus the
    * change from previous transactions from this account which has not yet been
    * confirmed by any blocks. Note that the change from previous unconfirmed
    * transactions cannot be sent to other accounts until they have been
    * confirmed by a block.
    * <p>
    * Note that estimated balance does not contain unconfirmed transactions sent
    * to this account by another account.
    * 
    * @return The estimated balance of this account.
    */
   public long getEstimatedBalance() {
      return _estimatedBalance;
   }

   @Override
   public String toString() {
      return "Keys: " + getKeys() + " Balance: " + CoinUtils.valueString(getEstimatedBalance()) + " ("
            + CoinUtils.valueString(getAvailableBalance()) + ")";
   }

   /**
    * Deserialize an {@link AccountInfo} from a {@link DataInputStream}.
    * 
    * @param stream
    *           The {@link DataInputStream} to deserialize from.
    * @return A {@link AccountInfo}.
    * @throws IOException
    */
   public static AccountInfo fromStream(DataInputStream stream) throws IOException {
      int keys = (int) BitUtils.uint32FromStream(stream);
      long available = BitUtils.uint64FromStream(stream);
      long estimated = BitUtils.uint64FromStream(stream);
      return new AccountInfo(keys, available, estimated);
   }

   /**
    * Serialize this {@link AccountInfo} instance.
    * 
    * @param out
    *           The output stream to serialize to.
    * @throws IOException
    */
   public void toStream(OutputStream out) throws IOException {
      BitUtils.uint32ToStream(_keys, out);
      BitUtils.uint64ToStream(_availableBalance, out);
      BitUtils.uint64ToStream(_estimatedBalance, out);
   }

}
