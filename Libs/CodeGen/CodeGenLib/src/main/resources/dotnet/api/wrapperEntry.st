/**
* <if (deprecated)>@deprecated <deprecated><endif>
*/
<if (deprecated)>@Deprecated<endif>
@Override
public <ret> <name>(<params; separator=", ">) {
    <name; format="upcase">Payload requestObj = new <name; format="upcase">Payload();
    requestObj.setContext(context);
    <rest(callParams):apiset(); separator="\n">
    ContextValidator.validateContext(context, EntitlementSet.<apitype; format="upcase">_<name>, requestObj);
    
    Kernel.getApiHooksService().pre(context, CallName.<apitype>_<name>);
    <ret> returnValue = apiImpl.<name>(<callParams; separator=", ">);
    Kernel.getApiHooksService().post(context, CallName.<apitype>_<name>);
    
    return returnValue;
}
