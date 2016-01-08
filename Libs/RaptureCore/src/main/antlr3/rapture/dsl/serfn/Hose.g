grammar Hose;

options {
  output=AST;
  backtrack=true;
}
 
tokens {
  CALL;
  ARGLIST;
  BODY;
  ARG;
  ARG_D;
  ARG_O;
  ARG_OD;
  OUTKEY;
  OUTINDEX;
  ASSIGN;
  LINK;
  DEREF;
  DEREFLIST;
  SIGNATURE;
  PARM;
  PARMLIST;
}


@parser::header{
package rapture.generated;
}

@lexer::header{
package rapture.generated;
}

program 
  : signature statement* -> ^(BODY signature statement*)
  ;

signature
  : OPENP out=parmlist CLOSEP LINKS Identifier OPENP in=parmlist CLOSEP -> ^(SIGNATURE Identifier $in $out)
  ;

parmlist
  : parm (COMMA parm)* -> ^(PARMLIST parm*)
  ;

parm
  : type Identifier -> ^(PARM type Identifier)
  ;
  
type
  : INT_T
  | DECIMAL_T
  | STRING_T
  | STREAM_T
  ;

statement 
  : Identifier LINKS arg SEMI -> ^(LINK Identifier arg)
  ;
  
call 
  : f=Identifier OPENP arglist CLOSEP -> ^(CALL $f arglist?)
  ;

arglist
  : arg (COMMA arg)* -> ^(ARGLIST arg+)
  ;

baseArg 
  : literal -> literal
  | Identifier -> Identifier
  | call -> call
  ;
  
deref
  : DOT Identifier -> ^(DEREF Identifier)
  ;

derefList
  : deref+ -> ^(DEREFLIST deref+)
  ;
  
arg
  : baseArg outkey derefList -> ^(ARG_OD baseArg outkey? derefList)
  | baseArg outkey -> ^(ARG_O baseArg outkey?)
  | baseArg derefList -> ^(ARG_D baseArg derefList)
  | baseArg -> ^(ARG baseArg)
  ;

outkey
  : OPENI Identifier CLOSEI -> ^(OUTKEY Identifier)
  | OPENI Long CLOSEI -> ^(OUTINDEX Long)
  ;

literal 
  : String
  | Long
  | Double
  ;

DOT : '.';
SEMI : ';';
OPENP : '(';
CLOSEP : ')';
COMMA : ',';
EQUALS : '=';
LINKS : '<-';
OPENI : '[';
CLOSEI : ']';

INT_T : 'int';
DECIMAL_T : 'decimal';
STRING_T : 'string';
STREAM_T : 'stream';
  
Number
  :  Long 
  |  Long '.' Digit (Digit)*
  ;
  
Identifier
  :  ('a'..'z' | 'A'..'Z' | '_') ('a'..'z' | 'A'..'Z' | '_' | Digit)*
  ;

String
@after {
  setText(getText().substring(1, getText().length()-1).replaceAll("\\\\(.)", "$1"));
}
  :  '"'  (~('"' | '\\')  | '\\' ('\\' | '"'))* '"' 
  |  '\'' (~('\'' | '\\') | '\\' ('\\' | '\''))* '\''
  ;

Comment
  :  '//' ~('\r' | '\n')* {skip();}
  |  '/*' .* '*/'         {skip();}
  ;

Space
  :  (' ' | '\t' | '\r' | '\n' | '\u000C') {skip();}
  ;

fragment Double
  : Long . Digit+
  ;
  
fragment Long
  :  '1'..'9' Digit*
  |  '0'
  ;
  
fragment Digit 
  :  '0'..'9'
  ;

  