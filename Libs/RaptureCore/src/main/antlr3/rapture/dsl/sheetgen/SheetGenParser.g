parser grammar SheetGenParser;

options {
    language  = Java;
    output    = AST;
    superClass = AbstractSheetParser;
    tokenVocab = SheetGenLexer;
}

tokens {
  MAIN;
  ENTRY;
  ENTRIES;
}

@header {
      package rapture.generated;
      import rapture.dsl.sheetgen.AbstractSheetParser;
}

loginfo    : pdef USING idef onInfo?;
onInfo     : ON x=ID { setInstance($x.text); };
pdef	   : s=pstyle config { addProcessorConfig($s.text); };
idef   : s=istyle config { addConfig($s.text); };

pstyle   : SHEET;
// CSV != FILE?
istyle : i=(MEMORY | FILE | MONGODB | CSV) { setImpl($i); };

config 	   : LBRACE entrylist RBRACE;

entrylist	: k=entry? (COMMA k=entry)*;
entry		: i=ID EQUALS v=STRING { addConfig($i.text, $v.text); };
 			

