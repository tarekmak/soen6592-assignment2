package soen6591.patterns;

import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;

import soen6591.bugInstances.DestructiveWrappingInstance;
import soen6591.handlers.SampleHandler;
import soen6591.visitors.CatchClauseVisitor;

public class ExceptionFinder {
   private static int n_destructiveWrapper = 0;
	
   public void findExceptions(IProject project) throws JavaModelException {
       IPackageFragment[] packages = JavaCore.create(project).getPackageFragments();
 
       for(IPackageFragment mypackage : packages){
           findTargetCatchClauses(mypackage);
       }      
   }
 
   private void findTargetCatchClauses(IPackageFragment packageFragment) throws JavaModelException {
       for (ICompilationUnit unit : packageFragment.getCompilationUnits()) {
    	   
    	   //getting the path of the java class being parsed
    	   String classPath = "NOT_FOUND";
    	   IResource resource = unit.getResource();
    	   
    	   if (resource.getType() == IResource.FILE)
    		   classPath = resource.getRawLocation().toString();
    	   
    	   SampleHandler.printMessage(String.format("DETECTING IN CLASS: %s\n...", classPath));
           CompilationUnit parsedCompilationUnit = parse(unit);
           CatchClauseVisitor exceptionVisitor = new CatchClauseVisitor(classPath, parsedCompilationUnit);
           parsedCompilationUnit.accept(exceptionVisitor);
          
           printExceptions(exceptionVisitor);
       }
      
   }
 
   private void printExceptions(CatchClauseVisitor visitor) {
//       SampleHandler.printMessage("__________________EMPTY CATCHES___________________");
//       for(CatchClause statement: visitor.getEmptyCatches()) {
//           SampleHandler.printMessage(statement.toString());
//       }
//      
//       SampleHandler.printMessage("__________________GENERIC CATCHES___________________");
//       for(CatchClause statement: visitor.getGenericCatches()) {
////    	   ICompilationUnit unit = (ICompilationUnit) statement.getJavaElement().getAncestor(IJavaElement.COMPILATION_UNIT);
//           SampleHandler.printMessage(statement.toString());
//       }
	   
//	   SampleHandler.printMessage("__________________DESTRUCTIVE WRAPPING___________________");
       HashSet<DestructiveWrappingInstance> destructiveCatches = visitor.getDestructiveCatches();
       for (DestructiveWrappingInstance statement : destructiveCatches)
    	   SampleHandler.printMessage(statement.getLogStatement());
       
       int destructiveWrappingCount = destructiveCatches.size();
       
       addDestructiveWrappingCount(destructiveWrappingCount);
       
       SampleHandler.printMessage(String.format("\nTotal destructive wrapping instances flagged: %d\n", destructiveWrappingCount));
   }
 
   public void addDestructiveWrappingCount(int n) {
	   n_destructiveWrapper += n;
   }
   
   public static int getDestructiveWrappingCount() {
	   return n_destructiveWrapper;
   }
   
   public static void resetDestructiveWrappingCount() {
	   n_destructiveWrapper = 0;
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