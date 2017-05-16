parser grammar EGenParser;

options {
    language  = Java;
    output    = AST;
    superClass = AbstractEParser;
    tokenVocab = EGenLexer;
}

tokens {
  MAIN;
  ENTRY;
  ENTRIES;
}

@header {
    package rapture.generated;
    import rapture.dsl.egen.*;
}

qinfo     : pdef USING qdef oninfo?;

oninfo    : ON x=ID { setInstance($x.text); };

pdef	  : s=pstyle config { addProcessorConfig($s.text); };
qdef   	  : s=qstyle config { addConfig($s.text); };

pstyle    : EXCHANGE;
qstyle    : i=(RABBITMQ | MEMORY | GCP_PUBSUB) { setImpl($i); };

config 	  : LBRACE entrylist RBRACE;

entrylist : k=entry? (COMMA k=entry)*;
entry	  : i=ID EQUALS v=STRING { addConfig($i.text, $v.text); };
 			
