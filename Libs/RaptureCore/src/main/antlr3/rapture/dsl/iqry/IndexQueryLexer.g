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


DISTINCT : 'DISTINCT' | 'distinct' | 'Distinct';
SELECT : 'SELECT' | 'select' | 'Select';
WHERE  : 'WHERE' | 'where' | 'Where';
ORDER  : 'ORDER' | 'order' | 'Order';
BY     : 'BY' | 'by' | 'By';
ASC    : 'ASC' | 'asc' | 'Asc';
DESC    : 'DESC' | 'desc' | 'Desc';
EQUAL  : '=';
MINUS  : '-';
GT     : '>';
LT     : '<';
NOTEQUAL : '!=';
COMMA  : ',';
OBRAC : '(';
CBRAC : ')';
AND   : 'AND' | 'and' | 'And';
OR    : 'OR' | 'or' | 'Or';
LIMIT : 'LIMIT' | 'limit' | 'Limit';
SKIP  : 'SKIP' | 'skip' | 'Skip';
LIKE  : 'LIKE' | 'like' | 'Like';

ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
STRING : '"' (~'"')* '"'
       | '\'' (~'\'')* '\'';

WS : (' ') {$channel=HIDDEN;} ;

NUMBER 
  :  (MINUS)? Int '.' Digit (Digit)* ( 'E' ('+' | '-')? Digit (Digit)* )?
  |  (MINUS)? Int 'E' ('+' | '-')? Digit (Digit)*
  |  (MINUS)? Int
  ;

fragment Int
  :  '1'..'9' Digit*
  |  '0'
  ;

fragment Digit
  :  '0'..'9'
  ;
