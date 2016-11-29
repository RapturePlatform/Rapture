parser grammar IndexQueryParser;

options {
    language  = Java;
    output    = AST;
//    superClass = AbstractLParser;
    tokenVocab = IndexQueryLexer;
}

tokens {

}

@header {
    package rapture.generated;
    import rapture.dsl.iqry.*;
}

// SELECT fielda,fieldb,fieldc WHERE (field='string' AND z>5) ORDER BY fieldd ASC

qry returns [IndexQuery qry] 
@init {
  $qry = new IndexQuery();
}     
: SELECT (DISTINCT { $qry.setDistinct(true); } )? x=fieldSet { $qry.setSelect($x.select); }
  (WHERE w=whereClause { $qry.setWhere($w.where); })?
  (
     (ORDER BY o=fieldSet { $qry.setOrderBy($o.select); } (ASC { $qry.setOrderDirection(OrderDirection.ASC); } | DESC { $qry.setOrderDirection(OrderDirection.DESC); })? ) 
   | (LIMIT l=NUMBER { $qry.setLimit(Integer.parseInt($l.text)); }) 
   | (SKIP n=NUMBER { $qry.setSkip(Integer.parseInt($n.text)); })
   | (s=ID) { if ($s != null) throw new IllegalArgumentException("Unexpected token "+$s.text+" found in "+input); }
   | (s=STRING) { if ($s != null) throw new IllegalArgumentException("Unexpected token "+$s.text+" found in "+input); }
  )*
  ;

fieldSet returns [SelectList select]
@init {
  $select = new SelectList();
} : x=ID { $select.add($x.text); } (COMMA y=ID { $select.add($y.text); } )*;


whereClause returns [WhereClause where]
@init {
  $where = new WhereClause();
} : w=whereStatement { $where.addStatement($w.stmt); } (z=(AND | OR) y=whereStatement { $where.appendStatement($z.text, $y.stmt); } )*;

value returns [WhereValue val]   : x=STRING { $val = new StringWhereValue($x.text);}  | y=NUMBER { $val = new NumberWhereValue($y.text); };
whereStatement returns [WhereStatement stmt] :
           a=ID EQUAL z=value { $stmt = new WhereStatement($a.text, WhereTest.EQUAL, $z.val); }
         | b=ID GT y=value { $stmt = new WhereStatement($b.text, WhereTest.GT, $y.val); }
         | c=ID LT x=value { $stmt = new WhereStatement($c.text, WhereTest.LT, $x.val); }
         | d=ID NOTEQUAL w=value { $stmt = new WhereStatement($d.text, WhereTest.NOTEQUAL, $w.val); }
         | e=ID LIKE w=value { $stmt = new WhereStatement($e.text, WhereTest.LIKE, $w.val); }
         ;
         
