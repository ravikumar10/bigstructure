package org.feiteira.bigstructure.core;

import org.feiteira.bigstructure.core.abstracts.BigSResponse;

public class EchoResponse extends BigSResponse {
	private static final long serialVersionUID = 1L;
	
	private String value;
	
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}

}
