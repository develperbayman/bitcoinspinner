package com.bccapi.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;

import com.bccapi.api.Network;
import com.bccapi.core.AddressUtil;
import com.bccapi.core.DeterministicECKeyExporter;
import com.bccapi.core.PrivateECKeyExporter;
import com.bccapi.core.SeedManager;

/**
 * Utility program for exporting private keys. Once the official bitcoin client
 * supports private key imports, the format that this utility exports will be
 * changed to match it.
 */
public class WalletExporter {

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
         String address = AddressUtil.publicKeyToStandardAddress(network, keyExporter.getPublicKey().getPubKeyBytes());
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
