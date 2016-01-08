parser grammar TGenParser;

options {
    language  = Java;
    output    = AST;
    superClass = AbstractTParser;
    tokenVocab = TGenLexer;
}

tokens {
  MAIN;
  ENTRY;
  ENTRIES;
}

@header {
   package rapture.generated;
   import rapture.dsl.tgen.*;
}

iinfo    : pdef USING idef;

pdef	   : s=pstyle config { addProcessorConfig($s.text); };
idef   : s=istyle config { addConfig($s.text); };

pstyle   : TABLE;
istyle : s=(MEMORY | FILE | MONGODB | POSTGRES) { setStore($s); };

config 	   : LBRACE entrylist RBRACE;

entrylist	: k=entry? (COMMA k=entry)*;
entry		: i=ID EQUALS v=STRING { addConfig($i.text, $v.text); };
 			

