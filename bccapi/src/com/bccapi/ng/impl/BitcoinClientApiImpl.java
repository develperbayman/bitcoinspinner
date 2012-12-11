package com.bccapi.ng.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.bccapi.bitlib.model.NetworkParameters;
import com.bccapi.bitlib.util.ByteReader;
import com.bccapi.bitlib.util.ByteWriter;
import com.bccapi.ng.api.ApiException;
import com.bccapi.ng.api.ApiObject;
import com.bccapi.ng.api.BitcoinClientApi;
import com.bccapi.ng.api.BroadcastTransactionRequest;
import com.bccapi.ng.api.BroadcastTransactionResponse;
import com.bccapi.ng.api.QueryBalanceRequest;
import com.bccapi.ng.api.QueryBalanceResponse;
import com.bccapi.ng.api.QueryTransactionInventoryRequest;
import com.bccapi.ng.api.QueryTransactionInventoryResponse;
import com.bccapi.ng.api.QueryTransactionSummaryRequest;
import com.bccapi.ng.api.QueryTransactionSummaryResponse;
import com.bccapi.ng.api.QueryUnspentOutputsRequest;
import com.bccapi.ng.api.QueryUnspentOutputsResponse;
import com.bccapi.ng.util.SslUtils;

public class BitcoinClientApiImpl implements BitcoinClientApi {
   private static final String BCCAPI_COM_SSL_THUMBPRINT = "b9:d9:0e:a2:7f:f4:79:3a:2b:54:be:40:ba:cb:56:65:56:13:0c:cc";

   private URL _url;
   private NetworkParameters _network;

   public BitcoinClientApiImpl(URL url, NetworkParameters network) {
      _url = url;
      _network = network;
   }

   @Override
   public NetworkParameters getNetwork() {
      return _network;
   }

   @Override
   public QueryBalanceResponse queryBalance(QueryBalanceRequest request) throws ApiException {
      HttpURLConnection connection = sendRequest(request, "queryBalance");
      return receiveResponse(QueryBalanceResponse.class, connection);
   }

   @Override
   public QueryUnspentOutputsResponse queryUnspentOutputs(QueryUnspentOutputsRequest request) throws ApiException {
      HttpURLConnection connection = sendRequest(request, "queryUnspentOutputs");
      return receiveResponse(QueryUnspentOutputsResponse.class, connection);
   }

   @Override
   public BroadcastTransactionResponse broadcastTransaction(BroadcastTransactionRequest request) throws ApiException {
      HttpURLConnection connection = sendRequest(request, "broadcastTransaction");
      return receiveResponse(BroadcastTransactionResponse.class, connection);
   }

   private HttpURLConnection sendRequest(ApiObject request, String function) throws ApiException {
      try {
         HttpURLConnection connection = getHttpConnection(function);
         byte[] toSend = request.serialize(new ByteWriter(1024)).toBytes();
         connection.setRequestProperty("Content-Length", String.valueOf(toSend.length));
         connection.getOutputStream().write(toSend);
         int status = connection.getResponseCode();
         if (status != 200) {
            throw new ApiException(BitcoinClientApi.ERROR_CODE_UNEXPECTED_SERVER_RESPONSE, "Unexpected status code: "
                  + status);
         }
         int contentLength = connection.getContentLength();
         if (contentLength == -1) {
            throw new ApiException(BitcoinClientApi.ERROR_CODE_UNEXPECTED_SERVER_RESPONSE, "Invalid content-length");
         }
         return connection;
      } catch (IOException e) {
         throw new ApiException(BitcoinClientApi.ERROR_CODE_COMMUNICATION_ERROR, e.getMessage());
      }
   }

   private <T> T receiveResponse(Class<T> klass, HttpURLConnection connection) throws ApiException {
      try {
         int contentLength = connection.getContentLength();
         byte[] received = readBytes(contentLength, connection.getInputStream());
         T response = ApiObject.deserialize(klass, new ByteReader(received));
         return response;
      } catch (IOException e) {
         throw new ApiException(BitcoinClientApi.ERROR_CODE_COMMUNICATION_ERROR, e.getMessage());
      }
   }

   private byte[] readBytes(int size, InputStream inputStream) throws IOException {
      byte[] bytes = new byte[size];
      int index = 0;
      int toRead;
      while ((toRead = size - index) > 0) {
         int read = inputStream.read(bytes, index, toRead);
         if (read == -1) {
            throw new IOException();
         }
         index += read;
      }
      return bytes;
   }

   private HttpURLConnection getHttpConnection(String function) throws IOException {
      StringBuilder sb = new StringBuilder();
      String spec = sb.append("/ng/").append(function).toString();
      URL url = new URL(_url, spec);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      SslUtils.configureTrustedCertificate(connection, BCCAPI_COM_SSL_THUMBPRINT);
      connection.setReadTimeout(60000);
      connection.setDoInput(true);
      connection.setDoOutput(true);
      return connection;
   }

   @Override
   public QueryTransactionInventoryResponse queryTransactionInventory(QueryTransactionInventoryRequest request)
         throws ApiException {
      HttpURLConnection connection = sendRequest(request, "queryTransactionInventory");
      return receiveResponse(QueryTransactionInventoryResponse.class, connection);
   }

   @Override
   public QueryTransactionSummaryResponse queryTransactionSummary(QueryTransactionSummaryRequest request)
         throws ApiException {
      HttpURLConnection connection = sendRequest(request, "queryTransactionSummary");
      return receiveResponse(QueryTransactionSummaryResponse.class, connection);
   }

}
