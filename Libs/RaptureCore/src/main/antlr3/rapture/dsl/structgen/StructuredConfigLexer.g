/*
 * Lexer for the structured repository config
 */

lexer grammar StructuredConfigLexer;

options {
   language=Java;  // Default
}

@header {
    package rapture.generated;
}

MEMORY : 'MEMORY';
POSTGRES: 'POSTGRES';
HSQLDB: 'HSQLDB';

STRUCTURED: 'STRUCTURED';

LBRACE : '{';
RBRACE : '}';
COMMA : ',';
EQUALS : '=';
USING : 'using' | 'USING';
WITH : 'with' | 'WITH';
ON: 'on' | 'ON';

ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
STRING : '"' (~'"')* '"';
                     
WS : (' ') {$channel=HIDDEN;} ;
