/**
 * Copyright 2011 bccapi.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bccapi.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.bccapi.api.APIException;
import com.bccapi.api.AccountInfo;
import com.bccapi.api.AccountStatement;
import com.bccapi.api.BitcoinClientAPI;
import com.bccapi.api.Network;
import com.bccapi.api.SendCoinForm;
import com.bccapi.api.Tx;
import com.bccapi.api.TxInput;
import com.bccapi.api.TxOutput;

/**
 * This class manages the client side of a Bitcoin wallet including wallet
 * private keys. It uses the BCCAPI for accessing the server side wallet, which
 * is responsible of tracking wallet transactions using the wallet public keys.
 * <p>
 * This class has all the functions you need to create a bitcoin wallet. Refer
 * to the SimpleClient in the example section for an example of how to use it.
 */
public class Account {

   private static final byte[] EMPTY_ARRAY = new byte[0];

   private ECKeyManager _keyManager;
   private BitcoinClientAPI _api;
   private String _sessionId;

   /**
    * Create a new wallet account instance.
    * 
    * @param keyManager
    *           The manager of the private keys belonging to this account
    * @param api
    *           The BCCAPI instance used for communicating with the BCCAPI
    *           server.
    * 
    */
   public Account(ECKeyManager keyManager, BitcoinClientAPI api) {
      _keyManager = keyManager;
      _api = api;
      _sessionId = "";
   }

   /**
    * Get the network used, test network or production network.
    * 
    * @return The network used, test network or production network.
    */
   public Network getNetwork() {
      return _api.getNetwork();
   }

   /**
    * Login to the BCCAPI server using the account public key
    * 
    * @throws IOException
    * @throws APIException
    */
   public synchronized void login() throws IOException, APIException {
      PublicECKey publicKey = getAccountPublicKey();
      // Get challenge
      byte[] challenge = _api.getLoginChallenge(publicKey.getPubKeyBytes());
      // Calculate challenge response
      byte[] response = calculateLoginChallengeResponse(challenge);
      // Login
      _sessionId = _api.login(publicKey.getPubKeyBytes(), response);
      AccountInfo info = getInfo();
      // Touch all keys that are registered with the server. If we are using a
      // key deriver this forces it to generate all keys
      if (info.getKeys() > 0) {
         getWalletPublicKey(info.getKeys() - 1);
      }
   }

   /**
    * Get the {@link AccountInfo} for this wallet account.
    * 
    * @return The {@link AccountInfo} for this account.
    * @throws APIException
    * @throws IOException
    */
   public synchronized AccountInfo getInfo() throws APIException, IOException {
      AccountInfo info = _api.getAccountInfo(_sessionId);
      return info;
   }

   /**
    * Get the {@link AccountStatement} for this wallet account.
    * <p>
    * Call this function to get the {@link AccountStatement} for an account,
    * which includes transaction records. The function allows the caller to
    * specify which records should be obtained. This enables a client to
    * minimize bandwidth by only requesting records not seen previously.
    * 
    * @param startIndex
    *           The first record index to retrieve. The first record registered
    *           with an account has index 0.
    * @param count
    *           The number of records to retrieve. If the number supplied is
    *           larger than what is available, only the available records are
    *           returned.
    * @return The {@link AccountStatement} for this account.
    * @throws APIException
    * @throws IOException
    */
   public synchronized AccountStatement getStatement(int startIndex, int count) throws APIException, IOException {
      AccountStatement statement = _api.getAccountStatement(_sessionId, startIndex, count);
      return statement;
   }

   /**
    * Add a new public key to the BCCAPI server. From the time the server
    * receives the public key it will track new transaction outputs to it and
    * associate it with the wallet of this account.
    * 
    * @throws APIException
    * @throws IOException
    */
   public synchronized void addKey() throws APIException, IOException {
      AccountInfo info = _api.getAccountInfo(_sessionId);
      byte[] publicKeyBytes = getWalletPublicKey(info.getKeys()).getPubKeyBytes();
      _api.addKeyToWallet(_sessionId, publicKeyBytes);
   }

   /**
    * Get a {@link SendCoinForm} from the BCCAPI server by supplying an amount
    * to send and the address of the receiver. The form obtained should be
    * verified using the {@link SendCoinFormSummary} and sent using the
    * {@link Account#signAndSubmitSendCoinForm} function.
    * 
    * @param receivingAddress
    *           The bitcoin address of the receiver
    * @param amount
    *           The number of bitcoins to send in satoshis (1 satoshi =
    *           0.000000001 BTC)
    * @param fee
    *           The size of the fee to include in the transaction in satoshis (1
    *           satoshi = 0.000000001 BTC)
    * @return A {@link SendCoinForm}
    * @throws APIException
    * @throws IOException
    */
   public synchronized SendCoinForm getSendCoinForm(String receivingAddress, long amount, long fee)
         throws APIException, IOException {
      SendCoinForm form = _api.getSendCoinForm(_sessionId, receivingAddress, amount, fee);
      return form;
   }

   /**
    * Sign a {@link SendCoinForm} and submit it to the BCCAPI server.
    * 
    * @param form
    *           The {@link SendCoinForm} to sign and submit.
    * @throws APIException
    * @throws IOException
    */
   public void signAndSubmitSendCoinForm(SendCoinForm form) throws APIException, IOException {
      signSendCoinForm(form, this);
      _api.submitTransaction(_sessionId, form.getTransaction());
   }

   /**
    * Get the list of Bitcoin addresses of this account.
    * 
    * @return The list of Bitcoin addresses of this account.
    */
   public List<String> getAddresses() {
      List<String> addresses = new ArrayList<String>();
      boolean first = true;
      for (PublicECKey key : _keyManager.getPublicKeys()) {
         if (first) {
            first = false;
            continue;
         }
         addresses.add(AddressUtil.publicKeyToStringAddress(_api.getNetwork(), key.getPubKeyBytes()));
      }
      return addresses;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      for (String address : getAddresses()) {
         sb.append(address).append('\n');
      }
      return sb.toString();
   }

   /**
    * Calculate login challenge response. The response is the signature on the
    * SHA-256 of the public key and the challenge. The signature is made with
    * the account private key.
    * 
    * @param challenge
    *           The challenge to calculate a response for
    * @return the login challenge response.
    */
   private byte[] calculateLoginChallengeResponse(byte[] challenge) {
      byte[] accountPublicKeyBytes = _keyManager.getPublicKey(0).getPubKeyBytes();
      byte[] toSign = HashUtils.sha256(accountPublicKeyBytes, challenge);
      ECSigner accountSigner = new ECSigner(_keyManager, 0);
      return accountSigner.sign(toSign);
   }

   /**
    * Get the public key corresponding to the private key used for
    * authenticating the account when logging in to the BCCAPI server.
    * 
    * @return the public key of this account.
    */
   private PublicECKey getAccountPublicKey() {
      return _keyManager.getPublicKey(0);
   }

   /**
    * Get the public key of a specified wallet key index.
    * 
    * @param index
    *           The index of the public key.
    * @return The public key of a specified wallet key index.
    */
   private PublicECKey getWalletPublicKey(int index) {
      return _keyManager.getPublicKey(index + 1);
   }

   /**
    * Get the {@link ECSigner} for a wallet key index. The {@link ECSigner} can
    * be used for signing data with the private key.
    * 
    * @param index
    *           The index of he private key to get an {@link ECSigner} for.
    * @return The {@link ECSigner} for a wallet private key index.
    */
   private ECSigner getWalletSignerByKeyIndex(int index) {
      return _keyManager.getSigner(index + 1);
   }

   private static void signSendCoinForm(SendCoinForm form, Account account) {
      Tx tx = form.getTransaction();
      List<TxOutput> funding = form.getFunding();
      List<Integer> keyIndexes = form.getKeyIndexes();
      List<TxInput> inputs = tx.getInputs();
      byte[][] signatures = new byte[inputs.size()][];
      ECSigner[] signingKeys = new ECSigner[inputs.size()];
      // Clear all inputs
      for (TxInput input : inputs) {
         input.setScript(EMPTY_ARRAY);
      }

      // Generate signature for each input
      for (int i = 0; i < inputs.size(); i++) {
         TxInput input = inputs.get(i);

         // Set the input to the script of its output.
         input.setScript(funding.get(i).getScript());

         // Find the signing key to use.
         int index = keyIndexes.get(i);
         ECSigner signer = account.getWalletSignerByKeyIndex(index);
         if (signer == null) {
            throw new RuntimeException("Unable to find signer for key with index: " + index);
         }
         signingKeys[i] = signer;
         byte[] hash = hashTx(tx);
         // Set the script to empty again for the next input.
         input.setScript(EMPTY_ARRAY);

         // Sign for the output, and put the resulting signature in the
         // script along with the public key further downstream.
         try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(signer.sign(hash));
            bos.write((0 + 1) | 0);
            signatures[i] = bos.toByteArray();
         } catch (IOException e) {
            throw new RuntimeException(e); // Cannot happen.
         }
      }

      // Now we have calculated each signature, go through and create the
      // scripts. Reminder: the script consists of
      // a signature (over a hash of the transaction) and the complete public
      // key needed to sign for the connected
      // output.
      for (int i = 0; i < inputs.size(); i++) {
         TxInput input = inputs.get(i);
         PublicECKey key = signingKeys[i].getPublicKey();
         BccScriptInput script = new BccScriptInput(signatures[i], key.getPubKeyBytes());
         input.setScript(script.toByteArray());
      }
   }

   private static byte[] hashTx(Tx tx) {
      try {
         ByteArrayOutputStream stream = new ByteArrayOutputStream();
         tx.toStream(stream);
         int hashType = 1;
         BitUtils.uint32ToStream(hashType, stream);
         return HashUtils.doubleSha256(stream.toByteArray());
      } catch (IOException e) {
         throw new RuntimeException(e); // Cannot happen.
      }
   }

}
