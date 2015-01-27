package com.bagri.xdm.cache.hazelcast.management;

import java.util.Date;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.naming.SelfNaming;

import com.bagri.common.manage.JMXUtils;
import com.bagri.xdm.common.XDMEntity;
import com.hazelcast.core.IMap;

public abstract class EntityManager<E extends XDMEntity> implements SelfNaming {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

	protected String entityName;
    //private HazelcastInstance hzInstance;
	//private IExecutorService execService;
	
	//@Autowired
    protected IMap<String, E> entityCache;
    
	public EntityManager() {
		//..
	}

	public EntityManager(String entityName) {
		this.entityName = entityName;
	}

	protected E getEntity() {
		return entityCache.get(entityName);
	}
	
	protected void flushEntity(E entity) {
		entityCache.set(entityName, entity);
	}
	
	public void setEntityCache(IMap<String, E> entityCache) {
		this.entityCache = entityCache;
	}
	
	public String getEntityName() {
		return entityName;
	}
	
	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}
	
	public int getVersion() {
		return getEntity().getVersion();
	}

	public Date getCreatedAt() {
		return getEntity().getCreatedAt();
	}

	public String getCreatedBy() {
		return getEntity().getCreatedBy();
	}
	
	@Override
	public ObjectName getObjectName() throws MalformedObjectNameException {
		return JMXUtils.getObjectName(getEntityType(), entityName);
	}
	
	protected abstract String getEntityType();


}
