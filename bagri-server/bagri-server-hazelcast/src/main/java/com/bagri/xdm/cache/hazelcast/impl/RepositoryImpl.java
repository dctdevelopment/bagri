package com.bagri.xdm.cache.hazelcast.impl;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.bagri.xdm.api.XDMDocumentManagement;
import com.bagri.xdm.api.XDMModelManagement;
import com.bagri.xdm.api.XDMQueryManagement;
import com.bagri.xdm.api.XDMRepository;
import com.bagri.xqj.BagriXQDataFactory;
import com.bagri.xquery.api.XQProcessor;
import com.bagri.xquery.saxon.XQProcessorServer;
import com.hazelcast.core.Client;
import com.hazelcast.core.ClientListener;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;

public class RepositoryImpl implements ApplicationContextAware, ClientListener, XDMRepository {

	private static final transient Logger logger = LoggerFactory.getLogger(RepositoryImpl.class);
	
    private ApplicationContext appContext;
    private HazelcastInstance hzInstance;
	private Map<String, XQProcessor> processors = new ConcurrentHashMap<String, XQProcessor>();

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.appContext = context;
	}

    //@Autowired
	public void setHzInstance(HazelcastInstance hzInstance) {
		this.hzInstance = hzInstance;
		hzInstance.getClientService().addClientListener(this);
		logger.debug("setHzInstange; got instance: {}", hzInstance.getName()); 
	}
	
	@Override
	public void clientConnected(Client client) {
		logger.trace("clientConnected.enter; client: {}", client); 
		// create queue
		IQueue queue = hzInstance.getQueue("client:" + client.getUuid());
		// create/cache new XQProcessor
		XQProcessor proc = getXQProcessor(client.getUuid());
		logger.trace("clientConnected.exit; queue {} created for client: {}; XQProcessor: {}", 
				queue.getName(), client.getSocketAddress(), proc);
	}

	@Override
	public void clientDisconnected(Client client) {
		logger.trace("clientDisconnected.enter; client: {}", client);
		String qName = "client:" + client.getUuid();
		boolean destroyed = false;
		Collection<DistributedObject> all = hzInstance.getDistributedObjects();
		int sizeBefore = all.size();
		for (DistributedObject obj: all) {
			if (qName.equals(obj.getName())) {
				// remove queue
				IQueue queue = hzInstance.getQueue(qName);
				queue.destroy();
				destroyed = true;
				break;
			}
		}
		int sizeAfter = hzInstance.getDistributedObjects().size(); 
		XQProcessor proc = processors.remove(client.getUuid());
		logger.trace("clientDisconnected.exit; queue {} {} for client: {}; size before: {}, after: {}", 
				qName, destroyed ? "destroyed" : "skipped", client.getSocketAddress(), sizeBefore, sizeAfter); 
	}
	
	HazelcastInstance getHzInstance() {
		return hzInstance;
	}
	
	XQProcessor getXQProcessor(String clientId) {
		XQProcessor result = processors.get(clientId);
		if (result == null) {
			result = appContext.getBean(XQProcessor.class, this);
			XDMQueryManagement qMgr = getQueryManagement();
			result.setRepository(this);
			((BagriXQDataFactory) ((XQProcessorServer) result).getXQDataFactory()).setProcessor(result);
			processors.put(clientId, result);
		}
		logger.trace("getXQProcessor; returning: {}", result);
		return result;
	}

	@Override
	public void close() {
		// TODO: disconnect all clients ?
	}

	@Override
	public XDMDocumentManagement getDocumentManagement() {
		return appContext.getBean(DocumentManagementImpl.class);
	}

	@Override
	public XDMQueryManagement getQueryManagement() {
		return appContext.getBean(QueryManagementImpl.class);
	}

	@Override
	public XDMModelManagement getModelManagement() {
		return appContext.getBean(ModelManagementImpl.class);
	}

	
}
