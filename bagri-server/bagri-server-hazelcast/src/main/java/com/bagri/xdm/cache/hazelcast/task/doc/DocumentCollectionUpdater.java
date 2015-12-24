package com.bagri.xdm.cache.hazelcast.task.doc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.bagri.xdm.api.XDMDocumentManagement;
import com.bagri.xdm.cache.hazelcast.impl.RepositoryImpl;
import com.bagri.xdm.system.XDMPermission.Permission;
import com.hazelcast.spring.context.SpringAware;

@SpringAware
public class DocumentCollectionUpdater extends com.bagri.xdm.client.hazelcast.task.doc.DocumentCollectionUpdater {

	private transient XDMDocumentManagement docMgr;
    
    @Autowired
    @Qualifier("docProxy")
	public void setDocManager(XDMDocumentManagement docMgr) {
		this.docMgr = docMgr;
	}
	    
    @Autowired
	public void setRepository(RepositoryImpl repo) {
		this.repo = repo;
	}

    @Override
	public Integer call() throws Exception {
    	
    	((RepositoryImpl) repo).getXQProcessor(clientId);
    	checkPermission(Permission.modify);
    	
    	
    	if (add) {
    		return docMgr.addDocumentToCollections(docId, collectIds);
    	} else {
    		return docMgr.removeDocumentFromCollections(docId, collectIds);
    	}    	
	}


}
