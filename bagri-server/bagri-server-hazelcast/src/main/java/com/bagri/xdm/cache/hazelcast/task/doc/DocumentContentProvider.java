package com.bagri.xdm.cache.hazelcast.task.doc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.bagri.xdm.api.XDMDocumentManagement;
import com.bagri.xdm.api.XDMException;
import com.bagri.xdm.cache.api.XDMAccessManagement;
import com.bagri.xdm.cache.api.XDMClientManagement;
import com.bagri.xdm.cache.hazelcast.impl.RepositoryImpl;
import com.bagri.xdm.system.XDMPermission.Permission;
import com.hazelcast.spring.context.SpringAware;

@SpringAware
public class DocumentContentProvider extends com.bagri.xdm.client.hazelcast.task.doc.DocumentContentProvider {

	private transient RepositoryImpl repo;
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
	public String call() throws Exception {
    	
    	XDMClientManagement clientMgr = repo.getClientManagement();
    	String user = clientMgr.getCurrentUser();
    	if (!((XDMAccessManagement) repo.getAccessManagement()).hasPermission(user, Permission.read)) {
    		throw new XDMException("User " + user + " has no permission to read documents", XDMException.ecAccess);
    	}
    	
		return docMgr.getDocumentAsString(docId);
	}
}
