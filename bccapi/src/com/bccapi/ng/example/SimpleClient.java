package com.bccapi.ng.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.bccapi.bitlib.StandardTransactionBuilder;
import com.bccapi.bitlib.StandardTransactionBuilder.InsufficientFundsException;
import com.bccapi.bitlib.StandardTransactionBuilder.UnsignedTransaction;
import com.bccapi.bitlib.crypto.KeyExporter;
import com.bccapi.bitlib.crypto.PrivateKey;
import com.bccapi.bitlib.crypto.PrivateKeyRing;
import com.bccapi.bitlib.crypto.PublicKey;
import com.bccapi.bitlib.crypto.PublicKeyRing;
import com.bccapi.bitlib.model.Address;
import com.bccapi.bitlib.model.NetworkParameters;
import com.bccapi.bitlib.model.Transaction;
import com.bccapi.bitlib.model.UnspentTransactionOutput;
import com.bccapi.bitlib.util.CoinUtil;
import com.bccapi.bitlib.util.Sha256Hash;
import com.bccapi.bitlib.util.StringUtils;
import com.bccapi.legacy.DeterministicECKeyManager;
import com.bccapi.legacy.ECKeyManager;
import com.bccapi.legacy.SeedManager;
import com.bccapi.ng.api.ApiException;
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
import com.bccapi.ng.api.TransactionSummary;
import com.bccapi.ng.impl.BitcoinClientApiImpl;
import com.bccapi.ng.util.TransactionSummaryUtils;
import com.bccapi.ng.util.TransactionSummaryUtils.TransactionType;

public class SimpleClient {

   /**
    * @param args
    */
   public static void main(String[] args) {
      try {
         if (args.length != 1) {
            usage();
         }
         clientMain(args[0]);
      } catch (Throwable t) {
         t.printStackTrace();
         print(t.getMessage());
      }
   }

   private static void usage() {
      print("Usage: SimpleClient <network>");
      print("where:");
      print("   <network> must be either \"testnet\" or \"prodnet\"");
      print("");
      print("Use \"prodnet\" to connect to the official Bitcoin production network.");
      print("");
      print("Use \"testnet\" to connect to the official Bitcoin test network. This is");
      print("useful for testing a BCCAPI client that can send/receive coins from other");
      print("client types using the test network. Obtain free test coins from the");
      print("Testnet Fauchet at \"http://testnet.freebitcoins.appspot.com/\". The test");
      print("network may be unstable at times, as the number of mining power varies a");
      print("lot.");
      print("");
      System.exit(1);
   }

   private static void clientMain(String net) throws Exception {
      // Determine which network to use
      String seedFile;
      URL url;
      NetworkParameters network;
      if (net.toLowerCase().equals("prodnet")) {
         seedFile = "seed.bin";
         url = new URL("http://bqs1.bccapi.com:80");
         // url = new URL("http://localhost:8080");
         network = NetworkParameters.productionNetwork;
      } else if (net.toLowerCase().equals("testnet")) {
         seedFile = "test-seed.bin";
         url = new URL("http://bqs1.bccapi.com:81");
         // url = new URL("http://localhost:8080");
         network = NetworkParameters.testNetwork;
         throw new RuntimeException("Test net is currently not supported");
      } else {
         usage();
         return;
      }

      // Determine seed for key manager
      byte[] seed = initializeOrLoadSeedFile(seedFile);

      // Create key manager
      ECKeyManager keyManager = new DeterministicECKeyManager(seed);

      // Create a key ring and add a single bitcoin key
      PrivateKeyRing keyRing = new PrivateKeyRing();
      // Skip the first key (key 0), we used to use this as an account key, but
      // account keys are no longer used
      keyRing.addPrivateKey(keyManager.getPrivateKey(1), network);

      // Create BCCAPI instance
      BitcoinClientApiImpl api = new BitcoinClientApiImpl(url, network);

      while (true) {
         printOptions();
         String option = readLine();
         if (option.equals("1")) {
            showBalance(api, keyRing);
         } else if (option.equals("2")) {
            showStatements(api, keyRing);
         } else if (option.equals("3")) {
            showAdresses(keyRing);
         } else if (option.equals("4")) {
            createNewAddress(keyManager, keyRing, network);
         } else if (option.equals("5")) {
            sendCoins(api, keyRing);
         } else if (option.equals("6")) {
            exportPrivateKeys(keyRing, network);
         } else if (option.equals("7")) {
            return;
         } else {
            print("Invalid Option");
         }

      }
   }

   private static void exportPrivateKeys(PrivateKeyRing keyRing, NetworkParameters network) {
      // Iterate through all our addresses and export each key one by one
      for (Address address : keyRing.getAddresses()) {

         // Find public key by address
         PublicKey publicKey = keyRing.findPublicKeyByAddress(address);
         if (publicKey == null) {
            continue;
         }

         // Find key exporter by public key
         KeyExporter keyExporter = keyRing.findKeyExporterByPublicKey(publicKey);
         if (keyExporter == null) {
            continue;
         }

         // Print out private key
         print(keyExporter.getBase58EncodedPrivateKey(network));
      }
   }

   private static void sendCoins(BitcoinClientApiImpl api, PrivateKeyRing keyRing) {
      print("Sending coins.");

      // Use any key as change address
      Address changeAddress = keyRing.getAddresses().iterator().next();
      if (changeAddress == null) {
         print("No keys in key ring");
         return;
      }

      // Receiving address
      printnln("Enter receiving address: ");
      String addressString = readLine().trim();
      Address receiver = Address.fromString(addressString, api.getNetwork());
      if (receiver == null) {
         print("Invalid address");
         return;
      }

      // Amount to send
      printnln("Enter amount to send in BTC: ");
      Long toSend;
      try {
         BigDecimal amount = new BigDecimal(readLine().trim());
         // Convert to satoshis
         amount = amount.multiply(new BigDecimal(100000000));
         toSend = amount.longValue();
      } catch (NumberFormatException e) {
         print("Invalid value entered.");
         return;
      }

      // Determine the list of unspent outputs
      QueryUnspentOutputsRequest unspentRequest = new QueryUnspentOutputsRequest(keyRing.getAddresses());
      QueryUnspentOutputsResponse unspentResponse;
      try {
         unspentResponse = api.queryUnspentOutputs(unspentRequest);
      } catch (ApiException e) {
         print(e.getMessage());
         return;
      }
      StandardTransactionBuilder builder = new StandardTransactionBuilder(api.getNetwork());
      builder.addOutput(receiver, toSend);

      // Build list of unspent transaction outputs
      List<UnspentTransactionOutput> unspent = new LinkedList<UnspentTransactionOutput>();
      unspent.addAll(unspentResponse.unspent);

      // Create an unsigned transaction with automatic fee calculation
      UnsignedTransaction unsigned;
      try {
         unsigned = builder.createUnsignedTransaction(unspent, changeAddress, keyRing, api.getNetwork());
      } catch (InsufficientFundsException e1) {
         // Null is returned if there are insufficient funds to pay the amount +
         // the calculated fee
         print("Insifficient funds");
         return;
      }

      // Print out fee
      long fee = unsigned.calculateFee();
      print("Network fee for this transaction: " + CoinUtil.valueString(fee));
      printnln("Hit enter.");
      readLine();

      // Print out a summary of the unsigned transaction
      print("==== Summary of transaction being sent ====");
      print(unsigned.toString());
      printnln("Hit enter to sign and send transaction.");
      readLine();

      // Sign all inputs
      List<byte[]> signatures = StandardTransactionBuilder.generateSignatures(unsigned.getSignatureInfo(), keyRing);

      // Apply signatures to unsigned transaction, and create final transaction
      Transaction transaction = StandardTransactionBuilder.finalizeTransaction(unsigned, signatures);

      // Broadcast transaction
      BroadcastTransactionRequest broadcastRequest = new BroadcastTransactionRequest(transaction);
      try {
         BroadcastTransactionResponse broadcastResponse = api.broadcastTransaction(broadcastRequest);
         print("Coins sent in transaction ID: " + broadcastResponse.hash.toString());
      } catch (ApiException e) {
         print(e.getMessage());
      }
   }

   private static void showStatements(BitcoinClientApiImpl api, PrivateKeyRing keyRing) throws ApiException {

      // Get transaction inventory for the last 10 transactions to or from our
      // addresses
      QueryTransactionInventoryRequest invRequest = new QueryTransactionInventoryRequest(keyRing.getAddresses(), 10);
      QueryTransactionInventoryResponse inv = api.queryTransactionInventory(invRequest);

      // Build a list of transaction IDs to fetch. A smart client may have
      // cached some transactions previously, and would only need to fetch a
      // subset
      List<Sha256Hash> transactionHashes = new LinkedList<Sha256Hash>();
      for (QueryTransactionInventoryResponse.Item item : inv.transactions) {
         transactionHashes.add(item.hash);
      }

      // Fetch transaction summaries
      QueryTransactionSummaryRequest summaryRequest = new QueryTransactionSummaryRequest(transactionHashes);
      QueryTransactionSummaryResponse summaryResponse = api.queryTransactionSummary(summaryRequest);

      // Make a set of addresses for fast lookup
      Set<Address> addressSet = new HashSet<Address>(keyRing.getAddresses());

      // Print out transaction summary
      if (!summaryResponse.transactions.isEmpty()) {
         Date midnight = getMidnight();
         DateFormat hourFormat = DateFormat.getDateInstance(DateFormat.SHORT);
         DateFormat dayFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
         print("-----------------------------------------------------------------------------------------------------");
         print("|###|Confirmations|Date       |Description                                             |      Amount|");
         print("|---|-------------|-----------|--------------------------------------------------------|------------|");
         List<TransactionSummary> records = summaryResponse.transactions;
         int index = 0;
         for (TransactionSummary record : records) {

            // Determine which date formatter to use
            Date date = new Date(record.time * 1000L);
            DateFormat dateFormat = date.before(midnight) ? hourFormat : dayFormat;

            // Create description
            String description;
            TransactionType type = TransactionSummaryUtils.getTransactionType(record, addressSet);
            if (type == TransactionType.SentToOthers) {
               String[] receivers = TransactionSummaryUtils.getReceiversNotMe(record, addressSet);
               description = "Sent to:       " + StringUtils.join(receivers, ", ");
            } else if (type == TransactionType.ReceivedFromOthers) {
               String[] senders = TransactionSummaryUtils.getSenders(record);
               description = "Received from: " + StringUtils.join(senders, ", ");
               description = StringUtils.cap(description, 56);
            } else {
               description = "Sent to your self";
            }
            description = StringUtils.cap(description, 56);

            // String representation of amount
            String valueString = CoinUtil.valueString(TransactionSummaryUtils
                  .calculateBalanceChange(record, addressSet));

            // Calculate confirmations
            int confirmations;
            if (record.height == -1) {
               confirmations = 0;
            } else {
               confirmations = inv.chainHeight - record.height + 1;
            }

            // Format & print record
            StringBuilder sb = new StringBuilder();
            Formatter f = new Formatter(sb, Locale.US);
            f.format("|%1$3s|%2$-13s|%3$-11s|%4$-56s|%5$12s|", index++, confirmations, dateFormat.format(date),
                  description, valueString);
            print(sb.toString());
            f.close();
         }
         print("-----------------------------------------------------------------------------------------------------");
      } else {
         print("There is no record of any transactions for this wallet");
      }
   }

   private static Date getMidnight() {
      Calendar midnight = Calendar.getInstance();
      midnight.set(midnight.get(Calendar.YEAR), midnight.get(Calendar.MONTH), midnight.get(Calendar.DAY_OF_MONTH), 0,
            0, 0);
      return midnight.getTime();
   }

   private static void createNewAddress(ECKeyManager keyManager, PrivateKeyRing keyRing, NetworkParameters network) {
      // Generate the next private key
      PrivateKey key = keyManager.getPrivateKey(keyManager.getPublicKeys().size());
      // Add to key ring
      keyRing.addPrivateKey(key, network);
   }

   /**
    * Print out the address of each key in our key ring
    */
   private static void showAdresses(PublicKeyRing keyRing) {
      StringBuilder sb = new StringBuilder();
      sb.append("Addresses: ");
      boolean first = true;
      for (Address address : keyRing.getAddresses()) {
         if (first) {
            sb.append(address).append('\n');
            first = false;
            continue;
         }
         sb.append("           ").append(address).append('\n');
      }
      System.out.print(sb.toString());
   }

   /**
    * Print out the combined balance of all address in our key ring
    */
   private static void showBalance(BitcoinClientApi api, PublicKeyRing keyRing) throws ApiException {
      QueryBalanceRequest request = new QueryBalanceRequest(keyRing.getAddresses());
      QueryBalanceResponse result = api.queryBalance(request);
      // The balance available for spending is unspent + unconfirmed change sent
      // to self
      long available = result.balance.unspent + result.balance.pendingChange;
      print("Available:" + CoinUtil.valueString(available) + " BTC  Sending:"
            + CoinUtil.valueString(result.balance.pendingSending) + " BTC  Receiving:"
            + CoinUtil.valueString(result.balance.pendingReceiving) + " BTC");
   }

   private static byte[] initializeOrLoadSeedFile(String seedFilePath) throws IOException, GeneralSecurityException {
      File seedFile = new File(seedFilePath);
      if (!seedFile.exists()) {
         // No seed file found, first time we start the client
         return initializeSeedFile(seedFilePath);
      } else {
         // Seed file found, load the PIN protected file
         return loadSeed(seedFilePath);
      }
   }

   private static byte[] initializeSeedFile(String seedPath) throws IOException, GeneralSecurityException {
      print("");
      print(" === Initializing Client for first use ===");
      print("");
      print("The client is about to be initialized for first use. All keys managed by");
      print("this client are generated from a strong seed which is based on a user");
      print("entered passphrase and salt. Both of these values should be memorized or");
      print("written down and kept in a secure place. You will only ever need these");
      print("values again if you lose your client device and want to recover your");
      print("wallet.");
      print("");
      print("The longer the passphrase the stronger the seed. The passphrase you enter");
      print("can be any length and contain any characters.");
      print("");
      printnln("Enter passphrase:");
      String passphrase = readLine();
      print("");
      print("The salt you enter is not secret. For maximum security it should be a");
      print("unique value which is not used by other clients. Enter something you can");
      print("remember such as your email address.");
      print("");
      printnln("Enter salt:");
      String salt = readLine();
      print("");
      print("The seed is stored encrypted in the file seed.bin in the current");
      print("working directory.");
      print("Generating strong seed ...");
      byte[] seed = SeedManager.generateSeed(passphrase, salt);
      print("");
      print("The PIN you enter is used for encrypting the seed file which is kept on");
      print("your client device. You will have to enter it every time you use the client.");
      print("");
      printnln("Enter PIN:");
      String pin = readLine();
      OutputStream output = new FileOutputStream(seedPath);
      SeedManager.savePinProtectedSeed(pin, salt, output, seed);
      print("");
      print("If you at some point believe that someone has obtained a copy of the");
      print("iencrypted seed file in your client device you should immediately transfer");
      print("your funds to another client and never use the same PIN or passphrase.");
      print("");
      print("Initialization complete.");
      printnln("Hit Enter:");
      readLine();
      return seed;
   }

   private static byte[] loadSeed(String seedPath) throws IOException, GeneralSecurityException {
      byte[] seed = null;
      while (true) {
         printnln("Enter PIN:");
         String pin = readLine();
         InputStream input = new FileInputStream(seedPath);
         seed = SeedManager.loadPinProtectedSeed(pin, input);
         input.close();
         if (seed != null) {
            break;
         }
         System.out.println("Invalid PIN.");
      }
      return seed;
   }

   private static void printOptions() {
      print("=================");
      print("==   Options   ==");
      print("=================");
      print("1. Get wallet balance");
      print("2. Get wallet statement");
      print("3. My receiving addresses");
      print("4. Create new receiving address");
      print("5. Send coins");
      print("6. Export private keys");
      print("7. Exit");
      printnln("Enter option:");
   }

   private static String readLine() {
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      try {
         return in.readLine();
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private static void print(String s) {
      System.out.println(s);
   }

   private static void printnln(String s) {
      System.out.print(s);
   }

}
