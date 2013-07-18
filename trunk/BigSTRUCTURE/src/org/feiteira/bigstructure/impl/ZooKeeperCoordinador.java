package org.feiteira.bigstructure.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.retry.ZooKeeperRetry;
import org.feiteira.bigstructure.BigStructure;
import org.feiteira.bigstructure.auxi.BigSCoordinator;
import org.feiteira.bigstructure.auxi.BigSWatcher;
import org.feiteira.bigstructure.auxi.CoordinatorException;

/**
 * Core implementation of the {@link BigSCoordinator} using the ZooKeeper. Uses
 * properties {@code ZooKeeperConnection} and {@code ZooKeeperSessionTimeout}
 * from file {@code properties.ini}.
 * 
 * 
 * @author jlfeitei
 * 
 */
public class ZooKeeperCoordinador extends BigSCoordinator {
	public static final String PROP_ZOOKEEPER_CONNECTION_STRING = "ZooKeeperConnection";
	public static final String PROP_ZOOKEEPER_SESSION_TIMEOUT = "ZooKeeperSessionTimeout";
	public static Logger log = Logger.getLogger(ZooKeeperCoordinador.class);

	private static Properties properties = new Properties();
	private ZooKeeperRetry zookeeper = null;

	public ZooKeeperCoordinador() {
		synchronized (ZooKeeperCoordinador.properties) {
			ZooKeeperCoordinador.properties = BigStructure.getProperties();
			
			try {
				if (zookeeper == null)
					zookeeper = new ZooKeeperRetry(
							properties
									.getProperty(PROP_ZOOKEEPER_CONNECTION_STRING),
							Integer.parseInt(properties
									.getProperty(PROP_ZOOKEEPER_SESSION_TIMEOUT)),
							null);
			} catch (IOException e) {
				log.fatal(
						"Could not connect to ZooKeeper on startup, exiting.",
						e);
				System.exit(0);
			}
		}
	}

	@Override
	public boolean create(String path, Serializable data)
			throws CoordinatorException {

		byte[] bytes = getBytes(data);

		String cpath = null;
		try {
			cpath = zookeeper.create(path, bytes,
					Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		} catch (KeeperException e) {
			log.error("ZooKeeper exception", e);
			throw new CoordinatorException(e);
		} catch (InterruptedException e) {
			log.error("Node creation has been interrupted", e);
		}

		return path.equals(cpath);
	}

	@Override
	public boolean createEphemeral(String path, Serializable data)
			throws CoordinatorException {

		byte[] bytes = getBytes(data);

		String cpath = null;
		try {
			cpath = zookeeper.create(path, bytes,
					Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
		} catch (KeeperException e) {
			log.error("ZooKeeper exception", e);
			throw new CoordinatorException(e);
		} catch (InterruptedException e) {
			log.error("Node creation has been interrupted", e);
		}

		return path.equals(cpath);
	}

	@Override
	public Serializable get(String path) throws CoordinatorException {
		byte[] data = null;
		try {
			data = zookeeper.getData(path, false, null);
		} catch (KeeperException e) {
			throw new CoordinatorException("Node not found by ZooKeeper", e);
		} catch (InterruptedException e) {
			log.error("ZooKeeper call has been interrupted", e);
			return null;
		}

		return getObject(data);
	}

	@Override
	public void put(String path, Serializable data) throws CoordinatorException {
		try {
			zookeeper.setData(path, getBytes(data), -1);
		} catch (KeeperException e) {
			throw new CoordinatorException("on ZooKeeper.setData", e);
		} catch (InterruptedException e) {
			log.error("ZooKeeper call has been interrupted", e);
		}
	}

	@Override
	public boolean delete(String path) {
		try {
			zookeeper.delete(path, -1);
		} catch (InterruptedException e) {
			log.error("ZooKeeper call has been interrupted", e);
			return false;
		} catch (KeeperException e) {
			log.warn("Zookeeper exception while deleting node " + path, e);
			return false;
		}
		return true;
	}

	@Override
	public boolean hasChildren(String path) throws CoordinatorException {
		return getChildren(path).size() != 0;
	}

	@Override
	public List<String> getChildren(String path) throws CoordinatorException {
		log.debug("Get children of node: " + path);
		try {
			return (zookeeper.getChildren(path, null));
		} catch (KeeperException e) {
			throw new CoordinatorException("on zookeeper.getChildren", e);
		} catch (InterruptedException e) {
			log.warn("Zookeeper exception while deleting node", e);
			throw new RuntimeException("zookeeper.getChildren was interrupted",
					e);
		}
	}

	@Override
	public boolean exists(String path) {
		try {
			return zookeeper.exists(path, false) != null;
		} catch (KeeperException e) {
			log.error("ZooKeeper exception", e);
			return false;
		} catch (InterruptedException e) {
			log.warn("Zookeeper exception while deleting node", e);
			throw new RuntimeException("zookeeper.exists was interrupted", e);
		}
	}

	@Override
	public List<String> addChildChangeWatcher(final String path,
			final BigSWatcher watcher) throws CoordinatorException {
		List<String> ret = null;

		try {
			ret = zookeeper.getChildren(path, new Watcher() {

				@Override
				public void process(WatchedEvent event) {
					String epath = event.getPath();
					if (event.getType() == Event.EventType.None) {
						// We are are being told that the state of the
						// connection has changed
						switch (event.getState()) {
						case SyncConnected:
							// In this particular example we don't need to do
							// anything
							// here - watches are automatically re-registered
							// with
							// server and any watches triggered while the client
							// was
							// disconnected will be delivered (in order of
							// course)
							break;
						case Expired:
							// It's all over
							log.warn("Session expired");
							break;
						default:
							log.error("Unexpected event type: " + event);
							break;
						}
					} else {
						if (epath != null && epath.equals(path)) {
							// Something has changed on the node, let's find out
							try {
								watcher.childrenChanged(path, getChildren(path));
							} catch (CoordinatorException e) {
								log.error("Unexpected exception", e);
							}
						}
					}

				}
			});
		} catch (KeeperException e) {
			throw new CoordinatorException(e);
		} catch (InterruptedException e) {
			log.warn("Zookeeper exception while deleting node", e);
		}
		return ret;

	}

	@Override
	public Serializable addDataChangeWatcher(final String path,
			final BigSWatcher watcher) throws CoordinatorException {
		byte[] retBytes = null;
		try {
			retBytes = zookeeper.getData(path, new Watcher() {

				@Override
				public void process(WatchedEvent event) {
					String epath = event.getPath();
					if (event.getType() == Event.EventType.None) {
						// We are are being told that the state of the
						// connection has changed
						switch (event.getState()) {
						case SyncConnected:
							// In this particular example we don't need to do
							// anything
							// here - watches are automatically re-registered
							// with
							// server and any watches triggered while the client
							// was
							// disconnected will be delivered (in order of
							// course)
							break;
						case Expired:
							// It's all over
							log.warn("Session expired");
							break;
						default:
							log.error("Unexpected event type: " + event);
							break;
						}
					} else {
						if (epath != null && epath.equals(path)) {
							// Something has changed on the node, let's find out
							try {
								Serializable obj = get(path);
								watcher.dataChanged(path, obj);
							} catch (CoordinatorException e) {
								log.error("Unexpected exception", e);
							}
						}
					}

				}
			}, null);
		} catch (KeeperException e) {
			throw new CoordinatorException(e);
		} catch (InterruptedException e) {
			log.warn("Zookeeper interrupted", e);
		}

		return getObject(retBytes);
	}

	private byte[] getBytes(Serializable data) {
		if (data == null) {
			return new byte[0];
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(baos);
			oos.writeObject(data);
			oos.close();
		} catch (IOException e) {
			log.error("Strange error while serializing an object", e);
			return null;
		}
		return baos.toByteArray();
	}

	private Serializable getObject(byte[] data) {
		if (data.length == 0)
			return null;

		Serializable retObj = null;
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(new ByteArrayInputStream(data));
			retObj = (Serializable) ois.readObject();
			log.debug("RET " + retObj);
		} catch (IOException e1) {
			log.error("Strange error while serializing an object", e1);
			return null;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Serializable not found? Really weird!",
					e);
		}
		return retObj;
	}

	@Override
	public void deleteAll(String path) throws CoordinatorException {
		if (!path.equals("/zookeeper"))
			super.deleteAll(path);
	}

	@Override
	public void disconnect() throws CoordinatorException {
		try {
			zookeeper.close();
		} catch (InterruptedException e) {
			throw new CoordinatorException(e);
		}
	}
}
