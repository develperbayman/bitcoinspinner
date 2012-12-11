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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bccapi.bitlib.crypto.BitcoinSigner;
import com.bccapi.bitlib.crypto.InMemoryPrivateKey;
import com.bccapi.bitlib.crypto.PrivateKey;
import com.bccapi.bitlib.crypto.PublicKey;

/**
 * An {@link ECKeyManager} that generates deterministic key-pairs using a seed
 * and a PRNG.
 */
public class DeterministicECKeyManager implements ECKeyManager {

   private SecureRandom _prng;
   protected List<InMemoryPrivateKey> _privateKeys;
   protected Map<PublicKey, Integer> _publicKeyMap;

   /**
    * Construct a deterministic key manager using a seed. The quality and
    * security of generated keys depend on the quality of the seed provided.
    * Refer to {@link SeedManager} for details on how to get a strong seed.
    * 
    * @param seed
    *           The seed to use.
    */
   public DeterministicECKeyManager(byte[] seed) {
      try {
         _prng = new HmacPRNG(seed);
      } catch (NoSuchAlgorithmException e) {
         // This never happens
         throw new RuntimeException("Unable to create PRNG");
      }
      _privateKeys = new ArrayList<InMemoryPrivateKey>();
      _publicKeyMap = new HashMap<PublicKey, Integer>();
   }

   @Override
   public PublicKey getPublicKey(int index) {
      if (index > _privateKeys.size() - 1) {
         generateKeysForIndex(index);
      }
      return _privateKeys.get(index).getPublicKey();
   }

   @Override
   public List<PublicKey> getPublicKeys() {
      List<PublicKey> keys = new ArrayList<PublicKey>(_privateKeys.size());
      for (PrivateKey privateKey : _privateKeys) {
         keys.add(privateKey.getPublicKey());
      }
      return keys;
   }

   @Override
   public BitcoinSigner getSigner(int index) {
      return getPrivateKey(index);
   }

   @Override
   public PrivateKey getPrivateKey(int index) {
      if (index > _privateKeys.size() - 1) {
         generateKeysForIndex(index);
      }
      return _privateKeys.get(index);
   }

   protected void generateKeysForIndex(int index) {
      int missingKeys = index + 1 - _privateKeys.size();
      for (int i = 0; i < missingKeys; i++) {
         InMemoryPrivateKey key = generateNextKey();
         _privateKeys.add(key);
         _publicKeyMap.put(key.getPublicKey(), _privateKeys.size() - 1);
      }
   }

   private InMemoryPrivateKey generateNextKey() {
      InMemoryPrivateKey key = new InMemoryPrivateKey(_prng);
      return key;
   }

}
