parser grammar CubeDefParser;

options {
    language  = Java;
    output    = AST;
    tokenVocab = CubeDefLexer;
//    superClass = AbstractCubeDefParser;
}

@header {
   package rapture.generated;
   import rapture.dsl.cdef.*;
}

//
// when string dimensional autodate(YYYYMMDD,'when'), 
//   strategy string dimensional, 
//   profit number value, 
//   contribution number value perc(profit), 
//   strategyBucket string dimensional group(strategy, 'HY*'='HighYield', '*' = 'Other')
 
cubeSchemaDefinition returns [CubeSchemaDefinition def]
@init {
   $def = new CubeSchemaDefinition();
} : x=fieldDefinition { $def.addField($x.field); } (COMMA y=fieldDefinition { $def.addField($y.field); } )*;

fieldDefinition returns [FieldDefinition field]
@init {
  $field = new FieldDefinition();
} : x=ID {$field.setName($x.text); } fieldType { $field.setType($fieldType.text); } dimensionalType { $field.setDimensional($dimensionalType.text); } calc?;

calc returns [FieldCalc c]
: (perc { $c=$perc.c; } | group { $c=$group.c; });

perc returns [PercCalc c]
@init {
   $c = new PercCalc();
} : PERC OBRAC ID { $c.setSrcField($ID.text); } CBRAC;

group returns [GroupCalc c]
@init {
   $c = new GroupCalc();
} : GROUP OBRAC ID { $c.setSrcField($ID.text); } CBRAC;


fieldType : (STRINGTYPE | NUMBERTYPE | BOOLTYPE | DATETYPE);

dimensionalType : (DIMENSIONAL | VALUE);