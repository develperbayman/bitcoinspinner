package com.bccapi.ng.util;

import java.util.HashSet;
import java.util.Set;

import com.bccapi.bitlib.model.Address;
import com.bccapi.ng.api.TransactionSummary;
import com.bccapi.ng.api.TransactionSummary.Item;

public class TransactionSummaryUtils {

   private static final String[] EMPTY_STRING_ARRAY = new String[0];

   public enum TransactionType {
      ReceivedFromOthers, SentToOthers, SentToSelf
   };

   /**
    * Determine whether a transaction is a send, receive, or send to self
    * 
    * @param transaction
    *           The transaction
    * @param addresses
    *           The set of addresses owned by us \
    */
   public static TransactionType getTransactionType(TransactionSummary transaction, Set<Address> addresses) {
      if (areAllSendersMe(transaction, addresses)) {
         // Either sent to others or sent to self
         if (areAllReceiversMe(transaction, addresses)) {
            // Sent to self
            return TransactionType.SentToSelf;
         } else {
            // Sent to others
            return TransactionType.SentToOthers;
         }
      } else {
         // Received from others
         return TransactionType.ReceivedFromOthers;
      }
   }

   private static boolean areAllSendersMe(TransactionSummary transaction, Set<Address> addresses) {
      for (Item item : transaction.inputs) {
         if (!addresses.contains(item.address)) {
            return false;
         }
      }
      return true;
   }

   private static boolean areAllReceiversMe(TransactionSummary transaction, Set<Address> addresses) {
      for (Item item : transaction.outputs) {
         if (!addresses.contains(item.address)) {
            return false;
         }
      }
      return true;
   }

   /**
    * Calculate how the balance of a set of addresses is affected by this
    * transaction
    * 
    * @param transaction
    *           The transaction
    * @param addresses
    *           The addresses owned by us
    * @return the sum of satoshis that have been spent/received by our addresses
    *         in this transaction
    */
   public static long calculateBalanceChange(TransactionSummary transaction, Set<Address> addresses) {
      long in = 0, out = 0;
      for (Item item : transaction.inputs) {
         if (addresses.contains(item.address)) {
            in += item.value;
         }
      }
      for (Item item : transaction.outputs) {
         if (addresses.contains(item.address)) {
            out += item.value;
         }
      }
      return out - in;
   }

   public static String[] getReceiversNotMe(TransactionSummary transaction, Set<Address> addresses) {
      Set<String> receivers = new HashSet<String>();
      for (Item item : transaction.outputs) {
         if (!addresses.contains(item.address)) {
            receivers.add(item.address.toString());
         }
      }
      return receivers.toArray(EMPTY_STRING_ARRAY);
   }

   public static String[] getSenders(TransactionSummary transaction) {
      Set<String> senders = new HashSet<String>();
      for (Item item : transaction.inputs) {
         senders.add(item.address.toString());
      }
      return senders.toArray(EMPTY_STRING_ARRAY);
   }

}
