package com.bagri.server.hazelcast.task.doc;

import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;

import com.bagri.core.api.DocumentManagement;
import com.bagri.core.server.api.SchemaRepository;
import com.bagri.core.server.api.TransactionManagement;
import com.bagri.core.system.Permission;
import com.bagri.server.hazelcast.impl.SchemaRepositoryImpl;
import com.hazelcast.spring.context.SpringAware;

@SpringAware
public class CollectionDocumentsRemover extends com.bagri.client.hazelcast.task.doc.CollectionDocumentsRemover {

	private transient DocumentManagement docMgr;
	private transient TransactionManagement txMgr;
    
    @Autowired
	public void setRepository(SchemaRepository repo) {
		this.repo = repo;
		this.docMgr = repo.getDocumentManagement();
		this.txMgr = (TransactionManagement) repo.getTxManagement();
	}

    @Override
	public Integer call() throws Exception {

    	((SchemaRepositoryImpl) repo).getXQProcessor(clientId);
    	checkPermission(Permission.Value.modify);
    	
    	return txMgr.callInTransaction(txId, false, new Callable<Integer>() {
    		
	    	public Integer call() throws Exception {
	    		return docMgr.removeCollectionDocuments(collection);
	    	}
    	});
	}

}