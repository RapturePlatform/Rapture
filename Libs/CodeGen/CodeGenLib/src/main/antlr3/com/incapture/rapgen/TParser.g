// The ANTLR parser for the Rapture API generator

parser grammar TParser;

options {
    language  = Java;
    output    = AST;
    ASTLabelType = CommonTree;
    superClass = AbstractTParser;
    tokenVocab = TLexer;
}

tokens {
    SCRIPT;
    STATEMENT;
    DATA;
    APISEC;
    APIENTRY;
    EXPR;
    ENUM;
    PARAM;
    RETTYPE;
    FNNAME;
    TYPE;
    NAME;
    PARAMS;
    MAIN;
    TYPEDEF;
    CRUDTYPEDEF;
    CRUDINFO;
    CRUDINFOENTRY;
    CRUDPACKAGEANNOTATION;
    VERSIONDEF;
    MINVERSIONDEF;
    SDKDEF;
    TYPEASPECT;
    ENTITLEASPECT;
    STREAMINGASPECT;
    VISIBILITY;
    PRIVATEVIS;
    PUBLICVIS;
    MEMBER;
    MEMBERS;
    RAW;
    COMPLEX;
    TYPEFIELDS;
    TYPEMEMBER;
    INNERT;
    INNERC;
    INNERA;
    STORAGE_PATH_ADDER;
    STORABLE_SEPARATOR;
    STORABLE_PREFIX;
    STORABLE_REPO_NAME;
    STORABLE_ENCODING;
    STORABLE_TTL_DAYS;
    FIELD_CONSTRUCTOR;
    INDEX_COMPONENT;
    INDEX_NAME;
}

@header {
    package com.incapture.rapgen;
}

// hmxdef is the root of the parser

hmxdef      : sdkExpr? versionExpr minVerExpr innerExpr* -> ^(MAIN sdkExpr? versionExpr minVerExpr innerExpr*);

innerExpr   : typeExpr -> ^(INNERT typeExpr)
              | crudTypeExpr -> 
                    ^(INNERT
                      ^(TYPEDEF 
                      DOC["[Type doc]"]
                      ID[$crudTypeExpr.name]
                      ^(TYPEASPECT PACKAGENAME["rapture.common"])
                      ^(TYPEFIELDS 
                          ^(TYPEMEMBER
                              ^(TYPE ^(RAW STRINGTYPE["Test"]))
                              ^(NAME ID["Hello"])
                              ^(FIELD_CONSTRUCTOR)
                           )
                       )
                       )
                   )
                   ^(INNERC crudTypeExpr)                   
                    ^(INNERA 
                        ^(APISEC 
                             DOC["[Hello]"]
                             ID[$crudTypeExpr.name]
                             ^(APIENTRY 
                                 DOC["[Some doc]"]
                                 ^(ENTITLEASPECT REGULARENTITLE["/admin/main"]) 
                                 ^(VISIBILITY PUBLICVIS) 
                                 ^(RETTYPE ^(COMPLEX ID[$crudTypeExpr.name]))
                                 ^(FNNAME ID["get" + $crudTypeExpr.name]) 
                                 ^(PARAMS 
                                     ^(PARAM 
                                         ^(TYPE ^(RAW STRINGTYPE["String"])) 
                                         ^(NAME ID["uri"])
                                      )
                                  )
                              )
                             ^(APIENTRY 
                                 DOC["[Some doc]"]
                                 ^(ENTITLEASPECT REGULARENTITLE["/admin/main"]) 
                                 ^(VISIBILITY PUBLICVIS) 
                                 ^(RETTYPE ^(RAW BOOLTYPE["Boolean"]))
                                 ^(FNNAME ID["delete" + $crudTypeExpr.name]) 
                                 ^(PARAMS 
                                     ^(PARAM 
                                         ^(TYPE ^(RAW STRINGTYPE["String"])) 
                                         ^(NAME ID["uri"])
                                      )
                                  )
                              ) 
                             ^(APIENTRY 
                                 DOC["[Some doc]"]
                                 ^(ENTITLEASPECT REGULARENTITLE["/admin/main"]) 
                                 ^(VISIBILITY PUBLICVIS) 
                                 ^(RETTYPE ^(COMPLEX ID[$crudTypeExpr.name]))
                                 ^(FNNAME ID["put" + $crudTypeExpr.name]) 
                                 ^(PARAMS 
                                     ^(PARAM 
                                         ^(TYPE ^(COMPLEX ID[$crudTypeExpr.name])) 
                                         ^(NAME ID["content"])
                                      )
                                  )
                              )
                             ^(APIENTRY 
                                 DOC["[Some doc]"]
                                 ^(ENTITLEASPECT REGULARENTITLE["/admin/main"]) 
                                 ^(VISIBILITY PUBLICVIS) 
                                 ^(RETTYPE ^(LISTTYPE ^(COMPLEX ID["RaptureFolderInfo"])))
                                 ^(FNNAME ID["getChildren"]) 
                                 ^(PARAMS 
                                     ^(PARAM 
                                         ^(TYPE ^(RAW STRINGTYPE["String"])) 
                                         ^(NAME ID["prefix"])
                                      )
                                  )
                              )
                          )
                     )
 
              | apiExpr -> ^(INNERA apiExpr);

sdkExpr     :
    SDKNAME LBRAC name=ID RBRAC -> ^(SDKDEF $name);
    
versionExpr
    :
    ID { 
        if (!"version".equals($ID.text)) {
            throw new IllegalArgumentException(
                String.format("Error: Expecting 'version' but found '\%s' at \%s:\%s", $ID.text, $ID.line, $ID.getCharPositionInLine()));
        }
    }
    LBRAC major=INT DOT minor=INT( DOT micro=INT)? RBRAC -> ^(VERSIONDEF $major $minor( $micro)?)
    ;

minVerExpr
    :
    ID { 
        if (!"minVer".equals($ID.text)) {
            throw new IllegalArgumentException(
                String.format("Error: Expecting 'version' but found '\%s' at \%s:\%s", $ID.text, $ID.line, $ID.getCharPositionInLine()));
        }
    }
    LBRAC major=INT DOT minor=INT( DOT micro=INT)? RBRAC -> ^(MINVERSIONDEF $major $minor( $micro)?)
    ;

dynList : (DYNENT! ID (LBRAC! ID RBRAC!)?)*;

regList : REGULARENTITLE+;

entitleAspect : ENTITLE EQUAL a=regList b=dynList -> ^(ENTITLEASPECT $a $b*);

deprecatedAspect : AT! DEPRECATED EQUAL! (STRING | ID) ;

crudTypeExpr returns[String name]:
        doc=DOC typeAnnotation* crudPackageAnnotation? CRUDTYPED n=ID { $name = $n.text; } crudTypeInfo typeFields -> ^(CRUDTYPEDEF $doc typeAnnotation* crudPackageAnnotation? $n crudTypeInfo typeFields);
  
crudTypeInfo :
        LBRAC crudDirective* RBRAC -> ^(CRUDINFO crudDirective*);
        
crudDirective :
        n=ID -> ^(CRUDINFOENTRY $n);
              
vardefs     :
        LPAREN vardef+ RPAREN -> ^(MEMBERS vardef+);

vardef      :
            vartype n=ID SEMI-> ^(MEMBER vartype $n);

vartype     :
            INTTYPE                         -> ^(RAW INTTYPE)
            | STRINGTYPE                    -> ^(RAW STRINGTYPE)
            | OBJECTTYPE                    -> ^(RAW OBJECTTYPE)
            | DATETYPE            			-> ^(RAW DATETYPE)
            | LONGTYPE                      -> ^(RAW LONGTYPE)
            | LONGCTYPE                     -> ^(RAW LONGCTYPE)
            | BOOLTYPE                      -> ^(RAW BOOLTYPE)
            | DOUBLETYPE                    -> ^(RAW DOUBLETYPE)
            | BYTEARRAYTYPE                 -> ^(RAW BYTEARRAYTYPE)
            | VOIDTYPE                      -> ^(RAW VOIDTYPE)
            | generics1D (LBRAC|L_THAN) vartype (RBRAC|G_THAN) -> ^(generics1D vartype)
            | generics2D (LBRAC|L_THAN) k=vartype COMMA v=vartype (RBRAC|G_THAN) -> ^(generics2D $k $v)
            | a=ID                          -> ^(COMPLEX $a)
    ;

generics1D :
    LISTTYPE | SETTYPE | CLASSTYPE
    ;

generics2D :
    MAPTYPE
    ;

apiExpr :
    doc=DOC deprecated=deprecatedAspect? API LBRAC apiType=ID RBRAC LPAREN apistmt* RPAREN -> ^(APISEC $doc $deprecated? $apiType apistmt*);
            
apistmt :   doc=DOC ent=entitleAspect deprecated=deprecatedAspect? streaming=STREAMING? visibility vartype fnName=ID LBRAC paramList RBRAC SEMI -> ^(APIENTRY $doc $ent $deprecated? $streaming? ^(VISIBILITY visibility) ^(RETTYPE vartype) ^(FNNAME $fnName) paramList);

visibility : PRIVATE -> PRIVATEVIS 
        | PUBLIC -> PUBLICVIS;

paramList : param? (COMMA param)* -> ^(PARAMS param*);
            
param :  vartype name=ID BANG?-> ^(PARAM ^(TYPE vartype) ^(NAME $name) BANG?);
            
//type rules
typeExpr  :
    //when there is a storable annotation, this should behave as a 'crud' type (look at SdkGen for example)
    //when there is an addressable annotation, we need to add an entry in the schemes for this guy
    doc=DOC typeAnnotation* TYPED name=ID typeAspect  typeFields -> ^(TYPEDEF $doc typeAnnotation* $name typeAspect typeFields);

typeAnnotation
    : AT! 
    (beanAnnotation|cacheableAnnotation|storableAnnotation|addressableAnnotation|searchableAnnotation|extendsAnnotation|deprecatedAnnotation|indexedAnnotation)  
    ;
    
crudPackageAnnotation
    : PACKAGE EQUAL PACKAGENAME -> ^(CRUDPACKAGEANNOTATION PACKAGENAME)
    ;

beanAnnotation
//example: Bean
    : BEAN
    ;

cacheableAnnotation
// example: Cacheable
    : CACHEABLE (LBRAC ID EQUAL (TRUE|FALSE) RBRAC)?
    ;
    
extendsAnnotation
//example: Extends(Object)
    : EXTENDS LBRAC! PACKAGENAME ID RBRAC!
    ;

deprecatedAnnotation
//example: Deprecated("Don't use this, it's bobbins")
// note - use single word for now
    : DEPRECATED LBRAC! ( STRING | ID ) RBRAC!
    ;

indexedAnnotation
//example: Indexable(name: "myIndex1", fields: {id, lastSeen, theDouble})
    : INDEXED LBRAC! index_name COMMA! ID! {
           if (!"fields".equals($ID.text)) {
               throw new IllegalArgumentException(
                    String.format("Error: Expecting 'field' but found '\%s' at \%s:\%s", $ID.text, $ID.line, $ID.getCharPositionInLine()));
           }
       }
       COLUMN! LPAREN! index_component (COMMA! index_component)* RPAREN! RBRAC!
//    : INDEXED LBRAC! ID RBRAC!
    ;

index_name
	: ID {
          if (!"name".equals($ID.text)) {
          throw new IllegalArgumentException(
                String.format("Error: Expecting 'name' but found '\%s' at \%s:\%s", $ID.text, $ID.line, $ID.getCharPositionInLine()));
          }
      }
      COLUMN value=STRING -> ^(INDEX_NAME $value)
	;


index_component
	: value=(ID|STRING) -> ^(INDEX_COMPONENT $value)
	;
	
addressableAnnotation
//example: Addressable(scheme = MAILBOX(false))
    : ADDRESSABLE (LBRAC! SCHEME! EQUAL! ID (LBRAC! (TRUE|FALSE) RBRAC!)? RBRAC!)
    ;

storableAnnotation
//example Storable(storagePath : {partition, documentPath, id})
    : STORABLE LBRAC! storagePath (COMMA! storableAnnotationField)*  RBRAC!
    ; 
    
searchableAnnotation
//example: Searchable
	: SEARCHABLE
	;
    
storableAnnotationField
scope {
    boolean isSeparator;
    boolean isEncoding;
    boolean isTtlDays;
    boolean isPrefix;
    boolean isRepoName;
}
//example: separator="."
    : operator=ID {
        if ("separator".equals($operator.text)) {
          $storableAnnotationField::isSeparator = true;
        } else if ("encoding".equals($operator.text)) {
          $storableAnnotationField::isEncoding = true;
        } else if ("ttlDays".equals($operator.text)) {
          $storableAnnotationField::isTtlDays = true;
        } else if ("prefix".equals($operator.text)) {
          $storableAnnotationField::isPrefix = true;
        } else if ("repoName".equals($operator.text)) {
          $storableAnnotationField::isRepoName = true;
        } else {
         throw new IllegalArgumentException(
                String.format("Error: Expecting 'separator', 'encoding', 'ttlDays', 'prefix', or 'repoName' but found '\%s' at \%s:\%s", $operator.text, $operator.line, $operator.getCharPositionInLine()));
        }
      }
      EQUAL value=(STRING|INT|ID)
      -> {$storableAnnotationField::isSeparator}? ^(STORABLE_SEPARATOR $value)
      -> {$storableAnnotationField::isEncoding}? ^(STORABLE_ENCODING $value)
      -> {$storableAnnotationField::isPrefix}? ^(STORABLE_PREFIX $value)
      -> {$storableAnnotationField::isRepoName}? ^(STORABLE_REPO_NAME $value)
      -> ^(STORABLE_TTL_DAYS $value)
    ;

storagePath
    : (STORAGE_PATH! COLUMN! LPAREN! storagePathAdder (COMMA! storagePathAdder)* RPAREN!)
    ;

storagePathAdder
    : value=(ID|STRING) -> ^(STORAGE_PATH_ADDER $value)
    ;

typeFields :
    LPAREN typeFieldDef* RPAREN -> ^(TYPEFIELDS typeFieldDef*);
    
typeFieldDef :
    vartype n=ID fieldConstructor? SEMI -> ^(TYPEMEMBER ^(TYPE vartype) ^(NAME $n) ^(FIELD_CONSTRUCTOR fieldConstructor?));

fieldConstructor
    : EQUAL! .+
    ;
    
typeAspect  : 
            LBRAC PACKAGE EQUAL packageid=PACKAGENAME RBRAC -> ^(TYPEASPECT $packageid);
            

