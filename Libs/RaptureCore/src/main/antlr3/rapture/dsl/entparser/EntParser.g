parser grammar EntParser;

options {
    language  = Java;
    superClass = AbstractEntParser;
    output    = AST;
    tokenVocab = EntLexer;
    rewrite = true;
}

@header {
   package rapture.generated;
   import rapture.dsl.entparser.AbstractEntParser;
}

entpath  : (SLASH idpart)+;

idpart : docpath
           | authority
           | fullpath
           | ID { addPath($ID.text); };
        
docpath: DOCPATH ARG { addPath(getDocPath());};
authority : AUTHORITY ARG { addPath(getAuthority()); };
fullpath : FULLPATH ARG { addPath(getFullPath()); };
