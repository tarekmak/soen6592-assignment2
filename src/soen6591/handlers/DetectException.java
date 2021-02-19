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

public class DetectException extends AbstractHandler {
	 
	   @Override
	   public Object execute(ExecutionEvent arg0) throws ExecutionException {
	       IWorkspace workspace = ResourcesPlugin.getWorkspace();
	       IWorkspaceRoot root = workspace.getRoot();
	       IProject[] projects = root.getProjects();
	       
	       long start = System.currentTimeMillis();
	       
	       detectInProjects(projects);
	       
	       long timeElapsed = System.currentTimeMillis() - start;
	       
	       SampleHandler.printMessage(String.format("DONE DETECTING\nTIME ELAPSED: %d ms\nTOTAL NUMBER OF DESTRUCTIVE WRAPPING INSTANCES FALGGED: %d", 
	    		   timeElapsed, ExceptionFinder.getDestructiveWrappingCount()));
	      
	       ExceptionFinder.resetDestructiveWrappingCount();
	       
	       return null;
	   }
	 
	   private void detectInProjects(IProject[] projects) {
	       for(IProject project : projects) {
	           SampleHandler.printMessage(String.format("DETECTING IN PROJECT: %s\n", project.getName()));
	           ExceptionFinder exceptionFinder = new ExceptionFinder();
	          
	           try {
	               exceptionFinder.findExceptions(project);
	               
	           } catch (JavaModelException e) {
	               e.printStackTrace();
	           }  
	       }
	   }
	  
	}
