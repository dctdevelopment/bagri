package com.bagri.xdm.cache.hazelcast.management;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.naming.SelfNaming;

import com.bagri.common.manage.JMXUtils;
import com.bagri.common.util.FileUtils;
import com.bagri.xdm.access.api.XDMCacheConstants;
import com.bagri.xdm.access.api.XDMDocumentManagementBase;
import com.bagri.xdm.access.api.XDMSchemaDictionary;
import com.bagri.xdm.access.hazelcast.impl.DocumentManagementClient;
import com.bagri.xdm.domain.XDMDocument;
import com.bagri.xdm.process.hazelcast.DocumentManagementServer;
import com.bagri.xdm.process.hazelcast.DocumentStatsCollector;
import com.bagri.xdm.process.hazelcast.DocumentStatsReseter;
import com.bagri.xdm.process.hazelcast.schema.SchemaStatsAggregator;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.Member;

@ManagedResource(description="Schema Documents Management MBean")
public class DocumentManagement implements SelfNaming {

    private static final transient Logger logger = LoggerFactory.getLogger(DocumentManagement.class);
    private static final String type_schema = "Schema";
    
	private DocumentManagementClient docManager;
	private XDMSchemaDictionary schemaDictionary;
	private IExecutorService execService;
    
    private String schemaName;
    
    public DocumentManagement(String schemaName) {
    	this.schemaName = schemaName;
    }

	public void setExecService(IExecutorService execService) {
		this.execService = execService;
	}
	
	public void setDocumentManager(DocumentManagementClient docManager) {
		this.docManager = docManager;
	}
	
	public void setSchemaDictionary(XDMSchemaDictionary schemaDictionary) {
		this.schemaDictionary = schemaDictionary;
	}
	
	@ManagedAttribute(description="Returns corresponding Schema name")
	public String getSchema() {
		return schemaName;
	}
    
	@ManagedAttribute(description="Returns Schema size in documents")
	public Integer getDocumentCount() {
		return docManager.getXddSize(); 
	}
    
	@ManagedAttribute(description="Returns Schema size in documents, per document type")
	public CompositeData getTypedDocumentCount() {
		Map<Integer, Integer> counts = null; //((HazelcastDocumentServer) docManager).getTypeDocuments();
		return null;
	}
    
	@ManagedAttribute(description="Returns Schema size in elements")
	public Integer getElementCount() {
		return docManager.getXdmSize(); 
	}
    
	@ManagedAttribute(description="Returns Schema size in elements, per document type")
	public CompositeData getTypedElementCount() {
		Map<Integer, Integer> counts = null; //((HazelcastDocumentServer) docManager).getTypeElements();
		return null;
	}
    
	@ManagedAttribute(description="Returns Schema size in bytes")
	public Long getSchemaSize() {
		//return docManager.getSchemaSize(); 
	
		long stamp = System.currentTimeMillis();
		logger.trace("getSchemaSize.enter;");
		
		SchemaStatsAggregator task = new SchemaStatsAggregator();
		Map<Member, Future<Long>> results = execService.submitToAllMembers(task);
		long fullSize = 0;
		for (Map.Entry<Member, Future<Long>> entry: results.entrySet()) {
			try {
				Long size = entry.getValue().get();
				fullSize += size;
			} catch (InterruptedException | ExecutionException ex) {
				logger.error("getSchemaSize.error; ", ex);
			}
		}
		stamp = System.currentTimeMillis() - stamp;
		logger.trace("getSchemaSize.exit; returning: {}; timeTaken: {}", fullSize, stamp);
    	return fullSize;
	}
    
	@ManagedAttribute(description="Returns Schema size in bytes, per document type")
	public CompositeData getTypedSchemaSize() {
		Map<Integer, Long> counts = null; //((HazelcastDocumentServer) docManager).getTypeSchemaSize();
		return null;
	}
	
	@ManagedOperation(description="Returns Document XML")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "docId", description = "Internal Document identifier")})
	public String getDocumentXML(long docId) {
		//
		return docManager.getDocumentAsString(docId);
	}

    
	@ManagedOperation(description="Register Document")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "docFile", description = "A full path to XML file to register")})
	public int registerDocument(String docFile) {
		
		String uri = "file:///" + docFile;
		try {
			String xml = FileUtils.readTextFile(docFile);
			XDMDocument doc = docManager.storeDocument(xml); //(uri, xml);
			return 1;
		} catch (IOException ex) {
			logger.error("registerDocument.error: " + ex.getMessage(), ex);
		}
		return 0;
	}
	
	private int processFilesInCatalog(File catalog) {
		int result = 0;
	    for (File file: catalog.listFiles()) {
	        if (file.isDirectory()) {
	            result += processFilesInCatalog(file);
	        } else {
	            result += registerDocument(file.getPath());
	        }
	    }
	    return result;
	}

	@ManagedOperation(description="Register Documents")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "docCatalog", description = "A full path to the directory containing XML files to register")})
	public int registerDocuments(String docCatalog) {
		File catalog = new File(docCatalog);
		return processFilesInCatalog(catalog);	
	}

	@Override
	public ObjectName getObjectName() throws MalformedObjectNameException {
		return JMXUtils.getObjectName("type=" + type_schema + ",name=" + schemaName + ",kind=DocumentManagement");
	}

	@ManagedAttribute(description="Returns aggregated DocumentManagement invocation statistics, per method")
	public TabularData getInvocationStatistics() {
		DocumentStatsCollector task = new DocumentStatsCollector(); 
		logger.trace("getInvocationStatistics.enter; going to collect stats for schema: {}", schemaName);

		int cnt = 0;
		TabularData result = null;
		Map<Member, Future<TabularData>> futures = execService.submitToAllMembers(task);
		for (Map.Entry<Member, Future<TabularData>> entry: futures.entrySet()) {
			try {
				TabularData stats = entry.getValue().get();
				result = JMXUtils.aggregateStats(stats, result);
			} catch (InterruptedException | ExecutionException ex) {
				logger.error("getInvocationStatistics.error: " + ex.getMessage(), ex);
			}
		}
		logger.trace("getInvocationStatistics.exit; got stats from {} nodes", cnt);
		return result;
	}
	
	//private TabularData aggregateStats(TabularData source, TabularData target) {
		//
	//	return target;
	//}
    
	@ManagedOperation(description="Reset DocumentManagement invocation statistics")
	public void resetStatistics() {
		//
		DocumentStatsReseter task = new DocumentStatsReseter(); 
		logger.trace("resetStatistics.enter; going to reset stats for schema: {}", schemaName);

		int cnt = 0;
		Map<Member, Future<Boolean>> futures = execService.submitToAllMembers(task);
		for (Map.Entry<Member, Future<Boolean>> entry: futures.entrySet()) {
			try {
				if (entry.getValue().get()) {
					cnt++;
				}
			} catch (InterruptedException | ExecutionException ex) {
				logger.error("resetStatistics.error: " + ex.getMessage() + " on member " + entry.getKey(), ex);
			}
		}
		logger.trace("resetStatistics.exit; reset stats on {} nodes", cnt);
	}
}