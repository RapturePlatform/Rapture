<if (!isVoid)>
    private static final class <name; format="upcase">TypeReference extends TypeReference\<<ret>\> {
    }
<endif>
    
<if (deprecated)>   /**
    * @deprecated <deprecated>
    */
    @Deprecated<endif>
    @Override
    public <ret> <name>(<params; separator=", ">) {
<if (deprecated)>        Map\<String, String\> metadata = (context == null) ? null : context.getMetadata();
        log.warn("Deprecated method <name> called by user " + ((context == null) ? "UNKNOWN" : context.getUser()) 
		+ " from host "+ ((metadata == null) ? "UNIDENTIFIED" : metadata.get("IPADDRESSES"))
		+ " by class " + new Throwable().getStackTrace()[1].getFileName());<endif>
        <name; format="upcase">Payload requestObj = new <name; format="upcase">Payload();
        requestObj.setContext(context == null ? this.getContext() : context);
        <rest(apiparams):apiset(); separator="\n">
<if (!isVoid)>
        return doRequest(requestObj, "<name; format="upper">", new <name; format="upcase">TypeReference());
<else>
        doRequest(requestObj, "<name; format="upper">", null);
<endif>
    }
