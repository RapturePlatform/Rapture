tree grammar TTreeSlateMd;

options {
    language   = Java;

    // Use the vocab from the parser (not the lexer)
    tokenVocab = TParser;
    ASTLabelType=CommonTree;
    output=template;
    superClass = AbstractTTreeSlateMd;
}

import TTreeShared ;

@header {
    package com.incapture.rapgen;
    import java.util.Map;
    import java.util.HashMap;
    import com.incapture.rapgen.annotations.*;
    import com.incapture.apigen.slate.*;
    import com.incapture.apigen.slate.type.*;
	import com.incapture.apigen.slate.function.*;
}

hmxdef
    : ^(MAIN versionExpr minVerExpr
          (^(INNERT typeExpr)
           | ^(INNERA a=apiExpr)
          )*
      )
{
    addVersionInfo($versionExpr.major, $versionExpr.minor, $versionExpr.micro);
    addMinVersionInfo($minVerExpr.major, $minVerExpr.minor, $minVerExpr.micro);
};

typeExpr
scope {
     boolean isDeprecated;
}
:
	^(TYPEDEF doc=DOC (beanAnnotation|cacheableAnnotation|extendsAnnotation|
	(deprecatedAnnotation { $typeExpr::isDeprecated = true; })
	|addressableAnnotation|storableAnnotation|indexedAnnotation)*  name=ID typeAspect typeFields) {
	   String d = doc.getText();
     d = d.substring(1,d.length()-1);
     addType($typeAspect.name, $ID.text, $typeFields.fields, d, $typeExpr::isDeprecated, $deprecatedAnnotation.text);
};
	
typeAspect returns [String name]: 
    ^(TYPEASPECT p=PACKAGENAME) { $name = $p.text; };

typeFields returns [List fields]  
@init {
    $fields = new ArrayList<Field>();
} :
    ^(TYPEFIELDS (t=typeFieldDef { $fields.add($t.field); })*);
    
typeFieldDef returns [Field field]:
    ^(TYPEMEMBER ^(TYPE vartype) ^(NAME ID) ^(FIELD_CONSTRUCTOR fieldConstructor?)) {
      $field = TypeFactory.createField($vartype.text, $ID.text);
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
    | ^(RAW VOIDTYPE) { $mainType = "void";}
    | ^(LISTTYPE v=vartype) { $mainType = "List<" + $v.mainType + ">" ;  }
    | ^(SETTYPE v=vartype) { $mainType = "Set<" + $v.mainType + ">" ;  }
    | ^(MAPTYPE k=vartype v=vartype) { $mainType = "Map<" + $k.mainType + "," + $v.mainType + ">" ;  }    
    | ^(COMPLEX typeid=ID) { $mainType = $typeid.text; };      
	        
apiExpr 
scope {
    String apiName;
    boolean isDeprecated;
}
:
    ^(APISEC doc=DOC (deprecated=deprecatedAnnotation { $apiExpr::isDeprecated = true; })?
        apiType=ID { $apiExpr::apiName = $apiType.text; } (a=apilist)) {
         String d = doc.getText();
         d = d.substring(1,d.length()-1);
         addApi($apiType.text, $a.functions, d, $apiExpr::isDeprecated, $deprecated.text);

};

/**
 * At the api list level we need to collect the dispatch templates (used above) and the api definitions
 * Here we are really just collecting it from the lower loop/tree
 */
apilist returns [List functions]
@init {
	$functions = new ArrayList<Function>();
	}
: (a=apistmt {
		$functions.add($a.function);
   })+;
   		
deprecatedAnnotation returns [String text]
    : ( DEPRECATED ID { $text = $ID.text; })
    | ( DEPRECATED STRING { $text = $STRING.text; })
    ;

visibility : PRIVATEVIS | PUBLICVIS;
 	
apistmt returns [Function function]
scope {
    boolean isDeprecated;
}
@init {
    List<String> regularEntitlements = new ArrayList<String>();
    Map<String, String> dynamicEntMap = new HashMap<String, String>();
} 
: ^(APIENTRY doc=DOC  ^(ENTITLEASPECT (rent=REGULARENTITLE {regularEntitlements.add($rent.text);})+
                        (k=ID v=ID {dynamicEntMap.put($k.text, $v.text);})*)
     (deprecatedAnnotation { $apistmt::isDeprecated = true; } )? ^(VISIBILITY visibility) ^(RETTYPE vartype)
     ^(FNNAME name=ID) p=paramList) {

    // This is a single api call
    String d = doc.getText();
    d = d.substring(1,d.length()-1);
    String ent = reassembleFullEntitlmentPath(regularEntitlements, dynamicEntMap);
    $function
        = FunctionFactory.createFunction($apiExpr::apiName, $vartype.mainType, $name.text, $p.parameters, d, ent,
            $apistmt::isDeprecated, $deprecatedAnnotation.text);
};

paramList returns[List parameters]
@init {
	$parameters = new ArrayList<Parameter>();
}
 :
 ^(PARAMS (a=param {
    $parameters.add($a.parameter);
 	})*);
					
param returns[Parameter parameter]:  ^(PARAM ^(TYPE t=vartype) ^(NAME n=ID) BANG?) {
    $parameter = new Parameter();
    $parameter.setName($n.text);
    $parameter.setType($t.mainType);
};
	 
						


