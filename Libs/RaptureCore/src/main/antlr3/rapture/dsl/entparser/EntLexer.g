/*
 *
 * Lexer for parsing of entitlement paths
 */
 
lexer grammar EntLexer;

options {
   language=Java;  // Default
}

@header {
   package rapture.generated;
}

// An entpath is simply a path, starting with / and containng path parts
// the path parts can include special directives
// $d, $a, $f

DOCPATH : '$d';
AUTHORITY : '$a';
FULLPATH : '$f';
SLASH : '/';
ARG : '(' ID ')';

ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
                     
WS : (' ') {$channel=HIDDEN;} ;


