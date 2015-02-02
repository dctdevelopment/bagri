package com.bagri.xdm.cache.hazelcast.task.query;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.bagri.xdm.api.XDMDocumentManagement;
import com.bagri.xdm.api.XDMQueryManagement;
import com.hazelcast.spring.context.SpringAware;

@SpringAware
public class DocumentUrisProvider extends com.bagri.xdm.client.hazelcast.task.query.DocumentUrisProvider {

	private static final transient Logger logger = LoggerFactory.getLogger(DocumentUrisProvider.class);
	    
	private transient XDMQueryManagement queryMgr;
    
    @Autowired
    @Qualifier("queryProxy")
	public void setQueryManager(XDMQueryManagement queryMgr) {
		this.queryMgr = queryMgr;
		logger.debug("setQueryManager; got QueryManager: {}", queryMgr); 
	}
	    
	@Override
	public Collection<String> call() throws Exception {
		logger.trace("call.enter; container: {}", exp); //eBuilder.getRoot());
		Collection<String> result = queryMgr.getDocumentURIs(exp);
		logger.trace("call.exit; returning: {}", result);
		return result;
	}

}