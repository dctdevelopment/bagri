package com.bagri.server.hazelcast.impl;

import static com.bagri.server.hazelcast.util.HazelcastUtils.findSystemContext;
import static com.bagri.support.security.Encryptor.encrypt;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import com.bagri.core.api.AccessManagement;
import com.bagri.core.system.Permission;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class AccessManagementImpl implements AccessManagement, InitializingBean {

	private static final transient Logger logger = LoggerFactory.getLogger(AccessManagementImpl.class);
	
	private String schemaName;
	private String schemaPass;
	private AccessManagementBridge bridge;

	@Override
	public void afterPropertiesSet() throws Exception {
		ApplicationContext context = findSystemContext();
		if (context != null) {
			bridge = context.getBean(AccessManagementBridge.class);
			logger.trace("afterPropertiesSet; got bridge: {}", bridge);
		}
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
		password = encrypt(password);
		if (bridge != null) {
			result = bridge.authenticate(schemaName, username, password);
		}
		// TODO: do we need this check any more?
		if (result == null) {
			result = username.equals(schemaName) && password.equals(schemaPass);
		}
		return result;
	}

	@Override
	public boolean hasPermission(String username, Permission.Value perm) {
		if (bridge != null) {
			Boolean result = bridge.hasPermission(schemaName, username, perm);
			if (result != null) {
				return result;  
			}
		}
		return false;
	}
	
}
