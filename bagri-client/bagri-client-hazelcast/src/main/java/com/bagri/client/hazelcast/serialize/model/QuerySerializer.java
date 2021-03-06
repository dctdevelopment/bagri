package com.bagri.client.hazelcast.serialize.model;

import java.io.IOException;

import com.bagri.client.hazelcast.serialize.DataSerializationFactoryImpl;
import com.bagri.core.model.Query;
import com.bagri.core.query.QueryBuilder;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

public class QuerySerializer implements StreamSerializer<Query> {

	@Override
	public int getTypeId() {
		return DataSerializationFactoryImpl.cli_XDMQuery;
	}

	@Override
	public void destroy() {
	}

	@Override
	public Query read(ObjectDataInput in) throws IOException {
		return new Query(in.readUTF(),
				in.readBoolean(),
				(QueryBuilder) in.readObject());
	}

	@Override
	public void write(ObjectDataOutput out, Query xQuery) throws IOException {
		out.writeUTF(xQuery.getQuery());
		out.writeBoolean(xQuery.isReadOnly());
		out.writeObject(xQuery.getXdmQuery());
	}

}
