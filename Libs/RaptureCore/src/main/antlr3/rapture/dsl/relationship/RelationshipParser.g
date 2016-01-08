parser grammar RelationshipParser;

options {
    language  = Java;
    output    = AST;
    superClass = AbstractRelationshipParser;
    tokenVocab = RelationshipLexer;
}

tokens {
  MAIN;
  ENTRY;
  ENTRIES;
}

@header {
       package rapture.generated;
       import rapture.dsl.relationship.AbstractRelationshipParser;
}

repinfo    : repdef USING storedef;

repdef	   : s=repstyle config { addProcessorConfig($s.text); };
storedef   : s=storestyle config { addConfig($s.text); };

repstyle   : RREP;
storestyle : s=(MEMORY | CASSANDRA) { setStore($s); };

config 	   : LBRACE entrylist RBRACE;

entrylist	: k=entry? (COMMA k=entry)*;
entry		: i=ID EQUALS v=STRING { addConfig($i.text, $v.text); };
 			
// RREP { x="y" } using MEMORY { prefix = "one", other = "two"}
