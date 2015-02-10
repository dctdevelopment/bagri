package com.bagri.xdm.cache.hazelcast.task.schema;

import static com.bagri.xdm.client.common.XDMCacheConstants.CN_XDM_DOCUMENT;
import static com.bagri.xdm.client.common.XDMCacheConstants.CN_XDM_ELEMENT;
import static com.bagri.xdm.client.hazelcast.serialize.XDMDataSerializationFactory.cli_XDMAggregateSchemaInfoTask;
import static com.bagri.xdm.client.hazelcast.serialize.XDMDataSerializationFactory.factoryId;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bagri.xdm.common.XDMDataKey;
import com.bagri.xdm.domain.XDMDocument;
import com.bagri.xdm.domain.XDMElement;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.monitor.LocalMapStats;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.spring.context.SpringAware;

@SpringAware
public class SchemaStatsAggregator implements Callable<Long>, IdentifiedDataSerializable { 
	
	private static final transient Logger logger = LoggerFactory.getLogger(SchemaStatsAggregator.class);
    
	private transient HazelcastInstance hzInstance;

	@Override
	public int getId() {
		return cli_XDMAggregateSchemaInfoTask;
	}

	@Override
	public int getFactoryId() {
		return factoryId;
	}

	
    @Autowired
	public void setHzInstance(HazelcastInstance hzInstance) {
		this.hzInstance = hzInstance;
		logger.trace("setHzInstance; got instance: {}", hzInstance); 
	}
    
	@Override
	public Long call() throws Exception {
		
		IMap<Long, XDMDocument> xddCache = hzInstance.getMap(CN_XDM_DOCUMENT);
		IMap<XDMDataKey, XDMElement> xdmCache = hzInstance.getMap(CN_XDM_ELEMENT);
		
    	LocalMapStats stats = xddCache.getLocalMapStats();
    	long size = stats.getBackupEntryMemoryCost() + stats.getOwnedEntryMemoryCost();
    	stats = xdmCache.getLocalMapStats();
    	size += stats.getBackupEntryMemoryCost() + stats.getOwnedEntryMemoryCost();
        return size; 
    }
    
	@Override
	public void readData(ObjectDataInput in) throws IOException {
		// nothing to read
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		// nothing to write
	}

}




