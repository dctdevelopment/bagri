package com.bagri.server.hazelcast.task.tx;

import org.springframework.beans.factory.annotation.Autowired;

import com.bagri.core.api.TransactionManagement;
import com.hazelcast.spring.context.SpringAware;

@SpringAware
public class TransactionStarter extends com.bagri.client.hazelcast.task.tx.TransactionStarter {

	private transient TransactionManagement txMgr;
    
    @Autowired
	public void setTxManager(TransactionManagement txMgr) {
		this.txMgr = txMgr;
	}
    
    @Override
	public Long call() throws Exception {
		return txMgr.beginTransaction(txIsolation);
	}
	
}
