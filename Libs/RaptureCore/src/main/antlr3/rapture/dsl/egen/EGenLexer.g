/*
 * Lexer for the Exchange Config helper
 */
 
lexer grammar EGenLexer;

options {
   language=Java;  // Default
}

@header {
    package rapture.generated;
}

// A queue configuration is basically the type of processor for the queue
// and the queue storage implementation
// [Processor] { x=y, ...} using [EXCHANGESTYLE] { x=y, ...}

RABBITMQ : 'RABBITMQ';
EXCHANGE : 'EXCHANGE';
MEMORY : 'MEMORY';

LBRACE : '{';
RBRACE : '}';
COMMA : ',';
EQUALS : '=';
USING : 'using' | 'USING';
ON: 'on' | 'ON';

ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
STRING : '"' (~'"')* '"';
                     
WS : (' ') {$channel=HIDDEN;} ;


