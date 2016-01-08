/*
 * Lexer for the Table Config helper
 */
 
lexer grammar TGenLexer;

options {
   language=Java;  // Default
}

@header {
   package rapture.generated;
}

// An index configuration is basically a simple INDEX with an implementation
// [INDEX] { x=y, ...} using [INDEXSTYLE] { x=y, ...}

TABLE : 'TABLE';

MEMORY : 'MEMORY';
FILE : 'FILE';
AWS : 'AWS';
MONGODB : 'MONGODB';
POSTGRES : 'POSTGRES';

LBRACE : '{';
RBRACE : '}';
COMMA : ',';
EQUALS : '=';
USING : 'USING' | 'using';

ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
STRING : '"' (~'"')* '"';
                     
WS : (' ') {$channel=HIDDEN;} ;


