package org.feiteira.bigstructure.auxi;

import java.io.Serializable;
import java.util.List;

/**
 * BigSCoordinator implementations are used to ensure coordination when working
 * with EPUs. For example a simultaneous call to {@code createNode} with the
 * same path, cannot return true to both calls. It's up to the Coordinator to
 * decide which call to succeed and which to fail. This is inspired by the
 * {@link http://zookeeper.apache.org/} project, but provided as is for
 * future-proof reasons. It also has added benefits as it allows nodes to store
 * simple {@code Serializable} objects as node data.
 * 
 * @author jlfeitei
 * 
 */
public abstract class BigSCoordinator {
	/**
	 * Creates a node with contents of {@code data}.
	 * 
	 * @param path
	 * @param data
	 * @return true if the node was created.
	 * @throws CoordinatorException
	 */
	public abstract boolean create(String path, Serializable data)
			throws CoordinatorException;

	/**
	 * Creates an ephemeral node with contents of {@code data}.
	 * 
	 * @param path
	 * @param data
	 * @return true if the node was created.
	 * @throws CoordinatorException
	 */

	public abstract boolean createEphemeral(String path, Serializable data)
			throws CoordinatorException;

	/**
	 * Returns the data in the node {@code path}.
	 * 
	 * @param path
	 * @return
	 * @throws CoordinatorException
	 */
	public abstract Serializable get(String path) throws CoordinatorException;

	/**
	 * Updates the content of node {@code path} to be {@code data}.
	 * 
	 * @param path
	 * @param data
	 * @throws CoordinatorException
	 */
	public abstract void put(String path, Serializable data)
			throws CoordinatorException;

	/**
	 * Deletes a node.
	 * 
	 * @param path
	 * @return
	 */
	public abstract boolean delete(String path);

	/**
	 * True if the node has children.
	 * 
	 * @param path
	 * @return
	 * @throws CoordinatorException
	 */
	public abstract boolean hasChildren(String path)
			throws CoordinatorException;

	/**
	 * True if the node exists.
	 * 
	 * @param path
	 * @return
	 */
	public abstract boolean exists(String path);

	/**
	 * 
	 * @param path
	 * @return A list of the node's children's names.
	 * @throws CoordinatorException
	 */
	public abstract List<String> getChildren(String path)
			throws CoordinatorException;

	/**
	 * Adds a Watcher that will be executed if any child is added or removed
	 * from the {@code path} node. Note: Watcher are 'run once', you must add
	 * another watcher after each notification if you want to keep tracking.
	 * 
	 * @param path
	 * @param watcher
	 * @return
	 * @throws CoordinatorException
	 */
	public abstract List<String> addChildChangeWatcher(String path,
			BigSWatcher watcher) throws CoordinatorException;

	/**
	 * Adds a Watcher that will be executed if the data of node {@code path}
	 * changes. Note: Watcher are 'run once', you must add another watcher after
	 * each notification if you want to keep tracking.
	 * 
	 * @param path
	 * @param watcher
	 * @return
	 * @throws CoordinatorException
	 */
	public abstract Serializable addDataChangeWatcher(String path,
			BigSWatcher watcher) throws CoordinatorException;

	/**
	 * Creates a node with null contents.
	 * 
	 * @param path
	 * @return
	 * @throws CoordinatorException
	 */
	public boolean create(String path) throws CoordinatorException {
		return create(path, null);
	}

	/**
	 * Deletes all nodes
	 * 
	 * @throws CoordinatorException
	 */
	public void deleteAll() throws CoordinatorException {
		deleteAll("/");
	}

	/**
	 * Deletes a node {@code path} and all it's children (think: {@code rm -rf)}
	 * .
	 * 
	 * @param path
	 * @throws CoordinatorException
	 */
	public void deleteAll(String path) throws CoordinatorException {
		List<String> children = getChildren(path);
		for (String child : children) {
			if (path == "/")
				deleteAll(path + child);
			else
				deleteAll(path + "/" + child);
		}

		if (!path.equals("/"))
			delete(path);
	}

}
