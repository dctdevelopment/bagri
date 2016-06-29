package com.bagri.xdm.cache.hazelcast.task.doc;

import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;

import com.bagri.xdm.api.DocumentManagement;
import com.bagri.xdm.cache.api.SchemaRepository;
import com.bagri.xdm.cache.api.TransactionManagement;
import com.bagri.xdm.cache.hazelcast.impl.RepositoryImpl;
import com.bagri.xdm.domain.Document;
import com.bagri.xdm.system.Permission;
import com.hazelcast.spring.context.SpringAware;

@SpringAware
public class DocumentMapCreator extends com.bagri.xdm.client.hazelcast.task.doc.DocumentMapCreator {

	private transient DocumentManagement docMgr;
	private transient TransactionManagement txMgr;
    
    @Autowired
	public void setRepository(SchemaRepository repo) {
		this.repo = repo;
		this.docMgr = repo.getDocumentManagement();
		this.txMgr = (TransactionManagement) repo.getTxManagement();
	}

    @Override
	public Document call() throws Exception {

    	((RepositoryImpl) repo).getXQProcessor(clientId);
    	checkPermission(Permission.Value.modify);
    	
    	return txMgr.callInTransaction(txId, false, new Callable<Document>() {
    		
	    	public Document call() throws Exception {
	    		return docMgr.storeDocumentFromMap(uri, fields, props);
	    	}
    	});
	}


}
