package soen6591.visitors;


import org.eclipse.jdt.core.dom.Expression;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;

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
//      if(isEmptyException(node)) {
//          emptyCatches.add(node);
//      }
//
//      if(isGenericException(node)){
//          genericCatches.add(node);
//      }
      
      DestructiveWrappingInstance bugInstance = isDestructiveWrapping(node);
      if (bugInstance != null)
    	  destructiveCatches.add(bugInstance);
     
      return super.visit(node);
  }
 
  //It will return the bug instance if the node satisfies the 
  //criteria to be considered a destructive wrapping case and null otherwise.
  private DestructiveWrappingInstance isDestructiveWrapping(CatchClause node) {

	  return throwsNewException(node, node.getBody());
  }
  
  //this method check if the block that is passed throws an exception of a different type than the type
  //represented by the string exceptionTypeStr. It will return the bug instance if the node satisfies the 
  //criteria to be considered a destructive wrapping case and null otherwise.
  private DestructiveWrappingInstance throwsNewException(CatchClause node, Block block) {
	  
	  //this string represent the type of exception that the catch clause is supposed to catch
	  String exceptionTypeStr = node.getException().getType().resolveBinding().getQualifiedName();
	  
	  //if the node doesn't have a body (i.e., if the catch clause is empty), then we return null
	  if (block == null)
		  return null;
	  
	  //collect all the statements inside the catch clause's body (we infer the ? generic type to mitigate the type safety warning)
	  List<?> statementList = block.statements();
	  
	  //if the catch clause does not contain any statements, then it cannot throw a new exception, so we return false
	  if (statementList.isEmpty())
		  return null;
	  
	  for (Object statement : statementList) {
		  if (((Statement) statement).getNodeType() == Statement.THROW_STATEMENT) {
			  ThrowStatement throwStatement = ((ThrowStatement) statement);
			  
			  //if we find a throwable statement, we resolve the type of exception it throws
			  String newExceptionTypeStr = throwStatement.getExpression().resolveTypeBinding().getQualifiedName();
			  
			  if (!newExceptionTypeStr.equals(exceptionTypeStr))  
				  return new DestructiveWrappingInstance(classPath, exceptionTypeStr, newExceptionTypeStr,
							cu.getLineNumber(node.getStartPosition()), cu.getLineNumber(throwStatement.getStartPosition()));
			  
		  } else if (((Statement) statement).getNodeType() == Statement.EXPRESSION_STATEMENT) {
			  Expression expr = ((ExpressionStatement) statement).getExpression();
			  if (expr.getNodeType() == Expression.METHOD_INVOCATION) {
				  MethodInvocation methodInvocation = ((MethodInvocation) expr);
				  
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
				  for (Object exceptionType : thrownExceptionTypes) {
					  String newExceptionTypeStr = ((Type) exceptionType).resolveBinding().getQualifiedName();
					  //if it is indicated that the method being called throws a exception that is different from the exception being
					  //caught initially in the catch block, then we return a bug instance
					  if (!newExceptionTypeStr.equals(exceptionTypeStr))
						  return new DestructiveWrappingInstance(classPath, exceptionTypeStr, newExceptionTypeStr,
									cu.getLineNumber(node.getStartPosition()), cu.getLineNumber(methodInvocation.getStartPosition()));
				  }
				  
				  //checking if a different exception is being thrown in the body of the method being called in the catch block
				  DestructiveWrappingInstance bugInstance = throwsNewException(node, methodDeclaration.getBody());
				  if (bugInstance != null) {
					  //we need to change the problematic line position (i.e., the line that throws the exception), so it corresponds
					  //to the line where the method is called in the java class being explored
					  bugInstance.setProblematicLineStartLine(cu.getLineNumber(methodInvocation.getStartPosition()));
					  return bugInstance;
				  }
			  }
		  } else if (((Statement) statement).getNodeType() == Statement.TRY_STATEMENT) {
			  TryStatement tryStatement = (TryStatement) statement;
			  //if we encounter another try statement, then get the catch clauses of this try statement
			  List<?> catchClauses = tryStatement.catchClauses();
			  
			  for (Object catchClause : catchClauses) {
				  //we get the type of exceptions that are caught by the corresponding catch clause
				  String newExceptionTypeStr = ((CatchClause) catchClause).getException().getType().resolveBinding().getQualifiedName();
				  
				  //if the exception being caught is different that the one that was caught initially, then we return a bug instance
				  if (!newExceptionTypeStr.equals(exceptionTypeStr))
					  return new DestructiveWrappingInstance(classPath, exceptionTypeStr, newExceptionTypeStr,
								cu.getLineNumber(node.getStartPosition()), cu.getLineNumber(tryStatement.getStartPosition()));
			  }
			  
		  }
	  }
	  return null;
  }
  
  private boolean isEmptyException(CatchClause node) {
      return node.getBody().statements().isEmpty();  
  }
 
  private boolean isGenericException(CatchClause node) {
      return node.getException().getType().resolveBinding().getQualifiedName().equals("java.lang.Exception");
  }

  public HashSet<CatchClause> getEmptyCatches() {
      return emptyCatches;
  }

  public HashSet<CatchClause> getGenericCatches() {
      return genericCatches;
  }

  public HashSet<DestructiveWrappingInstance> getDestructiveCatches() {
      return destructiveCatches;
  }
  
}
