package soen6591.visitors;


import org.eclipse.jdt.core.dom.Expression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.WhileStatement;

import soen6591.bugInstances.DestructiveWrappingInstance;

public class CatchClauseVisitor extends ASTVisitor {
  HashSet<CatchClause> emptyCatches = new HashSet<>();
  HashSet<CatchClause> genericCatches = new HashSet<>();
  HashSet<DestructiveWrappingInstance> destructiveCatches = new HashSet<>();
  String classPath;
  CompilationUnit cu;
 
  public CatchClauseVisitor(String classPath, CompilationUnit cu) {
	  super();
	  this.classPath = classPath;
	  this.cu = cu;
  }
  
  @Override
  public boolean visit(CatchClause node) {
      
      DestructiveWrappingInstance bugInstance = isDestructiveWrapping(node);
      if (bugInstance != null)
    	  destructiveCatches.add(bugInstance);
     
      return super.visit(node);
  }
 
  //It will return the bug instance if the node satisfies the 
  //criteria to be considered a destructive wrapping case and null otherwise.
  private DestructiveWrappingInstance isDestructiveWrapping(CatchClause node) {
	  
	  Block catchClauseBody = node.getBody();
	  
	  //if the node doesn't have a body (i.e., if the catch clause is empty), then we return null
	  if (catchClauseBody == null)
		  return null;
	  
	  //first, we check whether the catch clause is encapsulated inside another try statement(s) and, if it is, we get the exceptions caught the
	  //encapsulated try statement(s)
	  HashSet<ITypeBinding> caughtExceptions = getExceptionsCaughtInParents(new HashSet<ITypeBinding>(), new HashSet<CatchClause>(), node);
	    
	  return throwsNewException(node, catchClauseBody.statements(), caughtExceptions);
  }
  
  //this method check if the block that is passed throws an exception of a different type than the type
  //represented by the string exceptionTypeStr. It will return the bug instance if the node satisfies the 
  //criteria to be considered a destructive wrapping case and null otherwise.
  private DestructiveWrappingInstance throwsNewException(CatchClause node, List<?> statementList,
		  HashSet<ITypeBinding> caughtExceptions) {
	  
	  //this string represent the type of exception that the catch clause is supposed to catch
	  ITypeBinding initialExceptionType = node.getException().getType().resolveBinding();
	  
	  //if the catch clause does not contain any statements, then it cannot throw a new exception, so we return false
	  if (statementList.isEmpty())
		  return null;
	  
	  for (Object statement : statementList) {
		  
		  if (((Statement) statement).getNodeType() == Statement.THROW_STATEMENT) {
			  ThrowStatement throwStatement = ((ThrowStatement) statement);
			  
			  //we get the exceptions caught for the throw statement
			  HashSet<ITypeBinding> newCaughtExceptions = getExceptionsCaughtInParents(new HashSet<ITypeBinding>(caughtExceptions),
					  new HashSet<CatchClause>(), throwStatement);

			  //if we find a throwable statement, we resolve the type of exception it throws
			  ITypeBinding newExceptionType = throwStatement.getExpression().resolveTypeBinding();
			  
			  if (!newExceptionType.getQualifiedName().equals(initialExceptionType.getQualifiedName())
					  && !isCaught(newCaughtExceptions, newExceptionType))  
				  return new DestructiveWrappingInstance(classPath, initialExceptionType.getQualifiedName(), newExceptionType.getQualifiedName(),
						  "THROW_STATEMENT", cu.getLineNumber(node.getStartPosition()), cu.getLineNumber(throwStatement.getStartPosition()));

		  } else if (((Statement) statement).getNodeType() == Statement.EXPRESSION_STATEMENT) {
			  Expression expr = ((ExpressionStatement) statement).getExpression();
			  
			  
			  if (expr.getNodeType() == Expression.METHOD_INVOCATION) {
				  MethodInvocation methodInvocation = (MethodInvocation) expr;
				  
				  //we get the exceptions caught for the method invocation
				  HashSet<ITypeBinding> newCaughtExceptions = getExceptionsCaughtInParents(new HashSet<ITypeBinding>(caughtExceptions),
						  new HashSet<CatchClause>(), expr);

				  //trying to resolve the method binding of the method that was called
				  IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
				  
				  //if we weren't able to resolve the binding for the method, we move on to the next statement
				  if (methodBinding == null)
					  continue;
				  
				  //trying to resolve the binding of the compilation unit so we can access the implementation of method binding
				  ICompilationUnit unit = (ICompilationUnit) methodBinding.getJavaElement().getAncestor(IJavaElement.COMPILATION_UNIT);
				  
				  //if we weren't able to resolve the binding for the compilation unit, we move on to the next statement
				  if (unit == null)
				     continue;
				  
				  //parsing the java file that contains the declaration of the method being called
				  ASTParser parser = ASTParser.newParser(AST.JLS15);
			      parser.setKind(ASTParser.K_COMPILATION_UNIT);
			      parser.setSource(unit);
			      parser.setResolveBindings(true);
			      parser.setBindingsRecovery(true);
			      parser.setStatementsRecovery(true);
				  CompilationUnit methodCu = (CompilationUnit) parser.createAST(null);
				  MethodDeclaration methodDeclaration = (MethodDeclaration) methodCu.findDeclaringNode(methodBinding.getKey());
				  
				  //if we weren't able to find the method declaration, we move on to the next statement
				  if (methodDeclaration == null)
					  continue;
				  
				  //getting the exceptions that are thrown in the method (if any) 
				  //(we infer the ? generic type to mitigate the type safety warning)
				  List<?> thrownExceptionTypes = methodDeclaration.thrownExceptionTypes();
				  for (Object exceptionTypne : thrownExceptionTypes) {
					  ITypeBinding newExceptionType = ((Type) exceptionTypne).resolveBinding();
					  //if it is indicated that the method being called throws a exception that is different from the exception being
					  //caught initially in the catch block, then we return a bug instance
					  if (!newExceptionType.getQualifiedName().equals(initialExceptionType.getQualifiedName())
							  && !isCaught(newCaughtExceptions, newExceptionType))
						  return new DestructiveWrappingInstance(classPath, initialExceptionType.getQualifiedName(), newExceptionType.getQualifiedName(),
								  "METHOD_INVOCATION", cu.getLineNumber(node.getStartPosition()), cu.getLineNumber(methodInvocation.getStartPosition()));
				  }

				  Block methodBody = methodDeclaration.getBody();
				  
				  //if the method invoked has no body, we move on to the next method
				  if (methodBody == null)
					  continue;
				  
				  //checking if a different exception is being thrown in the body of the method being called in the catch block
				  DestructiveWrappingInstance bugInstance = throwsNewException(node, methodBody.statements(), newCaughtExceptions);
				  if (bugInstance != null) {
					  //we need to change the problematic line position (i.e., the line that throws the exception), so it corresponds
					  //to the line where the method is called in the java class being explored
					  bugInstance.setProblematicLineStartLine(cu.getLineNumber(methodInvocation.getStartPosition()));
					  bugInstance.setTypeOfInstance("METHOD_INVOCATION");
					  return bugInstance;
				  }
			  }
		  } else {
			  List<?> blockStatementList = getBlockStatement((Statement) statement);
			  
			  if (blockStatementList == null)
				  continue;
			  
			  //checking if a different exception is being thrown in the body of the statement
			  DestructiveWrappingInstance bugInstance = throwsNewException(node, blockStatementList, caughtExceptions);
			  if (bugInstance != null)
				  return bugInstance;
		  }
	  }
	  return null;
  }
  
  
  private HashSet<ITypeBinding> getExceptionsCaughtInParents(HashSet<ITypeBinding> exceptionsCaughtByParent,
		  HashSet<CatchClause> encapsulatingCatchClauses, ASTNode parent) {
	  if (parent == null)
		  return exceptionsCaughtByParent;
	  
	  if (parent.getNodeType() == ASTNode.CATCH_CLAUSE)
		  encapsulatingCatchClauses.add((CatchClause) parent);
	  
	  if (parent.getNodeType() == ASTNode.TRY_STATEMENT) {
		  TryStatement tryStatement = (TryStatement) parent;
		  List<?> catchClauses = tryStatement.catchClauses();
		  
		  //we keep track of the catch clauses that encapsulate the statement studied as, if the catch clause is from the try statement "parent",
		  //we dismiss the try statement, as the exception will only be caught if the code is executed inside the try block and not the catch block. 
		  for (Object catchClause : catchClauses) {
			  if (encapsulatingCatchClauses.contains((CatchClause) catchClause))
				  return getExceptionsCaughtInParents(exceptionsCaughtByParent, encapsulatingCatchClauses, parent.getParent());
		  }
		  
		  for (Object catchClause : catchClauses) {
			  exceptionsCaughtByParent.add(((CatchClause) catchClause).getException().getType().resolveBinding());
		  }
		  
	  }
	  
	  return getExceptionsCaughtInParents(exceptionsCaughtByParent, encapsulatingCatchClauses, parent.getParent());
  }
  
  private boolean isCaught(HashSet<ITypeBinding> exceptionsCaught, ITypeBinding newExceptionType) {
	  for (ITypeBinding exceptionCaught : exceptionsCaught) {
		  //if the exception itself (or is a subtype of an exception that was caught) is caught, we return true.
		  if (newExceptionType.isSubTypeCompatible(exceptionCaught))
			  return true;
	  }
	  return false;
  }

  
  //this method return a block statement if the statement passed has a block statement
  private List<?> getBlockStatement(Statement statement) {
	  if (statement == null)
		  return null;
	  
	  if (statement.getNodeType() == Statement.BLOCK)
		  return ((Block) statement).statements();
	  
	  if (statement.getNodeType() == Statement.FOR_STATEMENT && ((ForStatement) statement).getBody() != null)
		  return ((Block) ((ForStatement) statement).getBody()).statements();
	  
	  if (statement.getNodeType() == Statement.WHILE_STATEMENT && ((WhileStatement) statement).getBody() != null)
		  return ((Block) ((WhileStatement) statement).getBody()).statements();
	  
	  if (statement.getNodeType() == Statement.DO_STATEMENT && ((DoStatement) statement).getBody() != null)
		  return ((Block) ((DoStatement) statement).getBody()).statements();
	  
	  if (statement.getNodeType() == Statement.SWITCH_STATEMENT)
		  return ((SwitchStatement) statement).statements();
	  
	  if (statement.getNodeType() == Statement.IF_STATEMENT && ((IfStatement) statement).getElseStatement() == null)
		  return getBlockStatement(((IfStatement) statement).getThenStatement());
	  
	  if (statement.getNodeType() == Statement.IF_STATEMENT && ((IfStatement) statement).getElseStatement() != null) {
		  List<?> thenStatementList = getBlockStatement(((IfStatement) statement).getThenStatement());
		  List<?> elseStatementList = getBlockStatement(((IfStatement) statement).getElseStatement());

		  if (thenStatementList == null)
			  return elseStatementList;
		  
		  if (elseStatementList == null)
			  return thenStatementList;
		  
		  List<Statement> unifiedStatementsList = new ArrayList<Statement>();
		  
		  for (Object thenStatement : thenStatementList)
			  unifiedStatementsList.add((Statement) thenStatement);
		  
		  for (Object elseStatement : elseStatementList)
			  unifiedStatementsList.add((Statement) elseStatement);
		  
		  return unifiedStatementsList;
	  }
	  
	  if (statement.getNodeType() == Statement.TRY_STATEMENT && ((TryStatement) statement).getBody() != null) {
		  List<Statement> unifiedStatementsList = new ArrayList<Statement>();
		  
		  List<?> tryStatementsList = ((Block) ((TryStatement) statement).getBody()).statements();
		  for (Object tryStatement : tryStatementsList)
			  unifiedStatementsList.add((Statement) tryStatement);
		  
		  
		  List<?> catchClauseList = ((TryStatement) statement).catchClauses();
		  for (Object catchClause : catchClauseList) {
			  Block catchClauseBody = ((CatchClause) catchClause).getBody();
			  if (catchClauseBody == null)
				  continue;
			  
			  List<?> catchClauseStatementsList = catchClauseBody.statements();
			  for (Object catchClauseStatement : catchClauseStatementsList)
				  unifiedStatementsList.add((Statement) catchClauseStatement);
		  }
		  return unifiedStatementsList;
	  }
	  
	  return null;
  }

  public HashSet<DestructiveWrappingInstance> getDestructiveCatches() {
      return destructiveCatches;
  }
  
}
