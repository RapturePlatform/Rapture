/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2016 Incapture Technologies LLC
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.TreeNodeStream;
import org.antlr.runtime.tree.TreeParser;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.incapture.rapgen.annotations.AddressableAnnotation;
import com.incapture.rapgen.annotations.BeanAnnotation;
import com.incapture.rapgen.annotations.CacheableAnnotation;
import com.incapture.rapgen.annotations.DeprecatedAnnotation;
import com.incapture.rapgen.annotations.ExtendsAnnotation;
import com.incapture.rapgen.annotations.FTSAnnotation;
import com.incapture.rapgen.annotations.IndexedAnnotation;
import com.incapture.rapgen.annotations.SearchableAnnotation;
import com.incapture.rapgen.annotations.StorableAnnotation;
import com.incapture.rapgen.annotations.storable.EncodingMap;
import com.incapture.rapgen.annotations.storable.StorableField;
import com.incapture.rapgen.annotations.storable.StorableFieldType;
import com.incapture.rapgen.docString.DocumentationParser;
import com.incapture.rapgen.output.OutputWriter;
import com.incapture.rapgen.storable.StorableAttributes;
import com.incapture.rapgen.storable.StorableInfo;

/**
 * This class is used as the base for the parser - we call methods in this class from the Tree walker code.
 *
 * @author amkimian
 */
public abstract class AbstractTTree extends TreeParser {
    private static final String COMMONTEST_LOC = "build/generated-sources/test/java/rapture/%scommon/shared/";
    private static final String COMMONSHARED_LOC = "build/generated-sources/main/java/rapture/%scommon/shared/";
    public static final String GEN_PATH_PREFIX = "build/generated-sources/main/";
    public static final String GEN_PATH_PREFIX_JAVA = GEN_PATH_PREFIX + "java/";
    private static final String DEFAULT_PACKAGE = "rapture.common";
    /**
     * So this abstract tree will be used to host a number of generated files with a generated file simply being a list of templates ordered by some means
     * <p>
     * We can add a template to a file + section
     */

    private Map<String, Map<String, StringTemplate>> kernelTemplates = new HashMap<String, Map<String, StringTemplate>>();
    private Map<String, Map<String, StringTemplate>> apiTemplates = new HashMap<String, Map<String, StringTemplate>>();

    private List<String> purgeList = new LinkedList<>();
    private List<String> indexInfoList = new LinkedList<>();

    /**
     * Here we store type information (name and package really)
     */

    private Map<String, String> typeToPackage = new HashMap<String, String>();

    private Map<String, Boolean> schemesMap = new HashMap<String, Boolean>();

    private List<StorableAttributes> storableAttributes = new LinkedList<>();

    private Set<String> searchTypes = new HashSet<>();

    public List<StorableAttributes> getStorableAttributes() {
        return storableAttributes;
    }

    /**
     * Return the type imports for a given api.
     *
     * @param api
     * @return
     */
    protected String getTypeImports(String api) {
        Set<String> types = importsForApiType.get(api);
        StringBuilder ret = new StringBuilder();
        if (types != null) {
            for (String type : types) {
                ret.append("import ");
                ret.append(getPackage(type));
                ret.append(".");
                ret.append(type);
                ret.append(";\n");
            }
        }
        return ret.toString();
    }

    /**
     * Add a package to the type-&gt;package map for imports into the API file
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
        OutputWriter.writeMultiPartTemplates(outputKernelFolder, kernelTemplates);
        OutputWriter.writeMultiPartTemplates(outputApiFolder, apiTemplates);
        OutputWriter.writeList(outputKernelFolder, purgeList, "resources/rapture/storable/purgeList.txt");
        OutputWriter.writeList(outputKernelFolder, indexInfoList, "resources/rapture/storable/indexInfoList.txt");
    }

    protected String prefixCase(String part) {
        return part.substring(0, 1).toUpperCase() + part.substring(1);
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
            return String.format(COMMONSHARED_LOC, "/") + api.toLowerCase() + "/" + prefixCase(name) + suffix + ".java";
        } else {
            return String.format(COMMONSHARED_LOC, sdkName + "/") + api.toLowerCase() + "/" + prefixCase(name) + suffix + ".java";
        }
    }

    protected String getTestFileNameForType(String sdkName, String api, String name, String suffix) {
        if (sdkName == null) {
            return String.format(COMMONTEST_LOC, "/") + api.toLowerCase() + "/" + prefixCase(name) + suffix + "Test.java";
        } else {
            return String.format(COMMONTEST_LOC, sdkName + "/") + api.toLowerCase() + "/" + prefixCase(name) + suffix + "Test.java";
        }
    }

    protected void addApiTemplate(String fileName, String section, StringTemplate template) {
        if (!apiTemplates.containsKey(fileName)) {
            Map<String, StringTemplate> t = new HashMap<String, StringTemplate>();
            apiTemplates.put(fileName, t);
        }
        apiTemplates.get(fileName).put(section, template);
    }

    protected void addKernelTemplate(String fileName, String section, StringTemplate template) {
        if (!kernelTemplates.containsKey(fileName)) {
            Map<String, StringTemplate> t = new HashMap<String, StringTemplate>();
            kernelTemplates.put(fileName, t);
        }
        kernelTemplates.get(fileName).put(section, template);
    }

    protected void addType(String typeName, String packageName, BeanAnnotation bean, CacheableAnnotation cacheable, AddressableAnnotation addressable,
            StorableAnnotation storable, SearchableAnnotation searchable, FTSAnnotation fts, ExtendsAnnotation extend, DeprecatedAnnotation deprecated,
            List<IndexedAnnotation> indices, String sdkName,
            Map<String, String> fieldNameToType, List<StringTemplate> beanFields, List<String> constructors) {

        boolean isBean = bean != null || storable != null;
        boolean isSearchable = searchable != null;
        boolean isFTS = fts != null;

        typeToPackage.put(typeName, packageName);

        if (fieldNameToType != null) {
            FieldTypesRepo.INSTANCE.setClassFields(packageName + "." + typeName, fieldNameToType);
        } else {
            fieldNameToType = new HashMap<>();
        }
        if (addressable != null) {
            schemesMap.put(addressable.getScheme(), addressable.isSchemePrimitive());
        }

        String packagePath = packageName.replaceAll("\\.", "/");
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
                beanClassAttributes.put("extend", extend.getSuperclass());
                StringTemplate beanClass = getTemplateLib().getInstanceOf("beanClass", beanClassAttributes);
                String apiFullPath = GEN_PATH_PREFIX_JAVA + packagePath + "/" + typeName + ".java";
                // HACK ALERT
                if (typeName.indexOf("Storage") > 0) addKernelTemplate(apiFullPath, "1", beanClass);
                else addApiTemplate(apiFullPath, "1", beanClass);

            } else {
                List<StringTemplate> beanAdders = new LinkedList<StringTemplate>();
                List<StringTemplate> storageAdders = new LinkedList<StringTemplate>();

                List<StringTemplate> builderAdders = new LinkedList<StringTemplate>();
                List<StringTemplate> builderFields = new LinkedList<StringTemplate>();
                List<StringTemplate> storageFields = new LinkedList<StringTemplate>();
                List<StringTemplate> storageMethods = new LinkedList<StringTemplate>();

                if (storable != null) {
                    beanClassAttributes.put("searchable", isSearchable);
                    beanClassAttributes.put("storable", Boolean.TRUE);
                    storageMethodAttributes.put("searchable", isSearchable);
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

                    if (sdkName != null) {
                        beanClassAttributes.put("sdkName", sdkName);
                        storageMethodAttributes.put("sdkName", sdkName);
                    }
                    storageMethods.add(getTemplateLib().getInstanceOf("beanStorageMethods", storageMethodAttributes));
                }

                ImmutableMap.Builder<String, Object> storageClassAttributes = ImmutableMap.builder();
                Collection<String> importList = ImporterRegistry.getImportList(fieldNameToType.values());
                if (addressable != null) {
                    beanClassAttributes.put("potentialAddressable", ", Addressable");
                    importList.add("import rapture.object.Addressable;");
                    importList.add("import rapture.common.Scheme;");
                    importList.add("import rapture.common.model.IndexConfig;");
                    beanClassAttributes.put("addressMethod", getAddressMethod(addressable.getScheme()));

                    storageMethods.add(getTemplateLib().getInstanceOf("beanStorageAddressableMethods", storageMethodAttributes));
                    storageClassAttributes.put("importFactory", "import rapture.object.storage.StorageLocationFactory;");
                    storageClassAttributes.put("fts", isFTS);
                    if (isFTS || isSearchable) {
                        searchTypes.add(addressable.getScheme().toLowerCase());
                    }
                }

                if (deprecated != null) {
                    beanClassAttributes.put("deprecated", deprecated.getReason());
                }
                if (extend != null) {
                    beanClassAttributes.put("extend", extend.getSuperclass());
                }

                if (constructors != null) {
                    importList.addAll(ImporterRegistry.getImportList(constructors));
                }
                beanClassAttributes.put("importList", importList);

                StringTemplate hashCodeMethod = getHashCode(fieldNameToType.keySet());
                beanClassAttributes.put("hashCodeMethod", hashCodeMethod);
                StringTemplate equalsMethod = getEqualsMethod(fieldNameToType.keySet());
                beanClassAttributes.put("equalsMethod", equalsMethod);
                StringTemplate debugMethod = getDebugMethod(fieldNameToType.keySet());
                beanClassAttributes.put("debugMethod", debugMethod);

                StringTemplate beanClass = getTemplateLib().getInstanceOf("beanClass", beanClassAttributes);
                String apiFullPath = GEN_PATH_PREFIX_JAVA + packagePath + "/" + typeName + ".java";
                addApiTemplate(apiFullPath, "1", beanClass);

                if (storable != null) {
                    StorableInfo storableInfo = createStorableInfo(typeName, packageName, storable);

                    STAttrMap pathBuilderAttributes = new STAttrMap().put("sdkName", sdkName).put("name", typeName).put("fields", builderFields)
                            .put("adders", builderAdders).put("package", packageName);
                    if (storable.getEncodingType().length() > 0) {
                        pathBuilderAttributes.put("encodeMethod", EncodingMap.getEncodeMethod(storable.getEncodingType()));
                        pathBuilderAttributes.put("encodeImport", EncodingMap.getEncodeImport(storable.getEncodingType()));
                    }
                    if (indices.size() > 0) {
                        pathBuilderAttributes.put("isIndexable", true);
                    }
                    if (storableInfo.getRepoName().isPresent()) {
                        pathBuilderAttributes.put("repoNameOverride", storableInfo.getRepoName().get());
                    }

                    String prefix;
                    if (storableInfo.getPrefix().isPresent()) {
                        prefix = storableInfo.getPrefix().get();
                    } else {
                        prefix = String.format("\"versioned/%s/v1\"", storableInfo.getStorableName());
                    }

                    pathBuilderAttributes.put("prefix", prefix);

                    StringTemplate pathBuilderClass = getTemplateLib().getInstanceOf("beanBuilderClass", pathBuilderAttributes);
                    String pathBuilderFullPath = GEN_PATH_PREFIX_JAVA + packagePath + "/" + typeName + "PathBuilder.java";
                    addApiTemplate(pathBuilderFullPath, "1", pathBuilderClass);
                    storageClassAttributes.put("name", typeName).put("methods", storageMethods).put("adders", storageAdders).put("package", packageName);

                    storableAttributes.add(new StorableAttributes(typeName, packagePath, sdkName, packageName, storageClassAttributes, indices,
                            fieldNameToType, cacheable));

                    String fqdn = String.format("%s.%sIndexInfo", packageName, typeName);
                    indexInfoList.add(fqdn);

                    if (storable.getTtl() != null) {
                        generatePurgeInfoClass(typeName, storable, sdkName, packagePath, packageName);
                    }
                }
            }
        }
    }

    private StorableInfo createStorableInfo(String typeName, String packageName, StorableAnnotation storable) {
        StorableInfo storableInfo = new StorableInfo();
        storableInfo.setPrefix(storable.getPrefix());
        if (storable.getRepoName().isPresent()) {
            storableInfo.setRepoName(storable.getRepoName());
        } else if (storable.getRepoConstant().isPresent()) {
            storableInfo.setRepoName(Optional.of("RaptureConstants." + storable.getRepoConstant().get()));
        }
        storableInfo.setStorableName(typeName);
        storableInfo.setStorablePackage(packageName);
        return storableInfo;
    }

    private void generatePurgeInfoClass(String typeName, StorableAnnotation storable, String sdkName, String packagePath, Object packageName) {
        STAttrMap purgeClassAttributes = new STAttrMap().put("sdkName", sdkName).put("name", typeName).put("ttl", storable.getTtl())
                .put("package", packageName);
        StringTemplate purgeClass = getTemplateLib().getInstanceOf("purgeInfoClass", purgeClassAttributes);
        String purgeClassFullPath = GEN_PATH_PREFIX_JAVA + packagePath + "/" + typeName + "PurgeInfo.java";
        addKernelTemplate(purgeClassFullPath, "1", purgeClass);

        String fqdn = String.format("%s.%sPurgeInfo", packageName, typeName);
        purgeList.add(fqdn);
    }

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

    private StringTemplate getEqualsMethod(Set<String> fieldNames) {
        List<StringTemplate> equalsEntries = new LinkedList<StringTemplate>();
        for (String fieldName : fieldNames) {
            StringTemplate currentEntry = getTemplateLib().getInstanceOf("beanEqualsEntry", new STAttrMap().put("fieldName", fieldName));
            equalsEntries.add(currentEntry);
        }
        return getTemplateLib().getInstanceOf("beanEquals", new STAttrMap().put("entries", equalsEntries));
    }

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
    protected AbstractTTree(TreeNodeStream input) {
        super(input);
    }

    /**
     * Create a new parser instance, pre-supplying the input token stream and the shared state.
     * <p>
     * This is only used when a grammar is imported into another grammar, but we must supply this constructor to satisfy the super class contract.
     *
     * @param input
     *            The stream of tokesn that will be pulled from the lexer
     * @param state
     *            The shared state object created by an interconnectd grammar
     */
    protected AbstractTTree(TreeNodeStream input, RecognizerSharedState state) {
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
        raw = raw.replaceAll("   ", " ").replaceAll("\n", "");

        StringBuilder docStringBuilder = new StringBuilder(DocumentationParser.retrieveDescription(raw));
        LinkedHashMap<String, String> params = DocumentationParser.retrieveParams(raw);
        if (params.size() > 0) {
            for (Entry<String, String> entry : params.entrySet()) {
                docStringBuilder.append("@params ").append(entry.getKey()).append(" ").append(entry.getValue()).append("\n");
            }
        }

        if (raw.indexOf("@since") != -1) {
            docStringBuilder.append("@since ").append(DocumentationParser.retrieveSince(raw));
        }

        if (raw.indexOf("@return") != -1) {
            docStringBuilder.append("@return ").append(DocumentationParser.retrieveReturn(raw));
        }

        if (docStringBuilder.charAt(docStringBuilder.length() - 1) == '\n') {
            docStringBuilder = new StringBuilder(docStringBuilder.substring(0, docStringBuilder.length() - 1));
        }

        return docStringBuilder.toString().replaceAll("\n", "\n* ").replaceAll(" +", " ");

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
            return String.format(GENERATED_PATH, noSdkPackage, constantPart);
        } else {
            return String.format(GENERATED_PATH, sdkName + "/" + sdkPackage, constantPart);
        }
    }

    private static final String GENERATED_PATH = "build/generated-sources/main/java/rapture/%s/%s";

    public abstract StringTemplateGroup getTemplateLib();

    public void processHmxdef(String sdkName, List<StringTemplate> ents, List<StringTemplate> kEntries, List<StringTemplate> kSetups, int major, int minor,
            int micro, int minMajor, int minMinor, int minMicro) {
        StringTemplate entVals = getTemplateLib().getInstanceOf("entValues", new STAttrMap().put("sdkname", sdkName).put("ents", getEntPaths()));
        StringTemplate s = getTemplateLib().getInstanceOf("fullEnt", new STAttrMap().put("sdkname", sdkName).put("ents", ents));

        String fullPath = getGeneratedFilePath(sdkName, "server", "common", "EntitlementSet.java");
        addKernelTemplate(fullPath, "1", s);

        String scriptApiNamePath;
        if (sdkName != null) {
            String upperName = sdkName;
            upperName = upperName.substring(0, 1).toUpperCase() + upperName.substring(1);
            StringTemplate k = getTemplateLib().getInstanceOf("sdkKernel",
                    new STAttrMap().put("sdkname", sdkName).put("entries", kEntries).put("setups", kSetups));
            String kernelPath = "build/generated-sources/main/java/rapture/" + sdkName + "/kernel/" + upperName + "Kernel.java";
            addKernelTemplate(kernelPath, "1", k);
            StringTemplate kScript = getTemplateLib().getInstanceOf("sdkKernelScript", new STAttrMap().put("sdkname", sdkName).put("apiNames", apiNames));
            String kernelScriptPath = "build/generated-sources/main/java/rapture/" + sdkName + "/kernel/" + upperName + "KernelScript.java";
            addKernelTemplate(kernelScriptPath, "1", kScript);

            scriptApiNamePath = getGeneratedFilePath(sdkName, "common", "common", String.format("api/%sScriptingApi.java", prefixCase(sdkName)));
        } else {
            scriptApiNamePath = getGeneratedFilePath(null, "common", "common", "api/ScriptingApi.java");
        }

        String altPath = getGeneratedFilePath(sdkName, "server", "common", "EntitlementConst.java");
        addKernelTemplate(altPath, "1", entVals);

        String callNamePath = getGeneratedFilePath(sdkName, "server", "common", "hooks/CallName.java");
        StringTemplate callNameTemplate = getTemplateLib().getInstanceOf("callNamesEnum",
                new STAttrMap().put("sdkname", sdkName).put("ents", getCallNameEntries()));
        addKernelTemplate(callNamePath, "1", callNameTemplate);
        String apiNamePath = getGeneratedFilePath(sdkName, "server", "common", "hooks/ApiName.java");
        StringTemplate apiNameTemplate = getTemplateLib().getInstanceOf("apiNamesEnum", new STAttrMap().put("sdkname", sdkName).put("ents", getApiNames()));
        addKernelTemplate(apiNamePath, "1", apiNameTemplate);

        StringTemplate scriptApiNameTemplate = getTemplateLib().getInstanceOf("scriptApiInterface",
                new STAttrMap().put("sdkname", sdkName).put("ents", getApiNames()));
        addApiTemplate(scriptApiNamePath, "1", scriptApiNameTemplate);

        String serverVersionPath = getGeneratedFilePath(sdkName, "server", "server", "ServerApiVersion.java");
        StringTemplate serverVersionVal = getTemplateLib().getInstanceOf(
                "versionConstWithMinimum",
                new STAttrMap().put("sdkname", sdkName).put("major", major).put("minor", minor).put("micro", micro).put("minmajor", minMajor)
                        .put("minminor", minMinor).put("minmicro", minMicro).put("type", "Server").put("packageSuffix", "server"));
        addKernelTemplate(serverVersionPath, "1", serverVersionVal);

        String clientVersionPath = getGeneratedFilePath(sdkName, "client", "client", "ClientApiVersion.java");
        StringTemplate clientVersionVal = getTemplateLib().getInstanceOf(
                "versionConst",
                new STAttrMap().put("sdkname", sdkName).put("major", major).put("minor", minor).put("micro", micro).put("type", "Client")
                        .put("packageSuffix", "client"));
        addApiTemplate(clientVersionPath, "1", clientVersionVal);

        if (sdkName == null && schemesMap.keySet().size() > 0) {
            // currently don't generate Scheme.java file for sdk stuff
            generateSchemeTemplate(null);
        }

        String searchRepoTypePath = getGeneratedFilePath(sdkName, "search", "search", "SearchRepoType.java");
        STAttrMap searchRepoTypeAttributes = new STAttrMap();
        searchRepoTypeAttributes.put("schemes", searchTypes);
        if (sdkName != null) {
            searchRepoTypeAttributes.put("sdkName", sdkName + ".");
        }
        StringTemplate searchRepoType = getTemplateLib().getInstanceOf("searchRepoType", searchRepoTypeAttributes);
        addApiTemplate(searchRepoTypePath, "1", searchRepoType);
    }

    private void generateSchemeTemplate(String sdkName) {
        List<StringTemplate> fields = generateSchemeFieldTemplates();
        StringTemplate schemeTemplate = getTemplateLib().getInstanceOf("scheme", new STAttrMap().put("fields", fields));

        String path = getGeneratedFilePath(sdkName, "common", "common", "Scheme.java");
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
