package com.bagri.xdm.cache.hazelcast.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import com.bagri.xdm.api.XDMAccessManagement;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class AccessManagementImpl implements XDMAccessManagement, InitializingBean {

	private static final transient Logger logger = LoggerFactory.getLogger(AccessManagementImpl.class);
	
	private String schemaName;
	private String schemaPass;
	private AccessManagementBridge bridge;

	@Override
	public void afterPropertiesSet() throws Exception {
		HazelcastInstance sysInstance = Hazelcast.getHazelcastInstanceByName("hzInstance");
		ApplicationContext context = (ApplicationContext) sysInstance.getUserContext().get("context");
		bridge = context.getBean(AccessManagementBridge.class);
		logger.trace("afterPropertiesSet; got bridge: {}", bridge);
	}
	
	public String getSchemaName() {
		return schemaName;
	}
	
	public String getSchemaPass() {
		return schemaPass;
	}
	
	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}
	
	public void setSchemaPass(String schemaPass) {
		this.schemaPass = schemaPass;
	}
	
	@Override
	public boolean authenticate(String username, String password) {
		Boolean result = null;
		if (bridge != null) {
			result = bridge.authenticate(schemaName, username, password);
		}
		// TODO: do we need this check any more?
		if (result == null) {
			// encrypt pwd..
			if (username.equals(schemaName) && password.equals(schemaPass)) {
				return true;
			}
		}
		return false;
	}

	
}
