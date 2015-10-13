package com.bagri.xdm.client.hazelcast.serialize;

import static com.bagri.common.util.CollectionUtils.*; 

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bagri.common.util.CollectionUtils;
import com.bagri.xdm.domain.XDMResults;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

public class XDMResultsSerializer implements StreamSerializer<XDMResults> {

	@Override
	public void destroy() {
	}

	@Override
	public int getTypeId() {
		return XDMDataSerializationFactory.cli_XDMResults;
	}

	@Override
	public XDMResults read(ObjectDataInput in) throws IOException {
		int size = in.readInt();
		Map<String, Object> params = new HashMap<String, Object>(size);
		for (int i=0; i < size; i++) {
			params.put(in.readUTF(), in.readObject());
		}
		List<Long> docIds = toLongList(in.readLongArray());
		size = in.readInt();
		List results = new ArrayList(size);
		for (int i=0; i < size; i++) {
			results.add(in.readObject());
		}
		return new XDMResults(params, docIds, results);
	}

	@Override
	public void write(ObjectDataOutput out, XDMResults xreslts) throws IOException {
		out.writeInt(xreslts.getParams().size());
		for (Map.Entry e: xreslts.getParams().entrySet()) {
			out.writeUTF(e.getKey().toString());
			out.writeObject(e.getValue());
		}
		out.writeLongArray(toLongArray(xreslts.getDocIds()));
		out.writeInt(xreslts.getResults().size());
		for (Object o: xreslts.getResults()) {
			out.writeObject(o);
		}
	}

}
