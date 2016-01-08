parser grammar MetricGenParser;

options {
    language  = Java;
    output    = AST;
    superClass = AbstractMetricParser;
    tokenVocab = MetricGenLexer;
}

tokens {
  MAIN;
  ENTRY;
  ENTRIES;
}

@header {
    package rapture.generated;
    import rapture.dsl.metricgen.*;
}

minfo     : mstyle WITH config;

mstyle    : m=(VALUE | PRESENCE | MESSAGE | COUNTER) { setMetric($m); };

config 	  : LBRACE entrylist RBRACE;

entrylist : k=entry? (COMMA k=entry)*;
entry	  : i=ID EQUALS v=STRING { addConfig($i.text, $v.text); };
 			
