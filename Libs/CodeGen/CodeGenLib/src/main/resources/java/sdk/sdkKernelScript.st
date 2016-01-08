package rapture.<sdkname; format="lower">.kernel;

import rapture.common.CallingContext;
import rapture.<sdkname; format="lower">.common.api.<sdkname; format="upcase">ScriptingApi;
// import rapture.<sdkname; format="lower">.common.api.ScriptExecutionApi;
// import rapture.<sdkname; format="lower">.common.api.ScriptOrderApi;
// import rapture.<sdkname; format="lower">.common.api.ScriptOrderListApi;

<apiNames: {apiName |
import rapture.<sdkname; format="lower">.kernel.script.Script<apiName; format="upcase">;
import rapture.<sdkname; format="lower">.common.api.Script<apiName; format="upcase">Api;
}>

public class <sdkname; format="upcase">KernelScript implements <sdkname; format="upcase">ScriptingApi {
<apiNames: {apiName |
    private Script<apiName; format="upcase"> <apiName; format="lower">;
}>

    public <sdkname; format="upcase">KernelScript(<sdkname; format="upcase">Kernel kernel) {
<apiNames: {apiName |
        <apiName; format="lower"> = new Script<apiName; format="upcase">(kernel.get<apiName; format="upcase">());
}>
    }

    @Override
    public void setCallingContext(CallingContext ctx) {
<apiNames: {apiName |
        <apiName; format="lower">.setCallingContext(ctx);
}>
    }
<apiNames: {apiName |
    @Override
    public Script<apiName; format="upcase">Api get<apiName; format="upcase">() {
        return <apiName; format="lower">;
    }
}>

}