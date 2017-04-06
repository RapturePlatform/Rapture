/*
 * Lexer for the Pipeline2 Config helper
 */
 
lexer grammar P2GenLexer;

options {
   language=Java;  // Default
}

@header {
    package rapture.generated;
}

GCP_PUBSUB : 'GCP_PUBSUB';
RABBITMQ : 'RABBITMQ';
PIPELINE : 'PIPELINE';
MEMORY : 'MEMORY';

LBRACE : '{';
RBRACE : '}';
COMMA : ',';
EQUALS : '=';
USING : 'using' | 'USING';

ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
STRING : '"' (~'"')* '"';
                     
WS : (' ') {$channel=HIDDEN;} ;


