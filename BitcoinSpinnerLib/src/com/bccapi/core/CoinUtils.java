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

import java.math.BigInteger;

/**
 * Utility for turning an amount of satoshis into a user friendly string. 1
 * satoshi == 0.000000001 BTC
 */
public class CoinUtils {

   /**
    * Number of satoshis in a Bitcoin.
    */
   public static final BigInteger BTC = new BigInteger("100000000", 10);

   /**
    * Get the given value in satoshis as a string on the form "10.12345".
    * 
    * @param value
    *           The number of satoshis
    * @return The given value in satoshis as a string on the form "10.12345".
    */
   public static String valueString(long value) {
      return valueString(BigInteger.valueOf(value));
   }

   /**
    * Get the given value in satoshis as a string on the form "10.12345".
    * 
    * @param value
    *           The number of satoshis
    * @return The given value in satoshis as a string on the form "10.12345".
    */
   public static String valueString(BigInteger value) {
      boolean negative = value.compareTo(BigInteger.ZERO) < 0;
      if (negative)
         value = value.negate();
      BigInteger coins = value.divide(BTC);
      BigInteger cents = value.remainder(BTC);
      return String.format("%s%d.%08d", negative ? "-" : "", coins.intValue(), cents.intValue());
   }

}
