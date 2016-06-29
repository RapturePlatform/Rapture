tree grammar TTreeCpp;
 
options {
    language   = Java;
   tokenVocab = TParser;
    ASTLabelType=CommonTree;
    output=template;
    superClass = AbstractCppTTree;
}

import TTreeShared ;

@header {
    package com.incapture.rapgen;
    import com.incapture.rapgen.annotations.*;
}


hmxdef
scope { List<StringTemplate> ents; }
@init { $hmxdef::ents = new ArrayList<StringTemplate>(); }
    : ^(MAIN versionExpr minVerExpr
          (^(INNERT typeExpr { $hmxdef::ents.add($typeExpr.typeDef); }) 
           | ^(INNERA a=apiExpr)
          )*
      )
{
  // And finally here we need to build the typeFile
  StringTemplate typeFile = %CppStructFile(structs={$hmxdef::ents});
  String typeFullFileName = "types.h";
  addApiTemplate(typeFullFileName, "1", typeFile);
};

typeExpr returns [StringTemplate typeDef]
:
	^(TYPEDEF doc=DOC (beanAnnotation|cacheableAnnotation|extendsAnnotation|deprecatedAnnotation|addressableAnnotation|ftsAnnotation|storableAnnotation|searchableAnnotation|indexedAnnotation)*   name=ID typeAspect typeFields) { 
	   $typeDef = %CppStruct(name={$name.text}, fields={$typeFields.typeFields}); 
	};
	
typeAspect returns [String name]
: 
    ^(TYPEASPECT p=PACKAGENAME);


typeFields returns [List typeFields]
@init {
   $typeFields = new ArrayList<StringTemplate>();
} :
    ^(TYPEFIELDS (typeFieldDef { $typeFields.add($typeFieldDef.cppField); })*);
  
   
typeFieldDef returns [StringTemplate cppField]:
    ^(TYPEMEMBER ^(TYPE vartype) ^(NAME n=ID) ^(FIELD_CONSTRUCTOR fieldConstructor?)) { $cppField = %CppStructField(name={$n.text}, type={$vartype.cppType}); };

/**
 * Record whether the type is a mapped one (a complex one) or not
 */
 
vartype returns [String cppType]:
    ^(RAW INTTYPE) { $cppType = "int"; }
    | ^(RAW LONGTYPE) { $cppType = "long"; }
    | ^(RAW OBJECTTYPE) { $cppType = "void *";}
    | ^(RAW LONGCTYPE) { $cppType = "long"; }
    | ^(RAW DATETYPE) { $cppType = "long"; }
    | ^(RAW BOOLTYPE) { $cppType = "bool"; }
    | ^(RAW DOUBLETYPE) { $cppType = "double"; }
    | ^(RAW STRINGTYPE) { $cppType = "std::string"; }
    | ^(RAW BYTEARRAYTYPE) { $cppType = "char*"; }
    | ^(RAW VOIDTYPE) { $cppType = null; }
    | ^(LISTTYPE v=vartype) { $cppType = "std::list<" + $v.cppType + ">"; }
    | ^(SETTYPE v=vartype) { $cppType = "std::set<" + $v.cppType + ">"; }
    | ^(MAPTYPE k=vartype v=vartype) { $cppType = "std::map<" + $k.cppType + "," + $v.cppType + ">"; } 
    | ^(COMPLEX typeid=ID) { $cppType = "json"; };      
	//$cppType = $typeid.text        
apiExpr
scope { String apiName; } 
@init {
}

:
			^(APISEC DOC deprecated=deprecatedAspect? apiType=ID { $apiExpr::apiName = $apiType.text; } (a=apilist)) {
			// Here we need to generate a file
			             StringTemplate s = %CppApiSrc(api={$apiExpr::apiName}, types={$a.types}, fns={$a.fns});
                   String apiFullPath = $apiExpr::apiName + ".cpp";
                   addApiTemplate(apiFullPath, "1", s);
                   		             StringTemplate s2 = %CppApiHdr(api={$apiExpr::apiName}, types={$a.types}, fns={$a.hdr});
                   String apiHFullPath = $apiExpr::apiName + ".h";
                   addApiTemplate(apiHFullPath, "1", s2);
                       };

/**
 * At the api list level we need to collect the dispatch templates (used above) and the api definitions
 * Here we are really just collecting it from the lower loop/tree
 */
apilist returns [List types, List fns, List hdr]
@init {
	$types = new ArrayList<StringTemplate>();
	$fns = new ArrayList<StringTemplate>();
	$hdr = new ArrayList<StringTemplate>();
	}
: (a=apistmt {
		$types.add($a.apiTypes);
		$fns.add($a.apiFunc);
		$hdr.add($a.apiFuncH);
   })+;
   		
deprecatedAspect 
    : ( DEPRECATED ID ) 
    | ( DEPRECATED STRING )
    ;

visibility : PRIVATEVIS | PUBLICVIS;

/**
 * An apistmt is one api function call, we need to return separately the types and the function (method)
 */
 	
apistmt returns [StringTemplate apiTypes, StringTemplate apiFunc, StringTemplate apiFuncH]
: ^(APIENTRY DOC  ^(ENTITLEASPECT (rent=REGULARENTITLE)+ (k=ID v=ID)*) deprecatedAspect? ^(VISIBILITY visibility) ^(RETTYPE vartype) ^(FNNAME name=ID) p=paramList) {
      $apiTypes = %CppApiType(apitype={$apiExpr::apiName}, name={$name.text}, paramPayload={$p.apiPayloadParams}, ret={ $vartype.cppType});
      $apiFunc = %CppApiFn(apitype={$apiExpr::apiName}, name={$name.text}, apiParams={$p.apiParams}, setterParams={$p.setterParams}, ret={ $vartype.cppType});
      $apiFuncH = %CppApiH(apitype={$apiExpr::apiName}, name={$name.text}, apiParams={$p.apiParams}, setterParams={$p.setterParams}, ret={ $vartype.cppType});
		};

paramList returns[List apiParams, List apiPayloadParams, List setterParams]
@init {
	$apiParams = new ArrayList<StringTemplate>();
	$apiPayloadParams = new ArrayList<String>();
	$setterParams = new ArrayList<String>();
}
 :
 ^(PARAMS (a=param { 
 	$apiParams.add($a.apiParam); 
 	$apiPayloadParams.add($a.payloadParam);
 	$setterParams.add($a.setterParam);
 	})*);
		
/**
 * Each parameter is used (a) as a parameter to the api call method and (b) in the type structure of the payload
 */
 			
param returns[StringTemplate apiParam, StringTemplate payloadParam, StringTemplate setterParam] : ^(PARAM ^(TYPE t=vartype) ^(NAME n=ID) b=BANG?) {
//   boolean val = $b.text == null ? false : true;
  String nonReservedName = ensureNotReserved($n.text);
   $apiParam = %CppApiParam(type={$t.cppType}, name={nonReservedName});
   $payloadParam = %CppApiPayloadParam(type={$t.cppType}, name={nonReservedName});
   $setterParam = %CppApiSetter(type={$t.cppType}, name={nonReservedName});
	 };
	 
						


