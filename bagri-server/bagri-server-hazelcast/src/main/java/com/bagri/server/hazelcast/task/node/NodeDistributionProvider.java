package com.bagri.server.hazelcast.task.node;

import static com.bagri.core.Constants.ctx_context;
import static com.bagri.client.hazelcast.serialize.DataSerializationFactoryImpl.factoryId;
import static com.bagri.server.hazelcast.serialize.DataSerializationFactoryImpl.cli_GetNodeStatsTask;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.bagri.client.hazelcast.PartitionStatistics;
import com.bagri.server.hazelcast.impl.PartitionManagementService;
import com.bagri.server.hazelcast.impl.PopulationManagementImpl;
import com.bagri.server.hazelcast.impl.SchemaRepositoryImpl;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.spring.context.SpringAware;

@SpringAware
public class NodeDistributionProvider implements Callable<Collection<PartitionStatistics>>, IdentifiedDataSerializable {

	private HazelcastInstance hzInstance;
	private transient SchemaRepositoryImpl xdmRepo;
    
    @Autowired
	public void setHazelcastInstance(HazelcastInstance hzInstance) {
		this.hzInstance = hzInstance;
	}
	
    @Autowired
	public void setXDMRepository(SchemaRepositoryImpl xdmRepo) {
		this.xdmRepo = xdmRepo;
	}

	@Override
	public Collection<PartitionStatistics> call() throws Exception {
		ApplicationContext ctx = (ApplicationContext) hzInstance.getUserContext().get(ctx_context);
		PartitionManagementService svc = ctx.getBean(PartitionManagementService.class);
		return svc.getPartitionStatistics();
		//return ((PopulationManagementImpl) xdmRepo.getPopulationManagement()).getPartitionStatistics();
	}

	@Override
	public int getFactoryId() {
		return factoryId;
	}

	@Override
	public int getId() {
		return cli_GetNodeStatsTask;
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		// TODO nothing to read
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		// nothing to write
	}


}
