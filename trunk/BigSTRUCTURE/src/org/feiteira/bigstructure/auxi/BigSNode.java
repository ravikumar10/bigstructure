package org.feiteira.bigstructure.auxi;

import java.io.Serializable;

/**
 * Simple node representation.
 * 
 * @author jlfeitei
 * 
 */
public class BigSNode implements Serializable {
	private static final long serialVersionUID = 1L;

	private String path;
	private Serializable metaData;

	public BigSNode() {
		this(null, null);
	}

	public BigSNode(String path) {
		this(path, null);
	}

	public BigSNode(String path, Serializable metaData) {
		this.path = path;
		this.metaData = metaData;

	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Serializable getMetaData() {
		return metaData;
	}

	public void setMetaData(Serializable metaData) {
		this.metaData = metaData;
	}

}
