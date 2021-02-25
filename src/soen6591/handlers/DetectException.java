package soen6591.handlers;

import java.util.HashMap;

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
	//this map that contains the project names analyzed alongside the number of bug instances flagged
   HashMap<String, Integer> projectDestructiveWrappingInstanceCountMap = new HashMap<String, Integer>();
	 
   @Override
   public Object execute(ExecutionEvent arg0) throws ExecutionException {
       IWorkspace workspace = ResourcesPlugin.getWorkspace();
       IWorkspaceRoot root = workspace.getRoot();
       IProject[] projects = root.getProjects();
       
       long start = System.currentTimeMillis();
       
       detectInProjects(projects);
       
       long timeElapsed = System.currentTimeMillis() - start;
       
       SampleHandler.printMessage(String.format("DONE DETECTING\nTIME ELAPSED: %d ms\n"
       		+ "TOTAL NUMBER OF DESTRUCTIVE WRAPPING INSTANCES FALGGED: %d", 
    		   timeElapsed, getTotalDestructiveWrappingCount()));
      
       printExceptionCountBreakdown();
       
       //after printing all the info we got from the analysis, we reset map that contains the project names analyzed, alongside the number
       //of bug instances flagged. This is done in case we want to perform another analysis.
       projectDestructiveWrappingInstanceCountMap = new HashMap<String, Integer>();
       
       return null;
   }
 
   private void detectInProjects(IProject[] projects) {
       for(IProject project : projects) {
           SampleHandler.printMessage(String.format("DETECTING IN PROJECT: %s\n", project.getName()));
           ExceptionFinder exceptionFinder = new ExceptionFinder();
          
           try {
               int destructivewrappingCountInstanceInProj = exceptionFinder.findExceptions(project);
               
               projectDestructiveWrappingInstanceCountMap.put(project.getName(), destructivewrappingCountInstanceInProj);
               
               SampleHandler.printMessage(String.format("\nDETECTING IN PROJECT:Total destructive wrapping "
               		+ "instances flagged in the %s project: %d\n",
            		   project.getName(), destructivewrappingCountInstanceInProj));
           } catch (JavaModelException e) {
               e.printStackTrace();
           }  
       }
   }
   
   private int getTotalDestructiveWrappingCount() {
	   int count = 0;
	   for (String projectName : projectDestructiveWrappingInstanceCountMap.keySet())
		   count += projectDestructiveWrappingInstanceCountMap.get(projectName);
	   
	   return count;
   }
   
   //this method prints how many bug instances were flagged in each project
   private void printExceptionCountBreakdown() {
	   SampleHandler.printMessage("\nDestructive wrapping instances break down by project:");
	   for (String projectName : projectDestructiveWrappingInstanceCountMap.keySet()) {
		   SampleHandler.printMessage(String.format("%s project: %d destructive wrapping instances flagged",
				   projectName, projectDestructiveWrappingInstanceCountMap.get(projectName)));
	   }
   }
}
