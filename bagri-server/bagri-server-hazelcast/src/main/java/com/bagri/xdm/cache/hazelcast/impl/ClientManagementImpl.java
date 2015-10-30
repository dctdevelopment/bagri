package com.bagri.xdm.cache.hazelcast.impl;

import static com.bagri.xdm.common.XDMConstants.pn_schema_user;

import java.util.Collection;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bagri.common.stats.StatisticsEvent;
import com.bagri.xdm.cache.api.XDMClientManagement;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MapEvent;

public class ClientManagementImpl implements XDMClientManagement, EntryListener<String, Properties> {
	
	private static final transient Logger logger = LoggerFactory.getLogger(ClientManagementImpl.class);
	
	private RepositoryImpl repo;
    private HazelcastInstance hzInstance;
	private IMap<String, Properties> clientsCache;
	
    private boolean enableStats = true;
	private BlockingQueue<StatisticsEvent> queue;
	
	public void setHzInstance(HazelcastInstance hzInstance) {
		this.hzInstance = hzInstance;
		logger.debug("setHzInstange; got instance: {}", hzInstance.getName());
	}
	
	public void setClientsCache(IMap<String, Properties> clientsCache) {
		this.clientsCache = clientsCache;
		clientsCache.addEntryListener(this, false);
		//hzInstance.getClientService().addClientListener(this);
	}
	
    public void setRepository(RepositoryImpl repo) {
    	this.repo = repo;
    }	
	
    public void setStatsQueue(BlockingQueue<StatisticsEvent> queue) {
    	this.queue = queue;
    }

    public void setStatsEnabled(boolean enable) {
    	this.enableStats = enable;
    }

	private void updateStats(String name, boolean success, long duration) {
		if (enableStats) {
			if (!queue.offer(new StatisticsEvent(name, success, duration))) {
				logger.warn("updateStats; queue is full!!");
			}
		}
	}
    
	@Override
	public String[] getClients() {
		Set<String> clients = clientsCache.keySet(); 
		return clients.toArray(new String[clients.size()]);
	}

	@Override
	public String getCurrentUser() {
		String result = null;
		String clientId = repo.getClientId();
		if (clientId != null) {
			Properties props = clientsCache.get(clientId);
			if (props != null) {
				result = props.getProperty(pn_schema_user);
			}
		}
		return result;
	}
	
	@Override
	public Properties getClientProperties(String clientId) {
		return clientsCache.get(clientId);
	}
	
	@Override
	public void entryAdded(EntryEvent<String, Properties> event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void entryUpdated(EntryEvent<String, Properties> event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void entryRemoved(EntryEvent<String, Properties> event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void entryEvicted(EntryEvent<String, Properties> event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mapCleared(MapEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mapEvicted(MapEvent event) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void clientConnected(String clientId) {
		logger.trace("clientConnected.enter; client: {}", clientId); 
		// create queue
		//IQueue queue = hzInstance.getQueue("client:" + clientId);
		// create/cache new XQProcessor
		//XQProcessor proc = getXQProcessor(client.getUuid());
		//logger.trace("clientConnected.exit; queue {} created for client: {}; XQProcessor: {}", 
		//		queue.getName(), clientId, proc);
	}

	//@Override
	public void clientDisconnected(String clientId) {
		logger.trace("clientDisconnected.enter; client: {}", clientId);
		String qName = "client:" + clientId;
		boolean destroyed = false;
		Collection<DistributedObject> all = hzInstance.getDistributedObjects();
		int sizeBefore = all.size();
		for (DistributedObject obj: all) {
			if (qName.equals(obj.getName())) {
				// remove queue
				//IQueue queue = hzInstance.getQueue(qName);
				//queue.destroy();
				obj.destroy();
				destroyed = true;
				break;
			}
		}
		int sizeAfter = hzInstance.getDistributedObjects().size(); 
		//XQProcessor proc = processors.remove(client.getUuid());
		logger.trace("clientDisconnected.exit; queue {} {} for client: {}; size before: {}, after: {}", 
				qName, destroyed ? "destroyed" : "skipped", clientId, sizeBefore, sizeAfter); 
	}

	
}