package com.bccapi.ng.async;

import java.util.List;

import com.bccapi.bitlib.model.Address;
import com.bccapi.bitlib.util.Sha256Hash;
import com.bccapi.ng.api.Balance;
import com.bccapi.ng.api.TransactionSummary;

public interface ApiCache {

   /**
    * Store a balance in the cache for fast off-line retrieval
    */
   void cacheBalance(List<Address> addresses, Balance balance);

   /**
    * Retrieve a previously cached balance or null if no balance was ever
    * cached.
    */
   Balance getBalance(List<Address> addresses);

   /**
    * Store a transaction summary in the cache for fast off-line retrieval.
    */
   void cacheTransactionSummary(TransactionSummary transaction);

   /**
    * Determine whether the cache contains a transaction summary with a specific
    * transaction hash.
    */
   boolean hasTransactionSummary(Sha256Hash hash);

   /**
    * Get a transaction summary with a specific transaction hash, or null if
    * none is found.
    */
   TransactionSummary getTransactionSummary(Sha256Hash hash);
}
