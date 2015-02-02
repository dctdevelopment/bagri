package com.bagri.xdm.process.hazelcast;

import static com.bagri.xdm.client.hazelcast.impl.DocumentManagementImpl.*;
import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bagri.xdm.api.test.XDMModelManagementTest;
import com.bagri.xdm.client.hazelcast.impl.DocumentManagementImpl;
import com.bagri.xdm.client.hazelcast.impl.RepositoryImpl;
import com.hazelcast.core.Hazelcast;

public class ModelManagementTest extends XDMModelManagementTest {
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		//System.setProperty("hazelcast.config", "hazelcast/hazelcast.xml");
		//System.setProperty(PN_SERVER_ADDRESS, "localhost:10500");
		//System.setProperty(PN_POOL_SIZE, "10");
		//System.setProperty(PN_SCHEMA_NAME, "TPoX2");
		//System.setProperty(PN_SCHEMA_PASS, "TPoX2");
		sampleRoot = "..\\..\\etc\\samples\\tpox\\";
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		Hazelcast.shutdownAll();
	}

	@Before
	public void setUp() throws Exception {
		xRepo = new RepositoryImpl();
		mDictionary = xRepo.getModelManagement();

		registerSecuritySchemaTest();
		registerCustaccSchemaTest();
		//registerCommonSchemaTest();
	}

	@After
	public void tearDown() throws Exception {
		xRepo.close();
	}

}