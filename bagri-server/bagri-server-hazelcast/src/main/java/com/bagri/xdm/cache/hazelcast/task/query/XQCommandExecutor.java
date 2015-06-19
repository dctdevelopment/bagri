package com.bagri.xdm.cache.hazelcast.task.query;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.bagri.xdm.cache.api.XDMQueryManagement;
import com.bagri.xdm.cache.api.XDMTransactionManagement;
import com.bagri.xdm.client.hazelcast.impl.ResultCursor;
import com.hazelcast.spring.context.SpringAware;

@SpringAware
public class XQCommandExecutor extends com.bagri.xdm.client.hazelcast.task.query.XQCommandExecutor {

	//private static final transient Logger logger = LoggerFactory.getLogger(XQCommandExecutor.class);
    
	private transient XDMQueryManagement queryMgr;
	private transient XDMTransactionManagement txMgr;
    
    @Autowired
    @Qualifier("queryProxy") //queryProxy //queryManager
	public void setQueryManager(XDMQueryManagement queryMgr) {
		this.queryMgr = queryMgr;
		//logger.trace("setQueryManager; got QueryManager: {}", queryMgr); 
	}
    
    @Autowired
	public void setTxManager(XDMTransactionManagement txMgr) {
		this.txMgr = txMgr;
		//logger.trace("setTxManager; got TxManager: {}", txMgr); 
	}

    @Override
	public ResultCursor call() throws Exception {
		
    	long txId = XDMTransactionManagement.TX_NO;
    	String id = context.getProperty("txId");
		if (id != null) {
			txId = Long.parseLong(id);
		}
		boolean readOnly = queryMgr.isReadOnlyQuery(command);
		if (readOnly) {
			if (isQuery) {
				return (ResultCursor) queryMgr.executeXQuery(command, bindings, context);
			} else {
		        return (ResultCursor) queryMgr.executeXCommand(command, bindings, context);
			}
		}
		
    	return txMgr.callInTransaction(txId, false, new Callable<ResultCursor>() {
    		
	    	public ResultCursor call() {
				if (isQuery) {
					return (ResultCursor) queryMgr.executeXQuery(command, bindings, context);
				} else {
			        return (ResultCursor) queryMgr.executeXCommand(command, bindings, context);
				}
	    	}
    	});
    }

}
