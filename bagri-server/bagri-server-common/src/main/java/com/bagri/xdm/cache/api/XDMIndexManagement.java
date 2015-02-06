package com.bagri.xdm.cache.api;

import com.bagri.xdm.system.XDMIndex;

public interface XDMIndexManagement {

	/**
	 * @param pathId
	 * check is path indexed or not
	 * 
	 */
	boolean isPathIndexed(int pathId); 
	
	/**
	 * @param typeId
	 * registers a new index
	 * 
	 */
	boolean createIndex(XDMIndex index);
	
	/**
	 * @param typeId
	 * remove an existing index
	 * 
	 */
	boolean deleteIndex(int pathId);


	boolean rebuildIndex(int pathId);
	
}