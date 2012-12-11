package com.bccapi.ng.api;

import com.bccapi.bitlib.model.NetworkParameters;

/**
 * The Bitcoin Client API interface. This interface describes all the functions
 * implemented by the BCCAPI server.
 */
public interface BitcoinClientApi {

   public static final int ERROR_CODE_PARSER_ERROR = 1;
   public static final int ERROR_CODE_UNKNOWN_TYPE = 2;
   public static final int ERROR_CODE_COMMUNICATION_ERROR = 3;
   public static final int ERROR_CODE_UNEXPECTED_SERVER_RESPONSE = 4;
   public static final int ERROR_CODE_INVALID_SERVER_RESPONSE = 5;
   public static final int ERROR_CODE_INVALID_REQUEST = 6;

   /**
    * The maximal number of addresses allowed per request.
    */
   public static final int MAXIMUM_ADDRESSES_PER_REQUEST = 10;

   /**
    * Get the network used by this API instance.
    * 
    * @return The network used, test network or production network.
    */
   public NetworkParameters getNetwork();

   /**
    * Query the balance of a set of Bitcoin addresses.
    * <p>
    * No more than {@link MAXIMUM_ADDRESSES_PER_REQUEST} addresses can be
    * queried at a time.
    * 
    * @param request
    *           a {@link QueryBalanceRequest} containing the set of addresses to
    *           query
    * @return a {@link QueryBalanceResponse}.
    * @throws ApiException
    */
   public QueryBalanceResponse queryBalance(QueryBalanceRequest request) throws ApiException;

   /**
    * Query the unspent outputs of a set of Bitcoin addresses.
    * <p>
    * No more than {@link MAXIMUM_ADDRESSES_PER_REQUEST} addresses can be
    * queried at a time.
    * 
    * @param request
    *           a {@link QueryUnspentOutputsRequest} containing the set of
    *           addresses to query
    * @return a {@link QueryUnspentOutputsResponse}.
    * @throws ApiException
    */
   public QueryUnspentOutputsResponse queryUnspentOutputs(QueryUnspentOutputsRequest request) throws ApiException;

   /**
    * Query the transaction inventory of a set of Bitcoin addresses.
    * <p>
    * No more than {@link QueryTransactionInventoryRequest#MAXIMUM} transaction
    * IDs can be queried at a time.
    * 
    * @param request
    *           a {@link QueryTransactionInventoryRequest} containing the set of
    *           addresses to query
    * @return a {@link QueryTransactionInventoryResponse}.
    * @throws ApiException
    */
   public QueryTransactionInventoryResponse queryTransactionInventory(QueryTransactionInventoryRequest request)
         throws ApiException;

   /**
    * Query the transaction summary for a list of transaction IDs.
    * <p>
    * No more than {@link QueryTransactionSummaryRequest#MAXIMUM_TRANSACTIONS}
    * transaction IDs can be queried at a time.
    * 
    * @param request
    *           a {@link QueryTransactionSummaryRequest} containing the set of
    *           transaction IDs to query
    * @return a {@link QueryTransactionSummaryResponse}.
    * @throws ApiException
    */
   public QueryTransactionSummaryResponse queryTransactionSummary(QueryTransactionSummaryRequest request)
         throws ApiException;

   /**
    * Broadcast a Bitcoin transaction. The server will validate that the
    * transaction conforms to transaction integrity rules, is funded by unspent
    * transaction outputs, and has been signed appropriately before broadcasting
    * it to the bitcoin network
    * 
    * @param request
    *           a {@link BroadcastTransactionRequest} containing the transaction
    *           to broadcast.
    * @return a {@link BroadcastTransactionResponse}.
    * @throws ApiException
    */
   public BroadcastTransactionResponse broadcastTransaction(BroadcastTransactionRequest request) throws ApiException;
}
