/**
* <if (deprecated)>@deprecated <deprecated><endif>
*/
<if (deprecated)>@Deprecated<endif>
@Override
public <ret> <name>(<params; separator=", ">) {
<if (deprecated)>        Map\<String, String\> metadata = (context == null) ? null : context.getMetadata();
        log.warn("Deprecated method <name> called by user " + ((context == null) ? "UNKNOWN" : context.getUser())
                + " from host " + ((metadata == null) ? "UNIDENTIFIED" : metadata.get("IPADDRESSES"))
                + " by class " + new Throwable().getStackTrace()[1].getFileName());<endif>
    long functionStartTime = System.currentTimeMillis();
   
    <name; format="upcase">Payload requestObj = new <name; format="upcase">Payload();
    requestObj.setContext(context);
    <rest(callParams):apiset(); separator="\n">
    ContextValidator.validateContext(context, EntitlementSet.<apitype; format="upcase">_<name>, requestObj); 
    
    long preToPostStartTime = System.currentTimeMillis();
    
    Kernel.getApiHooksService().pre(context, CallName.<apitype>_<name>);
<if (!isVoid)>
    <ret> returnValue = apiImpl.<name>(<callParams; separator=", ">);
<else>
    apiImpl.<name>(<callParams; separator=", ">);
<endif>
    Kernel.getApiHooksService().post(context, CallName.<apitype>_<name>);
    
    long endFunctionTime = System.currentTimeMillis();
    Kernel.getMetricsService().recordTimeDifference("apiMetrics.<apitype; format="lower">Api.<name>.fullFunctionTime.succeeded", (endFunctionTime-functionStartTime));
    Kernel.getMetricsService().recordTimeDifference("apiMetrics.<apitype; format="lower">Api.<name>.preToPostTime.succeeded", (endFunctionTime-preToPostStartTime));
        
<if (!isVoid)>
    return returnValue;
<endif>
}
