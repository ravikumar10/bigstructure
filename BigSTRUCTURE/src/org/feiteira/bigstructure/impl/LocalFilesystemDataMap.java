package org.feiteira.bigstructure.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.feiteira.bigstructure.BigStructure;
import org.feiteira.bigstructure.auxi.BigSDataMap;

import com.google.common.io.ByteStreams;

/**
 * Core implementation of {@link BigSDataMap} for the local file system. Uses
 * property {@code LocalFilesystemDataMapRoot} to identify the root.
 * 
 * @author jlfeitei
 */
public class LocalFilesystemDataMap extends BigSDataMap {
	public static Logger log = Logger.getLogger(LocalFilesystemDataMap.class);

	public static final String PROP_LOCAL_ROOT = "LocalFilesystemDataMapRoot";

	public String rootURI = null;

	protected static Properties properties;

	public LocalFilesystemDataMap() {
		LocalFilesystemDataMap.properties = BigStructure.getProperties();
		this.rootURI = properties.getProperty(PROP_LOCAL_ROOT);
	}

	public LocalFilesystemDataMap(String rootURI) {
		this();
		if (rootURI.endsWith("/")) {
			this.rootURI = rootURI.substring(0, rootURI.length() - 1);
		} else {
			this.rootURI = rootURI;
		}
	}

	@Override
	public void store(String path, byte[] data) throws BPDataMapException {
		String fullpath = null;
		try {
			fullpath = getLocalFilesystemURL(path);
		} catch (MalformedURLException e1) {
			throw new BPDataMapException("Invalid path: " + fullpath
					+ " Input was: " + path, e1);
		}
		ensureFolders(fullpath);

		BufferedOutputStream bos;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(fullpath));
			if (data != null)
				bos.write(data);
			bos.close();

		} catch (FileNotFoundException e) {
			throw new BPDataMapException("Error writing into file:" + fullpath,
					e);
		} catch (IOException e) {
			throw new BPDataMapException("Error writing into file:" + fullpath,
					e);
		}
	}

	@Override
	public byte[] load(String path) throws BPDataMapException {
		String fullpath;
		byte[] ret = null;
		try {
			fullpath = getLocalFilesystemURL(path);
		} catch (MalformedURLException e1) {
			throw new BPDataMapException("Invalid path.", e1);
		}

		FileInputStream fis;
		try {
			fis = new FileInputStream(fullpath);
			ret = ByteStreams.toByteArray(fis);
			fis.close();
		} catch (FileNotFoundException e) {
			throw new BPDataMapException("Error writing into file:" + fullpath,
					e);
		} catch (IOException e) {
			throw new BPDataMapException("Error writing into file:" + fullpath,
					e);
		}
		return ret;
	}

	@Override
	public void delete(String path) throws BPDataMapException {
		if (!exists(path)) {
			throw new BPDataMapException("File not found to delete.");
		}

		try {
			File f = new File(getLocalFilesystemURL(path));
			f.delete();
		} catch (Exception e) {
			throw new BPDataMapException("Error deleting file.", e);
		}
	}

	@Override
	public void format() {
		File rootFile = null;
		try {
			rootFile = new File(getLocalFilesystemURL(""));
		} catch (MalformedURLException e1) {
			// Thrown as runtime because it's completely unexpected to happen
			// under normal circumstances
			throw new RuntimeException(e1);
		}

		File[] files = rootFile.listFiles();

		deleteRecursively(files);
	}

	private void deleteRecursively(File[] files) {
		for (File file : files) {
			if (file.isDirectory()) {
				deleteRecursively(file.listFiles());
				file.delete();
			} else {
				file.delete();
			}
		}

	}

	@Override
	public boolean exists(String path) throws BPDataMapException {
		File f;
		try {
			f = new File(getLocalFilesystemURL(path));
		} catch (MalformedURLException e) {
			throw new BPDataMapException(e);
		}

		return f.exists();
	}

	private String getLocalFilesystemURL(String nodepath)
			throws MalformedURLException {

		return (new URL(this.rootURI + nodepath)).getFile();
	}

	private void ensureFolders(String path) {
		File f = new File(path);
		File parent = f.getParentFile();
		parent.mkdirs();
	}

}
