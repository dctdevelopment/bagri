package com.bagri.xdm.cache.hazelcast.task.role;

import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bagri.xdm.cache.hazelcast.task.EntityProcessor;
import com.bagri.xdm.system.Role;
import com.hazelcast.map.EntryBackupProcessor;
import com.hazelcast.map.EntryProcessor;

public abstract class RoleProcessor extends EntityProcessor implements EntryProcessor<String, Role>, 
	EntryBackupProcessor<String, Role> {
	
	protected final transient Logger logger = LoggerFactory.getLogger(getClass());

	public RoleProcessor() {
		//
	}

	public RoleProcessor(int version, String admin) {
		super(version, admin);
	}

	@Override
	public void processBackup(Entry<String, Role> entry) {
		process(entry);		
	}

	@Override
	public EntryBackupProcessor<String, Role> getBackupProcessor() {
		return this;
	}

}
