/* Lexer for Index Query
 */
 
lexer grammar IndexQueryLexer;

options {
   language=Java;  // Default
}

@header {
   package rapture.generated;
}

// How do we query an index?
// SELECT fielda,fieldb,fieldc WHERE (field='string' AND z>5) ORDER BY fieldd ASC


DISTINCT : 'DISTINCT';
SELECT : 'SELECT';
WHERE  : 'WHERE';
ORDER  : 'ORDER';
BY     : 'BY';
ASC    : 'ASC';
DESC   : 'DESC';
EQUAL  : '=';
GT     : '>';
LT     : '<';
NOTEQUAL : '!=';
COMMA  : ',';
OBRAC : '(';
CBRAC : ')';
AND   : 'AND';
OR    : 'OR';
LIMIT : 'LIMIT';
SKIP  : 'SKIP';
LIKE  : 'LIKE';

ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
STRING : '"' (~'"')* '"'
       | '\'' (~'\'')* '\'';

WS : (' ') {$channel=HIDDEN;} ;

NUMBER
  :  Int '.' Digit (Digit)* ( 'E' ('+' | '-')? Digit (Digit)* )?
  |  Int 'E' ('+' | '-')? Digit (Digit)*
  |  Int
  ;

fragment Int
  :  '1'..'9' Digit*
  |  '0'
  ;

fragment Digit
  :  '0'..'9'
  ;
