/*
 * Copyright (c) 2011-2012 Alan Moore ukmoore@gmail.com
 * 
 * Lexer for display name parsing
 *
 * A display name is [type]/displayname?[directives]
 */
 
lexer grammar DParseLexer;

options {
   language=Java;  // Default
}

@header {
   package rapture.generated;
}


HOUR   : '!hour'   | '!hours'   | '!hr' DOT?  | '!hrs' DOT?;
MINUTE : '!minute' | '!minutes' | '!min' DOT? | '!mins' DOT?;
DAY    : '!day'    | '!days' ;
WEEK   : '!week'   | '!weeks'   | '!wks' DOT?;
MONTH  : '!month'  | '!months';
YEAR   : '!year'   ;

TODAY     : '!today';
TOMORROW  : '!tomorow'   | '!tomorrow'   | '!tommorow' | '!tommorrow';
TONIGHT   : '!tonight'; 
YESTERDAY : '!yesterday';

// ********** time rules ********** 

SECOND         : '!second' | '!seconds';
   
// ********** common rules **********

HASH  : '#';
COLON : ':';
COMMA : ',';
DASH  : '-';
SLASH : '/';
DOT   : '.';
PLUS  : '+';
DOLLAR : '$';
SINGLE_QUOTE : '\'';
QUERY : '@';
UNDER : '_';
SPACE : ' ';

DIGIT : '0'..'9';
LETTER : 'a'..'z' | 'A'..'Z';

AGO       : '!ago';
AND       : '!and';

STRING : '"' (~'"')* '"';  



