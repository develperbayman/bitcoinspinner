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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An {@link ECKeyManager} that generates deterministic key-pairs using a seed
 * and a PRNG.
 */
public class DeterministicECKeyManager implements ECKeyManager {

   private SecureRandom _prng;
   protected List<PrivateECKey> _privateKeys;
   protected Map<PublicECKey, Integer> _publicKeyMap;

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
      _privateKeys = new ArrayList<PrivateECKey>();
      _publicKeyMap = new HashMap<PublicECKey, Integer>();
   }

   @Override
   public PublicECKey getPublicKey(int index) {
      if (index > _privateKeys.size() - 1) {
         generateKeysForIndex(index);
      }
      return _privateKeys.get(index).getPublicKey();
   }

   @Override
   public byte[] sign(byte[] data, int keyIndex) {
      PrivateECKey key = getPrivateKey(keyIndex);
      return key.sign(data);
   }

   @Override
   public List<PublicECKey> getPublicKeys() {
      List<PublicECKey> keys = new ArrayList<PublicECKey>(_privateKeys.size());
      for (PrivateECKey privateKey : _privateKeys) {
         keys.add(privateKey.getPublicKey());
      }
      return keys;
   }

   @Override
   public ECSigner getSigner(int index) {
      return new ECSigner(this, index);
   }

   protected PrivateECKey getPrivateKey(int index) {
      if (index > _privateKeys.size() - 1) {
         generateKeysForIndex(index);
      }
      return _privateKeys.get(index);
   }

   protected void generateKeysForIndex(int index) {
      int missingKeys = index + 1 - _privateKeys.size();
      for (int i = 0; i < missingKeys; i++) {
         PrivateECKey key = generateNextKey();
         _privateKeys.add(key);
         _publicKeyMap.put(key.getPublicKey(), _privateKeys.size() - 1);
      }
   }

   private PrivateECKey generateNextKey() {
      PrivateECKey ecKey = new PrivateECKey(_prng);
      return ecKey;
   }

}
