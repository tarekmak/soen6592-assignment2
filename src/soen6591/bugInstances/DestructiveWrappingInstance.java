package soen6591.bugInstances;

public class DestructiveWrappingInstance {
	private String classPath, initialExceptionCaught, newExceptionCaught, typeOfInstance;
	private int catchClauseStartLine, problematicLineStartLine;
	
	public DestructiveWrappingInstance(String classPath, String initialExceptionCaught, String newExceptionCaught, String typeOfInstance,
			int catchClauseStartLine, int problematicLineStartLine) {
		this.setClassPath(classPath);
		this.setInitialExceptionCaught(initialExceptionCaught);
		this.setNewExceptionCaught(newExceptionCaught);
		this.setTypeOfInstance(typeOfInstance);
		this.setCatchClauseStartLine(catchClauseStartLine);
		this.setProblematicLineStartLine(problematicLineStartLine);
	}

	public String getLogStatement() {
		return String.format("DESTRUCTIVE WRAPPING INSTANCE: bug instance detected in %s at line %d. "
				+ "The catch caluse catches a %s exception, but might throw a %s exception at line %d.",
				getClassPath(), getCatchClauseStartLine(), getInitialExceptionCaught(), getNewExceptionCaught(), getProblematicLineStartLine());
	}
	
	public int getProblematicLineStartLine() {
		return problematicLineStartLine;
	}
	
	public void setProblematicLineStartLine(int problematicLineStartLine) {
		this.problematicLineStartLine = problematicLineStartLine;
	}

	public String getClassPath() {
		return classPath;
	}

	public void setClassPath(String classPath) {
		this.classPath = classPath;
	}

	public String getInitialExceptionCaught() {
		return initialExceptionCaught;
	}

	public void setInitialExceptionCaught(String initialExceptionCaught) {
		this.initialExceptionCaught = initialExceptionCaught;
	}

	public String getNewExceptionCaught() {
		return newExceptionCaught;
	}

	public void setNewExceptionCaught(String newExceptionCaught) {
		this.newExceptionCaught = newExceptionCaught;
	}

	public int getCatchClauseStartLine() {
		return catchClauseStartLine;
	}

	void setCatchClauseStartLine(int catchClauseStartLine) {
		this.catchClauseStartLine = catchClauseStartLine;
	}

	public String getTypeOfInstance() {
		return typeOfInstance;
	}

	public void setTypeOfInstance(String typeOfInstance) {
		this.typeOfInstance = typeOfInstance;
	}

}
