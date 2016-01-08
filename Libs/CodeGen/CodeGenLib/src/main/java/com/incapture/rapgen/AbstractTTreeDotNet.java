/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.incapture.rapgen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.TreeNodeStream;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

import com.incapture.rapgen.annotations.AddressableAnnotation;
import com.incapture.rapgen.annotations.BeanAnnotation;
import com.incapture.rapgen.annotations.CacheableAnnotation;
import com.incapture.rapgen.annotations.DeprecatedAnnotation;
import com.incapture.rapgen.annotations.ExtendsAnnotation;
import com.incapture.rapgen.annotations.IndexedAnnotation;
import com.incapture.rapgen.annotations.StorableAnnotation;
import com.incapture.rapgen.annotations.storable.StorableField;
import com.incapture.rapgen.annotations.storable.StorableFieldType;

/**
 * This class is used as the base for the parser - we call methods in this class from the Tree walker code.
 * 
 * @author amkimian
 * 
 */
public abstract class AbstractTTreeDotNet extends AbstractTTree {
    private static final String COMMONTEST_LOC = "Test/";
    private static final String COMMONSHARED_LOC = "Generated/";
    private static final String DEFAULT_PACKAGE = "rapture.common";
    /**
     * So this abstract tree will be used to host a number of generated files with a generated file simply being a list of templates ordered by some means
     * 
     * We can add a template to a file + section
     */

    private Map<String, Map<String, StringTemplate>> kernelTemplates = new HashMap<String, Map<String, StringTemplate>>();
    private Map<String, Map<String, StringTemplate>> apiTemplates = new HashMap<String, Map<String, StringTemplate>>();

    /**
     * Here we store type information (name and package really)
     * 
     */

    private Map<String, String> typeToPackage = new HashMap<String, String>();

    private Map<String, Boolean> schemesMap = new HashMap<String, Boolean>();

    /**
     * Return the type imports for a given api.
     * 
     * @param api
     * @return
     */
    protected String getTypeImports(String api) {
        Set<String> types = importsForApiType.get(api);
        // TODO: We should surely be using a string template for this.
        StringBuffer ret = new StringBuffer();
        if (types != null) {
            for (String type : types) {
                ret.append("using ");
                ret.append(getPackage(type));
                ret.append(".");
                ret.append(type);
                ret.append(";\n");
            }
        }
        return ret.toString();
    }

    /**
     * Add a package to the type -%gt; package map for imports into the API file
     * 
     * @param type
     *            - the type of object
     * @param pkg
     *            - the package where the type belongs
     */
    protected void addTypeImport(String type, String pkg) {
        typeToPackage.put(type, pkg);
    }

    private Map<String, Set<String>> importsForApiType = new HashMap<String, Set<String>>();

    protected void addImportForApi(String api, String typeName) {
        if (!typeName.isEmpty()) {
            if (!importsForApiType.containsKey(api)) {
                Set<String> types = new HashSet<String>();
                importsForApiType.put(api, types);
            }
            importsForApiType.get(api).add(typeName);
        }
    }

    /**
     * We need to break up the entitlement to parse out the dynamic entitlements and their associated arguments. So now we need a way to re-assemble for
     * documentation etc. Example string: /user/read/$d(docURI)
     * 
     * @param regularEntitlements
     *            - list of regular entitlement paths e.g. /user /read
     * @param dynamicEntitlements
     *            - map of Strings where key is the wildcard and value is the URI arg
     * @return - full entitlement string
     */
    protected String reassembleFullEntitlmentPath(List<String> regularEntitlements, Map<String, String> dynamicEntitlements) {
        StringBuilder fullEntBuilder = new StringBuilder();
        for (String s : regularEntitlements) {
            fullEntBuilder.append(s);
        }
        for (Map.Entry<String, String> entry : dynamicEntitlements.entrySet()) {
            fullEntBuilder.append("/$").append(entry.getKey()).append("(").append(entry.getValue()).append(")");
        }
        return fullEntBuilder.toString();
    }

    private Map<String, String> entPaths = new HashMap<String, String>();

    /**
     * This generates the entitlement name for a given api and a path.
     * 
     * @param apiName
     * @param path
     * @return
     */
    protected String generateEntName(String apiName, String path) {
        String upName = apiName.toUpperCase() + "_" + path.toUpperCase();
        String normName = upName.replaceAll("[/()]", "");
        entPaths.put(normName, path);
        return normName;
    }

    protected Map<String, String> getEntPaths() {
        return entPaths;
    }

    protected void addImportForApi(String api, Set<String> params) {
        for (String t : params) {
            addImportForApi(api, t);
        }
    }

    public void dumpFiles(String outputKernelFolder, String outputApiFolder) {
        dumpFor(outputKernelFolder, kernelTemplates);
        dumpFor(outputApiFolder, apiTemplates);
    }

    public void dumpFor(String outputFolder, Map<String, Map<String, StringTemplate>> templates) {
        // For each file, dump the templates
        for (Map.Entry<String, Map<String, StringTemplate>> file : templates.entrySet()) {
            String outputFile = outputFolder + "/" + file.getKey();
            File f = new File(outputFile);
            f.getParentFile().mkdirs();

            BufferedWriter bow = null;
            try {
                bow = new BufferedWriter(new FileWriter(f));
                Set<String> sections = file.getValue().keySet();
                SortedSet<String> sorted = new TreeSet<String>();
                sorted.addAll(sections);
                for (String sec : sorted) {
                    bow.write(file.getValue().get(sec).toString());
                    bow.newLine();
                }
                bow.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            } finally {
                if (bow != null) {
                    try {
                        bow.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    protected String prefixCase(String part) {
        StringBuffer ret = new StringBuffer();
        ret.append(part.substring(0, 1).toUpperCase());
        ret.append(part.substring(1));
        return ret.toString();
    }

    /**
     * Return the full filename for a java file given an api type and its name
     * 
     * @param api
     * @param name
     * @param suffix
     * @return
     */
    protected String getFileNameForType(String sdkName, String api, String name, String suffix) {
        if (sdkName == null) {
            return String.format(COMMONSHARED_LOC, "/") + api.toLowerCase() + "/" + prefixCase(name) + suffix + ".cs";
        } else {
            return String.format(COMMONSHARED_LOC, sdkName + "/") + api.toLowerCase() + "/" + prefixCase(name) + suffix + ".cs";
        }
    }

    protected String getFileNameForType(String api, String name, String suffix) {
        return getFileNameForType(null, api, name, suffix);
    }

    protected String getTestFileNameForType(String sdkName, String api, String name, String suffix) {
        if (sdkName == null) {
            return String.format(COMMONTEST_LOC, "/") + api.toLowerCase() + "/" + prefixCase(name) + suffix + "Test.cs";
        } else {
            return String.format(COMMONTEST_LOC, sdkName + "/") + api.toLowerCase() + "/" + prefixCase(name) + suffix + "Test.cs";
        }
    }

    protected String getTestFileNameForType(String api, String name, String suffix) {
        return getTestFileNameForType(null, api, name, suffix);
    }

    protected void addApiTemplate(String fileName, String section, StringTemplate template) {
        if (!apiTemplates.containsKey(fileName)) {
            Map<String, StringTemplate> t = new HashMap<String, StringTemplate>();
            apiTemplates.put(fileName, t);
        }
        apiTemplates.get(fileName).put(section, template);
    }

    protected void addKernelTemplate(String fileName, String section, StringTemplate template) {
        System.out.println("Adding kernel template " + fileName);
        if (!kernelTemplates.containsKey(fileName)) {
            Map<String, StringTemplate> t = new HashMap<String, StringTemplate>();
            kernelTemplates.put(fileName, t);
        }
        kernelTemplates.get(fileName).put(section, template);
    }

    protected void addType(String typeName, String packageName, BeanAnnotation bean, CacheableAnnotation cacheable, AddressableAnnotation addressable,
            StorableAnnotation storable, ExtendsAnnotation extend, DeprecatedAnnotation deprecated, IndexedAnnotation indexed, String sdkName,
            Map<String, String> fieldNameToType, List<StringTemplate> beanFields, List<String> constructors) {

        boolean isBean = bean != null || storable != null;

        typeToPackage.put(typeName, packageName);

        if (fieldNameToType != null) {
            FieldTypesRepo.INSTANCE.setClassFields(typeName, fieldNameToType);
        }
        if (addressable != null) {
            schemesMap.put(addressable.getScheme(), addressable.isSchemePrimitive());
        }

        STAttrMap beanClassAttributes = new STAttrMap().put("name", typeName).put("fields", beanFields).put("package", packageName);
        STAttrMap storageMethodAttributes = new STAttrMap().put("name", typeName).put("package", packageName);

        if (isBean) {
            if (storable == null && extend != null) {
                // Okay, this is a special case. I want to extend something
                // that
                // may be a Storable.
                // I don't want to ag the subclass as Storable too because
                // that's silly.
                // So there's going to be some duplication here.
                if (deprecated != null) {
                    beanClassAttributes.put("deprecated", deprecated.getReason());
                }
                if (indexed != null) {
                    beanClassAttributes.put("indexed", indexed.getFields());
                    storageMethodAttributes.put("indexed", indexed.getFields());
                }
                beanClassAttributes.put("extend", extend.getMainClass());
                StringTemplate beanClass = getTemplateLib().getInstanceOf("beanClass", beanClassAttributes);
                String apiFullPath = "Generated/Types/" + typeName + ".cs";
                // TODO HACK ALERT
                if (typeName.indexOf("Storage") > 0) addKernelTemplate(apiFullPath, "1", beanClass);
                else addApiTemplate(apiFullPath, "1", beanClass);

            } else {
                List<StringTemplate> beanAdders = new LinkedList<StringTemplate>();
                List<StringTemplate> storageAdders = new LinkedList<StringTemplate>();

                List<StringTemplate> builderAdders = new LinkedList<StringTemplate>();
                List<StringTemplate> builderFields = new LinkedList<StringTemplate>();
                List<StringTemplate> storageFields = new LinkedList<StringTemplate>();

                if (storable != null) {
                    beanClassAttributes.put("storable", Boolean.TRUE);

                    List<StorableField> pathFields = storable.getFields();
                    for (StorableField field : pathFields) {
                        STAttrMap builderAdderMap = new STAttrMap().put("name", field.getName()).put("separator", storable.getSeparator());
                        if (storable.getEncodingType().length() > 0) {
                            builderAdderMap.put("encoding", storable.getEncodingType());
                        }
                        StringTemplate builderAdder = getTemplateLib().getInstanceOf("builderAdder", builderAdderMap);
                        builderAdders.add(builderAdder);

                        if (StorableFieldType.ID.equals(field.getType())) {
                            StringTemplate beanAdder = getTemplateLib().getInstanceOf("beanAdder", new STAttrMap().put("name", field.getName()));
                            beanAdders.add(beanAdder);
                            StringTemplate storageAdder = getTemplateLib().getInstanceOf("storageAdder", new STAttrMap().put("name", field.getName()));
                            storageAdders.add(storageAdder);

                            String fieldType = fieldNameToType.get(field.getName());
                            if (fieldType == null || fieldType.length() == 0) {
                                if (extend != null && extend.getSuperclass() != null) {
                                    Map<String, String> superClassTypes = FieldTypesRepo.INSTANCE.getFieldToType(extend.getSuperclass());
                                    if (superClassTypes != null) {
                                        fieldType = superClassTypes.get(field.getName());
                                    } else {
                                        System.out.println(String.format("Tried %s from superclass %s... got null superclass", field.getName(),
                                                extend.getSuperclass()));
                                    }
                                }
                            }
                            StringTemplate builderField = getTemplateLib().getInstanceOf("builderEntry",
                                    new STAttrMap().put("fieldName", field.getName()).put("name", typeName).put("fieldType", fieldType));
                            builderFields.add(builderField);

                            storageFields.add(getTemplateLib().getInstanceOf("storageField",
                                    new STAttrMap().put("fieldName", field.getName()).put("fieldType", fieldType)));
                        }
                        beanClassAttributes.put("adders", beanAdders);
                        storageMethodAttributes.put("adders", storageAdders).put("fields", storageFields);
                    }
                    storageMethodAttributes.put("cacheable", cacheable == null ? false : true);

                    if (sdkName != null) {
                        beanClassAttributes.put("sdkName", sdkName);
                        storageMethodAttributes.put("sdkName", sdkName);
                    }

                }

                Collection<String> importList = ImporterRegistry.getImportList(fieldNameToType.values());

                if (deprecated != null) {
                    beanClassAttributes.put("deprecated", deprecated.getReason());
                }
                if (indexed != null) {
                    beanClassAttributes.put("indexed", indexed.getFields());
                }
                if (extend != null) {
                    beanClassAttributes.put("extend", extend.getSuperclass());
                }

                if (constructors != null) {
                    importList.addAll(ImporterRegistry.getImportList(constructors));
                }
                beanClassAttributes.put("importList", importList);

                StringTemplate beanClass = getTemplateLib().getInstanceOf("beanClass", beanClassAttributes);
                String apiFullPath = "Generated/Types/" + typeName + ".cs";
                addApiTemplate(apiFullPath, "1", beanClass);
            }
        }
    }

    @SuppressWarnings("unused")
    private StringTemplate getAddressMethod(String scheme) {
        return getTemplateLib().getInstanceOf("beanAddressMethod", new STAttrMap().put("scheme", scheme));
    }

    protected StringTemplate getDebugMethod(Set<String> fieldNames) {
        List<StringTemplate> equalsEntries = new LinkedList<StringTemplate>();
        for (String fieldName : fieldNames) {
            StringTemplate currentEntry = getTemplateLib().getInstanceOf("beanDebugEntry", new STAttrMap().put("fieldName", fieldName));
            equalsEntries.add(currentEntry);
        }
        return getTemplateLib().getInstanceOf("beanDebug", new STAttrMap().put("entries", equalsEntries));
    }

    @SuppressWarnings("unused")
    private StringTemplate getEqualsMethod(Set<String> fieldNames) {
        List<StringTemplate> equalsEntries = new LinkedList<StringTemplate>();
        for (String fieldName : fieldNames) {
            StringTemplate currentEntry = getTemplateLib().getInstanceOf("beanEqualsEntry", new STAttrMap().put("fieldName", fieldName));
            equalsEntries.add(currentEntry);
        }
        return getTemplateLib().getInstanceOf("beanEquals", new STAttrMap().put("entries", equalsEntries));
    }

    @SuppressWarnings("unused")
    private StringTemplate getHashCode(Set<String> fieldNames) {
        List<StringTemplate> hashCodeEntries = new LinkedList<StringTemplate>();
        for (String fieldName : fieldNames) {
            StringTemplate currentEntry = getTemplateLib().getInstanceOf("beanHashCodeEntry", new STAttrMap().put("fieldName", fieldName));
            hashCodeEntries.add(currentEntry);
        }
        return getTemplateLib().getInstanceOf("beanHashCode", new STAttrMap().put("entries", hashCodeEntries));
    }

    protected String getPackageAndClass(String typeName) {
        String pkg = getPackage(typeName);
        return pkg + "." + typeName;
    }

    private String getPackage(String typeName) {
        String pkg = typeToPackage.get(typeName);
        if (pkg == null) {
            pkg = DEFAULT_PACKAGE;
        }
        return pkg;
    }

    /**
     * Create a new parser instance, pre-supplying the input token stream.
     * 
     * @param input
     *            The stream of tokens that will be pulled from the lexer
     */
    protected AbstractTTreeDotNet(TreeNodeStream input) {
        super(input);
    }

    /**
     * Create a new parser instance, pre-supplying the input token stream and the shared state.
     * 
     * This is only used when a grammar is imported into another grammar, but we must supply this constructor to satisfy the super class contract.
     * 
     * @param input
     *            The stream of tokesn that will be pulled from the lexer
     * @param state
     *            The shared state object created by an interconnectd grammar
     */
    protected AbstractTTreeDotNet(TreeNodeStream input, RecognizerSharedState state) {
        super(input, state);
    }

    /**
     * Creates the error/warning message that we need to show users/IDEs when ANTLR has found a parsing error, has recovered from it and is now telling us that
     * a parsing exception occurred.
     * 
     * @param tokenNames
     *            token names as known by ANTLR (which we ignore)
     * @param e
     *            The exception that was thrown
     */
    @Override
    public void displayRecognitionError(String[] tokenNames, RecognitionException e) {

        // This is just a place holder that shows how to override this method
        //
        super.displayRecognitionError(tokenNames, e);
    }

    List<StringTemplate> callNameEntries = new LinkedList<StringTemplate>();

    public void addCallNameEntry(StringTemplate entry) {
        callNameEntries.add(entry);
    }

    public List<StringTemplate> getCallNameEntries() {
        return callNameEntries;
    }

    List<String> apiNames = new LinkedList<String>();

    public void addApiName(String apiName) {
        apiNames.add(apiName);
    }

    public List<String> getApiNames() {
        return apiNames;
    }

    public abstract void setTemplateLib(StringTemplateGroup templates);

    public String formatDoc(String raw) {
        return raw.replaceAll("@@", "\n").replaceAll("@", "\n@");
    }

    @Override
    public void reportError(RecognitionException e) {
        super.reportError(e);
        throw new IllegalArgumentException("Failed");
    }

    /**
     * Returns a generated path under build/generated-sources/main/java/rapture for the current file. The path will be slightly different if we have an sdkName
     * defined
     * 
     * @param sdkName
     *            The name of the SDK. If set, then sdkPackage will be appended to this. If null, noSdkPackage will be used instead
     * @param sdkPackage
     * @param noSdkPackage
     * @param constantPart
     *            This is always appended at the end
     * @return
     */
    protected String getGeneratedFilePath(String sdkName, String sdkPackage, String noSdkPackage, String constantPart) {
        if (sdkName == null) {
            return String.format(GENERATED_PATH, constantPart);
        } else {
            return String.format(GENERATED_PATH, constantPart);
        }
    }

    private static final String GENERATED_PATH = "Generated/%s";

    public abstract StringTemplateGroup getTemplateLib();

    protected String convertApiName(String apiName) {
        if (apiName.equals("Lock")) {
            return "Rlock";
        } else if (apiName.equals("Event")) {
            return "Revent";
        }
        return apiName;
    }

    protected String convertFieldName(String fieldName) {
        if (fieldName.equals("event")) {
            return "revent";
        } else if (fieldName.equals("lock")) {
            return "rlock";
        } else if (fieldName.equals("params")) {
            return "rparams";
        }
        return fieldName;
    }

    public void processHmxdef(String sdkName, List<StringTemplate> ents, List<StringTemplate> kEntries, List<StringTemplate> kSetups, int major, int minor,
            int micro, int minMajor, int minMinor, int minMicro) {

        String clientVersionPath = getGeneratedFilePath(sdkName, "client", "client", "ClientApiVersion.cs");
        StringTemplate clientVersionVal = getTemplateLib().getInstanceOf(
                "versionConst",
                new STAttrMap().put("sdkname", sdkName).put("major", major).put("minor", minor).put("micro", micro).put("type", "Client")
                        .put("packageSuffix", "client"));
        addApiTemplate(clientVersionPath, "1", clientVersionVal);

    }

    @SuppressWarnings("unused")
    private void generateSchemeTemplate(String sdkName) {
        List<StringTemplate> fields = generateSchemeFieldTemplates();
        StringTemplate schemeTemplate = getTemplateLib().getInstanceOf("scheme", new STAttrMap().put("fields", fields));

        String path = getGeneratedFilePath(sdkName, "common", "common", "Scheme.cs");
        addApiTemplate(path, "1", schemeTemplate);
    }

    private List<StringTemplate> generateSchemeFieldTemplates() {
        List<StringTemplate> list = new LinkedList<StringTemplate>();
        for (Entry<String, Boolean> entry : schemesMap.entrySet()) {
            StringTemplate template = getTemplateLib().getInstanceOf("schemeField",
                    new STAttrMap().put("name", entry.getKey()).put("isPrimitive", entry.getValue().toString()));
            list.add(template);
        }
        return list;
    }

    /**
     * allows convenient multi-value initialization: "new STAttrMap().put(...).put(...)"
     */
    @SuppressWarnings("serial")
    public static class STAttrMap extends HashMap<String, Object> {
        public STAttrMap put(String attrName, Object value) {
            super.put(attrName, value);
            return this;
        }
    }

}
