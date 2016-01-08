/*
 * Lexer for the Lock Config helper
 */
 
lexer grammar LGenLexer;

options {
   language=Java;  // Default
}

@header {
   package rapture.generated;
}

// A lock configuration is basically the type of processor for the queue
// and the queue storage implementation
// LOCKING using [lockingType] { config }

LOCKING : 'LOCKING';

MEMORY : 'MEMORY';
FILE : 'FILE';
REDIS : 'REDIS';
MONGODB : 'MONGODB';
DUMMY : 'DUMMY';
ZOOKEEPER : 'ZOOKEEPER';
ETCD : 'ETCD';


LBRACE : '{';
RBRACE : '}';
COMMA : ',';
EQUALS : '=';
USING : 'using' | 'USING';

ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
STRING : '"' (~'"')* '"';
                     
WS : (' ') {$channel=HIDDEN;} ;


