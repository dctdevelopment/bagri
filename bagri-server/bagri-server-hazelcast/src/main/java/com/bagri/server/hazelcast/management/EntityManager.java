package com.bagri.server.hazelcast.management;

import static com.bagri.core.Constants.pn_cluster_login;

import java.util.Date;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.naming.SelfNaming;

import com.bagri.core.system.Entity;
import com.bagri.support.util.JMXUtils;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;

public abstract class EntityManager<E extends Entity> implements SelfNaming {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

	protected String entityName;
    protected HazelcastInstance hzInstance;
	//private IExecutorService execService;
	
	//@Autowired
    protected IMap<String, E> entityCache;
    
	public EntityManager() {
		//..
	}

	public EntityManager(HazelcastInstance hzInstance, String entityName) {
		this.hzInstance = hzInstance;
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
	
	//public void setHzInstance(HazelcastInstance hzInstance) {
	//	this.hzInstance = hzInstance;
	//}
	
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
	
	protected String getCurrentUser() {
		return JMXUtils.getCurrentUser(((Member) hzInstance.getLocalEndpoint()).getStringAttribute(pn_cluster_login));
	}

}
