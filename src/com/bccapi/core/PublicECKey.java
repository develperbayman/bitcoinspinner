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

import java.io.IOException;
import java.util.Arrays;

import com.bouncycastle.asn1.ASN1InputStream;
import com.bouncycastle.asn1.DERInteger;
import com.bouncycastle.asn1.DERSequence;
import com.bouncycastle.asn1.sec.SECNamedCurves;
import com.bouncycastle.asn1.x9.X9ECParameters;
import com.bouncycastle.crypto.params.ECDomainParameters;
import com.bouncycastle.crypto.params.ECPublicKeyParameters;
import com.bouncycastle.crypto.signers.ECDSASigner;

/**
 * A public elliptic curve key used in the Bitcoin system.
 */
public class PublicECKey {

   public static final ECDomainParameters ecParams;

   static {
      X9ECParameters params = SECNamedCurves.getByName("secp256k1");
      ecParams = new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH());
   }

   private final byte[] _pubKeyBytes;
   private byte[] _pubKeyHash;

   /**
    * Create a public key based on an array of bytes
    * 
    * @param pubKeyBytes
    *           the array of bytes to use.
    */
   public PublicECKey(byte[] pubKeyBytes) {
      _pubKeyBytes = pubKeyBytes;
   }

   public boolean verify(byte[] data, byte[] signature) {
      ECDSASigner signer = new ECDSASigner();
      ECPublicKeyParameters params = new ECPublicKeyParameters(ecParams.getCurve().decodePoint(_pubKeyBytes), ecParams);
      signer.init(false, params);
      try {
         ASN1InputStream decoder = new ASN1InputStream(signature);
         DERSequence seq = (DERSequence) decoder.readObject();
         DERInteger r = (DERInteger) seq.getObjectAt(0);
         DERInteger s = (DERInteger) seq.getObjectAt(1);
         decoder.close();
         return signer.verifySignature(data, r.getValue(), s.getValue());
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Get the public key as an array of bytes.
    * 
    * @return The public key as an array of bytes.
    */
   public byte[] getPubKeyBytes() {
      return _pubKeyBytes;
   }

   /**
    * Gets the public key on the Bitcoin address form.
    * 
    * @return The public key on the Bitcoin address form.
    */
   public byte[] getPublicKeyHash() {
      if (_pubKeyHash == null)
         _pubKeyHash = HashUtils.addressHash(_pubKeyBytes);
      return _pubKeyHash;
   }

   @Override
   public int hashCode() {
      byte[] hash = getPublicKeyHash();
      return ((int) hash[0]) + (((int) hash[1]) << 8) + (((int) hash[1]) << 16) + (((int) hash[1]) << 32);
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof PublicECKey)) {
         return false;
      }
      PublicECKey other = (PublicECKey) obj;
      return Arrays.equals(getPublicKeyHash(), other.getPublicKeyHash());
   }

   @Override
   public String toString() {
      return HexUtils.toHex(_pubKeyBytes);
   }

}
