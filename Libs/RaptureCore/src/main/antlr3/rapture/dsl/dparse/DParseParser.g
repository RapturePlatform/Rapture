/*
 * Copyright (c) 2011-2012 Alan Moore ukmoore@gmail.com
 * 
 * Parser for display name parsing
 *
 * A display name is [type]/displayname?[directives]
 */

parser grammar DParseParser;

options {
    language  = Java;
    output    = AST;
//    superClass = AbstractDParser;
    tokenVocab = DParseLexer;
//    backtrack = true;
}


@header {
   package rapture.generated;
   import rapture.dsl.dparse.*;
   import rapture.common.model.DocumentAttribute;
   import rapture.common.DocumentAttributeFactory;
}

@members {
public String getErrorMessage(RecognitionException e,
                                     String[] tokenNames)
       {
           List stack = getRuleInvocationStack(e, this.getClass().getName());
           String msg = null;
           if ( e instanceof NoViableAltException ) {
              NoViableAltException nvae = (NoViableAltException)e;
              msg = " no viable alt; token="+e.token+
                 " (decision="+nvae.decisionNumber+
                 " state "+nvae.stateNumber+")"+
                 " decision=<<"+nvae.grammarDecisionDescription+">>";
}
else {
              msg = super.getErrorMessage(e, tokenNames);
           }
           return stack+" "+msg;
       }
public String getTokenErrorDisplay(Token t) { return t.toString();
}
}

displayname returns [String type, String disp, BaseDirective directive, DocumentAttribute attribute] 
  : typePart namePart? queryDirective? attributePart? { 
  $type = $typePart.text; 
  if ($namePart.text != null) {
    $disp = $namePart.text.substring(1);
  }
  $directive = $queryDirective.directive;
  $attribute = $attributePart.attributeRet;
  if ($attribute != null) {
    $attribute.setKey($text);
  }
};
  
typePart    : (LETTER | DIGIT | UNDER )+;
path    : SLASH (DIGIT | LETTER | UNDER | DOT )+;
namePart : path+;

queryDirective returns [BaseDirective directive] : QUERY absoluteVersion { $directive = $absoluteVersion.directive;};

attributePart returns [DocumentAttribute attributeRet]: SLASH attribute { $attributeRet = $attribute.att;};

// 5 days ago

valuetime returns [ValueTime vt]
@init { $vt = new ValueTime(); }
: amount=number td=timedimension { $vt.setAmount($amount.text); $vt.setDimension($td.d); };

datetime returns [BaseDirective directive] : 
    absolutetime { $directive = $absolutetime.directive; }
    | relativetime  { $directive = $relativetime.directive; }
    | relativeversion { $directive = $relativeversion.directive; };
    
relativetime returns [RelativeDirective directive ] 
@init { $directive = new RelativeDirective(); } 
: v1=valuetime { $directive.addValue($v1.vt); } ( (COMMA | AND) v2=valuetime { $directive.addValue($v2.vt); } )* AGO;


timedimension returns [Dimension d]  : 
            HOUR  { $d = Dimension.HOUR; } 
          | MINUTE { $d = Dimension.MINUTE; } 
          | DAY { $d = Dimension.DAY; } 
          | WEEK { $d = Dimension.WEEK; } 
          | MONTH { $d = Dimension.MONTH; } 
          | YEAR { $d = Dimension.YEAR; } 
          | SECOND { $d = Dimension.SECOND; } ;

time returns [RaptureTime t]
@init {$t = new RaptureTime(); }
: h=number { $t.setHour($h.text); } COLON m=number { $t.setMinute($m.text); } COLON s=number { $t.setSecond($s.text); };

date returns [RaptureDate d] 
@init { $d = new RaptureDate(); }
:   y=number { $d.setYear($y.text); } 
    SLASH m=number { $d.setMonth($m.text); } 
    SLASH da=number  { $d.setDay($da.text); };



absolutetime returns [AbsoluteDirective directive ]
@init { $directive = new AbsoluteDirective(); }
: DOLLAR date { $directive.setDate($date.d); } DASH time { $directive.setTime($time.t); };

relativeversion returns [RelativeVersion directive ]
@init { $directive = new RelativeVersion(); }
: negnumber { $directive.setBack($negnumber.text); };

absoluteVersion returns [AbsoluteVersion directive ]
@init { $directive = new AbsoluteVersion(); }
: number { $directive.setVersion($number.text); };

attribute returns [DocumentAttribute att]
: attributePartType attributePartKey? { 
  $att = DocumentAttributeFactory.create($attributePartType.text.substring(1));
  };

negnumber : DASH number;
number: DIGIT+;

attributePartType : DOLLAR LETTER+;
attributePartKey : SLASH STRING;

