tree grammar TTreeGo;
 
options {
    language   = Java;
   tokenVocab = TParser;
    ASTLabelType=CommonTree;
    output=template;
    superClass = AbstractTTree;
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
  StringTemplate typeFile = %GoStructFile(structs={$hmxdef::ents});
  String typeFullFileName = "types2.go";
  addApiTemplate(typeFullFileName, "1", typeFile);
};

typeExpr returns [StringTemplate typeDef]
:
	^(TYPEDEF doc=DOC (beanAnnotation|cacheableAnnotation|extendsAnnotation|deprecatedAnnotation|addressableAnnotation|storableAnnotation|indexedAnnotation)*   name=ID typeAspect typeFields) { 
	   $typeDef = %GoStruct(name={$name.text}, fields={$typeFields.typeFields}); 
	};
	
typeAspect returns [String name]
: 
    ^(TYPEASPECT p=PACKAGENAME);


typeFields returns [List typeFields]
@init {
   $typeFields = new ArrayList<StringTemplate>();
} :
    ^(TYPEFIELDS (typeFieldDef { $typeFields.add($typeFieldDef.goField); })*);
  
/*
 * For a type field def we need to return part of a GoType structure
 */
   
typeFieldDef returns [StringTemplate goField]:
    ^(TYPEMEMBER ^(TYPE vartype) ^(NAME n=ID) ^(FIELD_CONSTRUCTOR fieldConstructor?)) { $goField = %GoStructField(name={$n.text}, type={$vartype.goType}); };

/**
 * Record whether the type is a mapped one (a complex one) or not
 */
 
vartype returns [String goType]:
    ^(RAW INTTYPE) { $goType = "int32"; }
    | ^(RAW LONGTYPE) { $goType = "uint64"; }
    | ^(RAW OBJECTTYPE) { $goType = "interface{}";}
    | ^(RAW LONGCTYPE) { $goType = "uint64"; }
    | ^(RAW DATETYPE) { $goType = "Date"; }
    | ^(RAW BOOLTYPE) { $goType = "bool"; }
    | ^(RAW DOUBLETYPE) { $goType = "double"; }
    | ^(RAW STRINGTYPE) { $goType = "string"; }
    | ^(RAW BYTEARRAYTYPE) { $goType = "[]byte"; }
    | ^(RAW VOIDTYPE) { $goType = null; }
    | ^(LISTTYPE v=vartype) { $goType = "[]" + $v.goType; }
    | ^(SETTYPE v=vartype) { $goType = "[]" + $v.goType; }
    | ^(MAPTYPE k=vartype v=vartype) { $goType = "map[" +$k.goType + "]" + $v.goType; } 
    | ^(COMPLEX typeid=ID) { $goType = $typeid.text; };      
	        
apiExpr
scope { String apiName; } 
@init {
}

:
			^(APISEC DOC deprecated=deprecatedAspect? apiType=ID { $apiExpr::apiName = $apiType.text; } (a=apilist)) {
			// Here we need to generate a file
			             StringTemplate s = %GoApi(types={$a.types}, fns={$a.fns});
                   String apiFullPath = $apiExpr::apiName + ".go";
                   addApiTemplate(apiFullPath, "1", s);
                       };

/**
 * At the api list level we need to collect the dispatch templates (used above) and the api definitions
 * Here we are really just collecting it from the lower loop/tree
 */
apilist returns [List types, List fns]
@init {
	$types = new ArrayList<StringTemplate>();
	$fns = new ArrayList<StringTemplate>();
	}
: (a=apistmt {
		$types.add($a.apiTypes);
		$fns.add($a.apiFunc);
   })+;
   		
deprecatedAspect 
    : ( DEPRECATED ID ) 
    | ( DEPRECATED STRING )
    ;

visibility : PRIVATEVIS | PUBLICVIS;

/**
 * An apistmt is one api function call, we need to return separately the types and the function (method)
 */
 	
apistmt returns [StringTemplate apiTypes, StringTemplate apiFunc]
: ^(APIENTRY DOC  ^(ENTITLEASPECT (rent=REGULARENTITLE)+ (k=ID v=ID)*) deprecatedAspect? ^(VISIBILITY visibility) ^(RETTYPE vartype) ^(FNNAME name=ID) p=paramList) {
      $apiTypes = %GoApiType(apitype={$apiExpr::apiName}, name={$name.text}, paramPayload={$p.apiPayloadParams}, ret={ $vartype.goType});
      $apiFunc = %GoApiFn(apitype={$apiExpr::apiName}, name={$name.text}, apiParams={$p.apiParams}, setterParams={$p.setterParams}, ret={ $vartype.goType});
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
   $apiParam = %GoApiParam(type={$t.goType}, name={$n.text});
   $payloadParam = %GoApiPayloadParam(type={$t.goType}, name={$n.text});
   $setterParam = %GoApiSetter(type={$t.goType}, name={$n.text});
	 };
	 
						


