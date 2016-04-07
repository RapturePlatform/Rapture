/*
 * Lexer for the FullText Search Repository Config helper
 */
 
lexer grammar FTGenLexer;

options {
   language=Java;  // Default
}

@header {
    package rapture.generated;
}

// A repository configuration is basically
// [REPSTYLE] { x=y, ...} using [STORESTYLE] { x=y, ...}

ELASTIC : 'ELASTIC';

SEARCH : 'SEARCH';

LBRACE : '{';
RBRACE : '}';
COMMA : ',';
EQUALS : '=';
USING : 'using' | 'USING';
ON: 'on' | 'ON';


ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
STRING : '"' (~'"')* '"';
                     
WS : (' ') {$channel=HIDDEN;} ;


