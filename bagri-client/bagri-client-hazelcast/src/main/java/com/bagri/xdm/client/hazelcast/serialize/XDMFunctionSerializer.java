package com.bagri.xdm.client.hazelcast.serialize;

import java.io.IOException;

import com.bagri.xdm.system.XDMFunction;
import com.bagri.xdm.system.XDMParameter;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

public class XDMFunctionSerializer implements StreamSerializer<XDMFunction> {

	@Override
	public void destroy() {
	}

	@Override
	public int getTypeId() {
		return XDMDataSerializationFactory.cli_XDMFunction;
	}

	@Override
	public XDMFunction read(ObjectDataInput in) throws IOException {
		XDMFunction xFunc = new XDMFunction(
				in.readUTF(),
				in.readUTF(),
				in.readUTF(),
				in.readUTF());
		int cnt = in.readInt();
		for (int i=0; i < cnt; i++) {
			XDMParameter xp = in.readObject();
			xFunc.getParameters().add(xp);
		}
		return xFunc;
	}

	@Override
	public void write(ObjectDataOutput out, XDMFunction xFunc) throws IOException {
		out.writeUTF(xFunc.getClassName());
		out.writeUTF(xFunc.getMethod());
		out.writeUTF(xFunc.getResultClass());
		out.writeUTF(xFunc.getDescription());
		out.writeInt(xFunc.getParameters().size());
		for (XDMParameter xp: xFunc.getParameters()) {
			out.writeObject(xp);
		}
	}

}