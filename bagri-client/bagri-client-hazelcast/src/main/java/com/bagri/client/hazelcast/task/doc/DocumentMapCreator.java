package com.bagri.client.hazelcast.task.doc;

import static com.bagri.client.hazelcast.serialize.DataSerializationFactoryImpl.cli_CreateMapDocumentTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import com.bagri.core.model.Document;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

public class DocumentMapCreator extends DocumentAwareTask implements Callable<Document> {
	
	protected Map<String, Object> fields;

	public DocumentMapCreator() {
		super();
	}

	public DocumentMapCreator(String clientId, long txId, String uri, Properties props, Map<String, Object> fields) {
		super(clientId, txId, uri, props);
		this.fields = fields;
	}

	@Override
	public Document call() throws Exception {
		return null;
	}

	@Override
	public int getId() {
		return cli_CreateMapDocumentTask; 
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		super.readData(in);
		int size = in.readInt();
		fields = new HashMap<>(size);
		for (int i=0; i < size; i++) {
			fields.put(in.readUTF(), in.readObject());
		}
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		super.writeData(out);
		out.writeInt(fields.size());
		for (Map.Entry<String, Object> field: fields.entrySet()) {
			out.writeUTF(field.getKey());
			out.writeObject(field.getValue());
		}
	}

}
