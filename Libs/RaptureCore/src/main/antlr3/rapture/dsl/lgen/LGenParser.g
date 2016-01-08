parser grammar LGenParser;

options {
    language  = Java;
    output    = AST;
    superClass = AbstractLParser;
    tokenVocab = LGenLexer;
}

tokens {
  MAIN;
  ENTRY;
  ENTRIES;
}

@header {
    package rapture.generated;
    import rapture.dsl.lgen.*;
}

linfo     : LOCKING USING ldef;

ldef   	  : s=lstyle config { addConfig($s.text); };

lstyle    : s=(MEMORY | FILE | REDIS | MONGODB | DUMMY) { setStore($s); };

config 	  : LBRACE entrylist RBRACE;

entrylist : k=entry? (COMMA k=entry)*;
entry	  : i=ID EQUALS v=STRING { addConfig($i.text, $v.text); };
 			
