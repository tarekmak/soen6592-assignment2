package soen6591.bugInstances;

public class DestructiveWrappingInstance {
	String classPath, initialExceptionCaught, newExceptionCaught;
	int catchClauseStartLine, problematicLineStartLine;
	
	public DestructiveWrappingInstance(String classPath, String initialExceptionCaught, String newExceptionCaught,
			int catchClauseStartLine, int problematicLineStartLine) {
		this.classPath = classPath;
		this.initialExceptionCaught = initialExceptionCaught;
		this.newExceptionCaught = newExceptionCaught;
		this.catchClauseStartLine = catchClauseStartLine;
		this.problematicLineStartLine = problematicLineStartLine;
	}

	public String getLogStatement() {
		return String.format("DESTRUCTIVE WRAPPING INSTANCE: bug instance detected in %s at line %d. "
				+ "The catch caluse catches a %s exception, but might throw a %s exception at line %d.",
				classPath, catchClauseStartLine, initialExceptionCaught, newExceptionCaught, problematicLineStartLine);
	}
	
	public void setProblematicLineStartLine(int problematicLineStartLine) {
		this.problematicLineStartLine = problematicLineStartLine;
	}
}
