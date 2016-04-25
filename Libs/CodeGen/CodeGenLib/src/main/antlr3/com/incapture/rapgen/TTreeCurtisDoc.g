tree grammar TTreeCurtisDoc;

options {
    language   = Java;

    // Use the vocab from the parser (not the lexer)
    tokenVocab = TParser;
    ASTLabelType=CommonTree;
    output=template;
    superClass = AbstractTTreeCurtisDoc;
}

import TTreeShared ;

@header {
    package com.incapture.rapgen;
    import java.util.Map;
    import java.util.HashMap;
    import com.incapture.rapgen.annotations.*;
}


apiGen
    : hmxdef
    ;
    
hmxdef returns [List types]
@init {
 $types = new ArrayList<StringTemplate>();
}  : ^(MAIN versionExpr minVerExpr 
          (^(INNERT typeExpr { $types.add($typeExpr.st); }) 
           | ^(INNERA a=apiExpr)
          )*
      )
{
    
};

typeExpr :
	^(TYPEDEF doc=DOC (beanAnnotation|cacheableAnnotation|extendsAnnotation|deprecatedAnnotation|addressableAnnotation|storableAnnotation|searchableAnnotation|indexedAnnotation)*  name=ID typeAspect typeFields) {
	   String d = doc.getText();
     d = d.substring(1,d.length()-1);
     d = cleanDoc(d);
	   $st = %typeDef(name={$ID}, fields={$typeFields.fields}, doc={d});
	};
	
typeAspect returns [String name]: 
    ^(TYPEASPECT p=PACKAGENAME) { $name = $p.text; };

typeFields returns [List fields]  
@init {
    $fields = new ArrayList<StringTemplate>();
} :
    ^(TYPEFIELDS (t=typeFieldDef { $fields.add($t.st); })*);
    
typeFieldDef :
    ^(TYPEMEMBER ^(TYPE vartype) ^(NAME ID) ^(FIELD_CONSTRUCTOR fieldConstructor?)) {
      $st = %typeField(name={ $ID }, type={$vartype.text});
    };
    
/**
 * Record whether the type is a mapped one (a complex one) or not
 */
 
vartype returns [String mainType]:
    ^(RAW INTTYPE) { $mainType = "int"; }
    | ^(RAW LONGTYPE) { $mainType = "long"; }
    | ^(RAW DATETYPE) { $mainType = "Date"; }
    | ^(RAW OBJECTTYPE) { $mainType = "Object";}
    | ^(RAW LONGCTYPE) { $mainType = "Long";}
    | ^(RAW BOOLTYPE) { $mainType = "boolean"; }
    | ^(RAW DOUBLETYPE) { $mainType = "double"; }
    | ^(RAW STRINGTYPE) { $mainType = "String";}
    | ^(RAW BYTEARRAYTYPE) { $mainType = "byte[]";}
    | ^(RAW VOIDTYPE) { $mainType = "void"; }
    | ^(LISTTYPE v=vartype) { $mainType = "List<" + $v.mainType + ">" ;  }
    | ^(SETTYPE v=vartype) { $mainType = "Set<" + $v.mainType + ">" ;  }
    | ^(MAPTYPE k=vartype v=vartype) { $mainType = "Map<" + $k.mainType + "," + $v.mainType + ">" ;  }    
    | ^(COMPLEX typeid=ID) { $mainType = $typeid.text; };      
	        
apiExpr 
scope { String apiName; } 
:
			^(APISEC doc=DOC deprecated=deprecatedAspect? apiType=ID { $apiExpr::apiName = $apiType.text; } (a=apilist)) {
						 
                       };

/**
 * At the api list level we need to collect the dispatch templates (used above) and the api definitions
 * Here we are really just collecting it from the lower loop/tree
 */
apilist returns [List fns]
@init {
	$fns = new ArrayList<StringTemplate>();
	}
: (a=apistmt {
		$fns.add($a.st);
   })+;
   		
deprecatedAspect 
    : ( DEPRECATED ID ) 
    | ( DEPRECATED STRING )
    ;

visibility : PRIVATEVIS | PUBLICVIS;
 	
apistmt
@init {
    List<String> regularEntitlements = new ArrayList<String>();
    Map<String, String> dynamicEntMap = new HashMap<String, String>();
} 
: ^(APIENTRY doc=DOC  ^(ENTITLEASPECT (rent=REGULARENTITLE {regularEntitlements.add($rent.text);})+ (k=ID v=ID {dynamicEntMap.put($k.text, $v.text);})*) deprecatedAspect? ^(VISIBILITY visibility) ^(RETTYPE vartype) ^(FNNAME name=ID) p=paramList) {
		// An apistmt needs to generate in two files - the api file (which is done at a higher level) and in 
		// a whole new set of files which are dependent on the fnName

    // This is a single api call
      String d = doc.getText();
       d = d.substring(1,d.length()-1);
       d = cleanDoc(d);
      String ent = reassembleFullEntitlmentPath(regularEntitlements, dynamicEntMap);
 	    $st = %apiEntry(apitype={$apiExpr::apiName}, ret={$vartype.mainType}, name={$name.text}, params={$p.paramList}, paramNames={$p.paramNames}, doc={d}, ent={ent});
 	    addApiTemplate($apiExpr::apiName + "/" + $name.text + ".rdoc", "1", $st);
 		};

paramList returns[List paramList, List paramNames]
@init {
	$paramList = new ArrayList<StringTemplate>();
	$paramNames = new ArrayList<String>();
}
 :
 ^(PARAMS (a=param { 
 	$paramList.add($a.st); 
 	$paramNames.add($a.name);
 	})*);
					
param returns[String name]:  ^(PARAM ^(TYPE t=vartype) ^(NAME n=ID) BANG?) {
     $name = $n.text;
	 $st = %apiParam(type={$t.mainType},name={$n.text});
	 };
	 
						


