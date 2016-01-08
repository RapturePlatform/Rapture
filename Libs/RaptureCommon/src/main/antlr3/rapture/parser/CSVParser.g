/*

 Copyright 2011 by Nathaniel Harward <nharward@gmail.com>

 ANTLRv3 grammar for CSV files.

 * No trimming of spaces (disallowed in RFC4180)
 * No trimming of double-quotes, either to define/end a quoted field or
   when embedded inside one
 * Handles all/mixed newline formats (MSDOS/Windows; Unix; Mac OS)

 If you find any issues please email me so I can correct it.

*/

parser grammar CSVParser;

options {
    language  = Java;
    output    = AST;
    tokenVocab = CSVLexer;
    superClass = AbstractCSVParser;
}


@header {
package rapture.generated;

import rapture.parser.AbstractCSVParser;
}


file
    : record (NEWLINE record)* EOF
    ;

record
@init {
    startNewLine();
}
    : x=quoted_or_unquoted { addCell($x.name); } (COMMA y=quoted_or_unquoted { addCell($y.name); })*
    ;

quoted_or_unquoted returns [String name] 
    : (x=quoted_field { $name = $x.name; } | y=unquoted_field { $name = $y.name; } )
    ;
    
inner_field
    : ( CHAR
    | COMMA
    | DQUOTE DQUOTE
    | NEWLINE
    )*;
    
quoted_field returns [String name]
    : DQUOTE
    x=inner_field DQUOTE
    { $name = $x.text;} ;

unquoted_field returns [String name]
    : f=inner_char { $name = $f.text; }
    ;

inner_char :
     ( CHAR )*;
     
