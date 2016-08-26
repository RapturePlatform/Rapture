package rapture.field;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import rapture.field.model.Structure;
import rapture.field.model.FieldDefinition;
import rapture.field.model.FieldTransform;
import rapture.field.model.StructureField;
import rapture.field.model.FieldType;
import rapture.common.impl.jackson.JacksonUtil;
import reflex.value.ReflexValue;


/**
 * We use this to attempt to decide on transformations from one structure to another
 */
 
public class TransformEngine extends BaseEngine {
    public TransformEngine(StructureLoader sLoader, FieldLoader fLoader, ScriptLoader scLoader, FieldTransformLoader ftLoader) {
        super(sLoader, fLoader, scLoader, ftLoader);
    } 
    
    /**
     * Given a source document that should already have been validated as type sourceStructure, work out and
     * transform it into a form defined by targetStructure, using the available transformations defined in the
     * system
     */
     
    public String transform(String sourceDoc, String sourceStructure, String targetStructure) {
        // Step 1 - collect all of the none repeating (non ARRAY) fields in targetStructure
        Structure targetS = getStructure(targetStructure);
        List<String> fields = getBaseFields(targetS);
        System.out.println("Fields are " + fields);
        // Step 2 - find the fields in the source
        Structure sourceS = getStructure(sourceStructure);
        List<String> srcFields = getBaseFields(sourceS);
        // Step 3 - create a field where we can record whether we have found a set of transforms that
        // allow us to get to the target from the source
        List<TransformProduction> productionList = new ArrayList<TransformProduction>();
        Map<String, Boolean> satisfyMap = new HashMap<String, Boolean>();
        fields.forEach(f -> satisfyMap.put(f, false));
        // One pass
        List<String> transforms = ftLoader.getFieldTransforms("//test");
        //
        transforms.forEach(trId -> {
            System.out.println("Checking with " + trId);
            FieldTransform ft = ftLoader.getFieldTransform(trId);
            // (1) are the sources for this field transform in fields?
            // (2) are any of the targets false or available in target?
            // If so, this is a good transform we should do
            // If not (2), then record the output in the field list because we could have a second generation
            System.out.println("Does " + srcFields + " contain all of " + ft.getSourceFields());
            if (srcFields.containsAll(ft.getSourceFields())) {
                System.out.println("Transform " + trId + " can be run from this point");
                
                List<String> usedFields = ft.getTargetFields().stream().filter(tf -> {
                        boolean satisfy = satisfyMap.containsKey(tf) && !satisfyMap.get(tf);
                        if (satisfy) {
                            satisfyMap.put(tf, true);
                        }
                        return satisfy;
                }).collect(Collectors.toList());
                
                if (usedFields.size() != 0) {
                    System.out.println("This transform can generate output fields relevant");
                    TransformProduction tp = new TransformProduction();
                    tp.setTransformUri(trId);
                    productionList.add(tp);
                }
            }
        }
        );
        // Now check to see if we are now satisfied - this lambda finds any that have a value are false. If any
        // have a value of false we are not OK.
        boolean allOk = !satisfyMap.values().stream().anyMatch(v -> !v);
        
        if (allOk) {
            System.out.println("We can transform using " + productionList.size() + " transforms");
            Map<String, Object> srcObject = JacksonUtil.getMapFromJson(sourceDoc);
            Map<String, Object> targetObject = new HashMap<String, Object>();
            
            productionList.forEach(tp -> runTransform(tp, srcObject, targetObject, sourceS, targetS));
            
            String targDoc = JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(targetObject));
            return targDoc;
        }
        return sourceDoc;
    }
    
    
    private void runTransform(TransformProduction tp, Map<String, Object> srcObject, Map<String, Object> targetObject, Structure srcStructure, Structure targStructure) {
        FieldTransform ft = ftLoader.getFieldTransform(tp.getTransformUri());
        // Get the variables for the source fields for this transform, we use the sourceStructure to determine what key
        Map<String, Object> inputParams = new HashMap<String, Object>();
        List<Object> vals = new ArrayList<Object>();
        ft.getSourceFields().forEach(fieldUri -> {
            srcStructure.getFields().stream().anyMatch(sf -> {
                if (sf.getFieldUri().equals(fieldUri)) {
                    vals.add(srcObject.get(sf.getKey()));
                    return true;
                }
                return false;
            });
        });
        
        inputParams.put("vals", vals);
        System.out.println("Input parameters are " + inputParams);
        System.out.println("We expect to get " + ft.getTargetFields().size() + " return values in the list");
        String reflexScript = scriptLoader.getScript(ft.getTransformScript());
        List<Object> transList = container.runTransformScript(reflexScript, inputParams);
        System.out.println("Return values are " + transList);
        if (transList != null && transList.size() == ft.getTargetFields().size()) {
            System.out.println("Good to apply");
            int pos = 0;
            for(String fieldUri : ft.getTargetFields()) {
                for(StructureField sf : targStructure.getFields()) {
                    if (sf.getFieldUri().equals(fieldUri)) {
                        Object x = transList.get(pos);
                        if (x instanceof ReflexValue) {
                            x = ((ReflexValue)x).getValue();
                        }
                        targetObject.put(sf.getKey(), x);
                        pos++;
                        break;
                    }
                }
            }
        }
    }
    
    private List<String> getBaseFields(Structure s) {
        List<String> fields = new ArrayList<String>();
        s.getFields().forEach(sf -> {
            String fieldUri = sf.getFieldUri();
            FieldDefinition fd = getField(fieldUri);
            if (fd.getFieldType() == FieldType.MAP) {
                List<String> internFields = getBaseFields(getStructure(fd.getFieldTypeExtra()));
                fields.addAll(internFields);
            } else if (fd.getFieldType() != FieldType.ARRAY) {
                 fields.add(sf.getFieldUri());             
            }
        });
        
        return fields;
    }
}

class TransformProduction {
    private String transformUri;
    public void setTransformUri(String transformUri) {
        this.transformUri = transformUri;
    }
    public String getTransformUri() {
        return transformUri;
    }
}