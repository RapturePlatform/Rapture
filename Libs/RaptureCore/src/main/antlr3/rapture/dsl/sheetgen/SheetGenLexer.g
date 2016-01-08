/*
 * Lexer for the Sheet Config helper
 */
 
lexer grammar SheetGenLexer;

options {
   language=Java;
}

@header {
    package rapture.generated;
}

// An index configuration is basically a simple INDEX with an implementation
// [SHEET] { x=y, ...} using [SHEETIMPL] { x=y, ...}

SHEET : 'SHEET';

MEMORY : 'MEMORY';
FILE : 'FILE';
MONGODB : 'MONGODB';
CSV : 'CSV';

LBRACE : '{';
RBRACE : '}';
COMMA : ',';
EQUALS : '=';
USING : 'USING' | 'using';
ON : 'ON' | 'on';

ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
STRING : '"' (~'"')* '"';
                     
WS : (' ') {$channel=HIDDEN;} ;


