tree grammar TTreePython;
 
options {
    language   = Java;
    tokenVocab = TParser;
    ASTLabelType=CommonTree;
    output=template;
    superClass = AbstractPythonTTree;
}

import TTreeShared ;

@header {
    package com.incapture.rapgen;
    import com.incapture.rapgen.annotations.*;
}

@members {
    String sdkName = null;
}

hmxdef
scope { List<StringTemplate> apis; }
@init { $hmxdef::apis = new ArrayList<StringTemplate>(); }
: ^(MAIN sdkExpr? { sdkName = $sdkExpr.name; } versionExpr minVerExpr 
          (  ^(INNERT typeExpr)
           | ^(INNERA a=apiExpr { $hmxdef::apis.addAll($a.fns); })
           | ^(INNERC c=crudTypeExpr { })
          )*
      ) {
  // And finally here we need to build the typeFile
  StringTemplate typeFile 
    = %PythonApiFile(apis={$hmxdef::apis}, versionMajor={$versionExpr.major}, versionMinor={$versionExpr.minor}, versionMicro={$versionExpr.micro});
  String typeFullFileName = (sdkName == null) ? "raptureAPI.py" : sdkName + "API.py";
  addApiTemplate(typeFullFileName, "1", typeFile);
};

typeExpr
:
	^(TYPEDEF doc=DOC (beanAnnotation|cacheableAnnotation|extendsAnnotation|deprecatedAnnotation|addressableAnnotation|storableAnnotation|searchableAnnotation|indexedAnnotation)*   name=ID typeAspect typeFields) { 
	};
	
typeAspect returns [String name]
: 
    ^(TYPEASPECT p=PACKAGENAME);


typeFields
@init {
} :
    ^(TYPEFIELDS (typeFieldDef)*);
  
/*
 * For a type field def we need to return part of a GoType structure
 */
   
typeFieldDef:
    ^(TYPEMEMBER ^(TYPE vartype) ^(NAME n=ID) ^(FIELD_CONSTRUCTOR fieldConstructor?));

/**
 * Record whether the type is a mapped one (a complex one) or not
 */
 
vartype returns [String pythonType]:
    ^(RAW INTTYPE) { $pythonType = "int"; }
    | ^(RAW LONGTYPE) { $pythonType = "long"; }
    | ^(RAW OBJECTTYPE) { $pythonType = "interface{}";}
    | ^(RAW LONGCTYPE) { $pythonType = "long"; }
    | ^(RAW DATETYPE) { $pythonType = "date"; }
    | ^(RAW BOOLTYPE) { $pythonType = "bool"; }
    | ^(RAW DOUBLETYPE) { $pythonType = "double"; }
    | ^(RAW STRINGTYPE) { $pythonType = "str"; }
    | ^(RAW BYTEARRAYTYPE) { $pythonType = "bytearray"; }
    | ^(RAW VOIDTYPE) { $pythonType = ""; }
    | ^(LISTTYPE v=vartype) { $pythonType = "list"; }
    | ^(SETTYPE v=vartype) { $pythonType = "list"; }
    | ^(MAPTYPE k=vartype v=vartype) { $pythonType = "dict"; } 
    | ^(COMPLEX typeid=ID) { $pythonType = $typeid.text; };      
	        
apiExpr returns [List fns]
scope { String apiName; } 
@init {
     $fns = new ArrayList<StringTemplate>();
}

:
			^(APISEC DOC deprecated=deprecatedAspect? apiType=ID { $apiExpr::apiName = $apiType.text; } (a=apilist)) {
			    $fns = $a.fns;
                      };

crudTypeExpr scope {
        String crudTypeName;
        BeanAnnotation bean;
        AddressableAnnotation addressable;
        StorableAnnotation storable;
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
    // (2) A special API subsection for this type (with get, put, delete, getChildren type calls) (so that should be registerd in the same way as an API)
    // (4) Potentially a nice client set of classes to make it easier to interact with this type
};


crudPackageAnnotation returns [String pkg]:
    ^(CRUDPACKAGEANNOTATION p=PACKAGENAME) { $pkg = $p.text; };


crudTypeInfo returns [List<StringTemplate> storageAdders, List<StringTemplate> builderAdders, List<StringTemplate> builderEntries]
  @init { $storageAdders = new ArrayList<StringTemplate>(); $builderAdders = new ArrayList<StringTemplate>(); $builderEntries = new ArrayList<StringTemplate>(); }
    : ^(CRUDINFO (crudDirective { })*);

crudDirective returns [StringTemplate storageAdder, StringTemplate storageBuilder, StringTemplate builderAdder] : ^(CRUDINFOENTRY n=ID) { };

/**
 * At the api list level we need to collect the dispatch templates (used above) and the api definitions
 * Here we are really just collecting it from the lower loop/tree
 */
apilist returns [List fns]
@init {
	$fns = new ArrayList<StringTemplate>();
	}
: (a=apistmt {
		$fns.add($a.apiFunc);
   })+;
   		
deprecatedAspect returns [String reason]
//example: Deprecated="because it's rubbish"
    : ( DEPRECATED ID { $reason = $ID.text; } ) 
    | ( DEPRECATED STRING  { $reason = $STRING.text.substring(1, $STRING.text.length() -1); } )
    ;

visibility : PRIVATEVIS | PUBLICVIS;

/**
 * An apistmt is one api function call, we need to return separately the types and the function (method)
 */
 	
apistmt returns [StringTemplate apiFunc]
: ^(APIENTRY DOC  ^(ENTITLEASPECT (rent=REGULARENTITLE)+ (k=ID v=ID)*) deprecated=deprecatedAspect? ^(VISIBILITY visibility) ^(RETTYPE vartype) ^(FNNAME name=ID) p=paramList) {
      // Formatting for params and version in comment string
      String d = $DOC.getText();
      d = d.substring(1,d.length()-1);
      if(d.length() > 0){
          d = formatDoc(d);
      }

      $apiFunc = %PythonApiFn(sdkName={sdkName}, apitype={$apiExpr::apiName}, name={$name.text}, apiParams={$p.apiParams}, setterParams={$p.setterParams}, ret={ $vartype.pythonType}, comment={ d }, deprecated={$deprecated.reason} );
		};

paramList returns[List apiParams, List setterParams]
@init {
	$apiParams = new ArrayList<StringTemplate>();
	$setterParams = new ArrayList<String>();
}
 :
 ^(PARAMS (a=param { 
 	$apiParams.add($a.apiParam); 
 	$setterParams.add($a.setterParam);
 	})*);
		
/**
 * Each parameter is used (a) as a parameter to the api call method and (b) in the type structure of the payload
 */
 			
param returns[StringTemplate apiParam, StringTemplate setterParam] : ^(PARAM ^(TYPE t=vartype) ^(NAME n=ID) b=BANG?) {
//   boolean val = $b.text == null ? false : true;
   String nonReservedName = ensureNotReserved($n.text);
   $apiParam = %PythonApiParam(type={$t.pythonType}, name={nonReservedName});
   $setterParam = %PythonApiSetter(type={$t.pythonType}, paramName={$n.text}, name={nonReservedName});
	 };
	 
