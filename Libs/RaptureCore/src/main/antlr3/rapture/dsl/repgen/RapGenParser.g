parser grammar RapGenParser;

options {
    language  = Java;
    output    = AST;
    superClass = AbstractGenParser;
    tokenVocab = RapGenLexer;
}

tokens {
  MAIN;
  ENTRY;
  ENTRIES;
}

@header {
   package rapture.generated;
   import rapture.dsl.repgen.*;
}

repinfo    : readonly? cache? repdef USING storedef cacheinfo? shadowinfo? onInfo?;

cache      : CACHE { setGeneralCache(); };
readonly   : READONLY { setReadOnly(); };
onInfo     : ON x=ID { setInstance($x.text); };
shadowtype : s=(SHADOW | VSHADOW) { setShadowType($s); };
shadowinfo : shadowtype s=shadowstyle config { addShadowConfig($s.text); };
cacheinfo  : WITH c=cachestyle config { addCacheConfig($c.text); };
repdef	   : s=repstyle config { addProcessorConfig($s.text); };
storedef   : s=storestyle config { addConfig($s.text); };

repstyle   : RREP | VREP | REP | NREP | QREP;
cachestyle : c=EHCACHE { setCache($c); };
storestyle : s=(CASSANDRA | GCP_DATASTORE | GCP_STORAGE | REDIS | MEMORY | AWS | MEMCACHED | MONGODB | FILE | EHCACHE | JDBC | CSV | POSTGRES | NOTHING) { setStore($s); };
shadowstyle : s=(REDIS | MEMORY | AWS | MEMCACHED | MONGODB | FILE | EHCACHE ) { setShadow($s); };

config 	   : LBRACE entrylist RBRACE;

entrylist	: k=entry? (COMMA k=entry)*;
entry		: i=ID EQUALS v=STRING { addConfig($i.text, $v.text); };
 			
// VREP { x="y" } using REDIS { prefix = "one", other = "two"}
