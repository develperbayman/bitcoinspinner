package com.bccapi.api;

/**
 * Settings for the network used. Can be either the test or production network.
 */
public class Network {

   public static Network testNetwork = new Network(111);
   public static Network productionNetwork = new Network(0);

   /**
    * The first byte of a base58 encoded bitcoin address.
    */
   private int _addressHeader;

   private Network(int addressHeader) {
      _addressHeader = addressHeader;
   }

   /**
    * Get the first byte of a base58 encoded bitcoin address as an integer.
    * 
    * @return The first byte of a base58 encoded bitcoin address as an integer.
    */
   public int getAddressHeader() {
      return _addressHeader;
   }

}
