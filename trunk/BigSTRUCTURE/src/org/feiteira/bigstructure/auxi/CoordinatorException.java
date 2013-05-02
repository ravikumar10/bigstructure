package org.feiteira.bigstructure.auxi;

public class CoordinatorException extends Exception {
	private static final long serialVersionUID = 1L;

	public CoordinatorException(String message) {
		super(message);
	}

	public CoordinatorException(String message, Exception e) {
		super(message, e);
	}

	public CoordinatorException(Exception e) {
		super(e);
	}

}
