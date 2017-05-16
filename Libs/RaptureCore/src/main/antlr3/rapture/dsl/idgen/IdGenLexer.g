/*
 * Lexer for the Fountain Config helper
 */
 
lexer grammar IdGenLexer;

options {
   language=Java;  // Default
}

@header {
   package rapture.generated;
}

// An index configuration is basically a simple INDEX with an implementation
// [INDEX] { x=y, ...} using [INDEXSTYLE] { x=y, ...}

IDGEN : 'IDGEN';

MEMORY : 'MEMORY';
FILE : 'FILE';
GCP_DATASTORE : 'GCP_DATASTORE';
GCP_STORAGE : 'GCP_STORAGE';
MONGODB : 'MONGODB';

LBRACE : '{';
RBRACE : '}';
COMMA : ',';
EQUALS : '=';
USING : 'USING' | 'using';
ON: 'ON' | 'on';

ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
STRING : '"' (~'"')* '"';
                     
WS : (' ') {$channel=HIDDEN;} ;


