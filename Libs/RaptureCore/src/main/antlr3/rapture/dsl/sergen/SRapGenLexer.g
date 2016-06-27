/*
 * Lexer for the Series Repository Config helper
 */
 
lexer grammar SRapGenLexer;

options {
   language=Java;  // Default
}

@header {
    package rapture.generated;
}

// A repository configuration is basically
// [REPSTYLE] { x=y, ...} using [STORESTYLE] { x=y, ...}

MEMORY : 'MEMORY';
FILE : 'FILE';
CASSANDRA : 'CASSANDRA';
MONGODB: 'MONGODB';
CSV: 'CSV';

SREP : 'SREP';

LBRACE : '{';
RBRACE : '}';
COMMA : ',';
EQUALS : '=';
USING : 'using' | 'USING';
WITH : 'with' | 'WITH';

ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
STRING : '"' (~'"')* '"';
                     
WS : (' ') {$channel=HIDDEN;} ;


