/*
 * Lexer for the AuditLog Config helper
 */
 
lexer grammar AuditGenLexer;

options {
   language=Java;
}

@header {
    package rapture.generated;
}

// An index configuration is basically a simple INDEX with an implementation
// [LOG] { x=y, ...} using [LOGIMPL] { x=y, ...}

LOG : 'LOG';

MEMORY : 'MEMORY';
FILE : 'FILE';
MONGODB : 'MONGODB';
REDIS : 'REDIS';
LOG4J : 'LOG4J';
BLOB : 'BLOB';
ELASTIC : 'ELASTICSEARCH';
NOTHING : 'NOTHING';

LBRACE : '{';
RBRACE : '}';
COMMA : ',';
EQUALS : '=';
USING : 'USING' | 'using';
ON : 'ON' | 'on';

ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
STRING : '"' (~'"')* '"';
                     
WS : (' ') {$channel=HIDDEN;} ;


