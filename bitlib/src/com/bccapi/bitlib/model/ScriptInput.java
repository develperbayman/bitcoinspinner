package com.bccapi.bitlib.model;

public class ScriptInput extends Script {

   public static final ScriptInput EMPTY = new ScriptInput(new byte[][] {});

   public static ScriptInput fromScriptBytes(byte[] script) throws ScriptParsingException {
      byte[][] chunks = Script.chunksFromScriptBytes(script);
      if (ScriptInputStandard.isScriptInputStandard(chunks)) {
         return new ScriptInputStandard(chunks);
      } else if (ScriptInputPubKey.isScriptInputPubKey(chunks)) {
         return new ScriptInputPubKey(chunks);
      } else {
         return new ScriptInput(chunks);
      }

   }

   /**
    * Construct an input script from an output script.
    * <p>
    * This is used when verifying or generating signatures, where the input is
    * set to the output of the funding transaction.
    */
   public static ScriptInput fromOutputScript(ScriptOutput output) {
      return new ScriptInput(output._chunks);
   }

   protected ScriptInput(byte[][] chunks) {
      super(chunks);
   }

   /**
    * Special constructor for coinbase scripts
    * @param script
    */
   protected ScriptInput(byte[] script) {
	      super(script);
	   }

}
