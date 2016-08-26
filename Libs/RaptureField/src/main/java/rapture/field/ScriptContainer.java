package rapture.field;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import reflex.ReflexExecutor;
import reflex.ReflexException;
import reflex.value.internal.ReflexVoidValue;
import reflex.IReflexHandler;
import reflex.value.ReflexValue;

import reflex.StandardReflexHandler;

/**
 * The script container is used to manage the execution of Reflex scripts for a given
 * set of contexts. These contexts are:
 * (1) Validation as part of schema validation
 * (2) Transformations as part of schema transformation
 */
 
public class ScriptContainer {
    public void runValidationScript(Object val, String reflexScript, List<String> ret) {
          IReflexHandler handler = new StandardReflexHandler(null);
          Map<String, Object> masterParameterMap = new HashMap<String , Object>();
          masterParameterMap.put("value", val);
          Object resp = ReflexExecutor.runReflexProgram(reflexScript, handler, masterParameterMap);
          if (!(resp instanceof ReflexValue.Internal)) {
              ret.add(resp.toString());
          }
    }
    
    public List<Object> runTransformScript(String reflexScript, Map<String, Object> params) {
          IReflexHandler handler = new StandardReflexHandler(null);
          Object resp = ReflexExecutor.runReflexProgram(reflexScript, handler, params);
          if (resp instanceof List) {
              return (List<Object>) resp;
          }
        return null;
    }
}