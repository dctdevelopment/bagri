package com.bagri.client.hazelcast.serialize.query;

import static com.bagri.support.util.CollectionUtils.*;

import java.io.IOException;

import com.bagri.client.hazelcast.serialize.DataSerializationFactoryImpl;
import com.bagri.core.query.QueriedPath;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

public class QueriedPathSerializer implements StreamSerializer<QueriedPath> {

	@Override
	public void destroy() {
	}

	@Override
	public int getTypeId() {
		return DataSerializationFactoryImpl.cli_QueriedPath;
	}

	@Override
	public QueriedPath read(ObjectDataInput in) throws IOException {
		int dataType = in.readInt();
		boolean indexed = in.readBoolean();
		int[] pids = in.readIntArray();
		return new QueriedPath(dataType, indexed, toIntList(pids));
	}

	@Override
	public void write(ObjectDataOutput out, QueriedPath path) throws IOException {
		out.writeInt(path.getDataType());
		out.writeBoolean(path.isIndexed());
		out.writeIntArray(toIntArray(path.getPathIds()));
	}

}
