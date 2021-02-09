package soen6591.patterns;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;

import soen6591.handlers.SampleHandler;
import soen6591.visitors.CatchClauseVisitor;

public class ExceptionFinder {
	 
   public void findExceptions(IProject project) throws JavaModelException {
       IPackageFragment[] packages = JavaCore.create(project).getPackageFragments();
 
       for(IPackageFragment mypackage : packages){
           findTargetCatchClauses(mypackage);
       }      
   }
 
   private void findTargetCatchClauses(IPackageFragment packageFragment) throws JavaModelException {
       for (ICompilationUnit unit : packageFragment.getCompilationUnits()) {
           CompilationUnit parsedCompilationUnit = parse(unit);
                      
           CatchClauseVisitor exceptionVisitor = new CatchClauseVisitor();
           parsedCompilationUnit.accept(exceptionVisitor);
          
           printExceptions(exceptionVisitor);
       }
      
   }
 
   private void printExceptions(CatchClauseVisitor visitor) {
       SampleHandler.printMessage("__________________EMPTY CATCHES___________________");
       for(CatchClause statement: visitor.getEmptyCatches()) {
           SampleHandler.printMessage(statement.toString());
       }
      
       SampleHandler.printMessage("__________________GENERIC CATCHES___________________");
       for(CatchClause statement: visitor.getGenericCatches()) {
           SampleHandler.printMessage(statement.toString());
       }
      
   }
 
   private CompilationUnit parse(ICompilationUnit unit) {
       ASTParser parser = ASTParser.newParser(AST.JLS15);
       parser.setKind(ASTParser.K_COMPILATION_UNIT);
       parser.setSource(unit);
       parser.setResolveBindings(true);
       parser.setBindingsRecovery(true);
       parser.setStatementsRecovery(true);
       return (CompilationUnit) parser.createAST(null); // parse
   }
 
}