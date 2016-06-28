<if (deprecated)>   /**
    * @deprecated <deprecated>
    */
   @Deprecated<endif>
   @Override
   public <ret> <name>(<params; separator=",">) {
<if (deprecated)>	Throwable t = new Throwable();
	if (t.getStackTrace()[1].getFileName() != null) {
		log.warn("Deprecated method <name> called by "+ t.getStackTrace()[1].getFileName());
	} else {
		log.warn("Deprecated method <name> called : "+ rapture.common.exception.ExceptionToString.format(t));
	} <endif>
<if (!isVoid)>
      <ret> ret = api.<name>(callingCtx<if(apiparams)>, <apiparams; separator=", "><endif>);
      return ret;
<else>
      api.<name>(callingCtx<if(apiparams)>, <apiparams; separator=", "><endif>);
<endif>
   }
