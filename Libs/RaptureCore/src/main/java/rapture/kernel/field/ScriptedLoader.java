package rapture.kernel.field;
import rapture.field.ScriptLoader;
import rapture.field.StructureLoader;
import rapture.field.FieldLoader;
import rapture.field.FieldTransformLoader;
import rapture.common.RaptureStructure;
import rapture.common.RaptureField;
import java.util.List;
import rapture.common.RaptureFieldTransform;
import rapture.common.CallingContext;
import rapture.kernel.script.KernelScript;

public class ScriptedLoader implements ScriptLoader, StructureLoader, FieldLoader, FieldTransformLoader {
    private KernelScript ks;
    
    public ScriptedLoader(CallingContext ctx) {
        ks = new KernelScript();
        ks.setCallingContext(ctx);
    }
    
    public String getScript(String uri) {
       return ks.getScript().getScript(uri).getScript();
    }
    
    public RaptureField getField(String uri) {
        return ks.getTransform().getField(uri);
    }
    
    public RaptureFieldTransform getFieldTransform(String uri) {
        return ks.getTransform().getFieldTransform(uri);
    }
    
    public List<String> getFieldTransforms(String prefix) {
        return null;
    }
    
    public RaptureStructure getStructure(String uri) {
        return ks.getTransform().getStructure(uri);
    }
}