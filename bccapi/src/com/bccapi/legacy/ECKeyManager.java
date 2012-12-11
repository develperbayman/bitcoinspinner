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

package com.bccapi.legacy;

import java.util.List;

import com.bccapi.bitlib.crypto.BitcoinSigner;
import com.bccapi.bitlib.crypto.PrivateKey;
import com.bccapi.bitlib.crypto.PublicKey;

/**
 * Manages public and private key pairs.
 */
public interface ECKeyManager {

   /**
    * Get the public key for a given index. Calling this function may cause new
    * keys getting generated.
    * 
    * @param keyIndex
    *           The key store index to get the private key for.
    * @return The private key of the specified index.
    */
   public PrivateKey getPrivateKey(int keyIndex);

   /**
    * Get the private key for a given index. Calling this function may cause new
    * keys getting generated.
    * 
    * @param keyIndex
    *           The key store index to get the public key for.
    * @return The public key of the specified index.
    */
   public PublicKey getPublicKey(int keyIndex);

   /**
    * Get the list of public keys generated so far.
    * 
    * @return The list of public keys generated so far.
    */
   public List<PublicKey> getPublicKeys();

   /**
    * Get a {@link BitcoinSigner} instance for the private key for a given index.
    * Calling this function with a key index that does not exist may cause new
    * keys getting generated.
    * 
    * @param keyIndex
    *           The key store index of the private key to use.
    * @return An {@link ECSigner} that can generate signatures with the private
    *         key at the specified index.
    */
   public BitcoinSigner getSigner(int keyIndex);

}
