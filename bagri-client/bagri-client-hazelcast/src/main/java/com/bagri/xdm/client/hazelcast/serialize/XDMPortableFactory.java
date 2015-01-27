package com.bagri.xdm.client.hazelcast.serialize;

import com.bagri.xdm.client.hazelcast.impl.SecureCredentials;
import com.bagri.xdm.client.hazelcast.task.doc.DocumentBuilder;
import com.bagri.xdm.client.hazelcast.task.doc.DocumentCreator;
import com.bagri.xdm.client.hazelcast.task.doc.DocumentRemover;
import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableFactory;

public class XDMPortableFactory implements PortableFactory {
	
	public static final int factoryId = 1; 
	
	public static final int cli_XDMRole = 1;
	public static final int cli_XDMUser = 2;
	public static final int cli_XDMNode = 3;
	public static final int cli_XDMSchema = 4;
	public static final int cli_XDMPermission = 5;
	public static final int cli_XDMIndex = 6;
	
	public static final int cli_XDMDocument = 50;
	public static final int cli_XDMElement = 51; 
	public static final int cli_DataDocumentKey = 52; 
	public static final int cli_XDMDocumentType = 53;
	public static final int cli_XDMPath = 54;
	public static final int cli_XDMNamespace = 55;
	public static final int cli_XDMElements = 56; 
	public static final int cli_XDMQuery = 57;
	public static final int cli_XDMResults = 58;
	public static final int cli_XDMSource = 59;
	public static final int cli_XDMIndexedValue = 60;

	public static final int cli_XQItemType = 75;
	public static final int cli_XQItem = 76;
	public static final int cli_XQSequence = 77;

	public static final int cli_XDMCredentials = 79;
	
	public static final int cli_Expression = 80;
	public static final int cli_ExpressionBuilder = 81;
	public static final int cli_PathBuilder = 82;
	public static final int cli_ExpressionContainer = 83;
	//public static final int cli_StructuredQName = 85;
	
	@Override
	public Portable create(int classId) {
		switch (classId) {
			//case cli_XDMDocument: return new XDMDocumentPortable();
			//case cli_XDMData: return new XDMDataPortable();
			//case cli_DataDocumentKey: return new DataDocumentKey();
			case cli_XDMCredentials: return new SecureCredentials();
			//case cli_XDMDocumentTask: return new DocumentCreator();
			//case cli_TemplateResultTask: return new DocumentBuilder();
			//case cli_XDMDocumentRemover: return new DocumentRemover();

			//case cli_XDMInitSchemaTask: return new SchemaInitiator();
			//case cli_XDMDenitSchemaTask: return new SchemaDenitiator();
			//case cli_XDMSetNodeOptionTask: return new NodeOptionSetter();
		}
		return null;
	}
	
}