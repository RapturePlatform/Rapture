/* Lexer for Cube Schema Definition
 */
 
lexer grammar CubeDefLexer;

options {
   language=Java;  // Default
}

@header {
   package rapture.generated;
}

// A cube schema definition simply defines the fields
// that can be set by callers who add to a cube. The field types
// can then be used for slicing and dicing, depending on the types
// Fields can have a type and whether they are "dimensional". 
// Some fields can be dimensional on other fields - e.g. a "Tenor Bucket" field
// could be based on "tenor" and define the groupings, depending some lexical or numerical
// analysis (or simply definitions of groups).


OBRAC : '(';
CBRAC : ')';
DOLLAR : '$';
NUMBER : ('0' ..'9')+;
STRINGTYPE : 'string';
NUMBERTYPE : 'number';
DATETYPE : 'date';
BOOLTYPE : 'bool';
DIMENSIONAL : 'dimensional';
VALUE : 'value';

PERC : 'perc';
GROUP : 'group';

ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
STRING : '"' (~'"')* '"';

COMMA : ',';
                     
WS : (' ') {$channel=HIDDEN;} ;


