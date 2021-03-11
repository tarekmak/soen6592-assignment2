package soen6591.handlers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.JavaModelException;

import soen6591.bugInstances.DestructiveWrappingInstance;
import soen6591.patterns.ExceptionFinder;

public class DetectException extends AbstractHandler {
	//this map that contains the project names analyzed alongside the destructive wrapping instances flagged in the project
   HashMap<String, HashSet<DestructiveWrappingInstance>> projectDestructiveWrappingInstancesMap =
		   new HashMap<String, HashSet<DestructiveWrappingInstance>>();
   
	 
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
       
       logBugInstancesToCsvFile(root.getLocation().toString());
       
       //after printing all the info we got from the analysis, we reset map that contains the project names analyzed, alongside the number
       //of bug instances flagged. This is done in case we want to perform another analysis.
       projectDestructiveWrappingInstancesMap = new HashMap<String, HashSet<DestructiveWrappingInstance>>();
       
       return null;
   }
 
   private void detectInProjects(IProject[] projects) {
       for(IProject project : projects) {
           SampleHandler.printMessage(String.format("DETECTING IN PROJECT: %s\n", project.getName()));
           ExceptionFinder exceptionFinder = new ExceptionFinder();

           try {
        	   HashSet<DestructiveWrappingInstance> destructivewrappingCountInstanceInProj = exceptionFinder.findExceptions(project);
               
        	   projectDestructiveWrappingInstancesMap.put(project.getName(), destructivewrappingCountInstanceInProj);
               
               SampleHandler.printMessage(String.format("\nDETECTING IN PROJECT:Total destructive wrapping "
               		+ "instances flagged in the %s project: %d\n",
            		   project.getName(), destructivewrappingCountInstanceInProj.size()));
           } catch (JavaModelException e) {
        	   //according to ibm, this exception can safely be ignored as it does not affect the functionality of the code
        	   //(https://www.ibm.com/support/pages/java-model-exceptions-log)
               e.printStackTrace();
           }  
       }
   }
   
   private int getTotalDestructiveWrappingCount() {
	   int count = 0;
	   for (String projectName : projectDestructiveWrappingInstancesMap.keySet())
		   count += projectDestructiveWrappingInstancesMap.get(projectName).size();
	   
	   return count;
   }
   
   //this method prints how many bug instances were flagged in each project
   private void printExceptionCountBreakdown() {
	   SampleHandler.printMessage("\nDestructive wrapping instances break down by project:");
	   for (String projectName : projectDestructiveWrappingInstancesMap.keySet()) {
		   SampleHandler.printMessage(String.format("%s project: %d destructive wrapping instances flagged",
				   projectName, projectDestructiveWrappingInstancesMap.get(projectName).size()));
	   }
   }
   
   //this function will log the destructive wrapping instances in a log file
   private void logBugInstancesToCsvFile(String workspacePath) {
	   File logFile = new File(String.format("%s/LogFile.csv", workspacePath));
	   
	   try {
		   //we create the log file, if it already exists, this method will do nothing
		   logFile.createNewFile();
	   } catch (IOException e) {
		   e.printStackTrace();
	   }
	   
	   
	   try {
		   PrintWriter writer = new PrintWriter(logFile);
		   
		   StringBuilder sb = new StringBuilder();
		   
		   //preparing the columns of the csv file
		   sb.append("Project");
		   sb.append(",");
		   sb.append("Class path");
		   sb.append(",");
		   sb.append("Line of the catch clause");
		   sb.append(",");
		   sb.append("Line of the statement that throws the exception of the other type");
		   sb.append(",");
		   sb.append("Type of destructive wrapping instance");
		   sb.append(",");
		   sb.append("Type of the exception initialy caught");
		   sb.append(",");
		   sb.append("Type of the exception thrown");
		   sb.append("\n");
		   
		   for (String projectName : projectDestructiveWrappingInstancesMap.keySet()) {
			   HashSet<DestructiveWrappingInstance> destructiveWrappingInstancesInProj =
					   projectDestructiveWrappingInstancesMap.get(projectName);
			   
			   //adding the info of each destructive wrapping instance that was flagged during the analysis
			   for (DestructiveWrappingInstance destructiveWrappingInstance : destructiveWrappingInstancesInProj) {
				   sb.append(projectName);
				   sb.append(",");
				   sb.append(destructiveWrappingInstance.getClassPath());
				   sb.append(",");
				   sb.append(destructiveWrappingInstance.getCatchClauseStartLine());
				   sb.append(",");
				   sb.append(destructiveWrappingInstance.getProblematicLineStartLine());
				   sb.append(",");
				   sb.append(destructiveWrappingInstance.getTypeOfInstance());
				   sb.append(",");
				   sb.append(destructiveWrappingInstance.getInitialExceptionCaught());
				   sb.append(",");
				   sb.append(destructiveWrappingInstance.getNewExceptionCaught());
				   sb.append("\n");
			   }
		   }
		   
		   //writing the string that contains the info of all the destructive wrapping instances in the csv file
		   writer.write(sb.toString());
		   
		   writer.close();
	   } catch (FileNotFoundException e) {
		   e.printStackTrace();
	   }
   }
}
