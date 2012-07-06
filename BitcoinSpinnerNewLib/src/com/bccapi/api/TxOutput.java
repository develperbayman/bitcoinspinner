package com.bccapi.api;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.bccapi.core.BitUtils;
import com.bccapi.core.CompactInt;

/**
 * A transaction output.
 */
public class TxOutput {

   long _value;
   byte[] _script;

   /**
    * Construct a transaction output.
    * 
    * @param value
    *           The bitcoin value in satoshis (1 satoshi = 0.000000001 BTC).
    * @param script
    *           The output script.
    */
   public TxOutput(long value, byte[] script) {
      _value = value;
      _script = script;
   }

   /**
    * Copy construct a transaction output.
    * 
    * @param other
    *           The transaction output to copy
    */
   public TxOutput(TxOutput other) {
      _value = other._value;
      _script = BitUtils.copyByteArray(other._script);
   }

   /**
    * Get the bitcoin value in satoshis (1 satoshi = 0.000000001 BTC).
    * 
    * @return The bitcoin value in satoshis (1 satoshi = 0.000000001 BTC).
    */
   public long getValue() {
      return _value;
   }

   /**
    * Get the output script.
    * 
    * @return The output script.
    */
   public byte[] getScript() {
      return _script;
   }

   /**
    * Deserialize a {@link TxOutput} from a {@link DataInputStream}.
    * 
    * @param stream
    *           The {@link DataInputStream} to deserialize from.
    * @return A {@link TxOutput}.
    * @throws IOException
    */
   public static TxOutput fromStream(DataInputStream stream) throws IOException {
      long value = BitUtils.uint64FromStream(stream);
      byte[] script = new byte[(int) CompactInt.fromStream(stream)];
      stream.readFully(script);
      return new TxOutput(value, script);
   }

   /**
    * Serialize this {@link TxOutput} instance.
    * 
    * @param stream
    *           the stream to serialize to.
    * @throws IOException
    */
   public void toStream(OutputStream stream) throws IOException {
      BitUtils.uint64ToStream(_value, stream);
      CompactInt.toStream(_script.length, stream);
      stream.write(_script);
   }

}
