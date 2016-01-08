parser grammar NGenParser;

options {
    language  = Java;
    output    = AST;
    superClass = AbstractNParser;
    tokenVocab = NGenLexer;
}

tokens {
  MAIN;
  ENTRY;
  ENTRIES;
}

@header {
    package rapture.generated;
    import rapture.dsl.ngen.*;
}

ninfo     : NOTIFICATION USING ndef;

ndef   	  : s=nstyle config { addConfig($s.text); };

nstyle    : s=(MEMORY | FILE | REDIS | MONGODB | DUMMY) { setStore($s); };

config 	  : LBRACE entrylist RBRACE;

entrylist : k=entry? (COMMA k=entry)*;
entry	  : i=ID EQUALS v=STRING { addConfig($i.text, $v.text); };
 			
