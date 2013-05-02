package org.feiteira.bigstructure.auxi;

import java.io.Serializable;

/**
 * Used by clients, works as a pointer to Ethereal Processing Units.
 * 
 * @author jlfeitei
 */
public class EPUReference implements Serializable {
	private static final long serialVersionUID = 1L;

	String nodePath;
	int serverPort;
	String hostName;

	public EPUReference() {
	}

	public String getNodePath() {
		return nodePath;
	}

	public void setNodePath(String nodePath) {
		this.nodePath = nodePath;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
}
