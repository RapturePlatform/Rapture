/* Lexer for Index Definition
 */
 
lexer grammar IndexDefLexer;

options {
   language=Java;  // Default
}

@header {
   package rapture.generated;
}

// And Index definition needs to define the custom fields
// of the index and how those fields are populated.
// Basically INDEX fieldName($1) number, otherFieldName(positionInJson.otherSubPart.) string, ...


INDEX : 'INDEX';
OBRAC : '(';
CBRAC : ')';
DOLLAR : '$';
DOT  : '.';
AUTHORITY : '#';
STAR : '*';
NUMBER : ('0' ..'9')+;
ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
STRING : '"' (~'"')* '"';

COMMA : ',';
                     
WS : (' ') {$channel=HIDDEN;} ;


