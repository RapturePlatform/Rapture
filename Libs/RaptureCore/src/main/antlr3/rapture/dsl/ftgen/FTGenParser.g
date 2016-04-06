parser grammar FTGenParser;

options {
    language  = Java;
    output    = AST;
    superClass = AbstractFTGenParser;
    tokenVocab = FTGenLexer;
}

tokens {
  MAIN;
  ENTRY;
  ENTRIES;
}

@header {
   package rapture.generated;
   import rapture.dsl.ftgen.*;
}

repinfo    : repdef USING storedef onInfo?;

onInfo     : ON x=ID { setInstance($x.text); };
repdef	   : s=repstyle config { addProcessorConfig($s.text); };
storedef   : s=storestyle config { addConfig($s.text); };

repstyle   : FTREP;
storestyle : s=(ELASTIC) { setStore($s); };

config 	   : LBRACE entrylist RBRACE;

entrylist	: k=entry? (COMMA k=entry)*;
entry		: i=ID EQUALS v=STRING { addConfig($i.text, $v.text); };
 			