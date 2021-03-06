package com.bagri.server.hazelcast.task.doc;

import static com.bagri.core.Constants.pn_client_txLevel;
import static com.bagri.core.Constants.pv_client_txLevel_skip;

import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;

import com.bagri.core.api.DocumentManagement;
import com.bagri.core.model.Document;
import com.bagri.core.server.api.SchemaRepository;
import com.bagri.core.server.api.TransactionManagement;
import com.bagri.core.system.Permission;
import com.bagri.server.hazelcast.impl.SchemaRepositoryImpl;
import com.hazelcast.spring.context.SpringAware;

@SpringAware
public class DocumentBeanCreator extends com.bagri.client.hazelcast.task.doc.DocumentBeanCreator {

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

    	((SchemaRepositoryImpl) repo).getXQProcessor(clientId);
    	checkPermission(Permission.Value.modify);
    	
    	String txLevel = props.getProperty(pn_client_txLevel);
    	if (pv_client_txLevel_skip.equals(txLevel)) {
    		// bypass tx stack completely!
    		return docMgr.storeDocumentFromBean(uri, bean, props);
    	}
    	
    	return txMgr.callInTransaction(txId, false, new Callable<Document>() {
    		
	    	public Document call() throws Exception {
	    		return docMgr.storeDocumentFromBean(uri, bean, props);
	    	}
    	});
	}


}
