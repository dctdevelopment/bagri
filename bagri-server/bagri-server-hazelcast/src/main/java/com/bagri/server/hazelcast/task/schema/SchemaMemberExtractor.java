package com.bagri.server.hazelcast.task.schema;

import static com.bagri.server.hazelcast.serialize.DataSerializationFactoryImpl.cli_ExtractSchemaMemberTask;
import static com.bagri.server.hazelcast.util.HazelcastUtils.findSchemaInstance;

import java.util.concurrent.Callable;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;

//@SpringAware
public class SchemaMemberExtractor extends SchemaProcessingTask implements Callable<String> { 
	
	public SchemaMemberExtractor() {
		super();
	}

	public SchemaMemberExtractor(String schemaName) {
		super(schemaName);
	}

	@Override
	public String call() throws Exception {
    	logger.trace("call.enter; schema: {}", schemaName);
		HazelcastInstance hz = findSchemaInstance(schemaName);
    	Member member = hz.getCluster().getLocalMember();
		String result = member.getUuid();
    	logger.trace("call.exit; returning: {} for member: {}", result, member);
		return result;
	}

	@Override
	public int getId() {
		return cli_ExtractSchemaMemberTask;
	}

}
