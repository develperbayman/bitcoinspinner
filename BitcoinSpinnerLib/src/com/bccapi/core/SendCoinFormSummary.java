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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bccapi.api.Network;
import com.bccapi.api.SendCoinForm;
import com.bccapi.api.TxOutput;
import com.bccapi.core.BccScript.BccScriptException;

/**
 * Utility class for pretty-printing a {@link SendCoinForm}.
 */
public class SendCoinFormSummary {

   public static class Element {
      private long _amount;
      private String _address;

      public Element(String address, long amount) {
         _address = address;
         _amount = amount;
      }

      public String getAddress() {
         return _address;
      }

      public long getAmount() {
         return _amount;
      }

      @Override
      public String toString() {
         return _address + ":" + CoinUtils.valueString(_amount);
      }
   }

   private List<Element> _inputs;
   private List<Element> _outputs;
   private long _inCoins;
   private long _coinsSentToOtherWallet;
   private long _coinsSentToMe;

   /**
    * Constructor based on a {@link SendCoinForm} and the {@link Account} from
    * which it is sent.
    * 
    * @param form
    *           The form to get a summary for.
    * @param account
    *           The account sending the form
    * @throws BccScriptException
    */
   public SendCoinFormSummary(SendCoinForm form, List<String> myAddresses, Network network) throws BccScriptException {
      _inputs = new ArrayList<Element>();
      _outputs = new ArrayList<Element>();
      _inCoins = 0;
      _coinsSentToOtherWallet = 0;
      _coinsSentToMe = 0;
      Set<String> addresses = new HashSet<String>(myAddresses);
      for (TxOutput out : form.getFunding()) {
         BccScriptOutput script = new BccScriptOutput(out.getScript());
         String address = AddressUtil.byteAddressToStringAddress(network, script.getAddress());
         _inputs.add(new Element(address, out.getValue()));
         _inCoins += out.getValue();
      }
      for (TxOutput out : form.getTransaction().getOutputs()) {
         BccScriptOutput script = new BccScriptOutput(out.getScript());
         String address = AddressUtil.byteAddressToStringAddress(network, script.getAddress());
         _outputs.add(new Element(address, out.getValue()));
         if (addresses.contains(address)) {
            _coinsSentToMe += out.getValue();
         } else {
            _coinsSentToOtherWallet += out.getValue();
         }
      }
   }

   /**
    * Get the number of input coins in this transaction.
    * 
    * @return The number of input coins in this transaction.
    */
   public long getInputCoins() {
      return _inCoins;
   }

   /**
    * Get the number of coins sent to another wallet than mine.
    * 
    * @return The number of coins sent to another wallet than mine.
    */
   public long getCoinsSentToForeignWallet() {
      return _coinsSentToOtherWallet;
   }

   /**
    * Get the number of coins sent to my wallet.
    * 
    * @return The number of coins sent to my wallet.
    */
   public long getCoinsSentToMe() {
      return _coinsSentToMe;
   }

   /**
    * Get the number of coins going to the miner who confirms this transaction.
    * 
    * @return The number of coins going to the miner who confirms this
    *         transaction.
    */
   public long getFee() {
      return _inCoins - _coinsSentToOtherWallet - _coinsSentToMe;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Inputs:\n");
      for (Element e : _inputs) {
         sb.append("  ").append(e.toString()).append('\n');
      }
      sb.append("Outputs:\n");
      for (Element e : _outputs) {
         sb.append("  ").append(e.toString()).append('\n');
      }
      sb.append("Input Coins               : ").append(CoinUtils.valueString(getInputCoins())).append('\n');
      sb.append("Coins sent to other wallet: ").append(CoinUtils.valueString(getCoinsSentToForeignWallet()))
            .append('\n');
      sb.append("Coins sent back to me     : ").append(CoinUtils.valueString(getCoinsSentToMe())).append('\n');
      sb.append("Fee                       : ").append(CoinUtils.valueString(getFee())).append('\n');
      return sb.toString();
   }

}
