package org.feiteira.bigstructure.auxi;

import java.net.MalformedURLException;

/**
 * BigStructure is always attached to a DataMap, this can be either a simple
 * LocalFilesystem implementation or a big data solution like MongoDB or Apache
 * Hadoop. This abstract class is done so that extending it should be
 * straight forward.
 * 
 * @author jlfeitei
 * 
 */
public abstract class BigSDataMap {

	public abstract void store(String path, byte[] data)
			throws BPDataMapException;

	public abstract byte[] load(String path) throws BPDataMapException;

	public abstract void delete(String path) throws BPDataMapException;

	/**
	 * 
	 * @param path
	 * @return
	 * @throws BPDataMapException
	 *             Encapsulates Malformed URL exception
	 */
	public abstract boolean exists(String path) throws BPDataMapException;

	public abstract void format();

	public BigSDataMap() {
	}

	public class BPDataMapException extends Exception {
		private static final long serialVersionUID = 1L;

		public BPDataMapException(String msg, Exception e) {
			super(msg, e);
		}

		public BPDataMapException(String string) {
			super(string);
		}

		public BPDataMapException(MalformedURLException e) {
			super(e);
		}

	}

}
