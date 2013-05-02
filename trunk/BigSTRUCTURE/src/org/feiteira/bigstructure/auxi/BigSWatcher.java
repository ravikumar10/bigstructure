package org.feiteira.bigstructure.auxi;

import java.io.Serializable;
import java.util.List;

/**
 * Use with the {@code BigSCoordinator.}
 * 
 * @author jlfeitei
 * 
 */
public interface BigSWatcher {
	public void childrenChanged(String path, List<String> list);

	public void dataChanged(String path, Serializable newData);
}
