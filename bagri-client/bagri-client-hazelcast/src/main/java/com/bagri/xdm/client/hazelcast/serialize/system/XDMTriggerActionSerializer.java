package com.bagri.xdm.client.hazelcast.serialize.system;

import java.io.IOException;

import com.bagri.xdm.client.hazelcast.serialize.DataSerializationFactoryImpl;
import com.bagri.xdm.system.XDMTriggerAction;
import com.bagri.xdm.system.XDMTriggerAction.Order;
import com.bagri.xdm.system.XDMTriggerAction.Scope;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

public class XDMTriggerActionSerializer implements StreamSerializer<XDMTriggerAction> {

	@Override
	public void destroy() {
		// no-op
	}

	@Override
	public int getTypeId() {
		return DataSerializationFactoryImpl.cli_XDMTriggerAction;
	}

	@Override
	public XDMTriggerAction read(ObjectDataInput in) throws IOException {
		XDMTriggerAction xAction = new XDMTriggerAction(
				Order.values()[in.readInt()],
				Scope.values()[in.readInt()]);
		return xAction;
	}

	@Override
	public void write(ObjectDataOutput out, XDMTriggerAction xAction) throws IOException {
		out.writeInt(xAction.getOrder().ordinal());
		out.writeInt(xAction.getScope().ordinal());
	}


}