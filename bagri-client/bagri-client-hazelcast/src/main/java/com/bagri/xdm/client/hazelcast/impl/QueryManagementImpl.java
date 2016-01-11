package com.bagri.xdm.client.hazelcast.impl;

import static com.bagri.xdm.client.common.XDMCacheConstants.CN_XDM_RESULT;
import static com.bagri.xdm.client.common.XDMCacheConstants.PN_XDM_SCHEMA_POOL;
import static com.bagri.xdm.common.XDMConstants.pn_client_fetchSize;
import static com.bagri.xdm.common.XDMConstants.pn_client_id;
import static com.bagri.xdm.common.XDMConstants.pn_client_submitTo;
import static com.bagri.xdm.common.XDMConstants.pn_client_txId;
import static com.bagri.xdm.common.XDMConstants.pn_queryTimeout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bagri.common.query.ExpressionContainer;
import com.bagri.xdm.api.XDMException;
import com.bagri.xdm.api.XDMQueryManagement;
import com.bagri.xdm.client.common.impl.QueryManagementBase;
import com.bagri.xdm.client.hazelcast.task.query.DocumentIdsProvider;
import com.bagri.xdm.client.hazelcast.task.query.XMLBuilder;
import com.bagri.xdm.client.hazelcast.task.query.QueryExecutor;
import com.bagri.xdm.common.XDMDocumentId;
import com.bagri.xdm.domain.XDMResults;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;

public class QueryManagementImpl extends QueryManagementBase implements XDMQueryManagement {
	
    private final static Logger logger = LoggerFactory.getLogger(QueryManagementImpl.class);
	
    private RepositoryImpl repo;
	private IExecutorService execService;
    private Future execution = null; 
    private IMap<Long, XDMResults> resCache;
    
	public QueryManagementImpl() {
		// what should we do here? 
	}
	
	void initialize(RepositoryImpl repo) {
		this.repo = repo;
		execService = repo.getHazelcastClient().getExecutorService(PN_XDM_SCHEMA_POOL);
		resCache = repo.getHazelcastClient().getMap(CN_XDM_RESULT);
	}
	
	@Override
	public void cancelExecution() throws XDMException {
		if (execution != null) {
			// synchronize on it?
			if (!execution.isDone()) {
				execution.cancel(true);
			}
		}
	}
	
	@Override
	public Collection<XDMDocumentId> getDocumentIds(String query, Map<QName, Object> params, Properties props) throws XDMException {

		long stamp = System.currentTimeMillis();
		logger.trace("getDocumentIDs.enter; query: {}", query);
		DocumentIdsProvider task = new DocumentIdsProvider(repo.getClientId(), repo.getTransactionId(), query, params, props);
		Future<Collection<XDMDocumentId>> future = execService.submit(task);
		execution = future;
		Collection<XDMDocumentId> result = getResults(future, 0);
		stamp = System.currentTimeMillis() - stamp;
		logger.trace("getDocumentIDs.exit; time taken: {}; returning: {}", stamp, result);
		return result;
	}
	
	@Override
	public Iterator executeQuery(String query, Map<QName, Object> params, Properties props) throws XDMException {

		long stamp = System.currentTimeMillis();
		logger.trace("executeQuery.enter; query: {}; bindings: {}; context: {}", query, params, props);
		//QueryParamsKey key = new QueryParamsKey(getQueryKey(query), getParamsKey(bindings));
		long key = getResultsKey(query, params);
		XDMResults res = resCache.get(key);
		if (res != null) {
			logger.trace("execXQuery; got cached results: {}", res);
			return res.getResults().iterator();
		}
		
		props.setProperty(pn_client_id, repo.getClientId());
		//props.setProperty(pn_client_txId, String.valueOf(repo.getTransactionId()));
		
		String runOn = props.getProperty(pn_client_submitTo, "any");
		//String schemaName = repo.getSchemaName();

		boolean isQuery = true;
		QueryExecutor task = new QueryExecutor(repo.getClientId(), repo.getTransactionId(), query, params, props);
		Future<ResultCursor> future;
		if ("owner".equalsIgnoreCase(runOn)) {
			future = execService.submitToKeyOwner(task, key);
		} else if ("member".equalsIgnoreCase(runOn)) {
			Member member = repo.getHazelcastClient().getPartitionService().getPartition(key).getOwner();
			future = execService.submitToMember(task, member);
		} else {
			future = execService.submit(task);
		}
		execution = future;

		long timeout = Long.parseLong(props.getProperty(pn_queryTimeout, "0"));

		//if (cursor != null) {
		//	cursor.close(false);
		//}
		ResultCursor cursor = getResults(future, timeout);
		logger.trace("execXQuery; got cursor: {}", cursor);
		if (cursor != null) {
			cursor.deserialize(repo.getHazelcastClient());
		}
			
		Iterator result;
		int fetchSize = Integer.parseInt(props.getProperty(pn_client_fetchSize, "0"));
		if (fetchSize == 0) {
			result = extractFromCursor(cursor);
		} else {
			// possible memory leak with non-closed cursors !?
			result = cursor;
		}
		logger.trace("executeQuery.exit; time taken: {}; returning: {}", System.currentTimeMillis() - stamp, result);
		return result; 
	}
	
	private <T> T getResults(Future<T> future, long timeout) throws XDMException {

		T result;
		try {
			if (timeout > 0) {
				result = future.get(timeout, TimeUnit.MILLISECONDS); // SECONDS);
			} else {
				result = future.get();
			}
			return result;
		} catch (TimeoutException ex) {
			logger.warn("getResults.timeout; request timed out after {}; cancelled: {}", timeout, future.isCancelled());
			future.cancel(true);
			throw new XDMException(ex, XDMException.ecQueryTimeout);
		} catch (InterruptedException | ExecutionException ex) {
			int errorCode = XDMException.ecQuery;
			if (ex.getCause() != null && ex.getCause() instanceof CancellationException) {
				errorCode = XDMException.ecQueryCancel;
				logger.warn("getResults.interrupted; request cancelled: {}", future.isCancelled());
			} else {
				future.cancel(false); 
				logger.error("getResults.error; error getting result", ex);
				Throwable err = ex;
				while (err.getCause() != null) {
					err = err.getCause();
					if (err instanceof XDMException) {
						throw (XDMException) err;
					}
				}
			}
			throw new XDMException(ex, errorCode);
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Iterator extractFromCursor(ResultCursor cursor) {
		List result = new ArrayList(cursor.getQueueSize());
		while (cursor.hasNext()) {
			result.add(cursor.next());
		}
		return result.iterator();
	}
}
