// This is the grammer for Reflex

grammar Reflex;

options {
  output=AST;
  backtrack=true;
  //memoize=true;
}


tokens {
  BLOCK;
  METABLOCK;
  RETURN;
  STATEMENTS;
  ASSIGNMENT;
  CONSTASSIGNMENT;
  PLUSASSIGNMENT;
  DOTTEDASSIGNMENT;
  FUNC_CALL;
  EXP;
  EXP_LIST;
  KEYVAL_LIST;
  ID_LIST;
  IF;
  TERNARY;
  UNARY_MIN;
  NEGATE;
  FUNCTION;
  INDEXES;
  RANGEINDEX;
  LIST;
  MAPDEF;
  KEYVAL;
  LOOKUP;
  RANGELOOKUP;
  PUSH;
  PULL;
  SPARSE;
  SPARSELOOKUP;
  METAPULL;
  QUALIFIED_FUNC_CALL;
  KERNEL_CALL;
  FORLIST;
  FORTO;
  PFORLIST;
  PFORTO;
  PORTR;
  PORTF;
  BREAK;
  CONTINUE;
  IMPORT;
  IMPORTAS;
  IMPORTPARAMS;
  EXPORT;
  PATCH;
  MATCH;
  IS;
  OTHERWISE;
  SWITCH;
  CASE;
  DEFAULT;
}

@parser::header{
package reflex;
import java.util.Map;
import java.util.HashMap;
import reflex.util.function.*;
import reflex.structure.*;
import reflex.util.*;

}

@lexer::header{
package reflex;
}

@lexer::members {
	public static Stack<String> alias = new Stack<String>();
    public IReflexScriptHandler dataHandler = new DummyReflexScriptHandler();

    private boolean syntaxOnly = false;
    public void setSyntaxOnly() {
       syntaxOnly = true;
    }

    class SaveStruct {
      SaveStruct(CharStream input){
        this.input = input;
        this.marker = input.mark();
      }
      public CharStream input;
      public int marker;
     }

     Stack<SaveStruct> includes = new Stack<SaveStruct>();


     private void requireFile(String filename, String alias) {
         if (!syntaxOnly) {
             filename = filename.substring(1,filename.length()-1);
             try {
                // save current lexer's state
                SaveStruct ss = new SaveStruct(input);
                includes.push(ss);

                // switch on new input stream
                String scriptContent = dataHandler.getScript(filename);
                String exportContent = exportStart(alias) + scriptContent + exportEnd();
                setCharStream(new ANTLRStringStream(exportContent));
                reset();

             } catch(Exception fnf) {
                throw new ReflexException(-1, "Cannot open include source " + filename);
            }
         }
     }

     private String exportStart(String alias) {
        return "export " + alias + " ";
     }

     private String exportEnd() {
        return " end";
     }

     //This method includes another file into the lexer
     private void includeFile(String name) {
         if (!syntaxOnly) {
            name = name.substring(1,name.length()-1);
            try {
                // save current lexer's state
                SaveStruct ss = new SaveStruct(input);
                includes.push(ss);

                // switch on new input stream
                setCharStream(new ANTLRStringStream(dataHandler.getScript(name)));
                reset();

            } catch(Exception fnf) {
             throw new ReflexException(-1, "Cannot open include source " + name);
            }
         }
     }


    // We should override this method for handling EOF of included file
     public Token nextToken(){
       Token token = super.nextToken();
       if(token.getType() == Token.EOF && !includes.empty()){
        // We've got EOF and have non empty stack.
         SaveStruct ss = includes.pop();
         setCharStream(ss.input);
         input.rewind(ss.marker);
         //this should be used instead of super [like below] to handle exits from nested includes
         //it matters, when the 'include' token is the last in previous stream (using super, lexer 'crashes' returning EOF token)
         token = this.nextToken();
       }

      // Skip first token after switching on another input.
      // You need to use this rather than super as there may be nested include files
       if(((CommonToken)token).getStartIndex() < 0) {
         token = this.nextToken();
       }

       return token;
     }

    @Override
    public void reportError(RecognitionException e) {
        emitErrorMessage(ErrorHandler.getParserExceptionDetails(e));
        super.reportError(e);
    }
    
    @Override
    public void recover(RecognitionException e) {
        super.recover(e);
    }
    
  public boolean wibble(String error, IntStream input, boolean ignorable) throws ReflexRecognitionException {
	ReflexRecognitionException rre = new ReflexRecognitionException(error, input, ignorable);
	if (ignorable) emitErrorMessage(ErrorHandler.getParserExceptionDetails(rre));
	else throw rre;
	return ignorable;
  }
    
}

@lexer::rulecatch {
  catch(RecognitionException e) {
      reportError(e);
      throw e;
  }
}

@parser::rulecatch {
  catch(RecognitionException e) {
      reportError(e);
      throw e;
  }
}

@parser::members {
  public LanguageRegistry languageRegistry = new LanguageRegistry();

  private NamespaceStack namespaceStack = languageRegistry.getNamespaceStack();


  protected void mismatch(IntStream input, int ttype, BitSet follow) throws RecognitionException {
       throw new MismatchedTokenException(ttype, input);
  }

   private Stack<Structure> structureStack = new Stack<Structure>();
   private Stack<StructureType> structureTypeStack = new Stack<StructureType>();

  private void pushStructureMember(String name) {
   if (structureStack.isEmpty()) {
   	   structureStack.push(new Structure());
   }
   if (structureTypeStack.isEmpty()) {
       System.err.println("Empty type stack when defining " + name);
   } else {
   	   structureStack.peek().addMember(name, structureTypeStack.pop());
   	}
  }

  private void defineStructure(String name, int lineNumber) {
   Structure s = structureStack.pop();
   s.setName(name);

   StructureKey key = getStructureKey(name);
    try {
      languageRegistry.registerStructure(key, s);
    }
    catch (ReflexException e) {
      e.setLineNumber(lineNumber);
      throw e;
    }
  }

 	 public String getErrorMessage(RecognitionException e, String[] tokenNames) {
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

		public String getTokenErrorDisplay(Token t) {
			return t.toString();
		}


  public Object recoverFromMismatchedSet(IntStream input, RecognitionException e, BitSet follow) throws RecognitionException {
     throw e;
  }

  private void defineFunction(String id, Object idList, Object block, int lineNumber) {
    // `idList` is possibly null!  Create an empty tree in that case.
    CommonTree idListTree = idList == null ? new CommonTree() : (CommonTree)idList;

    // `block` is never null
    CommonTree blockTree = (CommonTree)block;

    // The function name with the number of parameters after it, is the unique key
    FunctionKey key = getFunctionKey(id, idListTree);
    try {
      languageRegistry.registerFunction(key, new Function(id, idListTree, blockTree, namespaceStack));
    }
    catch (ReflexException e) {
      e.setLineNumber(lineNumber);
      throw e;
    }
  }

  private StructureKey getStructureKey(String structureName) {
    if (namespaceStack.isEmpty()) {
      return StructureFactory.createStructureKey(structureName);
    }
    else {
      return StructureFactory.createStructureKey(namespaceStack.asPrefix(), structureName);
    }
  }

  private FunctionKey getFunctionKey(String functionName, CommonTree idListTree) {
    if (namespaceStack.isEmpty()) {
      return FunctionFactory.createFunctionKey(functionName, idListTree.getChildCount());
    }
    else {
      return FunctionFactory.createFunctionKey(namespaceStack.asPrefix(), functionName, idListTree.getChildCount());
    }
  }

  /*
  * This is used to preserve line numbers. See http://stackoverflow.com/questions/9954882/antlr-preserve-line-number-and-position-in-tree-grammar
  */
  private CommonToken token(String text, int type, int line) {
    CommonToken t = new CommonToken(type, text);
    t.setLine(line);
    return t;
  }

   public MetaScriptInfo scriptInfo = new MetaScriptInfo();

   private void addMetaProperty(String key, String value) {
        scriptInfo.setProperty(key, value);
   }

  private void defineMetaReturn(String retType, String meta) {
        scriptInfo.setReturn(retType, meta);
  }

  private void addMetaParameter(String parameterName, String parameterType, String description) {
        scriptInfo.addParameter(parameterName, parameterType, description);
  }

  @Override
  public void reportError(RecognitionException e) {
      emitErrorMessage(ErrorHandler.getParserExceptionDetails(e));
      super.reportError(e);
  }
  
  public void wibble(String error, IntStream input, Token t) throws ReflexRecognitionException {	
    CommonToken ct = (CommonToken) t;
	int length = ct.getStopIndex() - ct.getStartIndex() +1;
	int start = ct.getCharPositionInLine();
	throw new ReflexRecognitionException(error+" at token "+t.getText()+" "+ErrorHandler.displayError(ct.getInputStream(), ct.getLine(), start, length), input, false);
  }
}

parse
  :  metaBlock? mainBlock
  ;

metaBlock
  : 'meta' Do metaStatement* End -> METABLOCK
  ;

metaStatement
  : 'param'  name=String ',' metaType=('list' | 'map' | 'number' | 'string') ',' desc=String  ';' { addMetaParameter($name.text, $metaType.text, $desc.text); }
  | Return ret=('list' | 'map' | 'number' | 'string') ',' meta=String ';' { defineMetaReturn($ret.text, $meta.text); }
  | 'property' name=String ',' value=String ';' { addMetaProperty($name.text, $value.text); }
  ;

mainBlock
  :  block EOF -> block
  ;

block
  : (((statement | functionDecl | structureDecl )*  (Return expression ';')?))
     -> ^(BLOCK ^(STATEMENTS statement*) ^(RETURN expression?))
  ;


statement  :  assignment ';'   -> assignment
  |  importStatement ';' -> importStatement
  |  port ';' -> port
  |  pull ';' -> pull
  |  metapull ';' -> metapull
  |  push ';' -> push
  |  patchStatement
  |  functionCall ';' -> functionCall
  |  throwStatement ';' -> throwStatement
  |  breakStatement ';' -> breakStatement
  |  continueStatement ';' -> continueStatement
  |  matchStatement
  |  switchStatement
  |  ifStatement
  |  forStatement
  |  pforStatement
  |  whileStatement
  |  guardedStatement
  |  exportStatement
// Unexpected stuff that can throw off the parser.
// Need to catch it and flag it at the source
	|  Unsupported { wibble("Unsupported Operation", input, $Unsupported); }
    |  SColon { wibble("Unexpected character", input, $SColon); } 
  	|  Identifier { wibble("Unexpected identifier", input, $Identifier); } 
  ;
	
Unsupported 
	: '++' 
	| '--'
	| '-='
	;

exportStatement
@after {
  namespaceStack.pop();
}
  : Export Identifier { namespaceStack.push($Identifier.text); } block End
    -> ^(EXPORT[$Identifier] Identifier block)
  ;


assignment
  :  Const Identifier '=' expression
     -> ^(CONSTASSIGNMENT[$Identifier] Identifier expression)
  |   i=(Identifier | DottedIdentifier) indexes? '=' expression
     -> ^(ASSIGNMENT[$i] $i indexes? expression)
//  |   DottedIdentifier '=' expression
//     -> ^(DOTTEDASSIGNMENT[$DottedIdentifier] DottedIdentifier expression)
  |   Identifier '+=' expression
     -> ^(PLUSASSIGNMENT[$Identifier] Identifier expression)
  ;

breakStatement
  : Break -> BREAK
  ;

continueStatement
  : Continue -> CONTINUE
  ;

importStatement
  : Import l=Identifier ('as' r=Identifier)? ('with' '(' p=exprList ')')?-> ^(IMPORT[$Import] $l ^(IMPORTAS $r?) ^(IMPORTPARAMS $p?))
  ;

port
  :  l=expression PortA r=expression  -> ^(PORTF[$PortA] $l $r)
  |  PortA expression                 -> ^(PORTR[$PortA] expression)
  ;

patchStatement
  : expression '<-->' Identifier '{' block '}' -> ^(PATCH[$Identifier] expression Identifier block)
  | expression '<-->' Identifier Do block End -> ^(PATCH[$Identifier] expression Identifier block)
  ;

pull
  : Identifier '<--' expression
     -> ^(PULL[$Identifier] Identifier expression)
  ;

metapull
  : Identifier '<<--' expression
     -> ^(METAPULL[$Identifier] Identifier expression)
  ;

push
  : l=expression '-->' r=expression
     -> ^(PUSH[$l.start] $l $r)
  ;

throwStatement
  :  Throw expression
     -> ^(Throw expression);

functionCall
  :  PackageIdentifier '(' exprList? ')' -> ^(FUNC_CALL[$PackageIdentifier] PackageIdentifier exprList?)
  |  Println '(' expression? ')'  -> ^(FUNC_CALL[$Println] Println expression?)
  |  Print '(' expression ')'     -> ^(FUNC_CALL[$Print] Print expression)
  |  Size '(' expression ')'      -> ^(FUNC_CALL[$Size] Size expression)
  |  Keys '(' expression ')'      -> ^(FUNC_CALL[$Keys] Keys expression)
  |  Sort '(' arg=expression ',' asc=expression ')'      -> ^(FUNC_CALL[$Sort] Sort $arg $asc)
  |  Collate '(' arg=expression ',' locale=expression ')'   -> ^(FUNC_CALL[$Collate] Collate $arg $locale)
  |  Date '(' exprList? ')'       -> ^(FUNC_CALL[$Date] Date exprList?)
  |  Time '(' expression? ')'     -> ^(FUNC_CALL[$Time] Time expression?)
  |  GetLine '(' expression? ')'  -> ^(FUNC_CALL[$GetLine] GetLine expression?)
  |  GetCh '(' expression? ')'    -> ^(FUNC_CALL[$GetCh] GetCh expression?)
  |  Capabilities '(' ')' 		  -> ^(FUNC_CALL[$Capabilities] Capabilities)
  |  HasCapability '(' expression ')' -> ^(FUNC_CALL[$HasCapability] HasCapability expression)
  |  Cast  '(' a=expression ',' b=expression ')'    -> ^(FUNC_CALL[$Cast] Cast $a $b)
  |  Identifier '(' exprList? ')' -> ^(FUNC_CALL[$Identifier] Identifier exprList?)
  |  DottedIdentifier '(' exprList? ')'
                                  -> ^({token("QUALIFIED_FUNC_CALL", QUALIFIED_FUNC_CALL, $DottedIdentifier.getLine())} DottedIdentifier exprList?)
  | func2
  ;

func2
  :  TypeOf '(' expression ')'    -> ^(FUNC_CALL[$TypeOf] TypeOf expression)
  |  Assert '(' exp=expression ')'    -> ^(FUNC_CALL[$Assert] Assert $exp $exp)
  |  Assert '(' msg=expression ',' exp=expression ')'    -> ^(FUNC_CALL[$Assert] Assert $msg $exp)
  |  Replace '(' v=expression ',' s=expression ',' t=expression ')' -> ^(FUNC_CALL[$Replace] Replace $v $s $t)
  |  RPull '(' u=expression ')' -> ^(FUNC_CALL[$RPull] RPull $u)
  |  RPush '(' u=expression ',' v=expression (',' o=expression)?')' -> ^(FUNC_CALL[$RPush] RPush $u $v $o?)
  |  Transpose '(' expression ')' -> ^(FUNC_CALL[$Transpose] Transpose expression)
  |  B64Compress '(' expression ')' -> ^(FUNC_CALL[$B64Compress] B64Compress expression)
  |  B64Decompress '(' expression ')' -> ^(FUNC_CALL[$B64Decompress] B64Decompress expression)
  |  Debug '(' expression ')'     -> ^(FUNC_CALL[$Debug] Debug expression)
  |  Evals '(' expression ')'     -> ^(FUNC_CALL[$Evals] Evals expression)
  |  ReadDir '(' expression ')'   -> ^(FUNC_CALL[$ReadDir] ReadDir expression)
  |  MkDir '(' expression ')'     -> ^(FUNC_CALL[$MkDir] MkDir expression)
  |  IsFile '(' expression ')'    -> ^(FUNC_CALL[$IsFile] IsFile expression)
  |  IsFolder '(' expression ')'  -> ^(FUNC_CALL[$IsFolder] IsFolder expression)
  |  File '(' exprList ')'        -> ^(FUNC_CALL[$File] File exprList)
  |  Delete '(' expression ')'    -> ^(FUNC_CALL[$Delete] Delete expression)
  |  Archive '(' expression ')'   -> ^(FUNC_CALL[$Archive] Archive expression)
  |  Port '(' expression ')'      -> ^(FUNC_CALL[$Port] Port expression)
  |  Suspend '(' expression ')'   -> ^(FUNC_CALL[$Suspend] Suspend expression)
  |  Difference '(' exprList ')'  -> ^(FUNC_CALL[$Difference] Difference exprList)
  |  Remove '(' Identifier ',' k=expression ')' -> ^(FUNC_CALL[$Remove] Remove Identifier $k)
  |  Join '(' exprList ')'        -> ^(FUNC_CALL[$Join] Join exprList)
  |  Unique '(' exprList ')'      -> ^(FUNC_CALL[$Unique] Unique exprList)
  |  Copy '(' s=expression ',' t=expression ')' -> ^(FUNC_CALL[$Copy] Copy $s $t)
  |  Close '(' expression ')'     -> ^(FUNC_CALL[$Close] Close expression)
  |  Timer '(' expression? ')'     -> ^(FUNC_CALL[$Timer] Timer expression?)
  |  Vars '(' ')'                    -> ^(FUNC_CALL[$Vars] Vars)
  |  MergeIf '(' exprList ')'     -> ^(FUNC_CALL[$MergeIf] MergeIf exprList)
  |  Format '(' exprList ')' -> ^(FUNC_CALL[$Format] Format exprList)
  |  Merge '(' exprList ')'       -> ^(FUNC_CALL[$Merge] Merge exprList)
  |  Message '(' a=expression ',' m=expression ')' -> ^(FUNC_CALL[$Message] Message $a $m)
  |  PutCache '(' v=expression ',' n=expression (',' exp=expression)? ')' -> ^(FUNC_CALL[$PutCache] PutCache $v $n $exp?)
  |  GetCache '(' n=expression ')' -> ^(FUNC_CALL[$GetCache] GetCache $n)
  |  Json '(' expression ')'      -> ^(FUNC_CALL[$Json] Json expression)
//   |  NewInstance '(' expression ')'      -> ^(FUNC_CALL[$NewInstance] NewInstance expression)
  |  FromJson '(' expression ')'  -> ^(FUNC_CALL[$FromJson] FromJson expression)
  |  UrlEncode '(' expression ')'  -> ^(FUNC_CALL[$UrlEncode] UrlEncode expression)
  |  UrlDecode '(' expression ')'  -> ^(FUNC_CALL[$UrlDecode] UrlDecode expression)
  |  MD5 '(' expression ')'       -> ^(FUNC_CALL[$MD5] MD5 expression)
  |  MapFn '(' Identifier ',' expression ')' -> ^(FUNC_CALL[$MapFn] MapFn Identifier expression)
  |  FilterFn '(' Identifier ',' expression ')' -> ^(FUNC_CALL[$FilterFn] FilterFn Identifier expression)
  |  Fold '(' Identifier ',' expression ',' expression ')' -> ^(FUNC_CALL[$Fold] Fold Identifier expression expression)
  |  Any '(' Identifier ',' expression ')' -> ^(FUNC_CALL[$Any] Any Identifier expression)
  |  All '(' Identifier ',' expression ')' -> ^(FUNC_CALL[$All] All Identifier expression)
  |  TakeWhile '(' Identifier ',' expression ')' -> ^(FUNC_CALL[$TakeWhile] TakeWhile Identifier expression)
  |  DropWhile '(' Identifier ',' expression ')' -> ^(FUNC_CALL[$DropWhile] DropWhile Identifier expression)
  |  SplitWith '(' Identifier ',' expression ')' -> ^(FUNC_CALL[$SplitWith] SplitWith Identifier expression)
  |  Split '(' str=expression ',' sep=expression ',' quoter=expression ')' -> ^(FUNC_CALL[$Split] Split $str $sep $quoter)
  |  Uuid '(' ')'                 -> ^(FUNC_CALL[$Uuid] Uuid)
  |  AsyncCall '(' s=expression (',' p=expression)? ')'
                                  -> ^(FUNC_CALL[$AsyncCall] AsyncCall $s $p?)
  |  AsyncCallScript '(' r=expression ',' s=expression (',' p=expression)? ')'
                                  -> ^(FUNC_CALL[$AsyncCallScript] AsyncCallScript $r $s $p?)
  |  AsyncStatus '(' expression ')'        -> ^(FUNC_CALL[$AsyncStatus] AsyncStatus expression)
  |  SuspendWait '(' exprList ')'  -> ^(FUNC_CALL[$SuspendWait] SuspendWait exprList)
  |  Wait '(' d=expression (',' in=expression ',' re=expression)? ')'
                                  -> ^(FUNC_CALL[$Wait] Wait $d $in? $re?)
  |  Chain '(' s=expression (',' p=expression)? ')'
                                  -> ^(FUNC_CALL[$Chain] Chain $s $p?)
  |  Signal '(' d=expression ',' v=expression ')'
                                  -> ^(FUNC_CALL[$Signal] Signal $d $v)
  |  Sleep  '(' expression ')'    -> ^(FUNC_CALL[$Sleep] Sleep expression)
  |  Matches '(' s=expression ',' r=expression ')' -> ^(FUNC_CALL[$Matches] Matches $s $r)
  |  Rand   '(' expression ')'    -> ^(FUNC_CALL[$Rand] Rand expression)
  |  Spawn '(' p=expression (',' ex=expression ',' f=expression)? ')'
                                  -> ^(FUNC_CALL[$Spawn] Spawn $p $ex? $f?)
  |  Defined '(' Identifier ')'   -> ^(FUNC_CALL[$Defined] Defined Identifier)
  |  Round '(' v=expression (',' dp=expression)? ')'
                                  -> ^(FUNC_CALL[$Round] Round $v $dp?)
  |  Lib   '(' expression ')'     -> ^(FUNC_CALL[$Lib] Lib expression)
  |  Call  '(' a=expression ',' b=expression ',' c=expression ')'
                                  -> ^(FUNC_CALL[$Call] Call $a $b $c)
  |  New   '(' a=expression ')'
                                  -> ^(FUNC_CALL[$New] New $a)
  |  GenSchema '(' a=expression ')'
                                  -> ^(FUNC_CALL[$GenSchema] GenSchema $a)
  |  GenStruct '(' Identifier ',' b=expression ')'
                                  -> ^(FUNC_CALL[$GenStruct] GenStruct Identifier $b)
  |  Template '(' t=expression ',' p=expression ')'
                                  -> ^(FUNC_CALL[$Template] Template $t $p)
  |  KernelIdentifier '(' exprList? ')'
                                  -> ^({token("KERNEL_CALL", KERNEL_CALL, $KernelIdentifier.getLine())} KernelIdentifier exprList?)
  ;

// MATCH allows expressions as case values

matchStatement
  :  Match expression (As Identifier)? Do actions* otherwise? End -> MATCH Identifier? expression actions* otherwise?
  ;
  
actions
  : comparator+ Do block End	-> comparator+ block
  ;

otherwise
  : Otherwise Do block End	-> OTHERWISE block
  ;

comparator 
  : Is (Equals | NEquals | GTEquals | LTEquals | GT | LT) expression
  | Is Assign { wibble("Assignment found where comparator expected", input, $Is); } expression 
  | Is (Or | And | Excl | Add | Subtract | Multiply | Divide | Modulus) { wibble("Comparator expected", input, $Is); } expression 
  ;

// SWITCH requires constants as case values

switchStatement
  :  Switch expression Do caseStatement+ End -> SWITCH expression caseStatement+
  ;
  
caseStatement 
  : variant+ Do block End -> variant+ block
  ;
  
variant
  :  Case Integer -> Integer
  |  Case Number -> Number
  |  Case Long -> Long
  |  Case Bool -> Bool  
  |  Case String -> String
  |  Default
  |  Case QuotedString { wibble("Quoted String found where constant expected. Use single quotes.", input, $QuotedString);}
  |  expression {wibble("Expression found where constant expected.", input, $expression.start);}
  ;
  
ifStatement
  :  ifStat elseIfStat* elseStat? End? -> ^(IF[$ifStat.start] ifStat elseIfStat* elseStat?)
  ;

ifStat
  :  If expression Do block -> ^(EXP[$If] expression block)
  |  If expression '{' block '}' ->  ^(EXP[$If] expression block)
  ;

elseIfStat
  :  Else If expression Do block -> ^(EXP[$If] expression block)
  |  Else If expression '{' block '}' -> ^(EXP[$If] expression block)
  ;

elseStat
  :  Else Do block -> ^(EXP[$Else] block)
  |  Else '{' block '}' -> ^(EXP[$Else] block)
  ;

functionDecl
  :  Def Identifier '(' idList? ')' '{'? block (End | '}')
    { defineFunction($Identifier.text, $idList.tree, $block.tree, $Identifier.getLine()); }
  ;

structureDecl
  : Structure Identifier ('{' | Do) structureMemberList ('}' | End)
     {
        defineStructure($Identifier.text, $Identifier.getLine());
     }
  ;

structureMemberList
  :  structureMember+
  ;

structureMember
  :  Identifier structureType ';'
    {
       pushStructureMember($Identifier.text);
    }
  ;

structureType :
   ( objectStructureType | simpleStructureType | arrayStructureType )
;

objectStructureType :
   Structure {
		structureStack.push(new Structure());
   }
   ('{' | Do) structureMemberList ('}' | End) {
        Structure s = structureStack.pop();
        structureTypeStack.push(new InnerStructureType(s));
   }
;

arrayStructureType :
   'array' 'of' simpleStructureType { structureTypeStack.push(new ArrayStructureType(BasicStructureTypeFactory.createStructureType($simpleStructureType.text))); }
;

simpleStructureType :
   ( 'integer' | 'number' | 'string' )
  { structureTypeStack.push(BasicStructureTypeFactory.createStructureType($simpleStructureType.text)); };

forStatement
  :  For Identifier '=' expression To expression ((Do block End) | ( '{' block '}'))
     -> ^(FORTO[$Identifier] Identifier expression expression block)
  | For Identifier In expression Do block End
     -> ^(FORLIST[$Identifier] Identifier expression block)
  ;

pforStatement
  :  PFor Identifier '=' expression To expression ((Do block End) | ( '{' block '}'))
     -> ^(PFORTO[$Identifier] Identifier expression expression block)
  | PFor Identifier In expression Do block End
     -> ^(PFORLIST[$Identifier] Identifier expression block)
  ;

whileStatement
  :  While expression ((Do block End) | ('{' block '}')) -> ^(While expression block)
  ;

guardedStatement
  :  Try Do g=block End Catch Identifier Do c=block End -> ^(Try $g Identifier $c)
  |  Try '{' g=block '}' Catch Identifier '{' c=block '}' -> ^(Try $g Identifier $c)
  ;

idList
  :  Identifier (',' Identifier)* -> ^(ID_LIST Identifier+)
  ;

exprList
  :  expression (',' expression)* -> ^(EXP_LIST expression+)
  ;

expression
@init{
  ReflexLexer.alias.push("Expression");
}
@after {
  ReflexLexer.alias.pop();
}
  :  condExpr
  ;

condExpr
  :  (orExpr -> orExpr)
     (
       '?' a=expression ':' b=expression -> ^(TERNARY[$a.start] orExpr $a $b)
     | In expression                     -> ^(In orExpr expression)
     )?
  ;

orExpr
  :  andExpr ('||'^ andExpr)*
  ;

andExpr
  :  equExpr ('&&'^ equExpr)*
  ;

equExpr
  :  relExpr (('==' | '!=')^ relExpr)*
  ;

relExpr
  :  addExpr (('>=' | '<=' | '>' | '<')^ addExpr)*
  ;

addExpr
  :  mulExpr (('+' | '-')^ mulExpr)*
  ;

mulExpr
  :  powExpr (('*' | '/' | '%')^ powExpr)*
  ;

powExpr
  :  unaryExpr ('^'^ unaryExpr)*
  ;

unaryExpr
  :  '-' atom -> ^(UNARY_MIN[$atom.start] atom)
  |  '!' atom -> ^(NEGATE[$atom.start] atom)
  |  atom
  ;

sparsesep:
   '-';

sparsematrix
  :  '[' sparsesep+ ']' -> ^(SPARSE sparsesep+)
  ;

atom
  :  Integer
  |  Long
  |  Number
  |  Bool
  |  Null
  |  sparsematrix
  |  lookup
  ;

list
  :  '[' exprList? ']' -> ^(LIST exprList?)
  ;

mapdef
  :  '{' keyValList? '}' -> ^(MAPDEF keyValList?)
  ;

keyValList
  :  keyVal (',' keyVal)* -> ^(KEYVAL_LIST keyVal+)
  ;

keyVal
  :  k=expression ':' v=expression -> ^(KEYVAL[$k.start] $k $v)
  ;

lookup
  :  functionCall indexes?       -> ^(LOOKUP[$functionCall.start] functionCall indexes?)
  |  PropertyPlaceholder         -> ^(LOOKUP[$PropertyPlaceholder] PropertyPlaceholder)
  |  Identifier rangeindex       -> ^(RANGELOOKUP[$Identifier] Identifier rangeindex)
  |  DottedIdentifier rangeindex -> ^(RANGELOOKUP[$DottedIdentifier] DottedIdentifier rangeindex)
  |  list indexes?               -> ^(LOOKUP[$list.start] list indexes?)
  |  mapdef indexes?             -> ^(LOOKUP[$mapdef.start] mapdef indexes?)
  |  DottedIdentifier indexes?   -> ^(LOOKUP[$DottedIdentifier] DottedIdentifier indexes?)
  |  Identifier indexes?         -> ^(LOOKUP[$Identifier] Identifier indexes?)
  |  String indexes?             -> ^(LOOKUP[$String] String indexes?)
  |  QuotedString indexes?       -> ^(LOOKUP[$QuotedString] QuotedString indexes?)
  |  '(' expression ')' indexes? -> ^(LOOKUP[$expression.start] expression indexes?)
  ;

indexes
  :  ('[' exprList ']')+ -> ^(INDEXES exprList+)
  ;

rangeindex
  :  '[' from=expression? '..' to=expression? ']' -> ^(RANGEINDEX $from $to)
  ;

// lexer rule
 INCLUDE
  : 'include' (Space)? (f=String | g=QuotedString) ';' {
       String name = f==null ? g.getText() : f.getText();
       includeFile(name);
  }
  ;

// lexer rule
REQUIRE
  : 'require' (Space)+ (f=String | g=QuotedString) (Space)+ 'as' (Space)+ alias=Identifier ';' {
      requireFile(f == null ? g.getText() : f.getText(), alias.getText());
  }
  ;

Structure : 'struct';
Println  : 'println';
Capabilities : 'capabilities';
HasCapability : 'hascapability';
Print    : 'print';
GetLine  : 'getline';
GetCh    : 'getch';
Assert   : 'assert';
TypeOf   : 'typeof';
Debug	 : 'debug';
Keys     : 'keys';
Sort     : 'sort';
Collate  : 'collate';
Size     : 'size';
Date     : 'date';
Time     : 'time';
ReadDir  : 'readdir';
MkDir    : 'mkdir';
IsFile   : 'isFile';
IsFolder : 'isFolder';
File     : 'file';
Port     : 'port';
Close    : 'close';
Copy     : 'copy';
Join     : 'join';
Replace  : 'replace';
Remove   : 'remove';
Json     : 'json';
// NewInstance : 'newinstance';
Delete   : 'delete';
FromJson : 'fromjson';
UrlEncode : 'urlencode';
UrlDecode : 'urldecode';
Uuid     : 'uuid';
Wait     : 'wait';
Signal	 : 'signal';
Const    : 'const';
Chain    : 'chain';
Sleep    : 'sleep';
Rand     : 'rand';
Spawn    : 'spawn';
Defined  : 'defined';
Difference : 'difference';
Unique     : 'unique';
Round    : 'round';
Lib      : 'lib';
Call     : 'call';
Cast     : 'cast';
Archive  : 'archive';
MD5      : 'md5';
Break    : 'break';
Continue : 'continue';
Import   : 'import';
Suspend  : 'suspend';
Message  : 'message';
Format   : 'format';
Use      : 'use';
MapFn    : 'map';
FilterFn : 'filter';
Fold     : 'fold';
Any      : 'any';
All      : 'all';
TakeWhile: 'takewhile';
DropWhile: 'dropwhile';
SplitWith: 'splitwith';
Timer    : 'timer';
RPull    : 'rpull';
RPush    : 'rpush';
Transpose : 'transpose';
Evals    : 'evals';
Vars     : 'vars';
Matches  : 'matches';


Split    : 'split';

Def      : 'def';
Match    : 'match';
As       : 'as';
Is       : 'is';
Otherwise: 'otherwise';
Switch   : 'switch';
Case     : 'case';
Default  : 'default';
If       : 'if';
Else     : 'else';
Return   : 'return';
For      : 'for';
PFor     : 'pfor';
While    : 'while';
To       : 'to';
OBrace   : '{';
CBrace   : '}';
Do       : 'do';

// Without this a semicolon after end causes really unhelpful error messages
End      : 'end'
         | 'end' {wibble("Unexpected semicolon", input, true)}? SColon
         ;

In       : 'in';
Null     : 'null';
New      : 'new';
GenSchema : 'genschema';
GenStruct : 'genstruct';
Template : 'template';
Try      : 'try';
Catch    : 'catch';
Throw    : 'throw';
Merge    : 'merge';
MergeIf  : 'mergeif';
AsyncCall : '@call';
AsyncCallScript : '@script';
AsyncOneShot : '@oneshot';
AsyncStatus : '@status';
SuspendWait : '@wait';
PutCache : 'putcache';
GetCache : 'getcache';
Export   : 'export';
B64Compress : 'bcompress';
B64Decompress : 'bdecompress';

Or       : '||';
And      : '&&';
Equals   : '==';
NEquals  : '!=';
GTEquals : '>=';
LTEquals : '<=';
Pow      : '^';
Excl     : '!';
PortA    : '>>';
GT       : '>';
LT       : '<';
Add      : '+';
Subtract : '-';
Multiply : '*';
Divide   : '/';
Modulus  : '%';

OBracket : '[';
CBracket : ']';
OParen   : '(';
CParen   : ')';
SColon   : ';';
Assign   : '=';
Comma    : ',';
QMark    : '?';
Colon    : ':';
Patch    : '<-->';
PullVal  : '<--';
PushVal  : '-->';

Bool
  :  'true'
  |  'false'
  ;

Integer 
@after {
  String tx = getText();
  if (tx.endsWith("I")) setText(tx.substring(0, tx.length()-1));
}  
  : Int
  | Int 'I' 
  ;

Long
@after {
  setText(getText().substring(0,getText().length()-1));
}
  :  Int 'L'
  ;

Number
  :  Int '.' Digit (Digit)*
  ;

PackageIdentifier
@after {
  setText(getText().substring(1,getText().length()));
}
  : '$' Identifier '.' Identifier
  ;

PropertyPlaceholder
@after {
  setText(getText().substring(2,getText().length()-1));
}
  : '${' Identifier '}'
  ;

Identifier
  :  ('a'..'z' | 'A'..'Z' | '_') ('a'..'z' | 'A'..'Z' | '_' | Digit)*
  ;

KernelIdentifier
  : '#' Identifier ('.' Identifier)+
  ;

DottedIdentifier
  : Identifier ('.' Identifier)+
  ;



QuotedString
@init{
  StringBuilder lBuf = new StringBuilder();
  alias.push("Quoted String");
}
@after {
  alias.pop();
}
    :
           '"'
           ( escaped=ESC {lBuf.append(getText());} |
             normal=~('"'|'\\'|'\n'|'\r')     {lBuf.appendCodePoint(normal);} )*
           '"'
           {setText(lBuf.toString());}
    ;

fragment
ESC
    :   '\\'
        (   'n'    {setText("\n");}
        |   'r'    {setText("\r");}
        |   't'    {setText("\t");}
        |   'b'    {setText("\b");}
        |   'f'    {setText("\f");}
        |   '"'    {setText("\"");}
        |   '\''   {setText("\'");}
        |   '/'    {setText("/");}
        |   '\\'   {setText("\\");}
        )
    ;


String
@init{StringBuilder lBuf = new StringBuilder();}
    :
           '\''
           ( escaped=ESC {lBuf.append(getText());} |
             normal=~('\''|'\\'|'\n'|'\r')     {lBuf.appendCodePoint(normal);} )*
           '\''
           {setText(lBuf.toString());}
    ;

Comment
  :  '//' ~('\r' | '\n')* {skip();}
  |  '/*' .* '*/'         {skip();}
  ;

Space
  :  (' ' | '\t' | '\r' | '\n' | '\u000C') {skip();}
  ;

fragment Int
  :  '1'..'9' Digit*
  |  '0'
  ;

fragment Digit
  :  '0'..'9'
  ;