/*
 * Lexer for the Blob Repository Config helper
 */
 
lexer grammar RelationshipLexer;

options {
   language=Java;  // Default
}

@header {
       package rapture.generated;
}

// A repository configuration is basically
// RREP { x=y, ...} using [STORESTYLE] { x=y, ...}

MEMORY : 'MEMORY';
CASSANDRA : 'CASSANDRA';

RREP : 'RREP';


LBRACE : '{';
RBRACE : '}';
COMMA : ',';
EQUALS : '=';
USING : 'USING';


ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
STRING : '"' (~'"')* '"';
                     
WS : (' ') {$channel=HIDDEN;} ;


