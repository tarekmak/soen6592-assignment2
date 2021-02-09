package soen6591.visitors;


import java.util.HashSet;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;

public class CatchClauseVisitor extends ASTVisitor {
  HashSet<CatchClause> emptyCatches = new HashSet<>();
  HashSet<CatchClause> genericCatches = new HashSet<>();
 
 
  @Override
  public boolean visit(CatchClause node) {
      if(isEmptyException(node)) {
          emptyCatches.add(node);
      }

      if(isGenericException(node)){
          genericCatches.add(node);
      }
     
      return super.visit(node);
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

}
