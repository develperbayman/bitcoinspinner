package com.bccapi.ng.api;

import java.util.HashSet;
import java.util.Set;

import com.bccapi.bitlib.model.Script.ScriptParsingException;
import com.bccapi.bitlib.model.UnspentTransactionOutput;
import com.bccapi.bitlib.util.ByteReader;
import com.bccapi.bitlib.util.ByteReader.InsufficientBytesException;
import com.bccapi.bitlib.util.ByteWriter;

public class QueryUnspentOutputsResponse extends ApiObject {

   public Set<UnspentTransactionOutput> unspent;
   public Set<UnspentTransactionOutput> change;
   public Set<UnspentTransactionOutput> receiving;

   /**
    * Current height of the block chain.
    */
   public int chainHeight;

   public QueryUnspentOutputsResponse(Set<UnspentTransactionOutput> unspent, Set<UnspentTransactionOutput> change,
         Set<UnspentTransactionOutput> receiving, int chainHeight) {
      this.unspent = unspent;
      this.change = change;
      this.receiving = receiving;
      this.chainHeight = chainHeight;
   }

   protected QueryUnspentOutputsResponse(ByteReader reader) throws InsufficientBytesException, ApiException {
      try {
         unspent = setFromReader(reader);
         change = setFromReader(reader);
         receiving = setFromReader(reader);
      } catch (ScriptParsingException e) {
         throw new ApiException(BitcoinClientApi.ERROR_CODE_INVALID_SERVER_RESPONSE,
               "Invalid script returned by server");
      }
      chainHeight = reader.getIntLE();
      // Payload may contain more, but we ignore it for forwards
      // compatibility
   }

   private Set<UnspentTransactionOutput> setFromReader(ByteReader reader) throws InsufficientBytesException,
         ScriptParsingException {
      int size = reader.getIntLE();
      Set<UnspentTransactionOutput> set = new HashSet<UnspentTransactionOutput>();
      for (int i = 0; i < size; i++) {
         set.add(new UnspentTransactionOutput(reader));
      }
      return set;
   }

   private void setToWriter(Set<UnspentTransactionOutput> set, ByteWriter writer) {
      writer.putIntLE(set.size());
      for (UnspentTransactionOutput output : set) {
         output.toByteWriter(writer);
      }
   }

   @Override
   protected ByteWriter toByteWriter(ByteWriter writer) {
      setToWriter(unspent, writer);
      setToWriter(change, writer);
      setToWriter(receiving, writer);
      writer.putIntLE(chainHeight);
      return writer;
   }

   @Override
   protected byte getType() {
      return ApiObject.UNSPENT_OUTPUTS_RESPONSE_TYPE;
   }

}
