package rapture.<sdkname>.kernel;

import org.apache.log4j.Logger;

import rapture.common.exception.ExceptionToString;
import rapture.kernel.Kernel;
import rapture.common.InstallableKernelScript;
import rapture.common.InstallableKernel;

public class <sdkname; format="upcase">Kernel  implements InstallableKernel {
    private static final Logger logger = Logger.getLogger(<sdkname; format="upcase">Kernel.class);
    public static String NAME = "<sdkname; format="lower">";

    private <sdkname; format="upcase">KernelScript <sdkname; format="lower">KernelScript;

    <entries; separator="\n">
    
    public void restart() {
        try {
            <setups; separator="\n">
            
            <sdkname; format="lower">KernelScript = new <sdkname; format="upcase">KernelScript(this);
        } catch (Exception e) {
            logger.error("Could not start Kernel: " + ExceptionToString.format(e));
        }
    }
    
    @Override
    public InstallableKernelScript getKernelScript() {
        return <sdkname; format="lower">KernelScript;
    }

    @Override
    public String getName() {
         return NAME;
    }

}
