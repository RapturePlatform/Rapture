parser grammar SRapGenParser;

options {
    language  = Java;
    output    = AST;
    superClass = AbstractSGenParser;
    tokenVocab = SRapGenLexer;
}

tokens {
  MAIN;
  ENTRY;
  ENTRIES;
}

@header {
   package rapture.generated;
   import rapture.dsl.srepgen.*;
}

repinfo    : repdef USING storedef;

repdef	   : s=repstyle config { addProcessorConfig($s.text); };
storedef   : s=storestyle config { addConfig($s.text); };

repstyle   : SREP;
// CSV != FILE?
storestyle : s=(MEMORY | FILE | CASSANDRA | CSV | MONGO) { setStore($s); };

config 	   : LBRACE entrylist RBRACE;

entrylist	: k=entry? (COMMA k=entry)*;
entry		: i=ID EQUALS v=STRING { addConfig($i.text, $v.text); };
 			
// VREP { x="y" } using REDIS { prefix = "one", other = "two"}
