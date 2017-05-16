/*
 * Lexer for the Queue Config helper
 */
 
lexer grammar QGenLexer;

options {
   language=Java;  // Default
}

@header {
    package rapture.generated;
}

// A queue configuration is basically the type of processor for the queue
// and the queue storage implementation
// [Processor] { x=y, ...} using [QUEUESTYLE] { x=y, ...}

PUBSUB : 'PUBSUB';
QUEUE : 'QUEUE';

MEMORY : 'MEMORY';
FILE : 'FILE';
AWS : 'AWS';
REDIS : 'REDIS';
MONGODB : 'MONGODB';
PUBHUB : 'PUBHUB';
GCP_PUBSUB : 'GCP_PUBSUB';

ON: 'ON' | 'on';
LBRACE : '{';
RBRACE : '}';
COMMA : ',';
EQUALS : '=';
USING : 'using' | 'USING';

ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
STRING : '"' (~'"')* '"';
                     
WS : (' ') {$channel=HIDDEN;} ;


