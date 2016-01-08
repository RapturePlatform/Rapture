parser grammar IdGeneratorParser;

options {
    language  = Java;
    output    = AST;
    superClass = AbstractIdGeneratorParser;
    tokenVocab = IdGeneratorLexer;
}

tokens {
  MAIN;
  ENTRY;
  ENTRIES;
}

@header {
    package rapture.generated;
    import rapture.dsl.idgenerator.*;
}

iinfo    : pdef USING idef;

pdef	   : s=pstyle config { addProcessorConfig($s.text); };
idef   : s=istyle config { addConfig($s.text); };

pstyle   : INDEX;
istyle : MEMORY | FILE;

config 	   : LBRACE entrylist RBRACE;

entrylist	: k=entry? (COMMA k=entry)*;
entry		: i=ID EQUALS v=STRING { addConfig($i.text, $v.text); };
 			

