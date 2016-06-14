// The tree walker for the Java API generator

tree grammar TTree;

options {
    language   = Java;

    // Use the vocab from the parser (not the lexer)
    tokenVocab = TParser;
    ASTLabelType=CommonTree;
    output=template;
    superClass = AbstractTTree;
}

import TTreeShared ;

@header {
    package com.incapture.rapgen;
    import java.util.Collection;
    import java.util.LinkedList;
    import java.util.Map;
    import java.util.HashMap;
    import java.util.Set;
    import java.util.HashSet;
    import com.incapture.rapgen.annotations.*;
}

@members {
    boolean isSdkExpected = false;
    String sdkName;
}

innerExpr : ^(INNERT typeExpr)
            | ^(INNERC crudTypeExpr) { System.out.println("CRUD TYPE EXPR"); } 
            | ^(INNERA a=apiExpr {
    $hmxdef::ents.add($a.entList); $hmxdef::kEntries.add($a.kEntry); $hmxdef::kSetups.add($a.kSetup); } 
    );

sdkGen
@init {
    isSdkExpected = true;
    }
    : hmxdef
    ;

apiGen
@init {
    isSdkExpected = false;
    }
    : hmxdef
    ;
    
hmxdef 
scope { 
        List<StringTemplate> ents; 
        List<StringTemplate> kEntries; 
        List<StringTemplate> kSetups;
      }
@init {
        sdkName = null;
        $hmxdef::ents = new ArrayList<StringTemplate>();
        $hmxdef::kEntries = new ArrayList<StringTemplate>();
        $hmxdef::kSetups = new ArrayList<StringTemplate>(); 
      }
    : ^(MAIN (sdkExpr { sdkName = $sdkExpr.name; })?
        {
            if (isSdkExpected && sdkName == null) {
                throw new IllegalArgumentException("Error: SDK Name is required for this grammar");
            }
            else if (!isSdkExpected && sdkName != null) {
                throw new IllegalArgumentException("Error: SDK Name is not expected in this grammar, but found: " + sdkName);
            }
        }
        versionExpr minVerExpr innerExpr*)
    {processHmxdef(sdkName, $hmxdef::ents, $hmxdef::kEntries, $hmxdef::kSetups, $versionExpr.major, $versionExpr.minor, $versionExpr.micro, $minVerExpr.major, $minVerExpr.minor, $minVerExpr.micro);}
    ;

sdkExpr returns [String name]: ^(SDKDEF ID) { $name = $ID.text; };

crudTypeExpr scope { 
        String crudTypeName;
        BeanAnnotation bean;
        AddressableAnnotation addressable;
        StorableAnnotation storable;
        SearchableAnnotation searchable;
        ExtendsAnnotation extend;
        DeprecatedAnnotation deprecated;
    } :
    ^(CRUDTYPEDEF doc=DOC
        (
            addressableAnnotation { 
                $crudTypeExpr::addressable = $addressableAnnotation.result;
            }
        |   storableAnnotation {
                $crudTypeExpr::storable = $storableAnnotation.result;
            }
        |   searchableAnnotation {
                $crudTypeExpr::searchable = $searchableAnnotation.result;
            }
        |   extendsAnnotation {
                $crudTypeExpr::extend = $extendsAnnotation.result;
            }
        |   deprecatedAnnotation {
                $crudTypeExpr::deprecated = $deprecatedAnnotation.result;
            }
        |   beanAnnotation {
                $crudTypeExpr::bean = $beanAnnotation.result;
            }
        )*
        crudPackageAnnotation? name=ID { $crudTypeExpr::crudTypeName = $name.text; } crudTypeInfo typeFields) {
    // We need to generate
    // (1) A bean document for this type
    // (2) A special API subsection for this type (with get, put, delete, getChildren type calls) (so that should be registerd in the same way as an API)
    // (3) The implementation for this API in the Kernel code (special for this crud type)
    // (4) Potentially a nice client set of classes to make it easier to interact with this type
        
    String packageToUse = $crudPackageAnnotation.pkg == null ? "rapture.common" : $crudPackageAnnotation.pkg;
    addTypeImport($name.text, packageToUse);
    StringTemplate debugMeth = getDebugMethod($typeFields.fieldNameToType.keySet());
    Collection<String> importList = ImporterRegistry.getImportList($typeFields.fieldNameToType.values());
    StringTemplate beanClass = %beanClass(name={$name.text}, fields={$typeFields.beans}, adders={$crudTypeInfo.storageAdders}, package={packageToUse}, debugMethod={debugMeth}, storable={$crudTypeExpr::storable}, importList={importList});

    String packagePath = packageToUse.replaceAll("\\.", "/");
    String apiFullPath = "build/generated-sources/main/java/" + packagePath + "/" + $name.text + ".java";
    addApiTemplate(apiFullPath, "1", beanClass);
    
    StringTemplate builderClass = %beanBuilderClass(sdkName={sdkName}, name={$name.text}, fields={$crudTypeInfo.builderAdders}, adders={$crudTypeInfo.builderEntries}, package={packageToUse});
    String apiBuilderFullPath = "build/generated-sources/main/java/" +  packagePath + "/" + $name.text + "PathBuilder.java";
    addApiTemplate(apiBuilderFullPath, "1", builderClass);
    
    String crudApiImplPackage = "rapture." + sdkName + ".kernel";
    StringTemplate crudApiImpl = %crudApiImpl(name={$name.text}, package={crudApiImplPackage}, typePackage={packageToUse});
    String crudApiImplFullPath = "build/generated-sources/main/java/rapture/" + sdkName + "/kernel/" + $name.text + "ApiImpl.java";
    addKernelTemplate(crudApiImplFullPath, "1", crudApiImpl);
};

crudTypeInfo returns [List<StringTemplate> storageAdders, List<StringTemplate> builderAdders, List<StringTemplate> builderEntries] 
  @init { $storageAdders = new ArrayList<StringTemplate>(); $builderAdders = new ArrayList<StringTemplate>(); $builderEntries = new ArrayList<StringTemplate>(); } 
    : ^(CRUDINFO (crudDirective { $storageAdders.add($crudDirective.storageAdder); 
                                  $builderAdders.add($crudDirective.storageBuilder); 
                                  $builderEntries.add($crudDirective.builderAdder);
                                  })*);

crudDirective returns [StringTemplate storageAdder, StringTemplate storageBuilder, StringTemplate builderAdder] : ^(CRUDINFOENTRY n=ID) {
    $storageAdder = %storageAdder(name={$n.text});
    $storageBuilder = %builderEntry(fieldName={$n.text}, name={$crudTypeExpr::crudTypeName}, fieldType={"String"});
    $builderAdder = %builderAdder(name={$n.text}, separator={"\"/\""});
};

typeExpr
    scope {
        BeanAnnotation bean;
        AddressableAnnotation addressable;
        StorableAnnotation storable;
        SearchableAnnotation searchable;
        FTSAnnotation fts;
        ExtendsAnnotation extend;
        DeprecatedAnnotation deprecated;
        List<IndexedAnnotation> indices;
        CacheableAnnotation cacheable;
    }
    @init {
        $typeExpr::indices = new LinkedList<>();
    }
    :
    ^(TYPEDEF doc=DOC 
        (
            addressableAnnotation { 
                $typeExpr::addressable = $addressableAnnotation.result;
            }
        |   storableAnnotation {
                $typeExpr::storable = $storableAnnotation.result;
            }
        |   searchableAnnotation {
                $typeExpr::searchable = $searchableAnnotation.result;
            }
        |   extendsAnnotation {
                $typeExpr::extend = $extendsAnnotation.result;
            }
        |   ftsAnnotation {
        		$typeExpr::fts = $ftsAnnotation.result;
        	}
        |   deprecatedAnnotation {
                $typeExpr::deprecated = $deprecatedAnnotation.result;
            }
        |   indexedAnnotation {
                $typeExpr::indices.add($indexedAnnotation.result);
            }
        |   beanAnnotation {
                $typeExpr::bean = $beanAnnotation.result;
            }
        |   cacheableAnnotation {
                $typeExpr::cacheable = $cacheableAnnotation.result;
            }
        )*
        name=ID typeAspect typeFields) {
            // Record the type for later use, also build class for it
            addType($name.text, $typeAspect.name, $typeExpr::bean, $typeExpr::cacheable, $typeExpr::addressable, $typeExpr::storable, $typeExpr::searchable, $typeExpr::fts, $typeExpr::extend, $typeExpr::deprecated, $typeExpr::indices, sdkName, $typeFields.fieldNameToType,
                $typeFields.beans, $typeFields.constructors);
    };

typeAspect returns [String name]: 
    ^(TYPEASPECT p=PACKAGENAME) { $name = $p.text; };

crudPackageAnnotation returns [String pkg]:
    ^(CRUDPACKAGEANNOTATION p=PACKAGENAME) { $pkg = $p.text; };

deprecatedAspect returns [String reason]
//example: Deprecated="because it's rubbish"
    : ( DEPRECATED ID { $reason = $ID.text; } ) 
    | ( DEPRECATED STRING  { $reason = $STRING.text.substring(1, $STRING.text.length() -1); } )
    ;
    
typeFields returns [Map<String, String> fieldNameToType, List<StringTemplate> beans, List<String> constructors ] 
@init { 
    $fieldNameToType = new HashMap<String, String>();
    $beans = new ArrayList<StringTemplate>();
    $constructors = new ArrayList<String>();
} 
    :
    ^(TYPEFIELDS 
        (typeFieldDef {
            $fieldNameToType.put($typeFieldDef.fieldName, $typeFieldDef.varType);
            $beans.add($typeFieldDef.beanInfo);
            if ($typeFieldDef.constructor != null) {
                $constructors.add($typeFieldDef.constructor);
            }
         })*
     )
    ;

typeFieldDef returns [String fieldName, StringTemplate beanInfo, String varType, String constructor]
    :
    ^(TYPEMEMBER 
        ^(TYPE vartype) 
        ^(NAME name=ID) 
        ^(FIELD_CONSTRUCTOR (fieldConstructor { $constructor = $fieldConstructor.constructor; })?)
     ) { 
        boolean isURI = $vartype.mainType.endsWith("URI");
        String scheme = ($vartype.mainType).equals("RaptureURI") ? null : ($vartype.mainType).substring(0, $vartype.mainType.length()-3).toUpperCase();
        $fieldName = $name.text;
        $beanInfo = %beanInfo(varName={$name.text},
		varType={(isURI) ? "String" : $vartype.mainType},
		constructor={$constructor},
		isURI={isURI},
		scheme={scheme});
        $varType = (isURI) ? "String" : $vartype.mainType;
    }
    ;

/**
 * Record whether the type is a mapped one (a complex one) or not
 */
 
vartype returns [String mainType, boolean isMappedType, String importType, String defValue, boolean equalsTest, String genericAlt]:
    ^(RAW INTTYPE) { $mainType = "int"; $isMappedType = false; $importType = ""; $defValue = "0"; $equalsTest=true;}
    | ^(RAW LONGTYPE) { $mainType = "long"; $isMappedType = false; $importType = ""; $defValue = "0L"; $equalsTest=true;}
    | ^(RAW BYTEARRAYTYPE) { $mainType = "byte[]"; $isMappedType = false; $importType = ""; $defValue = "null"; $equalsTest=true;}
    | ^(RAW DATETYPE) { $mainType = "Date"; $isMappedType = false; $importType = ""; $defValue = "null"; $equalsTest=true;}
    | ^(RAW OBJECTTYPE) { $mainType = "Object"; $isMappedType = false; $importType = ""; $defValue = "null";$equalsTest=true;}
    | ^(RAW LONGCTYPE) { $mainType = "Long"; $isMappedType = false; $importType = ""; $defValue = "0L";$equalsTest=true;}
    | ^(RAW BOOLTYPE) { $mainType = "Boolean"; $isMappedType = false; $importType = ""; $defValue = "true";$equalsTest=true;}
    | ^(RAW DOUBLETYPE) { $mainType = "Double"; $isMappedType = false; $importType = ""; $defValue = "0.0";$equalsTest=false;}
    | ^(RAW STRINGTYPE) { $mainType = "String"; $isMappedType = false; $importType = ""; $defValue = "\"job://test\"";$equalsTest=false;}
    | ^(RAW VOIDTYPE) { $mainType = "void"; $isMappedType = false; $importType = "";}
    | ^(LISTTYPE v=vartype) {
            $isMappedType = $v.isMappedType; $importType = $v.mainType; $defValue="null"; $equalsTest=true;
            $mainType = String.format("List<\%s>", $v.genericAlt == null ? $v.mainType : $v.genericAlt);
        }
    | ^(SETTYPE v=vartype) { 
            $isMappedType = $v.isMappedType; $importType = $v.mainType; $defValue="null"; $equalsTest=true;
            $mainType = String.format("Set<\%s>", $v.genericAlt == null ? $v.mainType : $v.genericAlt);
        }
    | ^(MAPTYPE k=vartype v=vartype) { 
            $isMappedType = $v.isMappedType; $importType = $v.mainType; $defValue="null";$equalsTest=true;
            $mainType
                = String.format("Map<\%s, \%s>", $k.genericAlt == null ? $k.mainType : $k.genericAlt,
                    $v.genericAlt == null ? $v.mainType : $v.genericAlt);
        }
    | ^(COMPLEX typeid=ID) {
            $mainType = $typeid.text;
            $isMappedType = true;
            $importType = $mainType;
            $defValue = "null";
            $equalsTest=true;
            if ($mainType.endsWith("URI")) {
                $genericAlt = "String";
            }
            else {
                $genericAlt = $mainType;
            }
        }
    ;
       
apiExpr returns [StringTemplate entList, StringTemplate kEntry, StringTemplate kSetup]
scope { String apiName; } 
:
      ^(APISEC doc=DOC deprecated=deprecatedAspect? apiType=ID { $apiExpr::apiName = $apiType.text; } (a=apilist)) {
      // This is the definition of a set of apis in the same "class", so we
      // need to emit at this level the DispatchFunction and the API definition
            
      // Imports for all types referenced
      String imp = getTypeImports($apiType.text);
      // Construct the template for the API - not the passed apis etc are collected at the lower level
      StringTemplate s = %apilist(sdkname={sdkName}, apitype={$apiType.text}, apis={$a.main},typeimports={imp});
             
      String apiFullPath = getGeneratedFilePath(sdkName, "common", "common", "api/" + "" + $apiType + "Api.java");
      addApiTemplate(apiFullPath, "1", s);
                           
      // Generate the wrapper classes
      String wrapperImplPath = getGeneratedFilePath(sdkName, "kernel", "kernel", $apiType + "ApiImplWrapper.java");
      StringTemplate wrapperImpl = %wrapperImpl(sdkname={sdkName}, apitype={$apiType.text}, apis={$a.wrapperList}, typeimports={imp}, payloadImports={$a.payloadImports});
      addKernelTemplate(wrapperImplPath, "1", wrapperImpl);

      if (sdkName != null) {
          StringTemplate baseImpl = %baseImpl(sdkname={sdkName}, apitype={$apiType.text});
          String baseImplPath = "build/generated-sources/main/java/rapture/" + sdkName + "/kernel/" + $apiType + "ApiBase.java";
          addKernelTemplate(baseImplPath, "1", baseImpl);

          StringTemplate servletImpl = %servletImpl(sdkname={sdkName}, apitype={$apiType.text});
          String servletImplPath = "build/generated-sources/main/java/rapture/server/web/" + sdkName + "/servlet/" + $apiType + "Servlet.java";
          addKernelTemplate(servletImplPath, "1", servletImpl);
      }

      //script apis
      String scriptApiFullPath = getGeneratedFilePath(sdkName, "common", "common", "api/Script" + $apiType + "Api.java");
      StringTemplate scriptS = %scriptapilist(sdkname={sdkName}, apitype={$apiType.text}, apis={$a.scriptMain}, typeimports={imp}, deprecated={$deprecated.reason});
      addApiTemplate(scriptApiFullPath, "1", scriptS);

      // As well as the above, we need to generate an implementation of the above interface
      String scriptImplFullPath = getGeneratedFilePath(sdkName, "kernel", "kernel", "script/" + "Script" + $apiType + ".java");
      StringTemplate scriptImpl = %scriptapiimpl(sdkname={sdkName}, apitype={$apiType.text}, apis={$a.scriptImpl}, typeimports={imp}, deprecated={$deprecated.reason});
      addKernelTemplate(scriptImplFullPath, "1", scriptImpl);

      // DispatchXFunction template
      String reqFullPath = getFileNameForType(sdkName, $apiType.text, "Dispatch" + $apiType.text, "Function");
      String sdkPackagePrefix;
      if (sdkName != null) {
        sdkPackagePrefix = sdkName + ".";
      }
      else {
        sdkPackagePrefix = "";
      }
      StringTemplate s2 = %dispatchFn(sdkPackagePrefix={sdkPackagePrefix}, apitype={$apiType.text}, dispatcher={$a.disp});
      addKernelTemplate(reqFullPath, "1", s2);

      // Api function template
      String httpFullPath = getGeneratedFilePath(sdkName, "common", "common", "client/Http" + $apiType + "Api.java");
      // imp is not the right thing here, we should get it from the apilist
      StringTemplate s3 = %httpfile(sdkname={sdkName}, apitype={$apiType.text}, imports={imp}, fns={$a.httpApi},  scriptFns={$a.httpScriptApi}, payloadImports={$a.payloadImports}, deprecated={$deprecated.reason});
      addApiTemplate(httpFullPath, "1", s3); 

      addApiName($apiType.text);
      $entList = %entTypes(entTypes={$a.entTypes});
      $entList = %entTypes(sdkname={sdkName}, entTypes={$a.entTypes});

      if (sdkName != null) {
          $kEntry = %kernelEntry(apitype={$apiType.text});
          $kSetup = %kernelEntrySetup(apitype={$apiType.text});
      }
};

/**
 * At the api list level we need to collect the dispatch templates (used above) and the api definitions
 * Here we are really just collecting it from the lower loop/tree
 */
apilist returns [List main, List scriptMain, List scriptImpl, List disp, List httpApi, List httpScriptApi, List payloadImports, List entTypes, List wrapperList]
@init {
    $main = new ArrayList<StringTemplate>();
    $wrapperList = new ArrayList<StringTemplate>();
    $scriptMain = new ArrayList<StringTemplate>();
    $disp = new ArrayList<StringTemplate>();
    $httpApi = new ArrayList<StringTemplate>();
    $httpScriptApi = new ArrayList<StringTemplate>();
    $payloadImports = new ArrayList<StringTemplate>();
    $entTypes = new ArrayList<StringTemplate>();
    $scriptImpl = new ArrayList<StringTemplate>();
    }
: (a=apistmt {
        $main.add($a.st);
        $wrapperList.add($a.wrapperStatement);
        $scriptMain.add($a.scriptEntry);
        $disp.add($a.dispatchEntry);
        $httpApi.add($a.httpEntry);
        $httpScriptApi.add($a.httpScriptEntry);
        $payloadImports.add($a.imp);
        $entTypes.add($a.entType);
        $scriptImpl.add($a.scriptImpl);
   })+;
        
visibility returns [int type]
    : PRIVATEVIS { $type = PRIVATEVIS; } 
    | PUBLICVIS  { $type = PUBLICVIS;  }
    ;

/**
 * The definition of an api also implies the creation of the payload classes for that implementation
 * (the request and response files) and the information needed for the dispatch.
 * TODO: Also needed is the entry in the Http client implementation, to be returned up
 * dynamicEntMap is used to store the wildcard key to uri arg mapping for dynamic entitlements
 */

apistmt returns [StringTemplate scriptEntry, StringTemplate dispatchEntry, StringTemplate httpEntry, StringTemplate httpScriptEntry, StringTemplate imp, StringTemplate entType, StringTemplate scriptImpl, StringTemplate wrapperStatement]
@init {
    List<String> regularEntitlements = new ArrayList<String>();
    Map<String, String> dynamicEntMap = new HashMap<String, String>();
} 
: ^(APIENTRY doc=DOC ^(ENTITLEASPECT (rent=REGULARENTITLE {regularEntitlements.add($rent.text);})+ (k=ID v=ID {dynamicEntMap.put($k.text, $v.text);})*) deprecated=deprecatedAspect? streaming=STREAMING? ^(VISIBILITY visibility) ^(RETTYPE vartype) ^(FNNAME name=ID) p=paramList) {
        // An apistmt needs to generate in two files - the api file (which is done at a higher level) and in 
        // a whole new set of files which are dependent on the fnName
                
        // System.out.println("PVT is : " + $visibility.text);
        // Visibility can be either @public or @private
        // If it is @private, we don't generate much at all, particularly the Payload
        // And HttpEntry files
        
        boolean isPrivate = false;
        if (PRIVATEVIS == $visibility.type) {
          isPrivate = true;
        }       
        // store the reflection methods for the payload files
        List<StringTemplate> dynEntEntries = new ArrayList<StringTemplate>();
        for (Map.Entry<String, String> entry : dynamicEntMap.entrySet()) {
          String key = entry.getKey();
          String value = entry.getValue();
          if (key.equals("f")) {
            dynEntEntries.add(%dynamicentitlemententry(dynEntType={"fullPath"}, uriArg={value}));
          }
          else if (key.equals("d")) {
            dynEntEntries.add(%dynamicentitlemententry(dynEntType={"docPath"}, uriArg={value}));
          }
          else if (key.equals("a")) {
            dynEntEntries.add(%dynamicentitlemententry(dynEntType={"authority"}, uriArg={value}));
          }
        }
        String ent = reassembleFullEntitlmentPath(regularEntitlements, dynamicEntMap);

        boolean isStreaming = false;
        if($streaming != null ) {
          isStreaming = true;
        }
        
        boolean isVoid = $vartype.mainType.equals("void");
        

        String entName = generateEntName($apiExpr::apiName, ent);
        $entType = %entEntry(api={$apiExpr::apiName}, name={$name.text}, path={entName});

    addCallNameEntry(%callNameEntry(api={$apiExpr::apiName}, name={$name.text}));
                   
        // This is the one that is passed above (we could add it to the "api.java" here, but there's no real need
        String d = doc.getText();
        d = d.substring(1,d.length()-1);
        if (d.length() > 0){
            d = formatDoc(d);
        }

        $st = %apiEntry(ret={$vartype.mainType},name={$name},params={$p.main}, deprecated={$deprecated.reason}, doc={d});
        $wrapperStatement = %wrapperEntry(ret={$vartype.mainType},name={$name.text},params={$p.main}, callParams={$p.paramListAspect}, apitype={$apiExpr::apiName}, deprecated={$deprecated.reason}, isVoid={isVoid});
        
        $scriptEntry = %apiEntry(ret={$vartype.mainType},name={$name},params={$p.scriptMain}, deprecated={$deprecated.reason});
        $scriptImpl = %scriptapientry(ret={$vartype.mainType}, name={$name}, params={$p.scriptMain}, apiparams={$p.paramListAlt}, deprecated={$deprecated.reason}, apitype={$apiExpr::apiName}, isVoid={isVoid});
        // And this is the one that is used to create new files
        String retImport = "";
        if ($vartype.isMappedType) {
          retImport = "import " +  getPackageAndClass($vartype.importType) + ";";
          addImportForApi($apiExpr::apiName, $vartype.importType);
        }
                   
        if (!isPrivate && !isStreaming) {
        
          
          $dispatchEntry = %dispatchEntry(name={$name.text});
          $imp = %reqRespImp(sdkname={sdkName}, apitype={$apiExpr::apiName}, name={$name.text});
          
          $httpScriptEntry = %httpscriptapifn(apitype={$apiExpr::apiName}, ret={$vartype.mainType}, name={$name.text}, params={$p.scriptMain}, callParams={$p.paramListAlt}, deprecated={$deprecated.reason}, isVoid={isVoid});
                              
          $httpEntry = %httpapifn(apitype={$apiExpr::apiName}, ret={$vartype.mainType}, name={$name.text}, params={$p.main}, apiparams={$p.paramList}, deprecated={$deprecated.reason}, isVoid={isVoid});
                    
          StringTemplate s = %dispatchFile(sdkname={sdkName}, apitype={$apiExpr::apiName}, ret={$vartype.mainType}, name={$name.text}, params={$p.alternate}, apiparams={$p.paramList}, imports={$p.packageList}, retImport={retImport}, ent={ent}, isVoid={isVoid});
          String reqFullPath = getFileNameForType(sdkName, $apiExpr::apiName, $name.text, "Dispatch");
          addKernelTemplate(reqFullPath, "1", s);
                    
          // Also create a payload file
          StringTemplate s2 = %payloadFile(sdkname={sdkName}, apitype={$apiExpr::apiName}, ret={$vartype.mainType}, name={$name.text}, params={$p.alternate}, apiparams={$p.paramList}, imports={$p.packageList}, retImport={retImport}, dynEnt={dynEntEntries});
          String reqFullPath2 = getFileNameForType(sdkName, $apiExpr::apiName, $name.text, "Payload");
          addApiTemplate(reqFullPath2, "1", s2);    
                            
          // And create a test for the payload file
          StringTemplate s3 = %payloadTestFile(sdkname={sdkName}, apitype={$apiExpr::apiName}, ret={$vartype.mainType}, name={$name.text}, testSetters={$p.testSetters}, testGetters={$p.testGetters});
          String reqFullPath3 = getTestFileNameForType(sdkName, $apiExpr::apiName, $name.text, "Payload");
          addApiTemplate(reqFullPath3, "1", s3);
          //System.out.println("File name is " + reqFullPath3);    
          addImportForApi($apiExpr::apiName, $p.typeNames);
                            
        } else if(!isPrivate && isStreaming ) {
        }
         
};

paramList returns[Set<String> typeNames, List main, List scriptMain, List alternate, List paramList, List paramListAlt, List paramListAspect, 
                  List packageList, List testSetters, List testGetters]
@init {
  $typeNames = new HashSet<String>();
    $alternate = new ArrayList<StringTemplate>();
    $main = new ArrayList<StringTemplate>();
    $scriptMain = new ArrayList<StringTemplate>();
    $main.add(new StringTemplate("CallingContext context"));
    $paramList = new ArrayList<StringTemplate>();
    $paramList.add(new StringTemplate("payload.getContext()"));
    $paramListAlt = new ArrayList<StringTemplate>();
  $paramListAspect = new ArrayList<StringTemplate>();
  $paramListAspect.add(new StringTemplate("context"));
    $packageList = new ArrayList<StringTemplate>();
    $testSetters = new ArrayList<StringTemplate>();
    $testGetters = new ArrayList<StringTemplate>();
}
 :
 ^(PARAMS (a=param { 
    $alternate.add($a.alternate); 
    $main.add($a.st); 
    $scriptMain.add($a.st);
    $paramList.add($a.param.toString()); 
    $paramListAlt.add($a.param.toString());
    $paramListAspect.add($a.param.toString());
    $packageList.add($a.pack);
    $testSetters.add($a.testSetter);
    $testGetters.add($a.testGetter);
    $typeNames.add($a.typeName.toString());
    })*);
                    
param returns[StringTemplate alternate, StringTemplate param, StringTemplate pack, StringTemplate testSetter, StringTemplate testGetter, String typeName]:  ^(PARAM ^(TYPE t=vartype) ^(NAME n=ID) BANG?) {
     $st = %param(type={$t.mainType},name={$n.text});
     boolean isURI = $t.mainType.endsWith("URI");
     String scheme = ($t.mainType).equals("RaptureURI") ? null : ($t.mainType).substring(0, $t.mainType.length()-3).toUpperCase();
     $alternate = %attrForClass(name={$n.text},
		type={(isURI) ? "String" : $t.mainType},
		isURI={isURI},
		scheme={scheme});
     $param = %param2(name={$n.text});
     if ($t.isMappedType && !isURI) {
     $pack = %package(name={ getPackageAndClass($t.importType) });
     $typeName = $t.importType;
     } else {
        $pack = new StringTemplate();
        $typeName = "";
     }
     $testSetter = %callSetter(name={$n.text}, value={$t.defValue});
     $testGetter = %testGetter(name={$n.text}, value={$t.defValue}, equalsTest={$t.equalsTest});
     };
     
                        


