package com.bagri.xdm.cache.hazelcast.management;

import java.util.Map;
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
import com.bagri.xdm.cache.hazelcast.task.index.IndexCreator;
import com.bagri.xdm.cache.hazelcast.task.index.IndexRemover;
import com.bagri.xdm.cache.hazelcast.task.stats.StatisticSeriesCollector;
import com.bagri.xdm.system.XDMIndex;
import com.bagri.xdm.system.XDMSchema;
import com.hazelcast.core.Member;

@ManagedResource(description="Schema Indexes Management MBean")
public class IndexManagement extends SchemaFeatureManagement {
	
	private SchemaManager schemaManager;

	public IndexManagement(String schemaName) {
		super(schemaName);
	}

	protected String getFeatureKind() {
		return "IndexManagement";
	}
	
	public void setSchemaManager(SchemaManager schemaManager) {
		this.schemaManager = schemaManager;
	}

	@ManagedAttribute(description="Return indexes defined on Schema")
	public TabularData getIndexes() {
		// get XDMSchema somehow! then get its indexes and build TabularData
		XDMSchema schema = schemaManager.getEntity();
		Set<XDMIndex> indexes = schema.getIndexes();
		if (indexes.size() == 0) {
			return null;
		}
		
        TabularData result = null;
        for (XDMIndex index: indexes) {
            try {
                Map<String, Object> def = index.toMap();
                CompositeData data = JMXUtils.mapToComposite("index", "Index definition", def);
                result = JMXUtils.compositeToTabular("index", "Index definition", "name", result, data);
            } catch (Exception ex) {
                logger.error("getIndexes; error", ex);
            }
        }
        return result;
    }
	
	@ManagedAttribute(description="Return aggregated index usage statistics, per index")
	public TabularData getIndexStatistics() {
		//return super.getInvocationStatistics(new StatisticSeriesCollector(schemaName, "indexStats"));
		
		StatisticSeriesCollector statsTask = new StatisticSeriesCollector(schemaName, "indexStats");
		int cnt = 0;
		TabularData result = null;
		Map<Member, Future<TabularData>> futures = execService.submitToAllMembers(statsTask);
		for (Map.Entry<Member, Future<TabularData>> entry: futures.entrySet()) {
			try {
				TabularData stats = entry.getValue().get();
				result = stats; //JMXUtils.aggregateStats(stats, result);
				cnt++;
			} catch (InterruptedException | ExecutionException ex) {
				logger.error("getInvocationStatistics.error: " + ex.getMessage(), ex);
			}
		}
		logger.trace("getInvocationStatistics.exit; got stats from {} nodes", cnt);
		return result;
	}

	@ManagedOperation(description="Creates a new Index")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "name", description = "Index name to create"),
		@ManagedOperationParameter(name = "path", description = "XPath to index"),
		@ManagedOperationParameter(name = "docType", description = "Root path for document type"),
		@ManagedOperationParameter(name = "unique", description = "Is index unique"),
		@ManagedOperationParameter(name = "description", description = "Index description")})
	public void addIndex(String name, String path, String docType, boolean unique, String description) {

		logger.trace("addIndex.enter;");
		long stamp = System.currentTimeMillis();
		XDMIndex index = schemaManager.addIndex(name, path, docType, unique, description);
		if (index == null) {
			throw new IllegalStateException("Index '" + name + "' in schema '" + schemaName + "' already exists");
		}
		
		IndexCreator task = new IndexCreator(index);
		Map<Member, Future<Boolean>> results = execService.submitToAllMembers(task);
		int cnt = 0;
		for (Map.Entry<Member, Future<Boolean>> entry: results.entrySet()) {
			try {
				if (entry.getValue().get()) {
					cnt++;
				}
			} catch (InterruptedException | ExecutionException ex) {
				logger.error("addIndex.error; ", ex);
			}
		}
		stamp = System.currentTimeMillis() - stamp;
		logger.trace("addIndex.exit; index created on {} members; timeTaken: {}", cnt, stamp);
	}
	
	@ManagedOperation(description="Removes an existing Index")
	@ManagedOperationParameters({@ManagedOperationParameter(name = "name", description = "Index name to delete")})
	public void dropIndex(String name) {
		
		logger.trace("dropIndex.enter;");
		long stamp = System.currentTimeMillis();
		if (!schemaManager.deleteIndex(name)) {
			throw new IllegalStateException("Index '" + name + "' in schema '" + schemaName + "' does not exist");
		}

		IndexRemover task = new IndexRemover(name);
		Map<Member, Future<Boolean>> results = execService.submitToAllMembers(task);
		int cnt = 0;
		for (Map.Entry<Member, Future<Boolean>> entry: results.entrySet()) {
			try {
				if (entry.getValue().get()) {
					cnt++;
				}
			} catch (InterruptedException | ExecutionException ex) {
				logger.error("dropIndex.error; ", ex);
			}
		}
		stamp = System.currentTimeMillis() - stamp;
		logger.trace("dropIndex.exit; index deleted on {} members; timeTaken: {}", cnt, stamp);
	}

	@ManagedOperation(description="Rebuilds an existing Index")
	@ManagedOperationParameters({@ManagedOperationParameter(name = "name", description = "Index to rebuild")})
	public void rebuildIndex(String name) {
		// not implemented yet
	}

}
