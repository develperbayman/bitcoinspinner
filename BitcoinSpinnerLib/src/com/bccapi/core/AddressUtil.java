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

import com.bccapi.api.Network;

/**
 * Utilities for handling Bitcoin addresses.
 */
public class AddressUtil {

   /**
    * Get the string representation of a bitcoin address from a public key:
    * 
    * 1 byte (test net or production net) + 20 bytes (public key hash) + 4 bytes
    * (check sum, 4 first bytes of hash of previous bytes)
    * 
    * @param network
    *           The network this address is for.
    * @param publicKey
    *           The public key bytes
    * @return A Bitcoin address
    */
   public static String publicKeyToStandardAddress(Network network, byte[] publicKey) {
      return byteAddressToStandardAddress(network, HashUtils.addressHash(publicKey));
   }

   /**
    * Get the string representation of a standard bitcoin address from a byte
    * representation of a bitcoin address:
    * 
    * 1 byte (test net or production net) + 20 bytes (public key hash) + 4 bytes
    * (check sum, 4 first bytes of hash of previous bytes)
    * 
    * @param network
    *           The network this address is for.
    * @param bytes
    *           The address as an array of bytes
    * @return A Bitcoin address
    */
   public static String byteAddressToStandardAddress(Network network, byte[] bytes) {
      byte[] addressBytes = new byte[1 + 20 + 4];
      addressBytes[0] = (byte) (network.getStandardAddressHeader() & 0xFF);
      System.arraycopy(bytes, 0, addressBytes, 1, 20);
      byte[] checkSum = HashUtils.doubleSha256(addressBytes, 0, 21);
      System.arraycopy(checkSum, 0, addressBytes, 21, 4);
      return Base58.encode(addressBytes);
   }

   /**
    * Get the string representation of a standard bitcoin address from a byte
    * representation of a bitcoin address:
    * 
    * 1 byte (test net or production net) + 20 bytes (public key hash) + 4 bytes
    * (check sum, 4 first bytes of hash of previous bytes)
    * 
    * @param network
    *           The network this address is for.
    * @param bytes
    *           The address as an array of bytes
    * @return A Bitcoin address
    */
   public static String byteAddressToMultisigAddress(Network network, byte[] bytes) {
      byte[] addressBytes = new byte[1 + 20 + 4];
      addressBytes[0] = (byte) (network.getMultisigAddressHeader() & 0xFF);
      System.arraycopy(bytes, 0, addressBytes, 1, 20);
      byte[] checkSum = HashUtils.doubleSha256(addressBytes, 0, 21);
      System.arraycopy(checkSum, 0, addressBytes, 21, 4);
      return Base58.encode(addressBytes);
   }

   /**
    * Get the byte representation of an address from the string representation
    * of a Bitcoin address.
    * 
    * @param address
    *           The Bitcoin address
    * @return The byte representation of an address, which is the same as the
    *         hash representation of the corresponding public key, or null if
    *         the address is invalid
    */
   public static byte[] StringAddressToByteAddress(String address) {
      byte[] tmp = Base58.decodeChecked(address);
      if (tmp == null || tmp.length != 21) {
         return null;
      }
      byte[] bytes = new byte[tmp.length - 1];
      System.arraycopy(tmp, 1, bytes, 0, tmp.length - 1);
      return bytes;
   }

   /**
    * Validate the string representation of an address along with a network
    * 
    * @param address
    *           The address to verify
    * @param network
    *           The network it is supposed to be for
    * @return false if the address cannot be decoded, has invalid checksum, or
    *         is not for the specified network, true otherwise.
    */
   public static boolean validateAddress(String address, Network network) {
      byte[] tmp = Base58.decodeChecked(address);
      if (tmp == null || tmp.length != 21) {
         return false;
      }
      return ((byte) (network.getStandardAddressHeader() & 0xFF)) == tmp[0]
            || ((byte) (network.getMultisigAddressHeader() & 0xFF)) == tmp[0];
   }

}
