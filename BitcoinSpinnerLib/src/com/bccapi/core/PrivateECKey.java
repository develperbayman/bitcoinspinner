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
import java.math.BigInteger;
import java.security.SecureRandom;

import com.bccapi.api.Network;
import com.bouncycastle.asn1.DERInteger;
import com.bouncycastle.asn1.DERSequenceGenerator;
import com.bouncycastle.crypto.AsymmetricCipherKeyPair;
import com.bouncycastle.crypto.generators.ECKeyPairGenerator;
import com.bouncycastle.crypto.params.ECKeyGenerationParameters;
import com.bouncycastle.crypto.params.ECPrivateKeyParameters;
import com.bouncycastle.crypto.params.ECPublicKeyParameters;
import com.bouncycastle.crypto.signers.ECDSASigner;

/**
 * A private elliptic curve key used in the Bitcoin system.
 */
public class PrivateECKey {

   protected final BigInteger _privateKey;
   protected final PublicECKey _publicKey;

   /**
    * Create a private key based on a PRNG.
    * 
    * @param randomSource
    *           The PRNG to use.
    */
   public PrivateECKey(SecureRandom randomSource) {
      ECKeyGenerationParameters params = new ECKeyGenerationParameters(PublicECKey.ecParams, randomSource);
      ECKeyPairGenerator generator = new ECKeyPairGenerator();
      generator.init(params);
      AsymmetricCipherKeyPair keypair = generator.generateKeyPair();
      ECPrivateKeyParameters privParams = (ECPrivateKeyParameters) keypair.getPrivate();
      _privateKey = privParams.getD();
      ECPublicKeyParameters pubParams = (ECPublicKeyParameters) keypair.getPublic();
      _publicKey = new PublicECKey(pubParams.getQ().getEncoded());
   }

   /**
    * Construct from private key bytes
    * 
    * @param bytes
    *           The private key as an array of bytes
    */
   public PrivateECKey(byte[] bytes) {
      if (bytes.length != 32) {
         throw new IllegalArgumentException("The length of the array of bytes must be 32");
      }
      byte[] keyBytes = new byte[33];
      System.arraycopy(bytes, 0, keyBytes, 1, 32);
      _privateKey = new BigInteger(keyBytes);
      _publicKey = new PublicECKey(PublicECKey.ecParams.getG().multiply(_privateKey).getEncoded());
   }

   /**
    * Construct from private and public key bytes
    * 
    * @param bytes
    *           The private key as an array of bytes
    */
   public PrivateECKey(byte[] priBytes, byte[] pubBytes) {
      if (priBytes.length != 32) {
         throw new IllegalArgumentException("The length of the array of bytes must be 32");
      }
      byte[] keyBytes = new byte[33];
      System.arraycopy(priBytes, 0, keyBytes, 1, 32);
      _privateKey = new BigInteger(keyBytes);
      _publicKey = new PublicECKey(pubBytes);
   }

   /**
    * Construct from a base58 encoded key and a bitcoin network
    * 
    * @param base58Encoded
    *           The base58 encoded private key
    * @param network
    *           The network that this key should be for
    * @throws IllegalArgumentException
    *            If this is not a valid base58 encoded key for the specified
    *            network
    */
   public PrivateECKey(String base58Encoded, Network network) {
      byte[] bytes = Base58.decodeChecked(base58Encoded);
      if (bytes == null) {
         throw new IllegalArgumentException("The base58 encoded key is invalid");
      }
      if (bytes.length != 33) {
         throw new IllegalArgumentException("The length of the base58 encoded key is invalid");
      }
      if (network == Network.productionNetwork && bytes[0] != (byte) 0x80) {
         throw new IllegalArgumentException("The the base58 encoded key is not for the botcoin production network");
      }
      if (network == Network.testNetwork && bytes[0] != (byte) 0xEF) {
         throw new IllegalArgumentException("The the base58 encoded key is not for the botcoin test network");
      }
      bytes[0] = 0;
      _privateKey = new BigInteger(bytes);
      _publicKey = new PublicECKey(PublicECKey.ecParams.getG().multiply(_privateKey).getEncoded());
   }

   /**
    * Copy constructor
    * 
    * @param key
    *           The key to copy
    */
   public PrivateECKey(PrivateECKey key) {
      _privateKey = key._privateKey;
      _publicKey = key._publicKey;
   }

   /**
    * Generate a signature on an array of bytes using this private key.
    * 
    * @param input
    *           The bytes to sign
    * @return The signature.
    */
   public byte[] sign(byte[] input) {
      ECDSASigner signer = new ECDSASigner();
      ECPrivateKeyParameters params = new ECPrivateKeyParameters(_privateKey, PublicECKey.ecParams);
      signer.init(true, params);
      BigInteger[] signature = signer.generateSignature(input);
      try {
         ByteArrayOutputStream stream = new ByteArrayOutputStream();
         DERSequenceGenerator sequenceGenerator = new DERSequenceGenerator(stream);
         sequenceGenerator.addObject(new DERInteger(signature[0]));
         sequenceGenerator.addObject(new DERInteger(signature[1]));
         sequenceGenerator.close();
         return stream.toByteArray();
      } catch (IOException e) {
         // This never happens
         throw new RuntimeException(e);
      }
   }

   /**
    * Get the public key corresponding to the private key.
    * 
    * @return The public key corresponding to the private key.
    */
   public PublicECKey getPublicKey() {
      return _publicKey;
   }

}
