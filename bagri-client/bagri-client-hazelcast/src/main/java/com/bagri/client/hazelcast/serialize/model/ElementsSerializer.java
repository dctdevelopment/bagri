package com.bagri.client.hazelcast.serialize.model;

import java.io.IOException;
import java.util.Collection;

import com.bagri.client.hazelcast.serialize.DataSerializationFactoryImpl;
import com.bagri.core.model.Element;
import com.bagri.core.model.Elements;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

public class ElementsSerializer implements StreamSerializer<Elements> {

	@Override
	public void destroy() {
	}

	@Override
	public int getTypeId() {
		return DataSerializationFactoryImpl.cli_XDMElements;
	}

	@Override
	public Elements read(ObjectDataInput in) throws IOException {
		
		int pathId = in.readInt();
		Elements xelts = new Elements(pathId, null);
		int size = in.readInt();
		for (int i=0; i < size; i++) {
			xelts.addElement((Element) in.readObject());
		}
		return xelts;
	}

	@Override
	public void write(ObjectDataOutput out, Elements xelts) throws IOException {
		
		Collection<Element> elements = xelts.getElements();
		out.writeInt(xelts.getPathId());
		out.writeInt(elements.size());
		for (Element element: elements) {
			out.writeObject(element);
		}
	}


}
