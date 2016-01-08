/*
 * Lexer for the Notification Config Helper
 */
 
lexer grammar NGenLexer;

options {
   language=Java;  // Default
}

@header {
   package rapture.generated;
}

// A Notification processor is simply
// NOTIFICATION {} USING MONGODB {}

NOTIFICATION : 'NOTIFICATION';

MEMORY : 'MEMORY';
FILE : 'FILE';
REDIS : 'REDIS';
MONGODB : 'MONGODB';
DUMMY : 'DUMMY';


LBRACE : '{';
RBRACE : '}';
COMMA : ',';
EQUALS : '=';
USING : 'using' | 'USING';

ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
STRING : '"' (~'"')* '"';
                     
WS : (' ') {$channel=HIDDEN;} ;


