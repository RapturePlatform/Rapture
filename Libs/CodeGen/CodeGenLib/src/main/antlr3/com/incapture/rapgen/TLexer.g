/*
 * Lexer for the Rapture API parser, parseable using ANTLR
 */
 
lexer grammar TLexer;

options {
   language=Java;
   superClass = AbstractTLexer;
}

@header {
    package com.incapture.rapgen;
}

// Code for handling INCLUDE directives (include other file)

@lexer::members {
    class SaveStruct {
      SaveStruct(CharStream input){
        this.input = input;
        this.marker = input.mark();
      }
      public CharStream input;
      public int marker;
     }
 
     Stack<SaveStruct> includes = new Stack<SaveStruct>();
 
     public Token nextToken(){
       Token token = super.nextToken();
 
       if(token.getType() == Token.EOF && !includes.empty()){
        // We've got EOF and have non empty stack.
         SaveStruct ss = includes.pop();
         setCharStream(ss.input);
         input.rewind(ss.marker);
         //this should be used instead of super [like below] to handle exits from nested includes
         //it matters, when the 'include' token is the last in previous stream (using super, lexer 'crashes' returning EOF token)
         token = this.nextToken();
       }
 
      // Skip first token after switching on another input.
      // You need to use this rather than super as there may be nested include files
       if(((CommonToken)token).getStartIndex() < 0)
         token = this.nextToken();
 
       return token;
     }
     
     @Override
     public void reportError(RecognitionException e) {
       super.reportError(e);
       throw new IllegalArgumentException("Failed");
     }
}

STRING : '"' .* '"'
      ;

INCLUDE
     : 'include' (WS)? f=STRING {
       String name = f.getText();
       name = name.substring(1,name.length()-1);
       try {
        // save current lexer's state
         SaveStruct ss = new SaveStruct(input);
         includes.push(ss);
 
        // switch on new input stream
         setCharStream(getApiReader().read(name));
         reset();
 
       } catch(Exception fnf) { throw new Error("Cannot open file " + name); }
     }
     ;

//Annotations
AT  			: '@';     
STORAGE_PATH 	: 'storagePath';
SCHEME 			: 'scheme';
STORABLE 		: 'Storable';
CACHEABLE 		: 'Cacheable';
ADDRESSABLE 	: 'Addressable';
SEARCHABLE 	    : 'Searchable';
FTS				: 'FTS';
DEPRECATED 		: ('d'|'D') 'eprecated';
EXTENDS 		: 'Extends';
BEAN 			: 'Bean';
INDEXED 		: 'Indexable';
SDKNAME   		: 'sdk'; 
TYPED 			: 'type';
CRUDTYPED   	: 'crud';
PACKAGE 		: '@package';
ENTITLE   		: '@entitle';
PRIVATE   		: '@private';
PUBLIC    		: '@public';
STREAMING 		: '@streaming';
EQUAL 			: '=';
DATA   			: 'Data' ;
LPAREN 			: '{' ;
RPAREN 			: '}' ;
LBRAC 			: '(' ;
RBRAC 			: ')' ;
API 			: 'api';
BANG    		: '!';
INTTYPE 		: 'int';
LONGTYPE 		: 'long';
LONGCTYPE		: 'Long';
OBJECTTYPE		: 'Object';
STRINGTYPE 		: 'String';
DOUBLETYPE  	: 'Double';
BOOLTYPE 		: 'Boolean';
LISTTYPE 		: 'List';
MAPTYPE 		: 'Map';
SETTYPE     	: 'Set';
BYTEARRAYTYPE 	: 'ByteArray';
TRUE			: 'true';
FALSE			: 'false';
DATETYPE    	: 'Date';
NEW         	: 'new';
VOIDTYPE    	: 'void';

OPENSQUARE  	: '[' ;
CLOSESQUARE 	: ']' ;
COMMA 			: ',';
DOT       		: '.';
PACKAGENAME 	: ('a'..'z')+ ('.' ('a' ..'z')+)+;
REGULARENTITLE 	: '/' ('a'..'z')+;
DYNENT 			: '/$';
SEMI			: ';' ;
ID  			:	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;

INT 			:	'0'..'9'+;

DOC     		:   OPENSQUARE ( options {greedy=false;} : ~('\u0080'..'\uFFFE') )* CLOSESQUARE;

COMMENT     	:   '//' ~('\n'|'\r')* '\r'? '\n' { $channel = HIDDEN; }
    			|   '/*' ( options {greedy=false;} : . )* '*/' { $channel = HIDDEN; }
    			;

SQUOTE    		: '\'';
L_THAN    		: '<';
G_THAN    		: '>';
MINUS     		: '-';

WS      		:   ( ' '
          		| '\t'
          		| '\r'
          		| '\n'
          		| '\f'
          			) { $channel = HIDDEN;}
        		;

COLUMN 			: ':';
