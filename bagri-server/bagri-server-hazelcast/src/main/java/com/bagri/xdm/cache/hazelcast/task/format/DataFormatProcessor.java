package com.bagri.xdm.cache.hazelcast.task.format;

import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bagri.xdm.cache.hazelcast.task.EntityProcessor;
import com.bagri.xdm.system.DataFormat;
import com.hazelcast.map.EntryBackupProcessor;
import com.hazelcast.map.EntryProcessor;

public abstract class DataFormatProcessor extends EntityProcessor implements EntryProcessor<String, DataFormat>, 
	EntryBackupProcessor<String, DataFormat> {
		
	protected final transient Logger logger = LoggerFactory.getLogger(getClass());
	
	public DataFormatProcessor() {
		//
	}
	
	public DataFormatProcessor(int version, String admin) {
		super(version, admin);
	}
	
	@Override
	public void processBackup(Entry<String, DataFormat> entry) {
		process(entry);		
	}
	
	@Override
	public EntryBackupProcessor<String, DataFormat> getBackupProcessor() {
		return this;
	}

}
