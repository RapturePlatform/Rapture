parser grammar BlobGenParser;

options {
    language  = Java;
    output    = AST;
    superClass = AbstractBlobParser;
    tokenVocab = BlobGenLexer;
}

tokens {
  MAIN;
  ENTRY;
  ENTRIES;
}

@header {
       package rapture.generated;
       import rapture.dsl.blobgen.AbstractBlobParser;
}

repinfo    : repdef USING storedef;

repdef	   : s=repstyle config { addProcessorConfig($s.text); };
storedef   : s=storestyle config { addConfig($s.text); };

repstyle   : BLOB;
storestyle : s=(MEMORY | FILE | AWS | CASSANDRA | GCP_STORAGE | MONGODB) { setStore($s); };

config 	   : LBRACE entrylist RBRACE;

entrylist	: k=entry? (COMMA k=entry)*;
entry		: i=ID EQUALS v=STRING { addConfig($i.text, $v.text); };
 			
// BLOB { x="y" } using MEMORY { prefix = "one", other = "two"}
