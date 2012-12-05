package com.bccapi.api;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.bccapi.core.BitUtils;
import com.bccapi.core.CompactInt;

/**
 * A transaction input
 */
public class TxInput {

   private byte[] _outPointHash;
   private long _outPointIndex;
   private byte[] _script;
   private long _sequence;

   /**
    * Construct a transaction input.
    * 
    * @param outPointHash
    *           The hash of transaction funding the input
    * @param outPointIndex
    *           The index of the output in the funding transaction.
    * @param script
    *           The script of the input.
    * @param sequence
    *           The sequence number of the input.
    */
   public TxInput(byte[] outPointHash, long outPointIndex, byte[] script, long sequence) {
      _outPointHash = outPointHash;
      _outPointIndex = outPointIndex;
      _script = script;
      _sequence = sequence;
   }

   /**
    * Copy construct a transaction input.
    * 
    * @param other
    *           The transaction input to copy
    */
   public TxInput(TxInput other) {
      _outPointHash = BitUtils.copyByteArray(other._outPointHash);
      _outPointIndex = other._outPointIndex;
      _script = BitUtils.copyByteArray(other._script);
      _sequence = other._sequence;
   }

   /**
    * Get the hash of transaction funding the input.
    * 
    * @return The hash of transaction funding the input.
    */
   public byte[] getOutPointHash() {
      return _outPointHash;
   }

   /**
    * Get the index of the output in the funding transaction.
    * 
    * @return The index of the output in the funding transaction.
    */
   public long getOutPointIndex() {
      return _outPointIndex;
   }

   /**
    * Get the script of the input.
    * 
    * @return The script of the input.
    */
   public byte[] getScript() {
      return _script;
   }

   /**
    * Set the script of the input.
    * 
    * @param script
    */
   public void setScript(byte[] script) {
      _script = script;
   }

   /**
    * Get the sequence number of the input.
    * 
    * @return The sequence number of the input.
    */
   public long getSequence() {
      return _sequence;
   }

   /**
    * Deserialize a {@link TxInput} from a {@link DataInputStream}.
    * 
    * @param stream
    *           The {@link DataInputStream} to deserialize from.
    * @return A {@link TxInput}.
    * @throws IOException
    */
   public static TxInput fromStream(DataInputStream stream) throws IOException {
      byte[] outPointHash = new byte[32];
      stream.readFully(outPointHash);
      long index = BitUtils.uint32FromStream(stream);
      byte[] script = new byte[(int) CompactInt.fromStream(stream)];
      stream.readFully(script);
      long sequence = BitUtils.uint32FromStream(stream);
      return new TxInput(outPointHash, index, script, sequence);
   }

   /**
    * Serialize this {@link TxInput} instance.
    * 
    * @param out
    *           the output stream to serialize to.
    * @throws IOException
    */
   public void toStream(OutputStream out) throws IOException {
      out.write(_outPointHash);
      BitUtils.uint32ToStream(_outPointIndex, out);
      CompactInt.toStream(_script.length, out);
      out.write(_script);
      BitUtils.uint32ToStream(_sequence, out);
   }

}
