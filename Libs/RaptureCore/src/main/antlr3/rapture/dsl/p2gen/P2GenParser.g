parser grammar P2GenParser;

options {
    language  = Java;
    output    = AST;
    superClass = AbstractP2Parser;
    tokenVocab = P2GenLexer;
}

tokens {
  MAIN;
  ENTRY;
  ENTRIES;
}

@header {
    package rapture.generated;
    import rapture.dsl.p2gen.*;
}

qinfo     : pdef USING qdef;

pdef	  : s=pstyle config { addProcessorConfig($s.text); };
qdef   	  : s=qstyle config { addConfig($s.text); };

pstyle    : PIPELINE;
qstyle    : i=(RABBITMQ | MEMORY | GCP_PUBSUB) { setImpl($i); };

config 	  : LBRACE entrylist RBRACE;

entrylist : k=entry? (COMMA k=entry)*;
entry	  : i=ID EQUALS v=STRING { addConfig($i.text, $v.text); };
 			
