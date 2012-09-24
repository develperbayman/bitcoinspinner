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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bccapi.api.Network;
import com.bccapi.api.SendCoinForm;
import com.bccapi.api.TxOutput;
import com.bccapi.core.BccScript.BccScriptException;

/**
 * Validates that the BCCAPI server did in fact give us a {@link SendCoinForm}
 * that matches what we asked for.
 */
public class SendCoinFormValidator {

   /**
    * Validates that the BCCAPI server did in fact give us a
    * {@link SendCoinForm} that matches what we asked for. This ensures that the
    * BCCAPI server is not cheating, and sending coins to someone else.
    * 
    * @param form
    *           The {@link SendCoinForm} to validate
    * @param myAddresses
    *           The list of addresses sending coins
    * @param network
    *           The Bitcoin network used
    * @param amount
    *           The amount that should be sent
    * @param fee
    *           The fee that should be used for the transaction
    * @param receiver
    *           The receiving address
    * @return true is the form is valid, false otherwise.
    */
   public static boolean validate(SendCoinForm form, List<String> myAddresses, Network network, long amount, long fee,
         String receiver) {
      // Build the list of addresses controlled by this account
      Set<String> addressSet = new HashSet<String>(myAddresses);

      try {
         /**
          * Go through all the outputs that fund this transaction. Note: If the
          * server sent us invalid funding (funding which is already spent or
          * not belonging to us), then the worst thing that could happen is that
          * the transaction is denied by the network. The server cannot make us
          * loose coins.
          */
         long inCoins = 0;
         for (TxOutput out : form.getFunding()) {
            inCoins += out.getValue();
         }

         long sentToMe = 0;
         long sentToReceiver = 0;
         // Go through all the outputs of this transaction.
         for (TxOutput out : form.getTransaction().getOutputs()) {
            BccScriptOutput script = BccScriptOutput.fromScriptBytes(out.getScript());
            String address;
            if (script instanceof BccScriptMultisigOutput) {
               BccScriptMultisigOutput s = (BccScriptMultisigOutput) script;
               address = AddressUtil.byteAddressToMultisigAddress(network, s.getMultisigAddress());
            } else if (script instanceof BccScriptStandardOutput) {
               BccScriptStandardOutput s = (BccScriptStandardOutput) script;
               address = AddressUtil.byteAddressToStandardAddress(network, s.getAddress());
            } else {
               throw new BccScriptException("Invalid output script");
            }
            if (addressSet.contains(address)) {
               // Money for me. Either change going back to me or I am
               // sending
               // to myself
               sentToMe += out.getValue();
            } else if (receiver.equals(address)) {
               // Money for receiver, who is not me, but the right receiver
               // nonetheless
               sentToReceiver += out.getValue();
            } else {
               // Sent to someone else. This is not right
               return false;
            }
         }

         long outCoins = sentToMe + sentToReceiver;

         // Validate that the fee is right
         if (fee != inCoins - outCoins) {
            // Fee is not right
            return false;
         }

         boolean sendingToMyself = addressSet.contains(receiver);
         if (sendingToMyself) {
            // Validate that everything is accounted for
            return inCoins == sentToMe + fee;
         }

         // We are sending to someone else
         if (sentToReceiver != amount) {
            return false;
         }
         return true;
      } catch (BccScriptException e) {
         // One of the transaction scripts were invalid
         return false;
      }
   }

   /**
    * Calculate the fee of a SendCoinForm. This is useful if you want to know
    * which fee the server has calculated for a transaction.
    * 
    * @param form
    *           The {@link SendCoinForm} to calculate the fee on.
    * @return The fee that is used with this send coin form.
    */
   public static long calculateFee(SendCoinForm form) {
      long inCoins = 0;
      long outCoins = 0;
      // Sum up all the input values
      for (TxOutput out : form.getFunding()) {
         inCoins += out.getValue();
      }
      // Sum up the output value
      for (TxOutput out : form.getTransaction().getOutputs()) {
         outCoins += out.getValue();
      }
      // return fee
      return inCoins - outCoins;
   }
}
