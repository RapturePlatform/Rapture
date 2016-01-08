parser grammar QGenParser;

options {
    language  = Java;
    output    = AST;
    superClass = AbstractQParser;
    tokenVocab = QGenLexer;
}

tokens {
  MAIN;
  ENTRY;
  ENTRIES;
}

@header {
    package rapture.generated;
    import rapture.dsl.qgen.*;
}

qinfo     : pdef USING qdef onInfo?;
onInfo     : ON x=ID { setInstance($x.text); };
pdef	  : s=pstyle config { addProcessorConfig($s.text); };
qdef   	  : s=qstyle config { addConfig($s.text); };

pstyle    : PUBSUB | QUEUE;
qstyle    : MEMORY | FILE | AWS | REDIS | MONGODB | PUBHUB;

config 	  : LBRACE entrylist RBRACE;

entrylist : k=entry? (COMMA k=entry)*;
entry	  : i=ID EQUALS v=STRING { addConfig($i.text, $v.text); };
 			
