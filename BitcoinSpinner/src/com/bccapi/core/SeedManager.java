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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

import com.bouncycastle.util.Arrays;
import com.lambdaworks.crypto.SCrypt;

/**
 * The seed manager is responsible of generating seeds using strong key
 * stretching, storing and loading encrypted seeds. The seed manager is based on
 * the security of {@link SCrypt}, which is designed for making brute force
 * attacks harder by making hardware implementations impractical as they will
 * require very large amounts of internal CPU cache to perform well.
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

   private static class SeedGenerator implements Runnable, SeedGenerationTask {

      private String _passphrase;
      private String _salt;
      private int _depth;
      private int _progress;
      private byte[] _seed;

      public SeedGenerator(String passphrase, String salt, int depth) {
         _passphrase = passphrase;
         _salt = salt;
         _depth = depth;
         _progress = 0;
         _seed = null;
      }

      @Override
      public boolean isFinished() {
         return _seed != null;
      }

      @Override
      public int getProgress() {
         return _progress;
      }

      @Override
      public byte[] getSeed() {
         return _seed;
      }

      @Override
      public void run() {
         try {
            byte[] seed = HashUtils.sha256(_passphrase.getBytes());
            byte[] saltBytes = HashUtils.sha256(_salt.getBytes());
            for (int i = 0; i < _depth; i++) {
               seed = SCrypt.scrypt(seed, saltBytes, 1024, 8, 1, SEED_LENGTH);
               _progress = (i * 100 / _depth);
            }
            _seed = seed;
         } catch (GeneralSecurityException e) {
            throw new RuntimeException("Unable to create seed", e);
         }
      }

   }

   /**
    * Get a {@link SeedGenerationTask} instance, which generates a seed from a
    * passphrase and salt in a background thread, while providing progress
    * feedback.
    * 
    * @param passphrase
    *           The secret pass phrase to derive the seed from. A longer
    *           passphrase yields a stronger seed.
    * @param salt
    *           The salt to apply when deriving a seed. This should be a
    *           globally unique value such as an email address.
    * @param depth
    *           The number of iterations. A higher value yields a stronger seed.
    * @return A {@link SeedGenerationTask} instance.
    */
   public static SeedGenerationTask getSeedGenerationTask(String passphrase, String salt, int depth) {
      SeedGenerator generator = new SeedGenerator(passphrase, salt, depth);
      Thread t = new Thread(generator);
      t.start();
      return generator;
   }

   /**
    * Derive a seed by iteratively calling {@link SCrypt} a specified number of
    * times.
    * 
    * @param passphrase
    *           The secret pass phrase to derive the seed from. A longer
    *           passphrase yields a stronger seed.
    * @param salt
    *           The salt to apply when deriving a seed. This should be a
    *           globally unique value such as an email address.
    * @param depth
    *           The number of iterations. A higher value yields a stronger seed.
    * @return The calculated 32-byte seed.
    */
   public static byte[] generateSeed(String passphrase, String salt, int depth) {
      try {
         byte[] seed = HashUtils.sha256(passphrase.getBytes());
         byte[] saltBytes = HashUtils.sha256(salt.getBytes());
         for (int i = 0; i < depth; i++) {
            seed = SCrypt.scrypt(seed, saltBytes, 1024, 8, 1, SEED_LENGTH);
         }
         return seed;
      } catch (GeneralSecurityException e) {
         throw new RuntimeException("Unable to create seed", e);
      }
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
      if (!Arrays.areEqual(decryptedSeed, checksummedSeed)) {
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
