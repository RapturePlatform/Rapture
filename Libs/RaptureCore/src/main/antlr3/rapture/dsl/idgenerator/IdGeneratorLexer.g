/*
 * Lexer for the Full Text Index Config helper
 */
 
lexer grammar IdGeneratorLexer;

options {
   language=Java;  // Default
}

@header {
    package rapture.generated;
}

// An index configuration is basically a simple INDEX with an implementation
// [FTINDEX] { x=y, ...} using [FTINDEXSTYLE] { x=y, ...}

INDEX : 'FTINDEX';

MEMORY : 'MEMORY';
FILE : 'FILE';
// AWS : 'FILE';

LBRACE : '{';
RBRACE : '}';
COMMA : ',';
EQUALS : '=';
USING : 'USING' | 'using';

ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
STRING : '"' (~'"')* '"';
                     
WS : (' ') {$channel=HIDDEN;} ;


