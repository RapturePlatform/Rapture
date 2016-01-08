parser grammar StructuredConfigParser;

options {
    language  = Java;
    output    = AST;
    superClass = AbstractStructuredConfigParser;
    tokenVocab = StructuredConfigLexer;
}

tokens {
  MAIN;
  ENTRY;
  ENTRIES;
}

@header {
   package rapture.generated;
   import rapture.dsl.structured.*;
}

repinfo    : repdef USING storedef;

repdef     : s=repstyle config { addProcessorConfig($s.text); };
storedef   : s=storestyle config { addConfig($s.text); };

repstyle   : STRUCTURED;
storestyle : s=(MEMORY | POSTGRES | HSQLDB) { setStore($s); };

config     : LBRACE entrylist RBRACE;

entrylist : k=entry? (COMMA k=entry)*;
entry   : i=ID EQUALS v=STRING { addConfig($i.text, $v.text); };

