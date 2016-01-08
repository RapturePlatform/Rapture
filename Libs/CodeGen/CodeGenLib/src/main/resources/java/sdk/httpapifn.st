    private static final class <name; format="upcase">TypeReference extends TypeReference\<<ret>\> {
    }
    
    @Override
    public <ret> <name>(<params; separator=", ">) {
        <name; format="upcase">Payload requestObj = new <name; format="upcase">Payload();
        requestObj.setContext(context == null ? this.getContext() : context);
        <rest(apiparams):apiset(); separator="\n">
        return doRequest(requestObj, "<name; format="upper">", new <name; format="upcase">TypeReference());
    }