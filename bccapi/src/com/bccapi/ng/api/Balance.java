package com.bccapi.ng.api;

import com.bccapi.bitlib.util.ByteReader;
import com.bccapi.bitlib.util.ByteReader.InsufficientBytesException;
import com.bccapi.bitlib.util.ByteWriter;

/**
 * This class gives an overview of the balance of a set of bitcoin addresses.
 * <p>
 * The sum of funds that are available for spending depends on the wallet mode:
 * <ul>
 * <li>1) Spend only confirmed: unspet (Bitcoin Android)</li>
 * <li>2) Spend confirmed and change-sent-to-self: unspent + pendingChange
 * (Satoshi client, BitoinSpinner)</li>
 * <li>3) Spend confirmed, change-sent-to-self, and receiving: unspent +
 * pendingChange + pendingReceiving (SatoshiDice)</li>
 * </ul>
 */
public class Balance extends ApiObject {

   /**
    * The sum of the unspent outputs which are confirmed and currently not spent
    * in pending transactions.
    */
   public long unspent;

   /**
    * The sum of the outputs which are being received as part of pending
    * transactions from foreign addresses.
    */
   public long pendingReceiving;

   /**
    * The sum of outputs currently being sent from the address set.
    */
   public long pendingSending;

   /**
    * The sum of the outputs being sent from the address set to itself
    */
   public long pendingChange;

   public Balance(long unspent, long pendingReceiving, long pendingSending, long pendingChange) {
      this.unspent = unspent;
      this.pendingReceiving = pendingReceiving;
      this.pendingSending = pendingSending;
      this.pendingChange = pendingChange;
   }

   protected Balance(ByteReader reader) throws InsufficientBytesException {
      unspent = reader.getLongLE();
      pendingReceiving = reader.getLongLE();
      pendingSending = reader.getLongLE();
      pendingChange = reader.getLongLE();
      // Payload may contain more, but we ignore it for forwards
      // compatibility
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Unspent: ").append(unspent);
      sb.append(" Receiving: ").append(pendingReceiving);
      sb.append(" Sending: ").append(pendingSending);
      sb.append(" Change: ").append(pendingChange);
      return sb.toString();
   }

   @Override
   protected ByteWriter toByteWriter(ByteWriter writer) {
      writer.putLongLE(unspent);
      writer.putLongLE(pendingReceiving);
      writer.putLongLE(pendingSending);
      writer.putLongLE(pendingChange);
      return writer;
   }

   @Override
   public byte getType() {
      return ApiObject.BALANCE_TYPE;
   }

}
