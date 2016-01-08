tree grammar HoseWalker;

options {
  tokenVocab=Hose;
  ASTLabelType=CommonTree; 
}

@header {
package rapture.generated;
import rapture.dsl.serfun.*;
import rapture.common.Hose;
import rapture.common.SeriesValue;
import java.util.List;
import java.util.ArrayList;
}

@members {
  public HoseProgram program = new HoseProgram();
}

program
  : ^(BODY s=signature statement*) { }
  ;

statement
  : ^(LINK i=Identifier a=arg) { program.addLink($i.text, a); }
  ;

call returns [Hose c]
  : ^(CALL i=Identifier a=argList) { $c = HoseRegistry.call($i.text, a); }
  ;

argList returns [List<HoseArg> args]
  @init {
    $args = new ArrayList<HoseArg>();
  } : ^(ARGLIST (a=arg { args.add(a); })+ ) { }
  ;

signature
  : ^(SIGNATURE name=Identifier in=parmlist out=parmlist) { program.setName($name.text); program.setInParms(in); program.setOutParms(out); }
  ;
  
parmlist returns [List<HoseParm> parms]
  @init {
    $parms = new ArrayList<HoseParm>();
  } : ^(PARMLIST (p=parm { parms.add(p); })+ ) { }
  ;

parm returns [HoseParm p]
  : ^(PARM INT_T i=Identifier)     { $p = new HoseParm(HoseParm.Type.INT, $i.text); }
  | ^(PARM DECIMAL_T i=Identifier) { $p = new HoseParm(HoseParm.Type.DECIMAL, $i.text); }
  | ^(PARM STRING_T i=Identifier)  { $p = new HoseParm(HoseParm.Type.STRING, $i.text); }
  | ^(PARM STREAM_T i=Identifier)  { $p = new HoseParm(HoseParm.Type.STREAM, $i.text); }
  ;
  
baseArg returns [SeriesValue base]
  : l=literal { $base = l; }
  | i=Identifier { $base = program.getLink($i.text); }
  | c=call { $base = c; }
  ;

deref returns [String d]
  : ^(DEREF i=Identifier) { $d = $i.text; }
  ;

derefList returns [List<String> dl]
  @init {
    $dl = new ArrayList<String>();
  } : ^(DEREFLIST (d=deref { $dl.add(d); })+ ) { }
  ;
  
arg returns [HoseArg a]
  : ^(ARG_O b=baseArg o=outkey) { $a = HoseArg.make(b, o, new ArrayList<String>()); }
  | ^(ARG_OD b=baseArg o=outkey? d=derefList) { $a = HoseArg.make(b, o, d); }
  | ^(ARG_D b=baseArg d=derefList) { $a = HoseArg.make(b, null, d); }
  | ^(ARG b=baseArg) { $a = HoseArg.make(b, null, new ArrayList<String>()); }
  ;
  
outkey returns [SeriesValue k]
  : ^(OUTKEY i=Identifier) { $k = new StringSeriesValue($i.text); }
  | ^(OUTINDEX Long) { $k = new LongSeriesValue($Long.text); } 
  ;
  
literal returns [SeriesValue v]
  : String { $v = new StringSeriesValue($String.text); }
  | Long { $v = new LongSeriesValue($Long.text); }
  | Double { $v = new DecimalSeriesValue($Double.text); }
  ;
