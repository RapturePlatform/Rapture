/*
 * Lexer for the Blob Repository Config helper
 */
 
lexer grammar BlobGenLexer;

options {
   language=Java;  // Default
}

@header {
       package rapture.generated;
}

// A repository configuration is basically
// BLOB { x=y, ...} using [STORESTYLE] { x=y, ...}

MEMORY : 'MEMORY';
FILE : 'FILE';
AWS : 'AWS';
CASSANDRA : 'CASSANDRA';
MONGODB : 'MONGODB' | 'MONGO';

BLOB : 'BLOB';


LBRACE : '{';
RBRACE : '}';
COMMA : ',';
EQUALS : '=';
USING : 'USING';


ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
STRING : '"' (~'"')* '"';
                     
WS : (' ') {$channel=HIDDEN;} ;


