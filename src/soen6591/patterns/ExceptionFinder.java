package soen6591.patterns;

import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import soen6591.bugInstances.DestructiveWrappingInstance;
import soen6591.handlers.SampleHandler;
import soen6591.visitors.CatchClauseVisitor;

public class ExceptionFinder {
	
   //this method finds the exceptions in the project and and returns the number of exceptions flagged in the passed project
   public int findExceptions(IProject project) throws JavaModelException {
	   int destructivewrappingCountInstanceInProj = 0;
	   
       IPackageFragment[] packages = JavaCore.create(project).getPackageFragments();
 
       for(IPackageFragment mypackage : packages)
           destructivewrappingCountInstanceInProj += findTargetCatchClauses(mypackage);
       
       return destructivewrappingCountInstanceInProj;
   }
 
   //this method finds the target catch clause in the package passed and returns the number of bug instances found in the package
   //(in our case, the number of destructive wrapping instances)
   private int findTargetCatchClauses(IPackageFragment packageFragment) throws JavaModelException {
	   //this variable represents the number of destructive wrapping instances in the package passed as a parameter
	   int destructiveWrappingInstanceCountInFrag = 0;
	   
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
           
           int destrusctiveWrappingInstanceCountInClass = getExceptions(exceptionVisitor);
           
           SampleHandler.printMessage(String.format("\nDETECTING IN CLASS: Total destructive wrapping instances flagged in the %s class : %d\n",
        		   classPath, destrusctiveWrappingInstanceCountInClass));
           
           destructiveWrappingInstanceCountInFrag += destrusctiveWrappingInstanceCountInClass;
       }
       
       return destructiveWrappingInstanceCountInFrag;
   }
 
   //this method prints the log statements from the exceptions and returns the number of exceptions found in the class
   //(in our case, only the destructive wrapping patterns)
   private int getExceptions(CatchClauseVisitor visitor) {
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
       
//       addDestructiveWrappingCount(destructiveWrappingCount);
       
       return destructiveWrappingCount;
   }
 
//   public void addDestructiveWrappingCount(int n) {
//	   n_destructiveWrapper += n;
//   }
//   
//   public static int getDestructiveWrappingCount() {
//	   return n_destructiveWrapper;
//   }
//   
//   public static void resetDestructiveWrappingCount() {
//	   n_destructiveWrapper = 0;
//   }
   
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