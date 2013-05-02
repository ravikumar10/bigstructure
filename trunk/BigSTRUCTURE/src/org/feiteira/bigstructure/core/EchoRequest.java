package org.feiteira.bigstructure.core;

import org.feiteira.bigstructure.core.abstracts.BigSRequest;

public class EchoRequest extends BigSRequest {
	private static final long serialVersionUID = 1L;
	private String message = null;

	public EchoRequest(String message) {
		super();
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
