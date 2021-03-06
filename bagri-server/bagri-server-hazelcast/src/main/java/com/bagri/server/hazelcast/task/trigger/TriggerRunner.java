package com.bagri.server.hazelcast.task.trigger;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bagri.core.model.Document;
import com.bagri.core.system.TriggerAction;
import com.bagri.core.system.TriggerAction.Order;
import com.bagri.core.system.TriggerAction.Scope;
import com.bagri.server.hazelcast.impl.TriggerManagementImpl;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.spring.context.SpringAware;

import static com.bagri.client.hazelcast.serialize.DataSerializationFactoryImpl.factoryId;
import static com.bagri.server.hazelcast.serialize.DataSerializationFactoryImpl.cli_RunTriggerTask;

@SpringAware
public class TriggerRunner implements Callable<Void>, IdentifiedDataSerializable { //Runnable

	private static final transient Logger logger = LoggerFactory.getLogger(TriggerRunner.class);

	private Order order;
	private Scope scope;
	private int index;
	private Document xDoc;
	private String clientId;
	private TriggerManagementImpl trManager;

	public TriggerRunner() {
		// for de-ser
	}
	
	public TriggerRunner(Order order, Scope scope, int index, Document xDoc, String clientId) {
		this.order = order;
		this.scope = scope;
		this.index = index;
		this.xDoc = xDoc;
		this.clientId = clientId;
	}
		
    @Autowired
	public void setTriggerManager(TriggerManagementImpl trManager) {
    	this.trManager = trManager;
    }
    
	@Override
	public Void call() {
		try {
			trManager.runTrigger(order, scope, xDoc, index, clientId);
		} catch (Throwable ex) {
			logger.error("runTrigger.error", ex);
		}
		return null;
	}
	
	@Override
	public int getFactoryId() {
		return factoryId;
	}

	@Override
	public int getId() {
		return cli_RunTriggerTask;
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		order = Order.values()[in.readInt()];
		scope = Scope.values()[in.readInt()];
		index = in.readInt();
		xDoc = in.readObject();
		clientId = in.readUTF();
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		out.writeInt(order.ordinal());
		out.writeInt(scope.ordinal());
		out.writeInt(index);
		out.writeObject(xDoc);
		out.writeUTF(clientId);
	}

}

