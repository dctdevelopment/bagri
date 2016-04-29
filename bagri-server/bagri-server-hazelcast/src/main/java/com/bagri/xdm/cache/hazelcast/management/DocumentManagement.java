package com.bagri.xdm.cache.hazelcast.management;

import static com.bagri.common.util.PropUtils.propsFromString;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.bagri.common.manage.JMXUtils;
import com.bagri.common.manage.StatsAggregator;
import com.bagri.common.util.FileUtils;
import com.bagri.xdm.api.XDMDocumentManagement;
import com.bagri.xdm.api.XDMException;
import com.bagri.xdm.cache.hazelcast.task.doc.DocumentStructureProvider;
import com.bagri.xdm.cache.hazelcast.task.schema.SchemaDocCleaner;
import com.bagri.xdm.cache.hazelcast.task.stats.StatisticSeriesCollector;
import com.bagri.xdm.cache.hazelcast.task.stats.StatisticTotalsCollector;
import com.bagri.xdm.cache.hazelcast.task.stats.StatisticsReseter;
import com.bagri.xdm.domain.XDMDocument;
import com.bagri.xdm.system.XDMCollection;
import com.bagri.xdm.system.XDMSchema;
import com.hazelcast.core.Member;

@ManagedResource(description="Schema Documents Management MBean")
public class DocumentManagement extends SchemaFeatureManagement {

	private CompositeData docTotals = null;
	private XDMDocumentManagement docManager;
    
    public DocumentManagement(String schemaName) {
    	super(schemaName);
    }

	@Override
	protected String getFeatureKind() {
		return "DocumentManagement";
	}

    @Override
	public void setSchemaManager(SchemaManager schemaManager) {
    	super.setSchemaManager(schemaManager);
		this.docManager = schemaManager.getRepository().getDocumentManagement();
    }

	private void initAggregator() {
		if (aggregator == null) {
			aggregator = new StatsAggregator() {

				@Override
				public Object[] aggregateStats(Object[] source, Object[] target) {
					target[2] = source[2]; // collection
					target[3] = (Long) source[3] + (Long) target[3]; // size
					target[4] = (Integer) source[4] + (Integer) target[4]; // number of docs  
					target[5] = (Integer) source[5] + (Integer) target[5]; // number of elts
					target[6] = (Integer) source[6] + (Integer) target[6]; // number of fragments
					double size = (Long) target[3]; 
					target[0] = size/(Integer) target[4]; // avg in bytes
					size = (Integer) target[5]; 
					target[1] = size/(Integer) target[4]; // avg in elts  
					return target;
				}
				
			};
		}
	}
	
	@ManagedAttribute(description="Returns Schema Totals")
	public CompositeData getTotalCounts() {
		collectTotals();
		return docTotals;
	}
	
	private void collectTotals() {
		initAggregator();
		docTotals = super.getTotalsStatistics(new StatisticTotalsCollector(schemaName, "docStats"), aggregator);
	}
	
	@ManagedAttribute(description="Returns Schema size in documents")
	public Integer getDocumentCount() {
		collectTotals();
		return (Integer) docTotals.get("Number of documents"); 
	}
    
	@ManagedAttribute(description="Returns Schema size in elements")
	public Integer getElementCount() {
		if (docTotals == null) {
			collectTotals();
		}
		return (Integer) docTotals.get("Number of elements"); 
	}
    
	@ManagedAttribute(description="Returns Schema size in bytes")
	public Long getSchemaSize() {
		if (docTotals == null) {
			collectTotals();
		}
		return (Long) docTotals.get("Consumed size"); 
	}
    
	@ManagedOperation(description="Return Document Elements")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "uri", description = "Document identifier")})
	public CompositeData getDocumentElements(String uri) {
		//
		//docManager.
		DocumentStructureProvider task = new DocumentStructureProvider(null, uri); 
		Future<CompositeData> result = execService.submitToKeyOwner(task, uri); 
		try {
			return result.get();
		} catch (InterruptedException | ExecutionException ex) {
			logger.error("getDocumentElements.error; ", ex);
			throw new RuntimeException(ex.getMessage());
		}
	}

	@ManagedOperation(description="Return Document Fields")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "uri", description = "Document identifier")})
	public CompositeData getDocumentInfo(String uri) {
		try {
			XDMDocument doc = docManager.getDocument(uri);
			if (doc != null) {
				Map<String, Object> docInfo = doc.convert();
				return JMXUtils.mapToComposite("document", "Document Info", docInfo);
			} else {
				return null;
			}
		} catch (XDMException ex) {
			logger.error("getDocumentInfo.error: " + ex.getMessage(), ex);
			throw new RuntimeException(ex.getMessage());
		}
	}
	
	@ManagedOperation(description="Return Document Content")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "uri", description = "Document identifier")})
	public String getDocumentContent(String uri) {
		try {
			return docManager.getDocumentAsString(uri);
		} catch (XDMException ex) {
			logger.error("getDocumentXML.error: " + ex.getMessage(), ex);
			throw new RuntimeException(ex.getMessage());
		}
	}

	@Override
	protected Collection getSchemaFeatures(XDMSchema schema) {
		return schema.getCollections();
	}

	@ManagedAttribute(description="Return Collections registered in the Schema")
	public TabularData getCollections() {
		return getTabularFeatures("collection", "Collection definition", "name");
	}
	
	@ManagedOperation(description="Creates a new Collection")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "name", description = "Collection name to create"),
		@ManagedOperationParameter(name = "docType", description = "Root path for document type, if needed"),
		@ManagedOperationParameter(name = "description", description = "Collection description")})
	public void addCollection(String name, String docType, String description) {

		logger.trace("addCollection.enter;");
		long stamp = System.currentTimeMillis();
		XDMCollection collect = schemaManager.addCollection(name, docType, description);
		if (collect == null) {
			throw new IllegalStateException("Collection '" + name + "' in schema '" + schemaName + "' already exists");
		}
		
		int cnt = 0;
		stamp = System.currentTimeMillis() - stamp;
		logger.trace("addCollection.exit; collection added on {} members; timeTaken: {}", cnt, stamp);
	}
	
	@ManagedOperation(description="Removes an existing Collection")
	@ManagedOperationParameters({@ManagedOperationParameter(name = "name", description = "Collection name to remove")})
	public void removeCollection(String name) {
		
		logger.trace("removeCollection.enter;");
		long stamp = System.currentTimeMillis();
		if (!schemaManager.deleteCollection(name)) {
			throw new IllegalStateException("Collection '" + name + "' in schema '" + schemaName + "' does not exist");
		}

		int cnt = 0;
		stamp = System.currentTimeMillis() - stamp;
		logger.trace("removeCollection.exit; collection deleted on {} members; timeTaken: {}", cnt, stamp);
	}

	@ManagedOperation(description="Enables/Disables an existing Collection")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "name", description = "Collection name to enable/disable"),
		@ManagedOperationParameter(name = "enable", description = "enable/disable collection")})
	public void enableCollection(String name, boolean enable) {
		
		if (!schemaManager.enableCollection(name, enable)) {
			throw new IllegalStateException("Collection '" + name + "' in schema '" + schemaName + 
					"' does not exist or already " + (enable ? "enabled" : "disabled"));
		}
	}
	
	@ManagedOperation(description="delete all Schema documents")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "evictOnly", description = "Delete from Cache or from Cache and CacheStore too")})
	public boolean clear(boolean evictOnly) {
		
		SchemaDocCleaner task = new SchemaDocCleaner(schemaName, evictOnly);
		Map<Member, Future<Boolean>> results = execService.submitToAllMembers(task);
		boolean result = true;
		for (Map.Entry<Member, Future<Boolean>> entry: results.entrySet()) {
			try {
				if (!entry.getValue().get()) {
					result = false;
				}
			} catch (InterruptedException | ExecutionException ex) {
				logger.error("clear.error; ", ex);
				//throw new RuntimeException(ex.getMessage());
			}
		}
		return result;
	}
	    
	@ManagedOperation(description="Register Document")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "docFile", description = "A full path to XML file to register"),
		@ManagedOperationParameter(name = "properties", description = "A list of properties in key=value form separated by semicolon")})
	public String registerDocument(String docFile, String properties) {
		
		String uri = Paths.get(docFile).getFileName().toString(); 
		try {
			String xml = FileUtils.readTextFile(docFile);
			XDMDocument doc = docManager.storeDocumentFromString(uri, xml, propsFromString(properties));
			return doc.getUri();
		} catch (IOException | XDMException ex) {
			logger.error("registerDocument.error: " + ex.getMessage(), ex);
			throw new RuntimeException(ex.getMessage());
		}
	}
	
	@ManagedOperation(description="Updates already registerd Document")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "uri", description = "A new uri for the file or null"),
		@ManagedOperationParameter(name = "docFile", description = "A full path to XML file to register"),
		@ManagedOperationParameter(name = "properties", description = "A list of properties in key=value form separated by semicolon")})
	public String updateDocument(String uri, String docFile, String properties) {

		try {
			String content = FileUtils.readTextFile(docFile);
			XDMDocument doc = docManager.storeDocumentFromString(uri, content, propsFromString(properties));
			return doc.getUri();
		} catch (IOException | XDMException ex) {
			logger.error("updateDocument.error: " + ex.getMessage(), ex);
			throw new RuntimeException(ex.getMessage());
		}
	}
	
	@ManagedOperation(description="Remove Document")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "docId", description = "Registered Document identifier")})
	public boolean removeDocument(String uri) {
		
		try {
			docManager.removeDocument(uri);
			return true;
		} catch (Exception ex) {
			logger.error("removeDocument.error: " + ex.getMessage(), ex);
			throw new RuntimeException(ex.getMessage());
		}
	}

	private int processFilesInCatalog(Path catalog, String properties) {
		int result = 0;
		String docType = schemaManager.getDataFormat();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(catalog, "*." + docType.toLowerCase())) {
		    for (Path path: stream) {
		        if (Files.isDirectory(path)) {
		            result += processFilesInCatalog(path, properties);
		        } else {
		            registerDocument(path.toString(), properties);
		            result++;
		        }
		    }
		    return result;
		} catch (IOException ex) {
			logger.error("processFilesInCatalog.error: " + ex.getMessage(), ex);
			throw new RuntimeException(ex.getMessage());
		}
	}
	
	@ManagedOperation(description="Register Documents")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "path", description = "A full path to directory containing XML files to register"),
		@ManagedOperationParameter(name = "properties", description = "A list of properties in key=value form separated by semicolon")})
	public int registerDocuments(String path, String properties) {
		Path catalog = Paths.get(path);
		return processFilesInCatalog(catalog, properties);	
	}

	@ManagedAttribute(description="Returns aggregated DocumentManagement invocation statistics, per method")
	public TabularData getCollectionStatistics() {
		initAggregator();
		return super.getSeriesStatistics(new StatisticSeriesCollector(schemaName, "docStats"), aggregator);
	}
	
	@ManagedOperation(description="Reset DocumentManagement invocation statistics")
	public void resetStatistics() {
		super.resetStatistics(new StatisticsReseter(schemaName, "docStats")); 
	}
	
	@ManagedOperation(description="Add Document to Collection")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "uri", description = "Document identifier"),
		@ManagedOperationParameter(name = "clnName", description = "Collection name")})
	public int addDocumentToCollection(String uri, String clnName) {
		XDMCollection cln = this.schemaManager.getEntity().getCollection(clnName);
		if (cln != null) {
			return docManager.addDocumentToCollections(uri, new String[] {clnName});
		}
		logger.info("addDocumentToCollection; no collection found for name: {}", clnName);
		return 0;
	}

	@ManagedOperation(description="Remove Document from Collection")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "uri", description = "Document identifier"),
		@ManagedOperationParameter(name = "clnName", description = "Collection name")})
	public int removeDocumentFromCollection(String uri, String clnName) {
		XDMCollection cln = schemaManager.getEntity().getCollection(clnName);
		if (cln != null) {
			return docManager.removeDocumentFromCollections(uri, new String[] {clnName});
		}
		logger.info("removeDocumentFromCollection; no collection found for name: {}", clnName);
		return 0;
	}

	@ManagedOperation(description="Return Documents which belongs to Collection")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "clnName", description = "Collection name"),
		@ManagedOperationParameter(name = "props", description = "A list of properties in key=value form separated by semicolon")})
	public Collection<String> getCollectionDocuments(String clName, String props) {
		if (clName != null && !"All Documents".equals(clName)) {
			XDMCollection cln = schemaManager.getEntity().getCollection(clName);
			if (cln == null) {
				logger.info("getCollectionDocuments; got unknown collection: {}", clName);
				return null;
			}
		}

		try {
			Collection<String> result = docManager.getCollectionDocumentUris(clName);
			logger.debug("getCollectionDocuments; returning {} ids", result.size());
			return result;
		} catch (XDMException ex) {
			logger.error("getCollectionDocuments.error", ex);
			return null;
		}
	}

}
