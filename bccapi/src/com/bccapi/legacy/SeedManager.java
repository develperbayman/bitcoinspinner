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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;

import com.bccapi.bitlib.crypto.Hmac;
import com.bccapi.bitlib.util.BitUtils;
import com.bccapi.bitlib.util.HashUtils;

/**
 * The seed manager is responsible of generating seeds, storing and loading
 * encrypted seeds.
 */
public class SeedManager {

   private static final int SEED_LENGTH = 32;

   /**
    * Implementations of this interface allow you to monitor the progress of the
    * seed generation process, and finally return the generated seed.
    */
   public interface SeedGenerationTask {

      /**
       * Is the seed generation task complete?
       * 
       * @return true if the seed generation is complete and
       *         {@link SeedGenerationTask#getSeed()} contains the calculated
       *         seed, false otherwise.
       */
      boolean isFinished();

      /**
       * Get the seed generation progress in percent as an integer.
       * 
       * @return The seed generation progress in percent as an integer.
       */
      int getProgress();

      /**
       * Get the generated seed. If seed generation is in progress this function
       * will return null.
       * 
       * @return The generated seed if seed generation has completed.
       */
      byte[] getSeed();

   }

   /**
    * Derive a seed from a passphrase and salt.
    * 
    * @param passphrase
    *           The secret pass phrase to derive the seed from. A longer
    *           passphrase yields a stronger seed.
    * @param salt
    *           The salt to apply when deriving a seed. This should be a
    *           globally unique value such as an email address.
    * @return The calculated 32-byte seed.
    */
   public static byte[] generateSeed(String passphrase, String salt) {
      byte[] key = passphrase.getBytes(Charset.forName("UTF8"));
      byte[] message = salt.getBytes(Charset.forName("UTF8"));
      byte[] seed = Hmac.hmacSha256(key, message);
      return seed;
   }

   /**
    * Load a seed from an encrypted stream.
    * 
    * @param password
    *           The password from which the encryption key is derived.
    * @param stream
    *           The stream to read the encrypted seed from.
    * @return the seed bytes.
    * @throws IOException
    * @throws GeneralSecurityException
    */
   public static byte[] loadPinProtectedSeed(String password, InputStream stream) throws IOException,
         GeneralSecurityException {
      // Read salt
      DataInputStream dataInput = new DataInputStream(stream);
      String salt = dataInput.readUTF();
      // Read encrypted seed
      byte[] encryptedSeed = new byte[SEED_LENGTH + 4];
      dataInput.readFully(encryptedSeed);
      // Derive encryption key
      byte[] oneTimePad = deriveOneTimePad(password, salt);
      // Decrypt seed
      byte[] decryptedSeed = applyOneTimePad(encryptedSeed, oneTimePad);
      // Get seed
      byte[] seed = new byte[SEED_LENGTH];
      System.arraycopy(decryptedSeed, 0, seed, 0, seed.length);
      // Validate checksum
      byte[] checksummedSeed = addChecksum(seed);
      if (!BitUtils.areEqual(decryptedSeed, checksummedSeed)) {
         return null;
      }
      return seed;
   }

   /**
    * Save a seed to an encrypted stream.
    * 
    * @param password
    *           The password from which the encryption key is derived.
    * @param salt
    *           The salt from which the encryption key is derived.
    * @param stream
    *           The stream to write to.
    * @param seed
    *           The seed to store.
    * @throws GeneralSecurityException
    * @throws IOException
    */
   public static void savePinProtectedSeed(String password, String salt, OutputStream stream, byte[] seed)
         throws GeneralSecurityException, IOException {
      DataOutputStream dataOutput = new DataOutputStream(stream);
      // Derive encryption key
      byte[] oneTimePad = deriveOneTimePad(password, salt);
      // Generated checksummed seed
      byte[] checksummedSeed = addChecksum(seed);
      // Encrypt seed
      byte[] encryptedSeed = applyOneTimePad(checksummedSeed, oneTimePad);
      // Write salt
      dataOutput.writeUTF(salt);
      // Write encrypted seed
      stream.write(encryptedSeed);
   }

   private static byte[] deriveOneTimePad(String pin, String salt) throws GeneralSecurityException {
      // Seed the PRNG with the pin and salt
      HmacPRNG rng = new HmacPRNG(HashUtils.sha256(pin.getBytes(), salt.getBytes()));
      // generate one time pad
      byte[] otp = new byte[SEED_LENGTH + 4];
      rng.nextBytes(otp);
      return otp;
   }

   private static byte[] applyOneTimePad(byte[] data, byte[] oneTimePad) {
      assert data.length == oneTimePad.length;
      byte[] result = new byte[data.length];
      for (int i = 0; i < data.length; i++) {
         result[i] = (byte) (0xFF & ((int) data[i]) ^ (0xFF & ((int) oneTimePad[i])));
      }
      return result;
   }

   private static byte[] addChecksum(byte[] seed) {
      byte[] checksummedSeed = new byte[seed.length + 4];
      byte[] hash = HashUtils.sha256(seed);
      System.arraycopy(seed, 0, checksummedSeed, 0, seed.length);
      System.arraycopy(hash, 0, checksummedSeed, seed.length, 4);
      return checksummedSeed;
   }

}
