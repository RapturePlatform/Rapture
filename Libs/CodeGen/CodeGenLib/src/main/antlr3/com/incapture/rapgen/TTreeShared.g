tree grammar TTreeShared;
options {
    ASTLabelType=CommonTree;
}

/** 
 * This tree grammar is imported (inherited) by the other
 * tree grammars that actually do the work. 
 */

sdkExpr returns [String name]: ^(SDKDEF ID) { $name = $ID.text; };

versionExpr returns [int major, int minor, int micro]: ^(def=VERSIONDEF majorLocal=INT minorLocal=INT( microLocal=INT)?) {
  $major = Integer.parseInt($majorLocal.text);
  $minor = Integer.parseInt($minorLocal.text);
  if ($microLocal != null && $microLocal.text.length() > 0) {
       $micro = Integer.parseInt($microLocal.text);
  }
  else {
      $micro = 0; //assume 0 if no macro specified
  }
};

minVerExpr returns [int major, int minor, int micro]: ^(def=MINVERSIONDEF majorLocal=INT minorLocal=INT( microLocal=INT)?) {
  $major = Integer.parseInt($majorLocal.text);
  $minor = Integer.parseInt($minorLocal.text);
  if ($microLocal != null && $microLocal.text.length() > 0) {
       $micro = Integer.parseInt($microLocal.text);
  }
  else {
      $micro = 0; //assume 0 if no macro specified
  }
};


beanAnnotation returns [BeanAnnotation result]
//This is just a marker annotation. example: Bean
    : BEAN { $result = new BeanAnnotation(); }
    ;

searchableAnnotation returns [SearchableAnnotation result]
//This is just a marker annotation. example: Searchable
    : SEARCHABLE { $result = new SearchableAnnotation(); }
    ;
    
cacheableAnnotation returns [CacheableAnnotation result]
    : CACHEABLE { $result = new CacheableAnnotation(); } (LBRAC ID EQUAL (TRUE { $result.setShouldCacheNulls(true); } | FALSE) RBRAC)?
    ;
    
addressableAnnotation returns [AddressableAnnotation result]
scope {
    boolean isPrimitive;
}
@init {
    $addressableAnnotation::isPrimitive = false;
}
//example: Addressable(scheme = MAILBOX)
    : ADDRESSABLE ID 
        (TRUE {$addressableAnnotation::isPrimitive = true;} 
        | FALSE {$addressableAnnotation::isPrimitive = false;} )?
       { $result = new AddressableAnnotation($ID.text, $addressableAnnotation::isPrimitive); }
    ;

storableAnnotation returns [StorableAnnotation result]
    scope {
        StorableAnnotation annotation;
    }
    @init {
         $storableAnnotation::annotation = new StorableAnnotation();
    }
//example Storable(storagePath = [partition, documentPath, "constant"])
    : STORABLE 
      (^(STORAGE_PATH_ADDER 
       (s1=STRING { $storableAnnotation::annotation.addField($s1.text, com.incapture.rapgen.annotations.storable.StorableFieldType.STRING); }
       | field=ID { $storableAnnotation::annotation.addField($field.text, com.incapture.rapgen.annotations.storable.StorableFieldType.ID); }
      )))+
      (^(STORABLE_SEPARATOR separator=STRING { $storableAnnotation::annotation.setSeparator($separator.text); }) )?
      (^(STORABLE_ENCODING encoding=STRING { $storableAnnotation::annotation.setEncodingType($encoding.text); }) )?
      (^(STORABLE_TTL_DAYS ttlValue=INT { $storableAnnotation::annotation.setTtlDays($ttlValue); }) )?
      (^(STORABLE_PREFIX prefix=STRING { $storableAnnotation::annotation.setPrefix($prefix.text); }) )?
      (^(STORABLE_REPO_NAME repoConstant=ID { $storableAnnotation::annotation.setRepoConstant($repoConstant.text); }) )?
      (^(STORABLE_REPO_NAME repoName=STRING { $storableAnnotation::annotation.setRepoName($repoName.text); }) )?
      {$result = $storableAnnotation::annotation;}
    ; 

deprecatedAnnotation returns [DeprecatedAnnotation result]
//example: Deprecated("because it's rubbish")
    : ( DEPRECATED ID { $result = new DeprecatedAnnotation($ID.text); } ) 
    | ( DEPRECATED STRING  { $result = new DeprecatedAnnotation($STRING.text.substring(1, $STRING.text.length() -1)); } )
    ;

indexedAnnotation returns [IndexedAnnotation result]
//example: @Indexable(a,b)
    scope {
        IndexedAnnotation annotation;
    }
    @init {
         $indexedAnnotation::annotation = new IndexedAnnotation();
    }
    : INDEXED ^(INDEX_NAME(STRING { $indexedAnnotation::annotation.setName($STRING.text.substring(1, $STRING.text.length() - 1)); } ))
      (^(INDEX_COMPONENT(ID { $indexedAnnotation::annotation.addField($ID.text); } )))+
      {$result = $indexedAnnotation::annotation;}
    ;

extendsAnnotation returns [ExtendsAnnotation result]
scope {
    StringBuilder sb;
}
@init {
    $extendsAnnotation::sb = new StringBuilder();
}
//example: Extends(java.lang.Object)
    : EXTENDS PACKAGENAME ID
    { $result = new ExtendsAnnotation($PACKAGENAME.text + "." + $ID.text); }
    ;

fieldConstructor returns [String constructor]
scope {
    StringBuilder sb;
}
@init {
    $fieldConstructor::sb = new StringBuilder();
}
    : 
      (val=NEW { $fieldConstructor::sb.append($val).append(" "); } )?
      (val=(~NEW) { $fieldConstructor::sb.append($val); } ) +
      { $constructor = $fieldConstructor::sb.toString(); }
    ;
