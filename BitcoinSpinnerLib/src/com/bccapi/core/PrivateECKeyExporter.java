package com.bccapi.core;

import com.bccapi.api.Network;

/**
 * Extension of the {@link PrivateECKey}, which allows retrieving the private
 * key in plain text. Use with caution.
 */
public class PrivateECKeyExporter extends PrivateECKey {

   /**
    * Construct based on a {@link PrivateECKey}.
    * 
    * @param key
    *           The key to use.
    */
   public PrivateECKeyExporter(PrivateECKey key) {
      super(key);
   }

   /**
    * Get the bytes of the private key.
    * 
    * @return The bytes of the private key.
    */
   public byte[] getPrivateKeyBytes() {
      byte[] result = new byte[32];
      byte[] bytes = _privateKey.toByteArray();
      if (bytes.length <= result.length) {
         System.arraycopy(bytes, 0, result, result.length - bytes.length, bytes.length);
      } else {
         // This happens if the most significant bit is set and we have an
         // extra leading zero to avoid a negative BigInteger
         assert bytes.length == 33 && bytes[0] == 0;
         System.arraycopy(bytes, 1, result, 0, bytes.length - 1);
      }
      return result;
   }

   /**
    * Get the private key as a base-58 encoded key.
    * 
    * @param network
    *           The network parameters to use
    * @return The private key as a base-58 encoded key.
    */
   public String getBase58EncodedKey(Network network) {
      byte[] toEncode = new byte[1 + 32 + 4];
      // Set network
      toEncode[0] = network == Network.productionNetwork ? (byte) 0x80 : (byte) 0xEF;
      // Set key bytes
      byte[] keyBytes = getPrivateKeyBytes();
      System.arraycopy(keyBytes, 0, toEncode, 1, keyBytes.length);
      // Set checksum
      byte[] checkSum = HashUtils.doubleSha256(toEncode, 0, 1 + 32);
      System.arraycopy(checkSum, 0, toEncode, 1 + 32, 4);
      // Encode
      return Base58.encode(toEncode);
   }

}
