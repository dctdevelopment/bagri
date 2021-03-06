package com.bagri.server.hazelcast.predicate;

import static com.bagri.client.hazelcast.serialize.DataSerializationFactoryImpl.cli_CollectionPredicate;
import static com.bagri.client.hazelcast.serialize.DataSerializationFactoryImpl.factoryId;

import java.io.IOException;
import java.util.Map.Entry;

import com.bagri.core.DocumentKey;
import com.bagri.core.model.Document;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.query.Predicate;

public class CollectionPredicate implements Predicate<DocumentKey, Document>, IdentifiedDataSerializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int clnId;
	
	public CollectionPredicate() {
		// for de-serialization
	}
	
	public CollectionPredicate(int clnId) {
		this.clnId = clnId;
	}

	@Override
	public int getFactoryId() {
		return factoryId;
	}
	
	@Override
	public int getId() {
		return cli_CollectionPredicate;
	}

	@Override
	public boolean apply(Entry<DocumentKey, Document> docEntry) {
		Document doc = docEntry.getValue();
		// will perform this check later anyway..
		return doc.hasCollection(clnId); // && doc.getTxFinish() == 0;
	}
	
	@Override
	public void readData(ObjectDataInput in) throws IOException {
		clnId = in.readInt();
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		out.writeInt(clnId);
	}

}
