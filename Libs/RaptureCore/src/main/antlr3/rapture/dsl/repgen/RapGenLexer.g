/*
 * Lexer for the Repository Config helper
 */
 
lexer grammar RapGenLexer;

options {
   language=Java;  // Default
}

@header {
    package rapture.generated;
}

// A repository configuration is basically
// [REPSTYLE] { x=y, ...} using [STORESTYLE] { x=y, ...}

REDIS : 'REDIS';
MEMORY : 'MEMORY';
MEMCACHED : 'MEMCACHED';
AWS : 'AWS';
MONGODB : 'MONGODB';
FILE : 'FILE';
EHCACHE : 'EHCACHE';
JDBC : 'JDBC';
CASSANDRA : 'CASSANDRA';
GCP_DATASTORE : 'GCP_DATASTORE';
GCP_STORAGE : 'GCP_STORAGE';
CSV : 'CSV';
POSTGRES : 'POSTGRES';
NOTHING : 'NOTHING';

VREP : 'VREP';
REP  : 'REP';
NREP : 'NREP';
QREP : 'QREP';
RREP : 'RREP'; // Reflex REP

LBRACE : '{';
RBRACE : '}';
COMMA : ',';
EQUALS : '=';
USING : 'using' | 'USING';
WITH : 'with' | 'WITH';
SHADOW : 'shadow' | 'SHADOW';
VSHADOW : 'vshadow' | 'VSHADOW';
READONLY : 'readonly' | 'READONLY';
CACHE : 'cache' | 'CACHE';
ON: 'on' | 'ON';


ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;
STRING : '"' (~'"')* '"';
                     
WS : (' ') {$channel=HIDDEN;} ;


