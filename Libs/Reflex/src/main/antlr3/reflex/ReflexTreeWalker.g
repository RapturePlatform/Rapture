tree grammar ReflexTreeWalker;

options {
  tokenVocab=Reflex;
  ASTLabelType=CommonTree;
}

@header {
package reflex;
import java.math.*;
import java.io.PrintStream;
import reflex.function.*;
import reflex.node.*;
import reflex.node.io.*;
import reflex.node.functional.*;
import reflex.importer.*;
import reflex.debug.*;
import reflex.util.function.LanguageRegistry;
import reflex.util.*;
import reflex.value.ReflexValue;
}

@members {
  public LanguageRegistry languageRegistry = null;
  public Scope currentScope = null;
  private IReflexHandler handler = new DummyReflexHandler();
  public ImportHandler importHandler = new ImportHandler();
  private NamespaceStack namespaceStack;

  public ReflexTreeWalker(CommonTreeNodeStream nodes, LanguageRegistry languageRegistry) {
    this(nodes, null, languageRegistry);
  }

  public ReflexTreeWalker(CommonTreeNodeStream nds, Scope sc, LanguageRegistry languageRegistry) {
    this(nds, sc, languageRegistry, languageRegistry.getNamespaceStack());
  }

  public ReflexTreeWalker(CommonTreeNodeStream nds, Scope sc, LanguageRegistry languageRegistry, NamespaceStack namespaceStack) {
    super(nds);
    if (sc == null) {
      currentScope = Scope.getInitialScope();
    } else {
      currentScope = sc;
    }
    this.languageRegistry = languageRegistry;
    importHandler.setReflexHandler(handler);
    this.namespaceStack = namespaceStack;
  }

  public int countSyntaxErrors() {
    return state.syntaxErrors;
  }

  public void setReflexHandler(IReflexHandler handler) {
  	this.handler = handler;
  	importHandler.setReflexHandler(handler);
  }

  public IReflexHandler getReflexHandler() {
  	return handler;
  }

  public void setImportHandler(ImportHandler importHandler) {
     this.importHandler = importHandler;
  }

  public ImportHandler getImportHandler() {
     return importHandler;
  }

  @Override
  public void reportError(RecognitionException e) {
      super.reportError(e);
  }

}

walk returns [ReflexNode node]
  :  metaBlock? block {node = $block.node;}
  ;


metaBlock
  : METABLOCK
  ;

block returns [ReflexNode node]
@init {
  CommonTree ahead = (CommonTree) input.LT(1);
  int line = ahead.getToken().getLine();
  Scope scope = new Scope(currentScope);
  currentScope = scope;
  BlockNode bn = new BlockNode(line, handler, currentScope);
  node = bn;
}
@after {
  currentScope = currentScope.parent();
}
  :  ^(BLOCK
        ^(STATEMENTS (statement  { bn.addStatement($statement.node); })*)
        ^(RETURN     (expression { bn.addReturn($expression.node);   })?)
      )
  ;

exportStatement returns [ReflexNode node]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
@after {
  namespaceStack.pop();
}
  : ^(EXPORT i=Identifier { namespaceStack.push($i.text);} b=block) {  node = new ExportStatementNode(line, handler, currentScope, $b.node ); }
  ;


statement returns [ReflexNode node]
  :  assignment { node = $assignment.node; }
  |  pull { node = $pull.node; }
  |  metapull { node = $metapull.node; }
  |  push { node = $push.node; }
  |  patchStatement { node = $patchStatement.node; }
  |  port { node = $port.node; }
  |  importStatement { node = $importStatement.node; }
  |  exportStatement { node = $exportStatement.node; }
  |  breakStatement { node = $breakStatement.node; }
  |  continueStatement { node = $continueStatement.node; }
  |  functionCall { node = $functionCall.node; }
  |  throwStatement { node = $throwStatement.node; }
  |  matchStatement { node = $matchStatement.node; }
  |  switchStatement { node = $switchStatement.node; }
  |  ifStatement { node = $ifStatement.node; }
  |  forStatement { node = $forStatement.node;}
  |  pforStatement { node = $pforStatement.node; }
  |  whileStatement { node = $whileStatement.node; }
  |  guardedStatement { node = $guardedStatement.node; }
  ;
  
variant returns [ReflexNode node]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
  :  Integer { node = AtomNode.getIntegerAtom(line, handler, currentScope, $Integer.text); }
  |  Number { node = new AtomNode(line, handler, currentScope, new BigDecimal($Number.text, MathContext.DECIMAL128)); }
  |  String { node = AtomNode.getStringAtom(line, handler, currentScope, $String.text); }
  |  Long { node = new AtomNode(line, handler, currentScope, java.lang.Long.parseLong($Long.text)); }
  |  Bool { node = new AtomNode(line, handler, currentScope, Boolean.parseBoolean($Bool.text)); }
  |  Default { node = null; }
  ;

switchStatement returns [ReflexNode node]
@init  {
  CommonTree ahead = (CommonTree) input.LT(1);
  int line = ahead.getToken().getLine();
  SwitchNode switchNode = new SwitchNode(line, handler, currentScope);
  node = switchNode;
}
  :  SWITCH expression { switchNode.setSwitchValue($expression.node); } caseStatement[switchNode]+
  ;
  
caseStatement [SwitchNode switchNode]
@init  {
  List<ReflexNode> caseNodes = new ArrayList<>();
}
  : (v=variant { caseNodes.add(v); })+ block { for (ReflexNode caseNode : caseNodes) switchNode.addCase(caseNode, $block.node); }
  ;
  
  
matchStatement returns [ReflexNode node]
@init  {
  CommonTree ahead = (CommonTree) input.LT(1);
  int line = ahead.getToken().getLine();
  MatchNode matchNode = new MatchNode(line, handler, currentScope);
  node = matchNode;
  String matchName = "__mAtCh__";
}
  : MATCH ident=Identifier? { if (ident != null) matchName=ident.getText(); } 
  		expression { 
  			matchNode.setMatchValue(new AssignmentNode(line, handler, currentScope, matchName, null, $expression.node));
  			IdentifierNode idNode = new IdentifierNode(line, handler, currentScope, matchName, namespaceStack.asPrefix());
  		}
  		actions[idNode, matchNode]* 
  		otherwise[idNode, matchNode]?
  ;

actions[IdentifierNode idNode, MatchNode matchNode]
@init  {
  List<ReflexNode> compNodes = new ArrayList<>();
}
  : (comp=comparator[idNode] { compNodes.add(comp); })+ block { for (ReflexNode compNode : compNodes) matchNode.addCase(compNode, $block.node); }
  ;
  
comparator [IdentifierNode idNode] returns [ReflexNode node]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
  :  Is Equals rhs=expression { node = new EqualsNode(line, handler, currentScope, $idNode, $rhs.node); }
  |  Is NEquals rhs=expression { node = new NotEqualsNode(line,handler, currentScope, $idNode, $rhs.node); }
  |  Is GTEquals rhs=expression { node = new GTEqualsNode(line, handler, currentScope, $idNode, $rhs.node); }
  |  Is LTEquals rhs=expression { node = new LTEqualsNode(line, handler, currentScope, $idNode, $rhs.node); }
  |  Is GT rhs=expression { node = new GTNode(line, handler, currentScope, $idNode, $rhs.node); }
  |  Is LT rhs=expression { node = new LTNode(line, handler, currentScope, $idNode, $rhs.node); }
  ;

otherwise[ReflexNode exp, MatchNode matchNode]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
  : OTHERWISE block { matchNode.addCase(new AtomNode(line, handler, currentScope, new ReflexValue(line, Boolean.TRUE)), $block.node); }
  ;

assignment returns [ReflexNode node]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
  :  ^(CONSTASSIGNMENT i=Identifier e=expression) { node = new ConstAssignmentNode(line, handler, currentScope, $i.text, $e.node,
                                                                                    namespaceStack.asPrefix()); }
  | ^(ASSIGNMENT i=(Identifier | DottedIdentifier) x=indexes? e=expression)
     { node = new AssignmentNode(line, handler, currentScope, $i.text, $x.e, $e.node); }
  | ^(PLUSASSIGNMENT i=Identifier e=expression)
     { node = new PlusAssignmentNode(line, handler, currentScope, $i.text, $e.node); }
  | ^(MINUSASSIGNMENT i=Identifier e=expression)
     { node = new PlusAssignmentNode(line, handler, currentScope, $i.text, new UnaryMinusNode(line, handler, currentScope, $e.node)); }
  ;

breakStatement returns [ReflexNode node]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
  : BREAK {  node = new BreakNode(line, handler, currentScope); }
  ;

continueStatement returns [ReflexNode node]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
  : CONTINUE {  node = new ContinueNode(line, handler, currentScope); }
  ;

importStatement returns [ReflexNode node]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
  : ^(IMPORT l=Identifier ^(IMPORTAS alias=Identifier?) ^(IMPORTPARAMS params=exprList?) ^(IMPORTFROM jarUris=jarUriList?)) { node = new ImportNode(line, handler, currentScope, importHandler, $l.text, $alias.text, $exprList.e, $jarUris.jarUris); }
  ;

port returns [ReflexNode node]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
   : ^(PORTF l=expression r=expression)
      { node = new PortANode(line, handler, currentScope, $l.node, $r.node); }
   | ^(PORTR expression)
      { node = new PortANode(line, handler, currentScope, null, $expression.node); }
   ;

patchStatement returns [ReflexNode node]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
	: ^(PATCH l=expression i=Identifier b=block)
	   { node = new PatchNode(line, handler, currentScope, $l.node, $i.text, $b.node); }
	;

pull returns [ReflexNode node]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
  : ^(PULL i=Identifier e=expression)
     { node = new PullNode(line, handler, currentScope, $i.text, $e.node); }
  ;

metapull returns [ReflexNode node]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
  : ^(METAPULL i=Identifier e=expression)
     { node = new MetaPullNode(line, handler, currentScope, $i.text, $e.node); }
  ;


push returns [ReflexNode node]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
  : ^(PUSH l=expression r=expression)
     { node = new PushNode(line, handler, currentScope, $l.node, $r.node); }
  ;

throwStatement returns [ReflexNode node]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
  : ^(Throw e=expression)
     { node = new ThrowNode(line, handler, currentScope, $e.node); }
  ;


functionCall returns [ReflexNode node]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
  :  ^(FUNC_CALL Identifier exprList?) { node = new FunctionCallNode(line, handler, currentScope, $Identifier.text, $exprList.e, languageRegistry, importHandler, namespaceStack.asPrefix()); }
  |  ^(FUNC_CALL PackageIdentifier exprList?) { node = new PackageCallNode(line, handler, currentScope, importHandler, $PackageIdentifier.text, $exprList.e); }
  |  ^(FUNC_CALL Println expression?) { node = new PrintlnNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL GetLine expression?) { node = new GetLineNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL GetCh expression?) { node = new GetChNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL Capabilities ) { node = new CapabilitiesNode(line, handler, currentScope); }
  |  ^(FUNC_CALL HasCapability expression ) { node = new HasCapabilityNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL Print expression) { node = new PrintNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL MapFn Identifier expression) { node = new MapFnNode(line, handler, currentScope, $Identifier.text, $expression.node, languageRegistry, importHandler); }
  |  ^(FUNC_CALL FilterFn Identifier expression) { node = new FilterFnNode(line, handler, currentScope, $Identifier.text, $expression.node, languageRegistry, importHandler); }
  |  ^(FUNC_CALL Fold Identifier a=expression b=expression) { node = new FoldNode(line, handler, currentScope, $Identifier.text, $a.node, $b.node, languageRegistry, importHandler); }
  |  ^(FUNC_CALL Any Identifier expression) { node = new AnyNode(line, handler, currentScope, $Identifier.text, $expression.node, languageRegistry, importHandler); }
  |  ^(FUNC_CALL All Identifier expression) { node = new AllNode(line, handler, currentScope, $Identifier.text, $expression.node, languageRegistry, importHandler); }
  |  ^(FUNC_CALL TakeWhile Identifier expression) { node = new TakeWhileNode(line, handler, currentScope, $Identifier.text, $expression.node, languageRegistry, importHandler); }
  |  ^(FUNC_CALL DropWhile Identifier expression) { node = new DropWhileNode(line, handler, currentScope, $Identifier.text, $expression.node, languageRegistry, importHandler); }
  |  ^(FUNC_CALL SplitWith Identifier expression) { node = new SplitWithNode(line, handler, currentScope, $Identifier.text, $expression.node, languageRegistry, importHandler); }
  |  ^(FUNC_CALL Split str=expression sep=expression quoter=expression) { node = new SplitNode(line, handler, currentScope, $str.node, $sep.node, $quoter.node, languageRegistry, importHandler); }
  |  ^(FUNC_CALL TypeOf expression) { node = new TypeOfNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL Assert msg=expression exp=expression) { node = new AssertNode(line, handler, currentScope, $msg.node, $exp.node); }
  |  ^(FUNC_CALL Size expression) { node = new SizeNode(line, handler,currentScope,  $expression.node); }
  |  ^(FUNC_CALL RPull u=expression) { node = new RapturePullNode(line, handler, currentScope, $u.node, null); }
  |  ^(FUNC_CALL RPush u=expression v=expression o=expression?) { node = new RapturePushNode(line, handler, currentScope, $u.node, $v.node, $o.node); }
  |  ^(FUNC_CALL Transpose expression) { node = new TransposeNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL Keys expression) { node = new KeysNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL Sort arg=expression asc=expression) { node = new SortNode(line, handler, currentScope, $arg.node, $asc.node); }
  |  ^(FUNC_CALL Collate arg=expression locale=expression) { node = new CollateNode(line, handler, currentScope, $arg.node, $locale.node); }
  |  ^(FUNC_CALL B64Compress expression) { node = new B64Compress(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL B64Decompress expression) { node = new B64Decompress(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL Debug expression) { node = new DebugNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL Date exprList? ) { node = new DateNode(line, handler, currentScope, $exprList.e); }
  |  ^(FUNC_CALL Time expression? ) { node = new TimeNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL Evals expression ) { node = new QuotedStringNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL Vars) { node = new VarsNode(line, handler, currentScope); }
  |  ^(FUNC_CALL ReadDir expression) { node = new ReadDirNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL MkDir expression) { node = new MkDirNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL IsFile expression) { node = new IsFileNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL IsFolder expression) { node = new IsFolderNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL File exprList) { node = new FileNode(line, handler, currentScope, $exprList.e); }
  |  ^(FUNC_CALL Copy s=expression t=expression) { node = new CopyNode(line, handler, currentScope, $s.node, $t.node); }
  |  ^(FUNC_CALL Archive expression) { node = new ArchiveNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL Delete expression) { node = new DeleteNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL Port expression) { node = new PortNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL Suspend expression) { node = new SuspendNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL Close expression) { node = new CloseNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL Timer expression?) { node = new TimerNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL Merge exprList) { node = new MergeNode(line, handler, currentScope, $exprList.e); }
  |  ^(FUNC_CALL Format exprList) { node = new FormatNode(line, handler, currentScope, $exprList.e); }
  |  ^(FUNC_CALL DateFormat date=expression format=expression timezone=expression?) { node = new DateFormatNode(line, handler, currentScope, $date.node, $format.node, $timezone.node); }
  |  ^(FUNC_CALL MergeIf exprList) { node = new MergeIfNode(line, handler, currentScope, $exprList.e); }
  |  ^(FUNC_CALL Replace v=expression s=expression t=expression) { node = new ReplaceNode(line, handler, currentScope, $v.node, $s.node, $t.node); }
  |  ^(FUNC_CALL Message a=expression m=expression) { node = new MessageNode(line, handler, currentScope, $a.node, $m.node); }
  |  ^(FUNC_CALL PutCache v=expression n=expression exp=expression?) { node = new PutCacheNode(line, handler, currentScope, $v.node, $n.node, $exp.node); }
  |  ^(FUNC_CALL GetCache n=expression) { node = new GetCacheNode(line, handler, currentScope, $n.node); }
  |  ^(FUNC_CALL Difference exprList) { node = new DifferenceNode(line, handler, currentScope, $exprList.e); }
  |  ^(FUNC_CALL Join exprList) { node = new JoinNode(line, handler, currentScope, $exprList.e); }
  |  ^(FUNC_CALL Unique exprList) { node = new UniqueNode(line, handler, currentScope, $exprList.e); }
  |  ^(FUNC_CALL Json expression) { node = new JsonNode(line, handler, currentScope, $expression.node); }
//  |  ^(FUNC_CALL NewInstance expression) { node = new JavaClassNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL MD5  expression) { node = new MD5Node(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL FromJson expression) { node = new FromJsonNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL UrlEncode expression) { node = new UrlEncodeNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL UrlDecode expression) { node = new UrlDecodeNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL Uuid) { node = new UuidNode(line, handler, currentScope); }
  |  ^(FUNC_CALL Remove Identifier k=expression) { node = new RemoveNode(line, handler, currentScope, $Identifier.text, $k.node); }
  |  ^(FUNC_CALL Insert Identifier position=expression newvalue=expression) { node = new InsertNode(line, handler, currentScope, $Identifier.text, $position.node, $newvalue.node); }
  |  ^(FUNC_CALL AsyncCall s=expression p=expression?) { node = new AsyncCallNode(line, handler, currentScope, $s.node, $p.node); }
  |  ^(FUNC_CALL AsyncCallScript r=expression s=expression p=expression?) { node = new AsyncCallScriptNode(line, handler, currentScope, $r.node, $s.node, $p.node); }
  |  ^(FUNC_CALL AsyncStatus expression) { node = new AsyncStatusNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL SuspendWait exprList) { node = new SuspendWaitNode(line, handler, currentScope, $exprList.e); }
  |  ^(FUNC_CALL Wait d=expression (in=expression retry=expression)?) { node = new WaitNode(line, handler, currentScope, $d.node, $in.node, $retry.node); }
  |  ^(FUNC_CALL Signal d=expression v=expression) { node = new SignalNode(line, handler, currentScope, $d.node, $v.node); }
  |  ^(FUNC_CALL Chain s=expression p=expression?) { node = new ChainNode(line, handler, currentScope, $s.node, $p.node); }
  |  ^(FUNC_CALL Sleep expression) { node = new SleepNode(line, handler,currentScope,  $expression.node); }
  |  ^(FUNC_CALL Matches s=expression r=expression) { node = new MatchesNode(line, handler, currentScope, $s.node, $r.node); }
  |  ^(FUNC_CALL Cast a=expression b=expression) { node = new CastNode(line, handler, currentScope, $a.node, $b.node, languageRegistry, namespaceStack.asPrefix()); }
  |  ^(FUNC_CALL Rand expression) { node = new RandNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL Round v=expression dp=expression) { node = new RoundNode(line, handler, currentScope, $v.node, $dp.node); }
  |  ^(FUNC_CALL Lib expression) { node = new LibNode(line, handler, currentScope, $expression.node); }
  |  ^(FUNC_CALL Call a=expression b=expression c=expression) { node = new CallNode(line, handler, currentScope, $a.node, $b.node, $c.node); }
  |  ^(FUNC_CALL New  a=expression) { node = new NewNode(line, handler, currentScope, $a.node, languageRegistry, namespaceStack.asPrefix()); }
  |  ^(FUNC_CALL GenSchema a=expression) { node = new GenSchemaNode(line, handler, currentScope, $a.node, languageRegistry, namespaceStack.asPrefix()); }
  |  ^(FUNC_CALL GenStruct Identifier a=expression) { node = new GenStructNode(line, handler, currentScope, $Identifier.text, $a.node, languageRegistry, namespaceStack.asPrefix()); }
  |  ^(FUNC_CALL Template t=expression p=expression) { node = new TemplateNode(line, handler, currentScope, $t.node, $p.node); }
  |  ^(FUNC_CALL Spawn p=expression (e=expression f=expression)?) { node = new SpawnNode(line, handler, currentScope, $p.node, $e.node, $f.node); }
  |  ^(FUNC_CALL Defined Identifier) { node = new DefinedNode(line, handler, currentScope, $Identifier.text, namespaceStack.asPrefix()) ; }
  |  ^(FUNC_CALL Defined lookup) { node = new DefinedNode(line, handler, currentScope, $lookup.node, namespaceStack.asPrefix()) ; }
  |  ^(FUNC_CALL Contains i=Identifier e=expression) { node = new ContainsNode(line, handler, currentScope, $i.text, $e.node, namespaceStack.asPrefix()); }
  |  ^(KERNEL_CALL KernelIdentifier exprList?) { node = new KernelCallNode(line, handler, currentScope, $KernelIdentifier.text, $exprList.e); }
  |  ^(QUALIFIED_FUNC_CALL DottedIdentifier exprList?) { node = new QualifiedFuncCallNode(line, handler, currentScope, $DottedIdentifier.text,
                                                                      $exprList.e, languageRegistry, importHandler, namespaceStack.asPrefix()); }
  ;

ifStatement returns [ReflexNode node]
@init  {
  CommonTree ahead = (CommonTree) input.LT(1);
  int line = ahead.getToken().getLine();
  IfNode ifNode = new IfNode(line, handler, currentScope);
  node = ifNode;
}
  :  ^(IF
       (^(EXP expression b1=block){ifNode.addChoice($expression.node,$b1.node);})+
       (^(EXP b2=block)           {ifNode.addChoice(new AtomNode(line, handler, currentScope, true),$b2.node);})?
     )
  ;

forStatement returns [ReflexNode node]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
  :
     ^(FORLIST Identifier a=expression block) { node = new ForInStatementNode(line, handler, currentScope, $Identifier.text, $a.node, $block.node); }
  |  ^(FORTO Identifier a=expression b=expression block) { node = new ForStatementNode(line, handler, currentScope, $Identifier.text, $a.node, $b.node, $block.node); }
  ;

pforStatement returns [ReflexNode node]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
  :
     ^(PFORLIST Identifier a=expression block) { node = new PForInStatementNode(line, handler, currentScope, $Identifier.text, $a.node, $block.node); }
  |  ^(PFORTO Identifier a=expression b=expression block) { node = new PForStatementNode(line, handler, currentScope, $Identifier.text, $a.node, $b.node, $block.node); }
  ;

whileStatement returns [ReflexNode node]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
  :  ^(While expression block) { node = new WhileStatementNode(line, handler, currentScope,  $expression.node, $block.node); }
  ;

guardedStatement returns [ReflexNode node]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
  : ^(Try g=block Identifier c=block) { node = new GuardedNode(line, handler, currentScope, $g.node, $Identifier.text, $c.node); }
  ;

sparsesep:
   '-';

sparsematrix returns [int dim]
@init {
   dim = 1;
}
   : ^(SPARSE (sparsesep { dim++; }) +)
   ;

idList returns [java.util.List<String> i]
@init {
  i = new java.util.ArrayList<String>();
}
  :  ^(ID_LIST (Identifier { i.add($Identifier.text); })+)
  ;

exprList returns [java.util.List<ReflexNode> e]
@init {
   e = new java.util.ArrayList<ReflexNode>();
}
  :  ^(EXP_LIST (expression { e.add($expression.node); })+)
  ;

expression returns [ReflexNode node]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
  :  ^(TERNARY a=expression b=expression c=expression) { node = new TernaryNode(line, handler, currentScope, $a.node, $b.node, $c.node); }
  |  ^(In a=expression b=expression) { node = new InNode(line, handler, currentScope, $a.node, $b.node); }
  |  ^('||' a=expression b=expression) { node = new OrNode(line, handler, currentScope, $a.node, $b.node); }
  |  ^('&&' a=expression b=expression) { node = new AndNode(line, handler, currentScope, $a.node, $b.node); }
  |  ^('==' a=expression b=expression) { node = new EqualsNode(line, handler, currentScope, $a.node, $b.node); }
  |  ^('!=' a=expression b=expression) { node = new NotEqualsNode(line,handler, currentScope, $a.node, $b.node); }
  |  ^('>=' a=expression b=expression) { node = new GTEqualsNode(line, handler, currentScope, $a.node, $b.node); }
  |  ^('<=' a=expression b=expression) { node = new LTEqualsNode(line, handler, currentScope, $a.node, $b.node); }
  |  ^('>' a=expression b=expression) { node = new GTNode(line, handler, currentScope, $a.node, $b.node); }
  |  ^('<' a=expression b=expression) { node = new LTNode(line, handler, currentScope, $a.node, $b.node); }
  |  ^('+' a=expression b=expression) { node = new AddNode(line, handler,currentScope,  $a.node, $b.node); }
  |  ^('-' a=expression b=expression) { node = new SubNode(line, handler, currentScope, $a.node, $b.node); }
  |  ^('*' a=expression b=expression) { node = new MulNode(line, handler, currentScope, $a.node, $b.node); }
  |  ^('/' a=expression b=expression) { node = new DivNode(line, handler, currentScope, $a.node, $b.node); }
  |  ^('%' a=expression b=expression) { node = new ModNode(line, handler, currentScope, $a.node, $b.node); }
  |  ^('^' a=expression b=expression) { node = new PowNode(line, handler, currentScope, $a.node, $b.node); }
  |  ^(UNARY_MIN a=expression) { node = new UnaryMinusNode(line, handler, currentScope, $a.node); }
  |  ^(NEGATE a=expression) { node = new NegateNode(line, handler, currentScope, $a.node); }
  |  Number { node = new AtomNode(line, handler, currentScope, new BigDecimal($Number.text, MathContext.DECIMAL128)); }
  |  Integer { node = AtomNode.getIntegerAtom(line, handler, currentScope, $Integer.text); }
  |  Long { node = new AtomNode(line, handler, currentScope, java.lang.Long.parseLong($Long.text)); }
  |  Bool { node = new AtomNode(line, handler, currentScope, Boolean.parseBoolean($Bool.text)); }
  |  Null { node = new AtomNode(line, handler, currentScope); }
  |  sparsematrix { node = new AtomNode(line, handler, currentScope, new MatrixDim($sparsematrix.dim)); }
  |  lookup { node = $lookup.node; }
  ;


list returns [ReflexNode node]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
  : ^(LIST exprList?) { node = new ListNode(line, handler, currentScope, $exprList.e); }
  ;

mapdef returns [ReflexNode node]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
  : ^(MAPDEF keyValList?) { node = new MapNode(line, handler,currentScope,  $keyValList.e); }
  ;

jarUriList returns [java.util.List<String> jarUris]
@init {
   jarUris = new java.util.ArrayList<String>();
}
  :  ^(JARURI_LIST (jarUri { jarUris.add($jarUri.uri); })+)
  ;

jarUri returns [String uri]
  :  ^(JARURI (j=String | j=QuotedString)) { $uri = $j.text; }
  ;

keyValList returns [java.util.List<ReflexNode> e]
@init {
   e = new java.util.ArrayList<ReflexNode>();
}
  :  ^(KEYVAL_LIST (keyval { e.add($keyval.node); })+)
  ;

keyval returns [ReflexNode node]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
  :  ^(KEYVAL k=expression v=expression) { node = new KeyValNode(line, handler, currentScope, $k.node, $v.node); }
  ;

lookup returns [ReflexNode node]
@init {
    CommonTree ahead = (CommonTree) input.LT(1);
    int line = ahead.getToken().getLine();
}
  :  ^(LOOKUP functionCall i=indexes?) { node = $i.e != null ? new LookupNode(line, handler, currentScope, $functionCall.node, $i.e) : $functionCall.node; }
  |  ^(LOOKUP PropertyPlaceholder ) { node = new PropertyPlaceholderNode(line, handler, currentScope, $PropertyPlaceholder.text); }
  |  ^(RANGELOOKUP Identifier rangeindex) { node = new RangeLookupNode(line, handler, currentScope, new IdentifierNode(line, handler, currentScope, $Identifier.text, namespaceStack.asPrefix()), $rangeindex.ste, $rangeindex.ed); }
  |  ^(RANGELOOKUP DottedIdentifier rangeindex) { node = new RangeLookupNode(line, handler, currentScope, new IdentifierNode(line, handler, currentScope, $DottedIdentifier.text, namespaceStack.asPrefix()), $rangeindex.ste, $rangeindex.ed); }
  |  ^(LOOKUP list i=indexes?) {  node = $i.e != null ? new LookupNode(line, handler, currentScope, $list.node, $i.e) : $list.node; }
  |  ^(LOOKUP mapdef i=indexes?) { node = $i.e != null ? new LookupNode(line, handler, currentScope, $mapdef.node, $i.e) : $mapdef.node; }
  |  ^(LOOKUP expression i=indexes?) { node = $i.e != null ? new LookupNode(line, handler, currentScope, $expression.node, $i.e) : $expression.node; }
  |  ^(LOOKUP DottedIdentifier x=indexes?)
      {
        node = ($x.e != null)
          ? new LookupNode(line, handler, currentScope, new IdentifierNode(line, handler, currentScope, $DottedIdentifier.text, namespaceStack.asPrefix()), $x.e)
          : new IdentifierNode(line, handler, currentScope, $DottedIdentifier.text, namespaceStack.asPrefix());
      }
  |  ^(LOOKUP Identifier x=indexes?)
      {
        node = ($x.e != null)
          ? new LookupNode(line, handler, currentScope, new IdentifierNode(line, handler, currentScope, $Identifier.text, namespaceStack.asPrefix()), $x.e)
          : new IdentifierNode(line, handler, currentScope, $Identifier.text, namespaceStack.asPrefix());
      }
  |  ^(LOOKUP String x=indexes?)
    {
      node = ($x.e != null)
        ? new LookupNode(line, handler, currentScope, new AtomNode(line, handler, currentScope, $String.text), $x.e)
        : AtomNode.getStringAtom(line, handler,currentScope,  $String.text);
    }
  | ^(LOOKUP QuotedString x=indexes?)
    {
      node = ($x.e != null)
        ? new LookupNode(line, handler, currentScope, new QuotedStringNode(line, handler, currentScope, $QuotedString.text), $x.e)
        : new QuotedStringNode(line, handler, currentScope, $QuotedString.text);
    }
  ;

rangeindex returns [ReflexNode ste, ReflexNode ed]
: ^(RANGEINDEX lhs=expression rhs=expression) { $ste = $lhs.node; $ed = $rhs.node; }
;

indexes returns [java.util.List<List<ReflexNode>> e]
@init {
  e = new ArrayList<List<ReflexNode>>();
}
  :  ^(INDEXES (exprList { e.add($exprList.e); })+)
  ;
