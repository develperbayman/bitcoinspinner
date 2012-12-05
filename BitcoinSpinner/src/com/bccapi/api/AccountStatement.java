package com.bccapi.api;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.bccapi.core.Account;
import com.bccapi.core.BitUtils;
import com.bccapi.core.CompactInt;

/**
 * AccountStatement contains a detailed list of transaction records that send
 * bitcoins to and from a wallet account and the accounts current balance.
 * Instances are retrieved from the BCCAPI server by calling
 * {@link Account#getStatement}.
 */
public class AccountStatement {

   /**
    * Represents a transaction that occurred on an account.
    */
   public static class Record {

      /**
       * Specifies the type of a {@link Record}.
       */
      public enum Type {

         /**
          * The record represents a transaction that spends coins from the
          * account.
          */
         Sent,
         /**
          * The record represents a transaction that receives coins to the
          * account.
          */
         Received,
         /**
          * The record represents a transaction that sends coins from the
          * account to the account itself.
          */
         SentToSelf;

         private static final int SENT = 1;
         private static final int RECEIVED = 2;
         private static final int SENTTOSELF = 3;

         /**
          * Deserialize a {@link Type} from a {@link DataInputStream}.
          * 
          * @param stream
          *           The {@link DataInputStream} to deserialize from.
          * @return A {@link Type}.
          * @throws IOException
          */
         public static Type fromStream(DataInputStream stream) throws IOException {
            int type = stream.readByte();
            switch (type) {
            case SENT:
               return Type.Sent;
            case RECEIVED:
               return Type.Received;
            case SENTTOSELF:
               return Type.SentToSelf;
            default:
               return Type.Sent;
            }
         }

         /**
          * Serialize this {@link Type} instance.
          * 
          * @param out
          *           The output stream to serialize to.
          * @throws IOException
          */
         public void toStream(OutputStream out) throws IOException {
            if (this == Type.Sent) {
               out.write(SENT);
            } else if (this == Type.Received) {
               out.write(RECEIVED);
            } else if (this == Type.SentToSelf) {
               out.write(SENTTOSELF);
            }
         }
      }

      private int _index;
      private int _confirmations;
      private long _date;
      private String _addresses;
      private long _amount;
      private Type _type;

      public Record(int index, int confirmations, long date, Type type, String addresses, long amount) {
         _index = index;
         _confirmations = confirmations;
         _date = date;
         _addresses = addresses;
         _amount = amount;
         _type = type;
      }

      /**
       * Get the unique index of this record for the given account.
       * 
       * @return The unique index of this record for the given account.
       */
      public int getIndex() {
         return _index;
      }

      /**
       * Get the number of confirmations on the transaction that this record
       * represents.
       * 
       * @return The number of confirmations on the transaction that this record
       *         represents.
       */
      public int getConfirmations() {
         return _confirmations;
      }

      /**
       * Get the date at which the transaction occurred.
       * 
       * @return The date at which the transaction occurred.
       */
      public long getDate() {
         return _date;
      }

      /**
       * Get the addresses of the transaction that this record represents.
       * <p>
       * If this record represents a transaction that spends coins the addresses
       * will be a comma separated list of the recipient's receiving addresses.
       * <p>
       * If this record represents a transaction that receives coins the
       * addresses will be a comma separated list of your receiving addresses.
       * <p>
       * If this record represents a transaction that was sent to yourself the
       * addresses will be a comma separated list of your receiving addresses.
       * 
       * @return Addresses of this transaction.
       */
      public String getAddresses() {
         return _addresses;
      }

      /**
       * Get the amount of bitcoins in satoshis going to or from the account for
       * this record. A positive value indicates that coins were sent to the
       * account. A negative value indicates that coins were spent. Note that
       * the amount may be negative for records of the type
       * {@link Type#SentToSelf} as the amount also includes any transaction fee
       * paid.
       * 
       * @return The amount going to or from the account
       */
      public long getAmount() {
         return _amount;
      }

      /**
       * Get the type of the record.
       * <p>
       * The type {@link Type#Sent} is used for records that represent a
       * transaction that spends coins from your wallet.
       * <p>
       * The type {@link Type#Received} is used for records that represent a
       * transaction that sends coins from your wallet.
       * <p>
       * The type {@link Type#SentToSelf} is use for records that represent a
       * transaction that sends coins from your wallet to itself.
       * <p>
       * 
       * @return The type of the record.
       */
      public Type getType() {
         return _type;
      }

      /**
       * Deserialize a {@link Record} from a {@link DataInputStream}.
       * 
       * @param stream
       *           The {@link DataInputStream} to deserialize from.
       * @return A {@link Record}.
       * @throws IOException
       */
      public static Record fromStream(DataInputStream stream) throws IOException {
         int recordIndex = (int) CompactInt.fromStream(stream);
         int confirmations = (int) CompactInt.fromStream(stream);
         long date = BitUtils.uint64FromStream(stream);
         Type type = Type.fromStream(stream);
         byte[] bytes = new byte[(int) CompactInt.fromStream(stream)];
         stream.readFully(bytes);
         String addresses = new String(bytes);
         long credit = BitUtils.uint64FromStream(stream);
         return new Record(recordIndex, confirmations, date, type, addresses, credit);
      }

      /**
       * Serialize this {@link Record} instance.
       * 
       * @param out
       *           the output stream to serialize to.
       * @throws IOException
       */
      public void toStream(OutputStream out) throws IOException {
         CompactInt.toStream(_index, out);
         CompactInt.toStream(_confirmations, out);
         BitUtils.uint64ToStream(_date, out);
         _type.toStream(out);
         byte[] bytes = _addresses.getBytes();
         CompactInt.toStream(bytes.length, out);
         out.write(bytes);
         BitUtils.uint64ToStream(_amount, out);
      }

   }

   private AccountInfo _info;
   private int _totalRecords;
   private List<Record> _records;

   public AccountStatement(AccountInfo info, int totalRecords, List<Record> records) {
      _info = info;
      _totalRecords = totalRecords;
      _records = records;
   }

   /**
    * Get the {@link AccountInfo} for this statement.
    * 
    * @return The {@link AccountInfo} for this statement.
    */
   public AccountInfo getInfo() {
      return _info;
   }

   /**
    * Get the total number of records registered for this account.
    * 
    * @return The total number of records registered for this account.
    */
   public int getTotalRecordCount() {
      return _totalRecords;
   }

   /**
    * Get the list of records for this statement.
    * 
    * @return The list of records for this statement.
    */
   public List<Record> getRecords() {
      return _records;
   }

   /**
    * Deserialize an {@link AccountStatement} from a {@link DataInputStream}.
    * 
    * @param stream
    *           The {@link DataInputStream} to deserialize from.
    * @return A {@link AccountStatement}.
    * @throws IOException
    */
   public static AccountStatement fromStream(DataInputStream stream) throws IOException {
      AccountInfo info = AccountInfo.fromStream(stream);
      int totalRecords = (int) CompactInt.fromStream(stream);
      int numRecords = (int) CompactInt.fromStream(stream);
      List<Record> records = new ArrayList<Record>(numRecords);
      for (int i = 0; i < numRecords; i++) {
         records.add(Record.fromStream(stream));
      }
      return new AccountStatement(info, totalRecords, records);
   }

   /**
    * Serialize this {@link AccountStatement} instance.
    * 
    * @param out
    *           the output stream to serialize to.
    * @throws IOException
    */
   public void toStream(OutputStream out) throws IOException {
      _info.toStream(out);
      CompactInt.toStream(_totalRecords, out);
      CompactInt.toStream(_records.size(), out);
      for (Record r : _records) {
         r.toStream(out);
      }
   }

}
