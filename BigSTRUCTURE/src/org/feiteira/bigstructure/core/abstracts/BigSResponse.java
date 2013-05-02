package org.feiteira.bigstructure.core.abstracts;

import java.io.Serializable;

import org.feiteira.bigstructure.BigStructure;

/**
 * Responses sent from the service back to the clients must extend this class.
 * 
 * @author jlfeitei
 */
public abstract class BigSResponse implements Serializable {
	public static final long serialVersionUID = 1L;
	protected String id = null;
	private String nodePath = null;

	public BigSResponse() {
		id = BigStructure.getProperties().getProperty(
				BigStructure.PROP_STRUCTURE_ID);
	}

	public String getNodePath() {
		return nodePath;
	}

	public void setNodePath(String nodePath) {
		this.nodePath = nodePath;
	}
}
