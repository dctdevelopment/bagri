package com.bagri.xdm.cache.hazelcast.task.stats;

import static com.bagri.xdm.client.hazelcast.serialize.XDMDataSerializationFactory.factoryId;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.bagri.common.manage.InvocationStatistics;
import com.bagri.xdm.cache.hazelcast.util.SpringContextHolder;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

public abstract class InvocationStatsTask implements IdentifiedDataSerializable {

	protected final transient Logger logger = LoggerFactory.getLogger(getClass());

	//protected transient InvocationStatistics xdmStats;
	
	private String schemaName;
	private String statsName;
    
	public InvocationStatsTask() {
		// de-serialize
	}
	
	public InvocationStatsTask(String schemaName, String statsName) {
		this.schemaName = schemaName;
		this.statsName = statsName;
	}

	@Override
	public int getFactoryId() {
		return factoryId;
	}

    //@Autowired
    //@Qualifier("docStats")
	//public void setXdmStats(InvocationStatistics xdmStats) {
	//	this.xdmStats = xdmStats;
	//	logger.trace("setXdmStats; got statistics: {}", xdmStats); 
	//}
	
	protected InvocationStatistics getStats() {
		ApplicationContext ctx = (ApplicationContext) SpringContextHolder.getContext(schemaName, "appContext");
		InvocationStatistics stats = ctx.getBean(statsName, InvocationStatistics.class); 
		logger.trace("getStats; returning: {}, for name: {}", stats, statsName);
		return stats;
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		schemaName = in.readUTF();
		statsName = in.readUTF();
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		out.writeUTF(schemaName);
		out.writeUTF(statsName);
	}

}