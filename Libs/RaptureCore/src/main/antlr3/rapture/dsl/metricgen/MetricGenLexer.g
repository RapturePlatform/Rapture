/*
 * Lexer for the defining a metric
 */
 
lexer grammar MetricGenLexer;

options {
   language=Java;  // Default
}

@header {
    package rapture.generated;
}

// metric definition is
// [VALUE | MESSAGE | PRESENCE] WITH {}


VALUE : 'VALUE';
MESSAGE : 'MESSAGE';
PRESENCE : 'PRESENCE';
COUNTER : 'COUNTER';

LBRACE : '{';
RBRACE : '}';
COMMA : ',';
EQUALS : '=';
WITH : 'with' | 'WITH';

ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
STRING : '"' (~'"')* '"';
                     
WS : (' ') {$channel=HIDDEN;} ;


