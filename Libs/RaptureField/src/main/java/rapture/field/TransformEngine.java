package rapture.field;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import rapture.common.FieldType;
import rapture.common.RaptureField;
import rapture.common.RaptureFieldTransform;
import rapture.common.RaptureStructure;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.field.model.FieldDefinition;
import rapture.field.model.Structure;
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
     
    public String transform(String sourceDoc, String sourceStructure, String targetStructure, String transArea) {
        // Step 1 - collect all of the none repeating (non ARRAY) fields in targetStructure
        RaptureStructure targetS = getStructure(targetStructure);
        RaptureStructure sourceS = getStructure(sourceStructure);
        Map<String, Object> srcObject = JacksonUtil.getMapFromJson(sourceDoc);
        Map<String, Object> targetObject = new HashMap<String, Object>();
        Map<String, FieldStatus> targFields = new LinkedHashMap<String, FieldStatus>();
        Map<String, FieldStatus> srcFields = new LinkedHashMap<String, FieldStatus>();
        getBaseFields(targetS, targetObject, targFields);
        getBaseFields(sourceS, srcObject, srcFields);
        
        List<TransformProduction> productionList = new ArrayList<TransformProduction>();
        
        Map<String, Boolean> satisfyMap = new HashMap<String, Boolean>();
        targFields.keySet().forEach(f -> satisfyMap.put(f, false));
        
        // First construct identity transforms for fields that are in the source structure and target structure
        List<String> common = srcFields.keySet().stream().filter(targFields.keySet()::contains).collect(Collectors.toList());
        //System.out.println("Common fields are " + common);
        common.forEach(id -> {
                TransformProduction tp = new TransformProduction();
                tp.setIdentityField(id);
                productionList.add(tp);
                satisfyMap.put(id, true);
            });
        
        // One pass
        List<String> transforms = ftLoader.getFieldTransforms(transArea);
        if (transforms == null) {
            transforms = new ArrayList<String>();
        }
        //
        transforms.forEach(trId -> {
            //System.out.println("Checking with " + trId);
            RaptureFieldTransform ft = ftLoader.getFieldTransform(trId);
            // (1) are the sources for this field transform in fields?
            // (2) are any of the targets false or available in target?
            // If so, this is a good transform we should do
            // If not (2), then record the output in the field list because we could have a second generation
             if (srcFields.keySet().containsAll(ft.getSourceFields())) {
               // System.out.println("Contains all");
                List<String> usedFields = ft.getTargetFields().stream().filter(tf -> {
                    //System.out.println("Checking " + tf);
                        boolean satisfy = satisfyMap.containsKey(tf) && !satisfyMap.get(tf);
                        if (satisfy) {
                            //System.out.println("Adding to satisfyMap");
                            satisfyMap.put(tf, true);
                        }
                        return satisfy;
                }).collect(Collectors.toList());
                
                if (usedFields.size() != 0) {
                    //System.out.println("Adding production");
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
             
            productionList.forEach(tp -> runTransform(tp, srcFields, targFields));
            
            String targDoc = JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(targetObject));
            return targDoc;
        }
        return sourceDoc;
    }
    
    
    private void runTransform(TransformProduction tp, Map<String, FieldStatus> srcFields, Map<String, FieldStatus> targetFields) {
        if (tp.hasIdentity()) {
            Object v = srcFields.get(tp.getIdentityField()).getValue();
            targetFields.get(tp.getIdentityField()).setValue(v);
        } else {
            RaptureFieldTransform ft = ftLoader.getFieldTransform(tp.getTransformUri());
            // Get the variables for the source fields for this transform, we use the sourceStructure to determine what key
            Map<String, Object> inputParams = new HashMap<String, Object>();
            List<Object> vals = new ArrayList<Object>();
            // This transform production works off a list of fields
            ft.getSourceFields().forEach(fieldUri -> {
                vals.add(srcFields.get(fieldUri).getValue());
            });
            
            inputParams.put("vals", vals);
            String reflexScript = scriptLoader.getScript(ft.getTransformScript());
            List<Object> transList = container.runTransformScript(reflexScript, inputParams);
            if (transList != null && transList.size() == ft.getTargetFields().size()) {
                AtomicInteger pos = new AtomicInteger(0);
                ft.getTargetFields().forEach(k -> {
                    //System.out.println("Applying " + k);
                     Object x = transList.get(pos.getAndIncrement());
                            if (x instanceof ReflexValue) {
                                x = ((ReflexValue)x).getValue();
                            }
                     targetFields.get(k).setValue(x);
                });
            } else {
                // ERROR we didn't get an appropriate response from the transform script
            }
        }
    }
    
    /**
     * The field works on a context - part of a document that is used to retrieve
     * or set the values. We might as well store the field definition as well?
     */
     
    private void getBaseFields(RaptureStructure s, Map<String, Object> ctx, Map<String, FieldStatus> collector) {
        s.getFields().forEach(sf -> {
            String fieldUri = sf.getFieldUri();
            RaptureField fd = getField(fieldUri);
            if (fd.getFieldType() == FieldType.MAP) {
                Object x = ctx.get(sf.getKey());
                Map<String, Object> pass;
                if (x == null) {
                    pass = new HashMap<String, Object>();
                    ctx.put(sf.getKey(), pass);
                } else {
                    pass = (Map<String, Object>)x;
                }
                getBaseFields(getStructure(fd.getFieldTypeExtra()), pass, collector);
            } else if (fd.getFieldType() != FieldType.ARRAY) {
                collector.put(fieldUri, new FieldStatus(fieldUri, fd, ctx, sf.getKey()));
            } else {
                // ARRAYS eeekkkk
            }
        });
    }
}

class FieldStatus {
    public FieldStatus(String fieldUri, RaptureField fDef, Map<String, Object> context, String key) {
        this.fieldUri = fieldUri;
        this.fDef = fDef;
        this.context = context;
        this.key = key;
    }
    
    public String fieldUri;
    public Map<String, Object> context;
    public RaptureField fDef;
    public String key;
    
    public Object getValue() {
        return context.get(key);
    }
    
    public void setValue(Object x) {
        context.put(key, x);
    }
}

class TransformProduction {
    private String transformUri;
    private String identityField;
    private boolean isIdentity = false;
    
    public String toString() {
        return isIdentity ? ("I-"+identityField) : transformUri;
        
    }
    public void setIdentityField(String identityField) {
        this.identityField = identityField;
        this.isIdentity = true;
    }
    
    public boolean hasIdentity() {
        return isIdentity;
    }
    
    public String getIdentityField() {
        return identityField;
    }
    
    public void setTransformUri(String transformUri) {
        this.transformUri = transformUri;
    }
    public String getTransformUri() {
        return transformUri;
    }
}