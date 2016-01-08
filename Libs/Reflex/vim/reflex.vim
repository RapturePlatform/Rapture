" Vim syntax file
" Language: Reflex
" Maintainer: Alan Moore
" Latest Revision: 21 August 2012

if exists("b:current_syntax")
    finish
endif

syn keyword reflexLangKeywords for do in end if while def
syn keyword reflexFuncKeywords call lib sleep

" Integer with - + or nothing in front
syn match rfxNumber '\d\+'
syn match rfxNumber '[-+]\d\+'

" Floating point number with decimal no E or e (+,-)
syn match rfxNumber '\d\+\.\d*'
syn match rfxNumber '[-+]\d\+\.\d*'

" " Floating point like number with E and no decimal point (+,-)
syn match rfxNumber '[-+]\=\d[[:digit:]]*[eE][\-+]\=\d\+'
syn match rfxlNumber '\d[[:digit:]]*[eE][\-+]\=\d\+'

" Floating point like number with E and decimal point (+,-)
syn match rfxNumber '[-+]\=\d[[:digit:]]*\.\d*[eE][\-+]\=\d\+'
syn match rdxNumber '\d[[:digit:]]*\.\d*[eE][\-+]\=\d\+'

syn match rfxComment "////.*$"

syn region rfxString start='"' end='"'

