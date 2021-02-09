package soen6591.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.JavaModelException;

import soen6591.patterns.ExceptionFinder;
import soen6591.patterns.MethodFinder;

public class DetectException extends AbstractHandler {
	 
	   @Override
	   public Object execute(ExecutionEvent arg0) throws ExecutionException {
	       IWorkspace workspace = ResourcesPlugin.getWorkspace();
	       IWorkspaceRoot root = workspace.getRoot();
	       IProject[] projects = root.getProjects();
	      
	       detectInProjects(projects);
	      
	       SampleHandler.printMessage("DONE DETECTING");
	      
	       return null;
	   }
	 
	   private void detectInProjects(IProject[] projects) {
	       for(IProject project : projects) {
	           SampleHandler.printMessage("DETECTING IN: " + project.getName());
	           ExceptionFinder exceptionFinder = new ExceptionFinder();
	           MethodFinder methodFinder = new MethodFinder();
	          
	           try {
	              
	               // 1. find how many methods
	               methodFinder.findMethods(project);
	              
	               // 2. find the exceptions
	               exceptionFinder.findExceptions(project);
	              
	           } catch (JavaModelException e) {
	               e.printStackTrace();
	           }  
	       }
	   }
	  
	}
