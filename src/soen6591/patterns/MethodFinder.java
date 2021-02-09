package soen6591.patterns;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
 
import soen6591.handlers.SampleHandler;
import soen6591.visitors.MethodDeclarationVisitor;


public class MethodFinder {
 
   public void findMethods(IProject project) throws JavaModelException {
       IPackageFragment[] packages = JavaCore.create(project).getPackageFragments();
 
       for(IPackageFragment mypackage : packages){
           findTargeMethodDeclarations(mypackage);
       }
   }
 
   private void findTargeMethodDeclarations(IPackageFragment packageFragment) throws JavaModelException {
       for (ICompilationUnit unit : packageFragment.getCompilationUnits()) {
           CompilationUnit parsedCompilationUnit = parse(unit);
                      
           MethodDeclarationVisitor methodDeclarationVisitor = new MethodDeclarationVisitor();
           parsedCompilationUnit.accept(methodDeclarationVisitor);
          
            printMethodCount(methodDeclarationVisitor);
       }
      
   }
 
   private void printMethodCount(MethodDeclarationVisitor visitor) {
       SampleHandler.printMessage("__________________NUMBER OF METHODS IN THE PROJECT___________________");
       SampleHandler.printMessage(("Count:" + visitor.getMethodCount()));
      
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
