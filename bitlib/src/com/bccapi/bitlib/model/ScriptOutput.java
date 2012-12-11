package com.bccapi.bitlib.model;

public abstract class ScriptOutput extends Script {

   public static ScriptOutput fromScriptBytes(byte[] script) throws ScriptParsingException {
      byte[][] chunks = Script.chunksFromScriptBytes(script);
      if (chunks == null) {
         return null;
      }
      if (ScriptOutputStandard.isScriptOutputStandard(chunks)) {
         return new ScriptOutputStandard(chunks);
      } else if (ScriptOutputPubkey.isScriptOutputPubkey(chunks)) {
         return new ScriptOutputPubkey(chunks);
      } else if (ScriptOutputMultisig.isScriptOutputMultisig(chunks)) {
         return new ScriptOutputMultisig(chunks);
      } else if (ScriptOutputMsg.isScriptOutputMsg(chunks)) {
         return new ScriptOutputMsg(chunks);
      } else {
         return new ScriptOutputStrange(chunks);
      }

   }

   protected ScriptOutput(byte[][] chunks) {
      super(chunks);
   }
   
   public abstract Address getAddress(NetworkParameters network);

}
