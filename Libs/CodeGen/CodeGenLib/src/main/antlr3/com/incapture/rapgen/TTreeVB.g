tree grammar TTreeVB;

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
    import com.incapture.rapgen.annotations.*;
    
}

hmxdef returns[List vals]
@init {
	$vals = new ArrayList<StringTemplate>();
} 	: ^(MAIN versionExpr minVerExpr 
          (^(INNERT typeExpr) 
           | ^(INNERA a=apiExpr { $vals.add($a.apisec); })
          )*
      )
{
						 StringTemplate s 
						  = %VBapi(apilist={$vals}, versionMajor={$versionExpr.major}, versionMinor={$versionExpr.minor}, versionMicro={$versionExpr.micro});
           				 String apiFullPath = "rapture.vba";
           				 addApiTemplate(apiFullPath, "1", s);
};

typeExpr :
	^(TYPEDEF doc=DOC (beanAnnotation|cacheableAnnotation|extendsAnnotation|deprecatedAnnotation|addressableAnnotation|storableAnnotation|indexedAnnotation)*  name=ID typeAspect typeFields);
	
typeAspect returns [String name]: 
    ^(TYPEASPECT p=PACKAGENAME);

typeFields :
    ^(TYPEFIELDS typeFieldDef*);
    
typeFieldDef :
    ^(TYPEMEMBER ^(TYPE vartype) ^(NAME ID) ^(FIELD_CONSTRUCTOR fieldConstructor?));

    
/**
 * Record whether the type is a mapped one (a complex one) or not
 */
 
vartype:
    ^(RAW INTTYPE) 
    | ^(RAW LONGTYPE)
    | ^(RAW OBJECTTYPE)
    | ^(RAW LONGCTYPE)
    | ^(RAW BOOLTYPE)
    | ^(RAW DOUBLETYPE)
    | ^(RAW STRINGTYPE)
    | ^(RAW DATETYPE)
    | ^(RAW BYTEARRAYTYPE)
    | ^(RAW VOIDTYPE)    
    | ^(LISTTYPE v=vartype)
    | ^(SETTYPE v=vartype)
    | ^(MAPTYPE k=vartype v=vartype)  
    | ^(COMPLEX typeid=ID);      
	        
apiExpr returns[List apisec] 
scope { String apiName; } 
@init {
    $apisec = new ArrayList<StringTemplate>();
}

:
			^(APISEC DOC deprecated=deprecatedAspect? apiType=ID { $apiExpr::apiName = $apiType.text; } (a=apilist)) {
			             $apisec.addAll($a.main);
                       };

/**
 * At the api list level we need to collect the dispatch templates (used above) and the api definitions
 * Here we are really just collecting it from the lower loop/tree
 */
apilist returns [List main]
@init {
	$main = new ArrayList<StringTemplate>();
	}
: (a=apistmt {
		$main.add($a.st);
   })+;

deprecatedAspect 
    : ( DEPRECATED ID ) 
    | ( DEPRECATED STRING )
    ;

visibility : PRIVATEVIS | PUBLICVIS;

/**
 * The definition of an api also implies the creation of the payload classes for that implementation
 * (the request and response files) and the information needed for the dispatch.
 * TODO: Also needed is the entry in the Http client implementation, to be returned up
 */
 	
apistmt
: ^(APIENTRY DOC  ^(ENTITLEASPECT (rent=REGULARENTITLE)+ (k=ID v=ID)*) deprecatedAspect? ^(VISIBILITY visibility) ^(RETTYPE vartype) ^(FNNAME name=ID) p=paramList) {
			$st = %VBApiEntry(apitype={$apiExpr::apiName},name={$name.text},paramNames={$p.names}, params={$p.main});
		};

paramList returns[List main, List names]
@init {
	$main = new ArrayList<StringTemplate>();
	$names = new ArrayList<String>();
}
 :
 ^(PARAMS (a=param { 
 	$main.add($a.st); 
 	$names.add($a.name);
 	})*);
					
param returns[String name] : ^(PARAM ^(TYPE t=vartype) ^(NAME n=ID) b=BANG?) {
   boolean val = $b.text == null ? false : true;
	 $st = %VBParam(name={$n.text}, bang={val});
	 $name = $n.text;
	 };
	 
						


