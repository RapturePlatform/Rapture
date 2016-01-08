parser grammar AuditGenParser;

options {
    language  = Java;
    output    = AST;
    superClass = AbstractAuditParser;
    tokenVocab = AuditGenLexer;
}

tokens {
  MAIN;
  ENTRY;
  ENTRIES;
}

@header {
      package rapture.generated;
      import rapture.dsl.auditgen.AbstractAuditParser;
}

loginfo    : pdef USING idef onInfo?;
onInfo     : ON x=ID { setInstance($x.text); };
pdef	   : s=pstyle config { addProcessorConfig($s.text); };
idef   : s=istyle config { addConfig($s.text); };

pstyle   : LOG;
istyle : i=(MEMORY | FILE| MONGODB | REDIS | LOG4J | BLOB | ELASTIC | NOTHING) { setImpl($i); };

config 	   : LBRACE entrylist RBRACE;

entrylist	: k=entry? (COMMA k=entry)*;
entry		: i=ID EQUALS v=STRING { addConfig($i.text, $v.text); };
 			

