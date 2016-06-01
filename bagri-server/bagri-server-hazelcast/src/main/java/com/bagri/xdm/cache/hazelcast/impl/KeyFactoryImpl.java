/**
 * 
 */
package com.bagri.xdm.cache.hazelcast.impl;

import com.bagri.xdm.client.hazelcast.data.DocumentKey;
import com.bagri.xdm.client.hazelcast.data.DocumentPathKey;
import com.bagri.xdm.client.hazelcast.data.PathIndexKey;
import com.bagri.xdm.common.XDMDataKey;
import com.bagri.xdm.common.XDMDocumentKey;
import com.bagri.xdm.common.XDMKeyFactory;
import com.bagri.xdm.common.XDMIndexKey;
import com.bagri.xdm.domain.XDMElement;

/**
 * @author Denis Sukhoroslov: dsukhoroslov@gmail.com
 * @version 1.0
 *
 */
public final class KeyFactoryImpl implements XDMKeyFactory {
	
	/* (non-Javadoc)
	 * @see com.bagri.xdm.common.XDMFactory#newXDMDocumentKey(long)
	 */
	@Override
	public XDMDocumentKey newXDMDocumentKey(long documentKey) {
		return new DocumentKey(documentKey);
	}

	/* (non-Javadoc)
	 * @see com.bagri.xdm.common.XDMFactory#newXDMDocumentKey(long, int)
	 */
	@Override
	public XDMDocumentKey newXDMDocumentKey(long documentKey, int version) {
		return new DocumentKey(XDMDocumentKey.toHash(documentKey), XDMDocumentKey.toRevision(documentKey), version); 
	}

	/* (non-Javadoc)
	 * @see com.bagri.xdm.common.XDMFactory#newXDMDocumentKey(String, int, int)
	 */
	@Override
	public XDMDocumentKey newXDMDocumentKey(String documentUri, int revision, int version) {
		return new DocumentKey(documentUri.hashCode(), revision, version);
	}
	
	/* (non-Javadoc)
	 * @see com.bagri.xdm.common.XDMFactory#newXDMDataKey(long, int)
	 */
	@Override
	public XDMDataKey newXDMDataKey(long documentKey, int pathId) {
		return new DocumentPathKey(documentKey, pathId);
	}

	/* (non-Javadoc)
	 * @see com.bagri.xdm.common.XDMFactory#newXDMIndexKey(int, Object)
	 */
	@Override
	public XDMIndexKey newXDMIndexKey(int pathId, Object value) {
		return new PathIndexKey(pathId, value);
	}

}