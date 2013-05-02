package org.feiteira.bigstructure.core.abstracts;

import java.io.Serializable;

import org.feiteira.bigstructure.BigStructure;

/**
 * BigStructure requests must extend this class.
 * 
 * @author jlfeitei
 */
public abstract class BigSRequest implements Serializable {
	public static final long serialVersionUID = 1L;
	protected String id = null;
	protected String nodePath = null;

	public BigSRequest() {
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
