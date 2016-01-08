/*
 * Lexer for the Status Config helper
 */
 
lexer grammar SGenLexer;

options {
   language=Java;  // Default
}

@header {
   package rapture.generated;
}

// status defininition is basically
// STATUS {} USING [IMPL] {}
// E.g. STATUS {} USING MEMORY {}


STATUS : 'STATUS';

MEMORY : 'MEMORY';
FILE : 'FILE';
REDIS : 'REDIS';
MONGODB : 'MONGODB' | 'MONGO';
DUMMY : 'DUMMY';

LBRACE : '{';
RBRACE : '}';
COMMA : ',';
EQUALS : '=';
USING : 'using' | 'USING';

ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
STRING : '"' (~'"')* '"';
                     
WS : (' ') {$channel=HIDDEN;} ;


