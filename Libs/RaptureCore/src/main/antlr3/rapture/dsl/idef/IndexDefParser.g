parser grammar IndexDefParser;

options {
    language  = Java;
    output    = AST;
    tokenVocab = IndexDefLexer;
    superClass = AbstractIndexDefParser;
}

@header {
   package rapture.generated;
   import rapture.dsl.idef.*;
}

// INDEX fieldName($1) number, otherFieldName(positionInJson.otherSubPart.) string, ...
// list support is available for example, to get to a field in 2nd element of a list:
//       myList(positionInJson.aList.1.fieldInSecondObjectOfList)
// to get all elements in a list:
//       myList(positionInJson.aList.*.fieldInAllObjectsOfList)
// this will result in an IndexRecord where there is a list of values for a given key
// when using the wildcard, it does not currently go any deeper than 1 level after the wildcard

displayLocator returns [FieldLocator locator] :
   x=NUMBER { $locator = new DisplayLocator($x.text); }
  | AUTHORITY   { $locator = new AuthorityLocator(); }
  ;
  
displayPos returns [FieldLocator locator] : DOLLAR x=displayLocator { $locator = $x.locator; };
docPos returns [FieldLocator locator] 
@init {
	$locator = new DocLocator();
} : x=ID { ((DocLocator)$locator).addField($x.text);} (DOT (y=ID | y=NUMBER | y=STAR) { ((DocLocator)$locator).addField($y.text); })*;

fieldLocator returns [FieldLocator locator] : (x=displayPos { $locator = $x.locator; } | y=docPos { $locator = $y.locator; });

fieldDefinition returns [FieldDefinition def]
@init {
   $def = new FieldDefinition();
} : a=ID {$def.setName($a.text); } OBRAC fieldLocator { $def.setLocator($fieldLocator.locator); } CBRAC field=ID {
try {
  $def.setType($field.text); 
} catch (IllegalArgumentException iae) {
  StringBuilder sb = new StringBuilder();
  sb.append($field.text).append(" is not a recognised field type. Options are");
  for (IndexFieldType value : IndexFieldType.values()) sb.append(" ").append(value.name());
  throw new IllegalArgumentException(sb.toString());
}
};

indexName returns [ String nom ]
@init { $nom = "default"; } : INDEX ( OBRAC n=ID CBRAC )? { $nom = $n.text; };

indexDefinition returns [IndexDefinition idef] 
@init {
   $idef = new IndexDefinition();
} : name=indexName? { $idef.setIndexName($name.nom); } x=fieldDefinition { $idef.add($x.def); } (COMMA y=fieldDefinition { $idef.add($y.def); } )*;


