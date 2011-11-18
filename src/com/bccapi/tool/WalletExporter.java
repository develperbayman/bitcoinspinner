package com.bccapi.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;

import com.bccapi.api.Network;
import com.bccapi.core.AddressUtil;
import com.bccapi.core.Base58;
import com.bccapi.core.DeterministicECKeyManager;
import com.bccapi.core.HashUtils;
import com.bccapi.core.PrivateECKey;
import com.bccapi.core.SeedManager;

/**
 * Utility program for exporting private keys. Once the official bitcoin client
 * supports private key imports, the format that this utility exports will be
 * changed to match it.
 */
public class WalletExporter {

   /**
    * Extension of the {@link DeterministicECKeyManager}, which supports
    * exporting private keys in plain text.
    */
   private static class DeterministicECKeyExporter extends DeterministicECKeyManager {

      /**
       * When constructed with the same seed as the
       * {@link DeterministicECKeyManager} the same series of private keys will
       * get generated.
       * 
       * @param seed
       *           The seed to use.
       */
      public DeterministicECKeyExporter(byte[] seed) {
         super(seed);
      }

      /**
       * Get a {@link PrivateECKeyExporter} instance for a key index.
       * 
       * @param index
       *           The key index to get an exporter for.
       * @return A {@link PrivateECKeyExporter} instance for the given key index
       */
      public PrivateECKeyExporter getPrivateKeyExporter(int index) {
         return new PrivateECKeyExporter(getPrivateKey(index));
      }

   }

   /**
    * Extension of the {@link PrivateECKey}, which allows retrieving the private
    * key in plain text.
    */
   private static class PrivateECKeyExporter extends PrivateECKey {

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

   public static void main(String[] args) {
      try {
         if (args.length != 1) {
            usage();
         }
         if (args[0].toLowerCase().equals("testnet")) {
            exportMain(Network.testNetwork);
         } else if (args[0].toLowerCase().equals("prodnet")) {
            exportMain(Network.productionNetwork);
         } else {
            usage();
         }
      } catch (Throwable t) {
         t.printStackTrace();
         print(t.getMessage());
      }
   }

   private static void exportMain(Network network) throws Exception {
      byte[] seed = initializeSeed();
      printnln("Enter number of private keys to generate:");
      String numKeys = readLine();
      print("");
      int keys = 0;
      try {
         keys = Integer.parseInt(numKeys);
      } catch (NumberFormatException e) {
         // ignore
      }
      if (keys <= 0) {
         print("Invalid value '" + numKeys + "'");
         return;
      }

      DeterministicECKeyExporter exporter = new DeterministicECKeyExporter(seed);
      for (int i = 1; i < keys; i++) {
         PrivateECKeyExporter keyExporter = exporter.getPrivateKeyExporter(i + 1);
         String keyString = keyExporter.getBase58EncodedKey(network);
         String address = AddressUtil.publicKeyToStringAddress(network, keyExporter.getPublicKey().getPubKeyBytes());
         // XXX: Change the format once we know the format that the official
         // bitcoin client accepts
         print(keyString + " : " + address);
      }

   }

   private static byte[] initializeSeed() throws IOException, GeneralSecurityException {
      print("WARNING! This tool allows you to export private keys associated with a");
      print("BCCAPI deterministic wallet. These private keys allow the holder to claim");
      print("any bitcoins associated with them.");
      print("");
      printnln("Enter passphrase:");
      String passphrase = readLine();
      print("");
      printnln("Enter salt:");
      String salt = readLine();
      print("");
      print("Generating strong seed...");
      return SeedManager.generateSeed(passphrase, salt, 100);
   }

   private static void usage() {
      print("Usage: WalletExporter <network>");
      print("where:");
      print("   <network> must be either \"testnet\" or \"prodnet\"");
      System.exit(1);
   }

   private static String readLine() throws IOException {
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      return in.readLine();
   }

   private static void print(String s) {
      System.out.println(s);
   }

   private static void printnln(String s) {
      System.out.print(s);
   }
}
