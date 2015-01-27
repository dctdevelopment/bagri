package com.bagri.xdm.cache.hazelcast.management;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.xml.xquery.XQConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.naming.SelfNaming;

import com.bagri.common.manage.JMXUtils;
import com.bagri.xdm.api.XDMModelManagement;

public abstract class SchemaFeatureManagement implements SelfNaming {
	
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected String schemaName;
	protected XDMModelManagement schemaDictionary;
	
    public SchemaFeatureManagement(String schemaName) {
    	this.schemaName = schemaName;
    }

	@ManagedAttribute(description="Returns corresponding Schema name")
	public String getSchema() {
		return schemaName;
	}
	
	public void setSchemaDictionary(XDMModelManagement schemaDictionary) {
		this.schemaDictionary = schemaDictionary;
	}
	
	protected abstract String getFeatureKind();

	@Override
	public ObjectName getObjectName() throws MalformedObjectNameException {
		return JMXUtils.getObjectName("type=Schema,name=" + schemaName + ",kind=" + getFeatureKind());
	}

}