parser grammar IdGenParser;

options {
    language  = Java;
    output    = AST;
    superClass = AbstractIdGenParser;
    tokenVocab = IdGenLexer;
}

tokens {
  MAIN;
  ENTRY;
  ENTRIES;
}

@header {
    package rapture.generated;
    import rapture.dsl.idgen.AbstractIdGenParser;
}

iinfo    : pdef USING idef onInfo?;
onInfo     : ON x=ID { setInstance($x.text); };
pdef	   : s=pstyle config { addProcessorConfig($s.text); };
idef   : s=istyle config { addConfig($s.text); };

pstyle   : IDGEN;
istyle : s=(MEMORY | FILE| AWS | MONGODB) { setStore($s); };

config 	   : LBRACE entrylist RBRACE;

entrylist	: k=entry? (COMMA k=entry)*;
entry		: i=ID EQUALS v=STRING { addConfig($i.text, $v.text); };
 			

