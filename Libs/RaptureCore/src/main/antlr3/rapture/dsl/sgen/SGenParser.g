parser grammar SGenParser;

options {
    language  = Java;
    output    = AST;
    superClass = AbstractSParser;
    tokenVocab = SGenLexer;
}

tokens {
  MAIN;
  ENTRY;
  ENTRIES;
}

@header {
   package rapture.generated;
   import rapture.dsl.sgen.*;
}

sinfo     : pdef USING qdef;

pdef	  : s=pstyle config { addProcessorConfig($s.text); };
qdef   	  : s=qstyle config { addConfig($s.text); };

pstyle    : STATUS;
qstyle    : q=(MEMORY | FILE | REDIS | MONGODB | DUMMY) { setStore($q); };

config 	  : LBRACE entrylist RBRACE;

entrylist : k=entry? (COMMA k=entry)*;
entry	  : i=ID EQUALS v=STRING { addConfig($i.text, $v.text); };
 			
