package com.bagri.server.hazelcast.impl;

import static com.bagri.core.Constants.*;
import static com.bagri.core.api.BagriException.ecDocument;
import static com.bagri.core.api.TransactionManagement.TX_NO;
import static com.bagri.core.model.Document.clnDefault;
import static com.bagri.core.model.Document.dvFirst;
import static com.bagri.core.query.PathBuilder.*;
import static com.bagri.core.system.DataFormat.df_xml;
import static com.bagri.core.server.api.CacheConstants.*;
import static com.bagri.support.util.FileUtils.def_encoding;
import static com.bagri.support.util.XMLUtils.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.bagri.client.hazelcast.task.doc.DocumentContentProvider;
import com.bagri.core.DataKey;
import com.bagri.core.DocumentKey;
import com.bagri.core.KeyFactory;
import com.bagri.core.api.BagriException;
import com.bagri.core.model.Data;
import com.bagri.core.model.Document;
import com.bagri.core.model.Element;
import com.bagri.core.model.Elements;
import com.bagri.core.model.FragmentedDocument;
import com.bagri.core.model.Path;
import com.bagri.core.model.Transaction;
import com.bagri.core.server.api.ContentBuilder;
import com.bagri.core.server.api.ContentConverter;
import com.bagri.core.server.api.ContentParser;
import com.bagri.core.server.api.DocumentManagement;
import com.bagri.core.server.api.impl.DocumentManagementBase;
import com.bagri.core.system.Collection;
import com.bagri.core.system.DataFormat;
import com.bagri.core.system.Fragment;
import com.bagri.core.system.Schema;
import com.bagri.core.system.TriggerAction.Order;
import com.bagri.core.system.TriggerAction.Scope;
import com.bagri.server.hazelcast.predicate.CollectionPredicate;
import com.bagri.server.hazelcast.predicate.DocVisiblePredicate;
import com.bagri.server.hazelcast.predicate.DocumentPredicateBuilder;
import com.bagri.server.hazelcast.task.doc.DocumentProcessor;
import com.bagri.support.idgen.IdGenerator;
import com.bagri.support.stats.StatisticsEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.PagingPredicate;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;

public class DocumentManagementImpl extends DocumentManagementBase implements DocumentManagement {
	
	private static final String fnUri = "uri";
	//private static final String fnTxStart = "txStart";
	private static final String fnTxFinish = "txFinish";
	private static final String fnRoot = "root";
	
	private KeyFactory factory;
	private SchemaRepositoryImpl repo;
    private HazelcastInstance hzInstance;
    private IndexManagementImpl indexManager;
    private TransactionManagementImpl txManager;
    private TriggerManagementImpl triggerManager;
	private DataDistributionService ddSvc;

    private IdGenerator<Long> docGen;
    private IMap<DocumentKey, Object> cntCache;
	private IMap<DocumentKey, Document> xddCache;
    private IMap<DataKey, Elements> xdmCache;

    private boolean enableStats = true;
	private BlockingQueue<StatisticsEvent> queue;
	
    public void setRepository(SchemaRepositoryImpl repo) {
    	this.repo = repo;
    	this.factory = repo.getFactory();
    	//this.model = repo.getModelManagement();
    	this.txManager = (TransactionManagementImpl) repo.getTxManagement();
    	this.triggerManager = (TriggerManagementImpl) repo.getTriggerManagement();
    }
    
    IMap<DocumentKey, Object> getContentCache() {
    	return cntCache;
    }

    IMap<DocumentKey, Document> getDocumentCache() {
    	return xddCache;
    }

    IMap<DataKey, Elements> getElementCache() {
    	return xdmCache;
    }
    
    public void setDocumentIdGenerator(IdGenerator<Long> docGen) {
    	this.docGen = docGen;
    }
    
    public void setContentCache(IMap<DocumentKey, Object> cache) {
    	this.cntCache = cache;
    }
    
    public void setXddCache(IMap<DocumentKey, Document> cache) {
    	this.xddCache = cache;
    }

    public void setXdmCache(IMap<DataKey, Elements> cache) {
    	this.xdmCache = cache;
    }

    //@Autowired
	public void setHzInstance(HazelcastInstance hzInstance) {
		this.hzInstance = hzInstance;
	}
	
    public void setIndexManager(IndexManagementImpl indexManager) {
    	this.indexManager = indexManager;
    }
    
    public void setStatsQueue(BlockingQueue<StatisticsEvent> queue) {
    	this.queue = queue;
    }

    public void setStatsEnabled(boolean enable) {
    	this.enableStats = enable;
    }
    
    public void setDistrService(DataDistributionService ddSvc) {
    	this.ddSvc = ddSvc;
    }

    private Set<DataKey> getDocumentElementKeys(String path, long[] fragments) {
    	Set<Integer> parts = model.getPathElements(path);
    	Set<DataKey> keys = new HashSet<DataKey>(parts.size()*fragments.length);
    	// not all the path keys exists as data key for particular document!
    	for (long docKey: fragments) {
	    	for (Integer part: parts) {
	    		keys.add(factory.newDataKey(docKey, part));
	    	}
    	}
    	return keys;
    }
    
    public java.util.Collection<Elements> getDocumentElements(String uri) {
    	// could be faster to do this via EP..
		Document doc = getDocument(uri);
		if (doc == null) {
			return null;
		}

		Set<DataKey> keys = getDocumentElementKeys(doc.getTypeRoot(), doc.getFragments());
		Map<DataKey, Elements> elements = xdmCache.getAll(keys);
		return elements.values();
    }
    
	public String checkDocumentCommited(long docKey, int clnId) throws BagriException {
		
		Document doc = getDocument(docKey);
		if (doc != null) {
			if (clnId > 0 && !doc.hasCollection(clnId)) {
				return null;
			}
			if (doc.getTxFinish() > TX_NO && txManager.isTxVisible(doc.getTxFinish())) {
				return null;
			}
			if (txManager.isTxVisible(doc.getTxStart())) {
				return doc.getUri();
			}
		}
		return null;
	}

	int indexElements(int pathId) throws BagriException {
		Path path = model.getPath(pathId);
		Set<DocumentKey> docKeys = getDocumentsOfType(path.getRoot());
		int cnt = 0;
		for (DocumentKey docKey: docKeys) {
			DataKey xdk = factory.newDataKey(docKey.getKey(), pathId);
			Elements elts = xdmCache.get(xdk);
			if (elts != null) {
				for (Element elt: elts.getElements()) {
					indexManager.addIndex(docKey.getKey(), pathId, path.getPath(), elt.getValue());
					cnt++;
				}
			}
		}
		return cnt;
	}

	int deindexElements(int pathId) {
		Path path = model.getPath(pathId);
		Set<DocumentKey> docKeys = getDocumentsOfType(path.getRoot());
		int cnt = 0;
		for (DocumentKey docKey: docKeys) {
			DataKey xdk = factory.newDataKey(docKey.getKey(), pathId);
			Elements elts = xdmCache.get(xdk);
			if (elts != null) {
				for (Element elt: elts.getElements()) {
					indexManager.removeIndex(docKey.getKey(), pathId, elt.getValue());
					cnt++;
				}
			}
		}
		return cnt;
	}

	private int deindexElements(long docKey, int pathId) {
		int cnt = 0;
		DataKey xdk = factory.newDataKey(docKey, pathId);
		Elements elts = xdmCache.get(xdk);
		if (elts != null) {
			for (Element elt: elts.getElements()) {
				indexManager.removeIndex(docKey, pathId, elt.getValue());
				cnt++;
			}
		}
		logger.trace("deindexElements.exit; deindexed elements: {} for docKey: {}, pathId: {}", cnt, docKey, pathId);
		return cnt;
	}
	
	@SuppressWarnings("unchecked")
	private Set<DocumentKey> getDocumentsOfType(String root) {
   		Predicate<DocumentKey, Document> f = Predicates.and(Predicates.equal(fnRoot, root), 
   				Predicates.equal(fnTxFinish, TX_NO));
		return xddCache.keySet(f);
	}
	
	public Document getDocument(long docKey) {
		Document doc = getDocument(factory.newDocumentKey(docKey)); 
		//logger.trace("getDocument; returning: {}", doc);
		return doc;
	}
	
	private Document getDocument(DocumentKey docKey) {
		//return xddCache.get(docKey);
		return (Document) ddSvc.getCachedObject(CN_XDM_DOCUMENT, docKey, true);
	}

	@Override
	public Document getDocument(String uri) {
		Document doc = null;
    	DocumentKey key = ddSvc.getLastKeyForUri(uri);
    	if (key != null) {
    		doc = getDocument(key);
    		if (doc != null) {
    			if (doc.getTxFinish() != TX_NO) { // || !txManager.isTxVisible(lastDoc.getTxFinish())) {
    				logger.debug("getDocument; the latest document version is finished already: {}", doc);
    				doc = null;
    			}
    		}
    	}
		return doc;
	}
	
	private Object getDocumentContent(String uri) {
		Object content = null;
		DocumentKey docKey = getDocumentKey(uri, false, false);
		if (docKey != null) {
			content = getDocumentContent(docKey);
		}
		return content;
		
	}

	private Object getDocumentContent(DocumentKey docKey) {
		//Object content = cntCache.get(docKey);
		Object content = ddSvc.getCachedObject(CN_XDM_CONTENT, docKey, false);
		if (content == null) {
			// build it with builder!
		}
		return content; 
	}

	@Override
	public String getDocumentContentType(long docKey) throws BagriException {
		Document doc = getDocument(docKey);
		if (doc != null) {
			return doc.getContentType();
		}
		
		String def = repo.getSchema().getProperty(pn_schema_format_default);
		DataFormat df = repo.getDataFormat(def);
		if (df == null) {
			return mt_xml;
		}
		return df.getType();
	}
	
    private DocumentKey getDocumentKey(String uri, boolean next, boolean acceptClosed) {
    	DocumentKey last = ddSvc.getLastKeyForUri(uri);
    	if (last == null) {
			DocumentKey key = factory.newDocumentKey(uri, 0, dvFirst);
    		if (next) {
    			while (xddCache.getEntryView(key) != null) {
    				key = factory.newDocumentKey(uri, key.getRevision() + 1, dvFirst);
    			}
    			return key;
    		} 
    		
			if (ddSvc.isLocalKey(key)) {
				// we have not found corresponding Document for the uri provided, 
				// and no option to build a new key, so we returning null
	    		return null;
			} else {
				// actually, this is wrong scenario..
				logger.info("getDocumentKey; the uri provided {} does not belong to this Member", uri); 
				// think how to get it from concrete node?!
				//keys = xddCache.keySet(Predicates.equal(fnUri, uri));
				//if (keys.isEmpty()) {
		    		return null;
				//}
			}
    	}
    	
    	//DocumentKey last = Collections.max(keys, versionComparator);
    	if (next) {
    		return factory.newDocumentKey(uri, last.getRevision(), last.getVersion() + 1);
    	}
    	if (acceptClosed) {
    		return last;
    	}
    	Document lastDoc = xddCache.get(last);
    	try {
    		if (lastDoc.getTxFinish() == TX_NO || !txManager.isTxVisible(lastDoc.getTxFinish())) {
    			return last;
    		}
    		// shouldn't we return previous version otherwise?
    	} catch (BagriException ex) {
    		logger.error("getDocumentKey.error", ex);
    		// ??
    	}
    	logger.info("getDocumentKey; the latest document version is finished already: {}", lastDoc);
    	return null;
    }
 	
	@Override
	@SuppressWarnings("unchecked")
	public java.util.Collection<String> getDocumentUris(String pattern, Properties props) {
		logger.trace("getDocumentUris.enter; got pattern: {}; props: {}", pattern, props);
		Predicate<DocumentKey, Document> query;
		if (pattern != null) {
			query = DocumentPredicateBuilder.getQuery(pattern);
		} else {
			query = Predicates.equal(fnTxFinish, TX_NO);
		}
		
		if (props != null) {
			int pageSize = Integer.valueOf(props.getProperty(pn_client_fetchSize, "0"));
			if (pageSize > 0) {
				query = new PagingPredicate<>(query, pageSize);
				//query = Predicates.and(new PagingPredicate(pageSize), query);
			}
		} //else {
		//  Projection<Entry<DocumentKey, Document>, String> pro = Projections.singleAttribute(fnUri);
		//	uris = xddCache.project(pro, query);
		//}
		
		java.util.Collection<Document> docs = xddCache.values(query);
		java.util.Collection<String> uris = new ArrayList<>(docs.size());
		if (pattern.indexOf(fnTxFinish) < 0) {
			for (Document doc: docs) {
				if (doc.getTxFinish() == TX_NO) {
					uris.add(doc.getUri());
				}
			}
		} else {
			for (Document doc: docs) {
				uris.add(doc.getUri());
			}
		}
		
		// should also check if doc's start transaction is committed?
		logger.trace("getDocumentUris.exit; returning: {}", uris);
		return uris;
	}

	java.util.Collection<String> buildContent(Set<Long> docKeys, String template, Map<String, Object> params, String dataFormat) throws BagriException {
		
        logger.trace("buildContent.enter; docKeys: {}", docKeys.size());
        ContentBuilder builder = repo.getBuilder(dataFormat);
        if (builder == null) {
			logger.info("buildContent.exit; no Handler found for dataFormat {}", dataFormat);
        	return null;
        }
        
		long stamp = System.currentTimeMillis();
        java.util.Collection<String> result = new ArrayList<>(docKeys.size());
		
        
        String root = null;
		for (Iterator<Long> itr = docKeys.iterator(); itr.hasNext(); ) {
			DocumentKey docKey = factory.newDocumentKey(itr.next());
			if (ddSvc.isLocalKey(docKey)) {
				Document doc = xddCache.get(docKey);
				if (doc == null) {
					logger.info("buildContent; lost document for key {}", docKey);
					continue;
				}

				StringBuilder buff = new StringBuilder(template);
				for (Map.Entry<String, Object> param: params.entrySet()) {
					String key = param.getKey();
					String path = param.getValue().toString();
					Object content = null;
					if (path.equals(root)) {
						// TODO: get and convert to string?
						content = cntCache.get(docKey);
					}
					if (content == null) {
				        logger.trace("buildContent; no content found for doc key: {}", docKey);
						content = buildElement(path, doc.getFragments(), builder);
					}
					if (content != null) {
						String str = content.toString();
						int pos = 0;
						while (true) {
							int idx = buff.indexOf(key, pos);
							if (idx < 0) break;
							buff.replace(idx, idx + key.length(), str);
							pos = idx + str.length();
						}
					}
				}
				result.add(buff.toString());
			} else {
				// remove is not supported by the HZ iterator provided! 
				// actually, don't think we have to do it at all..
				//itr.remove();
		        logger.debug("buildDocument; docId {} is not local, processing skipped", docKey);
			}
		}
        
		stamp = System.currentTimeMillis() - stamp;
        logger.trace("buildDocument.exit; time taken: {}; returning: {}", stamp, result.size()); 
        return result;
	}
    
	@SuppressWarnings("unchecked")
	private Object buildElement(String path, long[] fragments, ContentBuilder<?> builder) throws BagriException {
    	Set<DataKey> xdKeys = getDocumentElementKeys(path, fragments);
    	return builder.buildContent(xdmCache.getAll(xdKeys));
    }
    
	@Override
	public Object getDocumentAsBean(String uri, Properties props) throws BagriException {
		DocumentKey docKey = ddSvc.getLastKeyForUri(uri);
		return getDocumentAsBean(docKey, props);  
	}

	public Object getDocumentAsBean(DocumentKey docKey, Properties props) throws BagriException {
		Document doc = getDocument(docKey);
		if (doc == null) {
			logger.info("getDocumentAsBean; no document found for key: {}", docKey);
			return null;
		}

		if (!"MAP".equalsIgnoreCase(doc.getContentType()) && !repo.getHandler(doc.getContentType()).isStringFormat()) {
			return getDocumentContent(docKey);
		}

		String content = getDocumentAsString(docKey, props);
		if (content == null) {
			return null;
		}
		// TODO: convert from JSON as well!
		return beanFromXML(content);
	}
	
	@Override
	public Map<String, Object> getDocumentAsMap(String uri, Properties props) throws BagriException {
		//DocumentKey docKey = ddSvc.getLastKeyForUri(uri);
		//return getDocumentAsMap(docKey, props);
		Document doc = getDocument(uri);
		if (doc == null) {
			logger.info("getDocumentAsMap; no document found for uri: {}", uri);
			return null;
		}

		String cType = doc.getContentType();
		DocumentKey docKey = factory.newDocumentKey(doc.getDocumentKey());
		if ("MAP".equalsIgnoreCase(cType)) {
			return (Map<String, Object>) getDocumentContent(docKey);
		}
		
		ContentConverter<String, Map<String, Object>> cc = repo.getConverter(cType, Map.class);
		if (cc != null) {
			String content = getDocumentAsString(docKey, props);
			if (content != null) {
				return cc.convertTo(content);
			}
		}
		return null;
	}

	@Override
	public Map<String, Object> getDocumentAsMap(long docKey, Properties props) throws BagriException {
		DocumentKey xdmKey = factory.newDocumentKey(docKey);
		return getDocumentAsMap(xdmKey, props);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> getDocumentAsMap(DocumentKey docKey, Properties props) throws BagriException {
		Document doc = getDocument(docKey);
		if (doc == null) {
			logger.info("getDocumentAsMap; no document found for key: {}", docKey);
			return null;
		}

		String cType = doc.getContentType();
		if ("MAP".equalsIgnoreCase(cType)) {
			return (Map<String, Object>) getDocumentContent(docKey);
		}
		
		ContentConverter<String, Map<String, Object>> cc = repo.getConverter(cType, Map.class);
		if (cc != null) {
			String content = getDocumentAsString(docKey, props);
			if (content != null) {
				return cc.convertTo(content);
			}
		}
		return null;
	}

	public InputStream getDocumentAsStream(long docKey, Properties props) throws BagriException {
		String content = getDocumentAsString(docKey, props);
		if (content != null) {
			try {
				return new ByteArrayInputStream(content.getBytes(def_encoding));
			} catch (UnsupportedEncodingException ex) {
				throw new BagriException(ex, BagriException.ecInOut);
			}
		}
		return null;
	}
	
	@Override
	public String getDocumentAsString(String uri, Properties props) throws BagriException {
		DocumentKey docKey = getDocumentKey(uri, false, false);
		if (docKey == null) {
			//throw new XDMException("No document found for document Id: " + docId, XDMException.ecDocument);
			logger.info("getDocumentAsString; can not find active document for uri: {}", uri);
			return null;
		}
		return getDocumentAsString(docKey, props);
	}

	@Override
	public String getDocumentAsString(long docKey, Properties props) throws BagriException {
		DocumentKey xdmKey = factory.newDocumentKey(docKey);
		return getDocumentAsString(xdmKey, props);
	}
	
	@Override
	public String getDocumentAsString(DocumentKey docKey, Properties props) throws BagriException {
		
		Document doc = getDocument(docKey);
		if (doc == null) {
			logger.info("getDocumentAsString; no document found for key: {}", docKey);
			return null;
		}

		String docFormat = doc.getContentType();
		String dataFormat = null;
		if (props != null) {
			dataFormat = props.getProperty(pn_document_data_format);
		}
		if (dataFormat == null) {
			dataFormat = docFormat;
		}
		if (!repo.getHandler(dataFormat).isStringFormat()) {
			logger.info("getDocumentAsString; no String format specified for document {}", docKey);
			return null;
		}

		Object content = null;
		boolean sameFormat = dataFormat.equals(docFormat); 
		if (sameFormat) {
			// no need for conversion
			content = cntCache.get(docKey);
		}
		if (content == null) {
			// if docId is not local then buildDocument returns null!
			// query docId owner node for the XML instead
			if (ddSvc.isLocalKey(docKey)) {
				Map<String, Object> params = new HashMap<>();
				params.put(":doc", doc.getTypeRoot());
				java.util.Collection<String> results = buildContent(Collections.singleton(docKey.getKey()), ":doc", params, dataFormat);
				if (sameFormat && !results.isEmpty()) {
					content = results.iterator().next();
					cntCache.set(docKey, content);
				}
			} else {
				DocumentContentProvider xp = new DocumentContentProvider(repo.getClientId(), txManager.getCurrentTxId(), doc.getUri(), props); 
				content = xddCache.executeOnKey(docKey, xp);
			}
		}
		return (String) content;
	}

	private Collection getTypedCollection(Schema schema, String typePath) {
		for (Collection collect: schema.getCollections()) {
			String cPath = collect.getDocumentType();
			if (cPath != null && typePath.equals(cPath)) {
				return collect;
			}
		}
		return null;
	}
	
	public String checkDefaultDocumentCollection(Document doc) {
		Collection cln = getTypedCollection(repo.getSchema(), doc.getTypeRoot());
		logger.trace("checkDefaultDocumentCollection; got collection: {} for typePath: {}", cln, doc.getTypeRoot());
		if (cln != null) {
			doc.addCollection(cln.getId());
			return cln.getName();
		}
		return null;
	}
	
	private Document createDocument(DocumentKey docKey, String uri, Object content, Properties props) throws BagriException {
		logger.trace("createDocument.enter; uri: {}; props: {}", uri, props);
		String dataFormat = null;
		int[] collections = null; 
		if (props != null) {
			dataFormat = props.getProperty(pn_document_data_format); 
			String prop = props.getProperty(pn_document_collections);
			if (prop != null) {
				StringTokenizer tc = new StringTokenizer(prop, ", ", false);
				collections = new int[tc.countTokens()];
				int idx = 0;
				while (tc.hasMoreTokens()) {
					String clName = tc.nextToken();
					Collection cln = repo.getSchema().getCollection(clName);
					if (cln != null) {
						collections[idx] = cln.getId();
					}
					idx++;
				}
			}
		}
		if (dataFormat == null) {
			dataFormat = uri.substring(uri.lastIndexOf(".") + 1);
		}

		Document doc = createDocument(docKey, uri, content, dataFormat, new Date(), repo.getUserName(), txManager.getCurrentTxId(), collections, false);
		
		Scope scope;
		if (docKey.getVersion() == dvFirst) {
			scope = Scope.insert;
			triggerManager.applyTrigger(doc, Order.before, scope);
		} else {
			scope = Scope.update;
			// trigger has been already invoked in storeDocument..
		}
		xddCache.set(docKey, doc);
		cntCache.set(docKey, content);
		triggerManager.applyTrigger(doc, Order.after, scope);

		logger.trace("createDocument.exit; returning: {}", doc);
		return doc;
	}

	//@Override
	@SuppressWarnings("unchecked")
	public Document createDocument(DocumentKey docKey, String uri, Object content, String dataFormat, Date createdAt, String createdBy, 
			long txStart, int[] collections, boolean addContent) throws BagriException {
		
		List<Data> data;
		int length = 0;
		dataFormat = repo.getHandler(dataFormat).getDataFormat();
		ContentParser<Object> parser = repo.getParser(dataFormat);
		try {
			data = parser.parse(content);
			// TODO: get length from parser
		} catch (BagriException ex) {
			logger.info("createDocument; parse error. content: {}", content);
			throw ex;
		}

		Object[] ids = loadElements(docKey.getKey(), data);
		List<Long> fragments = (List<Long>) ids[0];
		if (fragments == null) {
			logger.warn("createDocument.exit; the document is not valid as it has no root element");
			throw new BagriException("invalid document", BagriException.ecDocument);
		} 

		String root = data.get(0).getRoot();
		Document doc;
		if (fragments.size() == 0) {
			doc = new Document(docKey.getKey(), uri, root, txStart, TX_NO, createdAt, createdBy, dataFormat + "/" + def_encoding, length, data.size());
		} else {
			doc = new FragmentedDocument(docKey.getKey(), uri, root, txStart, TX_NO, createdAt, createdBy, dataFormat + "/" + def_encoding, length, data.size());
			long[] fa = new long[fragments.size()];
			fa[0] = docKey.getKey();
			for (int i=0; i < fragments.size(); i++) {
				fa[i] = fragments.get(i);
			}
			((FragmentedDocument) doc).setFragments(fa);
		}

		List<String> clns = new ArrayList<>();
		if (collections != null && collections.length > 0) {
			doc.setCollections(collections);
			for (Collection cln: repo.getSchema().getCollections()) {
				for (int clnId: collections) {
					if (clnId == cln.getId()) {
						clns.add(cln.getName());
						break;
					}
				}
			}
		}

		if (clns.size() == 0) {
			String cln = checkDefaultDocumentCollection(doc);
			if (cln != null) {
				clns.add(cln);
			}
		}
		
		if (addContent) {
			cntCache.set(docKey, content);
		}

		// invalidate cached query results. always do this, even on load?
		Set<Integer> paths = (Set<Integer>) ids[1];
		((QueryManagementImpl) repo.getQueryManagement()).invalidateQueryResults(paths);

		// update statistics
		for (String cln: clns) {
			updateStats(cln, true, data.size(), doc.getFragments().length);
			//updateStats(cln, true, paths.size(), doc.getFragments().length);
		}
		updateStats(null, true, data.size(), doc.getFragments().length);
		return doc;
	}
	
	private Object[] loadElements(long docKey, List<Data> data) throws BagriException {
		
		long stamp = System.currentTimeMillis();
		Data dRoot = getDataRoot(data);
		if (dRoot != null) {
			String root = dRoot.getRoot();
			Map<DataKey, Elements> elements = new HashMap<DataKey, Elements>(data.size());
			
			Set<Integer> fragments = new HashSet<>();
			for (Fragment fragment: repo.getSchema().getFragments()) {
				if (fragment.getDocumentType().equals(root)) {
					Path path = model.getPath(root, fragment.getPath());
					if (path != null) {
						fragments.add(path.getPathId());
					} else if (isRegexPath(fragment.getPath())) {
						String nPath = fragment.getPath();
						fragments.addAll(model.translatePathFromRegex(root, regexFromPath(nPath)));
					} else {	
						logger.info("loadElements; path not found for fragment: {}; docType: {} ({})", 
								fragment, dRoot.getPath(), root);
					}
				}
			}
			logger.debug("loadElements; fragments found: {}; for docType: {} ({}); docKey: {}", 
					fragments, dRoot.getPath(), root, docKey);
			
			long fraPath = docKey;
			long fraPost = 0;
			int size = 1;
			if (fragments.size() > 0) {
				size = data.size() / fragments.size();
			}
			Set<Integer> pathIds = new HashSet<>(size);
			List<Long> fragIds = new ArrayList<>(size);
			for (Data xdm: data) {
				if (fragments.contains(xdm.getPathId())) {
					int hash = docGen.next().intValue(); 
					fraPath = DocumentKey.toKey(hash, 0, 0);
					fragIds.add(fraPath);
					//fraPost = xdm.getPostId();
					fraPost = model.getPath(root, xdm.getPath()).getPostId();
				} else if (fraPost > 0 && xdm.getPathId() > fraPost) {
					fraPath = docKey;
					fraPost = 0;
				}
				pathIds.add(xdm.getPathId());
				if (xdm.getValue() != null) {
					DataKey xdk = factory.newDataKey(fraPath, xdm.getPathId());
					Elements xdes = elements.get(xdk);
					if (xdes == null) {
						xdes = new Elements(xdk.getPathId(), null);
						elements.put(xdk, xdes);
					}
					xdes.addElement(xdm.getElement());
					indexManager.addIndex(docKey, xdm.getPathId(), xdm.getPath(), xdm.getValue());
				}
			}
			xdmCache.putAll(elements);
			
			stamp = System.currentTimeMillis() - stamp;
			logger.debug("loadElements; cached {} elements for docKey: {}; fragments: {}; time taken: {}", 
					elements.size(), docKey, fragIds.size(), stamp);
			Object[] result = new Object[2];
			result[0] = fragIds;
			result[1] = pathIds;
			return result;
		}
		return null;
	}
	
	public Document processDocument(Map.Entry<DocumentKey, Document> old, long txId, String uri, Object content, List<Data> data, Properties props) throws BagriException {
		
		logger.trace("processDocument.enter; uri: {}; data length: {}; props: {}", uri, data.size(), props);
		
		//boolean update = (old.getValue() != null); // && (doc.getTxFinish() == TX_NO || !txManager.isTxVisible(doc.getTxFinish())));
		DocumentKey docKey = old.getKey();
		if (old.getValue() != null) {
	    	logger.trace("processDocument; going to update document: {}", old);
	    	// we must finish old Document and create a new one!
			//triggerManager.applyTrigger(doc, Order.before, Scope.update);
	    	Document updated = old.getValue();
	    	updated.finishDocument(txId);
		    old.setValue(updated);
		    docKey = factory.newDocumentKey(docKey.getKey(), docKey.getVersion() + 1); // docKey.getKey() + 1);
		}

		long key = docKey.getKey();
		int length = 0; // get it from parser somehow
		String root = data.get(0).getRoot();
		Set<Integer> ids = processElements(key, data);
		String dataFormat = props.getProperty(pn_document_data_format, df_xml);
		Document newDoc = new Document(key, uri, root, txId, TX_NO, new Date(), repo.getUserName(), dataFormat + "/" + def_encoding, length, data.size());

		String collections = props == null ? null : props.getProperty(pn_document_collections);
		if (collections != null) {
			StringTokenizer tc = new StringTokenizer(collections, ", ", false);
			while (tc.hasMoreTokens()) {
				String clName = tc.nextToken();
				Collection cln = repo.getSchema().getCollection(clName);
				if (cln != null) {
					newDoc.addCollection(cln.getId());
					updateStats(clName, true, data.size(), 0);
					//updateStats(clName, true, paths.size(), doc.getFragments().length);
				}
			}
		} else {
			String clName = checkDefaultDocumentCollection(newDoc);
			if (clName != null) {
				updateStats(clName, true, data.size(), 0);
			}
		}
		updateStats(null, true, data.size(), 0);
		
		if (old.getValue() == null) {
	    	old.setValue(newDoc);
		} else {
			xddCache.set(docKey, newDoc);
			//ddSvc.storeData(docKey, newDoc, CN_XDM_DOCUMENT);
		}
		//ddSvc.storeData(docKey, content, CN_XDM_CONTENT);
		cntCache.set(docKey, content);
		
		logger.trace("processDocument.exit; returning: {}", newDoc);
		return newDoc;
	}
	
	private Set<Integer> processElements(long docKey, List<Data> data) throws BagriException {
		
		Data dRoot = getDataRoot(data);
		if (dRoot != null) {
			//String root = dRoot.getDataPath().getRoot();
			Map<DataKey, Elements> elements = new HashMap<DataKey, Elements>(data.size());
			Set<Integer> pathIds = new HashSet<>(data.size());
			for (Data xdm: data) {
				if (xdm.getValue() != null) {
					pathIds.add(xdm.getPathId());
					DataKey xdk = factory.newDataKey(docKey, xdm.getPathId());
					Elements xdes = elements.get(xdk);
					if (xdes == null) {
						xdes = new Elements(xdk.getPathId(), null);
						elements.put(xdk, xdes);
					}
					xdes.addElement(xdm.getElement());
				}
			}
			// TODO: do it directly via RecordStore
			//xdmCache.putAll(elements);
			for (Map.Entry<DataKey, Elements> e: elements.entrySet()) {
				xdmCache.set(e.getKey(), e.getValue());
				//ddSvc.storeData(e.getKey(), e.getValue(), CN_XDM_ELEMENT);
			}
			return pathIds;
		}
		throw new BagriException("invalid document: has no root element", ecDocument);
	}

	
	@Override
	public Document storeDocumentFromBean(String uri, Object bean, Properties props) throws BagriException {
		String dataFormat = null;
		if (props != null) {
			dataFormat = props.getProperty(pn_document_data_format);
		}
		if (dataFormat == null) {
			dataFormat = repo.getSchema().getProperty(pn_schema_format_default);
		}
		if (!"MAP".equalsIgnoreCase(dataFormat) && !repo.getHandler(dataFormat).isStringFormat()) {
			return storeDocument(uri, bean, props);
		}
		
		// TODO: use JSON as well!
		String content = beanToXML(bean);
		if (content == null || content.trim().length() == 0) {
			throw new BagriException("Can not convert bean [" + bean + "] to XML", BagriException.ecDocument);
		}
		logger.trace("storeDocumentFromBean; converted bean: {}", content); 
		
		if (props != null) {
			props.setProperty(pn_document_data_format, df_xml);
		}
		return storeDocumentFromString(uri, content, props);
	}

	@Override
	public Document storeDocumentFromMap(String uri, Map<String, Object> fields, Properties props) throws BagriException {
		String dataFormat = null;
		if (props != null) {
			dataFormat = props.getProperty(pn_document_data_format);
		} else {
			props = new Properties();
		}
		if (dataFormat == null) {
			dataFormat = repo.getSchema().getProperty(pn_schema_format_default);
		}
		if (!props.containsKey(pn_document_data_format)) {
			props.setProperty(pn_document_data_format, dataFormat);
		}
		if ("MAP".equalsIgnoreCase(dataFormat)) {
			return storeDocument(uri, fields, props);
		}
		
		String content = null;
		ContentConverter<String, Map<String, Object>> cc = repo.getConverter(dataFormat, Map.class);
		if (cc != null) {
			content = cc.convertFrom(fields);
			if (content == null || content.trim().length() == 0) {
				throw new BagriException("Can not convert map [" + fields + "] to " + dataFormat, BagriException.ecDocument);
			}
		}
		logger.trace("storeDocumentFromMap; converted map: {}", content); 
		return storeDocumentFromString(uri, content, props);
	}
	
	@Override
	public Document storeDocumentFromString(String uri, String content, Properties props) throws BagriException {
		return storeDocument(uri, content, props);
	}
	
	private Document storeDocument(String uri, Object content, Properties props) throws BagriException {
	
		logger.trace("storeDocument.enter; uri: {}; content: {}; props: {}", uri, content.getClass().getName(), props);
		if (uri == null) {
			throw new BagriException("Empty URI passed", ecDocument); 
		}
		
		String storeMode;
		String dataFormat;
		if (props == null) {
			storeMode = pv_client_storeMode_merge;
			dataFormat = null;
		} else {
			storeMode = props.getProperty(pn_client_storeMode, pv_client_storeMode_merge); 
			dataFormat = props.getProperty(pn_document_data_format);
		}
		
		DocumentKey docKey = ddSvc.getLastKeyForUri(uri);
		if (docKey == null) {
			if (pv_client_storeMode_update.equals(storeMode)) {
				throw new BagriException("No document with URI '" +  uri + "' found for update", ecDocument); 
			}
			docKey = factory.newDocumentKey(uri, 0, dvFirst);
		} else {
			if (pv_client_storeMode_insert.equals(storeMode)) {
				throw new BagriException("Document with URI '" + uri + "' already exists; docKey: " + docKey, ecDocument); 
			}
		    //Document doc = getDocument(docKey);
			//update = (doc != null && (doc.getTxFinish() == TX_NO || !txManager.isTxVisible(doc.getTxFinish())));
			//triggerManager.applyTrigger(doc, Order.before, Scope.update);
	    	// do this asynch after tx?
	    	//((QueryManagementImpl) repo.getQueryManagement()).removeQueryResults(docKey.getKey());
		}

		if (dataFormat == null) {
			dataFormat = uri.substring(uri.lastIndexOf(".") + 1);
		}
		dataFormat = repo.getHandler(dataFormat).getDataFormat();
		ContentParser<Object> parser = repo.getParser(dataFormat);
		List<Data> data = parser.parse(content);
		if (props == null) {
			props = new Properties();
		}
		props.setProperty(pn_document_data_format, dataFormat);
		
		// if fragmented document - process it in the old style!
		
		Transaction tx = null;
    	String txLevel = props.getProperty(pn_client_txLevel);
    	if (!pv_client_txLevel_skip.equals(txLevel)) {
    		tx = txManager.getTransaction(txManager.getCurrentTxId()); 
    	}
		
		Object result = xddCache.executeOnKey(docKey, new DocumentProcessor(tx, uri, content, data, props));
		if (result instanceof Exception) {
			logger.error("storeDocument.error; uri: {}", uri, result);
			if (result instanceof BagriException) {
				throw (BagriException) result;
			}
			throw new BagriException((Exception) result, ecDocument);
		}

		Scope scope;
		Document newDoc = (Document) result;
		if (newDoc.getVersion() > dvFirst) {
			scope = Scope.update;
			if (tx != null) {
				txManager.updateCounters(0, 1, 0);
			}
		} else {
			scope = Scope.insert;
			if (tx != null) {
				txManager.updateCounters(1, 0, 0);
			}
		}
		triggerManager.applyTrigger(newDoc, Order.after, scope);

    	java.util.Collection<Path> paths = model.getTypePaths(newDoc.getTypeRoot());
    	Set<Integer> pathIds = new HashSet<>(paths.size());
		for (Path path: paths) {
			DataKey dKey = factory.newDataKey(newDoc.getDocumentKey(), path.getPathId());
			Elements elts = xdmCache.get(dKey);
			if (elts != null) {
				for (Element elt: elts.getElements()) {
					indexManager.addIndex(newDoc.getDocumentKey(), path.getPathId(), path.getPath(), elt.getValue());
				}
				pathIds.add(path.getPathId());
			}
		}

		// invalidate cached query results.
		((QueryManagementImpl) repo.getQueryManagement()).invalidateQueryResults(pathIds);
		
		logger.trace("storeDocument.exit; returning: {}", newDoc);
		return newDoc;
	}

	@Override
	public void removeDocument(String uri) throws BagriException {
		logger.trace("removeDocument.enter; uri: {}", uri);
		//XDMDocumentKey docKey = getDocumentKey(docId);
	    //if (docKey == null) {
    	//	throw new XDMException("No document found for document Id: " + docId, XDMException.ecDocument);
	    //}
		if (uri == null) {
			throw new BagriException("No Document URI passed", BagriException.ecDocument); 
		}
		
		DocumentKey docKey = getDocumentKey(uri, false, false);
		if (docKey == null) {
			logger.info("removeDocument; no active document found for uri: {}", uri);
			return;
		}
		
	    boolean removed = false;
		boolean locked = lockDocument(docKey, txManager.getTransactionTimeout());
		if (locked) {
			try {
			    Document doc = getDocument(docKey);
			    if (doc != null && (doc.getTxFinish() == TX_NO || !txManager.isTxVisible(doc.getTxFinish()))) {
					triggerManager.applyTrigger(doc, Order.before, Scope.delete); 
			    	doc.finishDocument(txManager.getCurrentTxId()); 
			    	xddCache.set(docKey, doc);
			    	((QueryManagementImpl) repo.getQueryManagement()).removeQueryResults(doc.getDocumentKey());
			    	triggerManager.applyTrigger(doc, Order.after, Scope.delete); 
			    	txManager.updateCounters(0, 0, 1);
				    removed = true;
			    }
			} catch (BagriException ex) {
				throw ex;
			} catch (Exception ex) {
				logger.error("removeDocument.error; uri: " + uri, ex);
				throw new BagriException(ex, BagriException.ecDocument);
			} finally {
				unlockDocument(docKey);
			}
		} else {
    		throw new BagriException("Was not able to aquire lock while removing Document: " + docKey + 
    				", timeout: " + txManager.getTransactionTimeout(), BagriException.ecTransTimeout);
		}
		logger.trace("removeDocument.exit; removed: {}", removed);
	}
	
	public void cleanDocument(DocumentKey docKey, boolean complete) {
		logger.trace("cleanDocument.enter; docKey: {}, complete: {}", docKey, complete);
	    Document doc = getDocument(docKey);
	    boolean cleaned = false;
	    if (doc != null) {
	    	// TODO: clean via EntryProcessor..
			cntCache.delete(docKey);
	    	int size = deleteDocumentElements(doc.getFragments(), doc.getTypeRoot());
	    	if (complete) {
	    		xddCache.delete(docKey);
	    	}
	    	cleaned = true;
	    	
			// update statistics
			for (Collection cln: repo.getSchema().getCollections()) {
				if (doc.hasCollection(cln.getId())) { 
					updateStats(cln.getName(), false, size, doc.getFragments().length);
				}
			}
			updateStats(null, false, size, doc.getFragments().length);
	    }
    	((QueryManagementImpl) repo.getQueryManagement()).removeQueryResults(docKey.getKey());
		logger.trace("cleanDocument.exit; cleaned: {}", cleaned);
	}

	public void evictDocument(DocumentKey xdmKey, Document xdmDoc) {
		logger.trace("evictDocument.enter; xdmKey: {}, xdmDoc: {}", xdmKey, xdmDoc);
		cntCache.delete(xdmKey);
    	int size = deleteDocumentElements(xdmDoc.getFragments(), xdmDoc.getTypeRoot());

    	//Collection<Integer> pathIds = indexManager.getTypeIndexes(xdmDoc.getTypeId(), true);
    	//for (int pathId: pathIds) {
    	//	deindexElements(docKey.getKey(), pathId);
    	//}
    	
		// update statistics
		//for (XDMCollection cln: repo.getSchema().getCollections()) {
		//	if (doc.hasCollection(cln.getId())) { 
		//		updateStats(cln.getName(), false, size, doc.getFragments().length);
		//	}
		//}
		//updateStats(null, false, size, doc.getFragments().length);
		logger.trace("evictDocument.exit; evicted: {}", size);
	}
	
	private int deleteDocumentElements(long[] fragments, String root) {

    	int cnt = 0;
    	java.util.Collection<Path> allPaths = model.getTypePaths(root);
		logger.trace("deleteDocumentElements; got {} possible paths to remove; xdmCache size: {}", 
				allPaths.size(), xdmCache.size());
		int iCnt = 0;
		for (long docId: fragments) {
	        for (Path path: allPaths) {
	        	int pathId = path.getPathId();
	        	DataKey dKey = factory.newDataKey(docId, pathId);
	        	if (indexManager.isPathIndexed(pathId)) {
		       		Elements elts = xdmCache.remove(dKey);
		       		if (elts != null) {
		       			for (Element elt: elts.getElements()) {
		       				indexManager.removeIndex(docId, pathId, elt.getValue());
		       				iCnt++;
		       			}
		       		}
	        	} else {
	        		xdmCache.delete(dKey);
	        	}
	   			cnt++;
	        }
		}
		logger.trace("deleteDocumentElements; deleted keys: {}; indexes: {}; xdmCache size after delete: {}",
				cnt, iCnt, xdmCache.size());
		return cnt;
	}

	public void rollbackDocument(DocumentKey docKey) {
		logger.trace("rollbackDocument.enter; docKey: {}", docKey);
		boolean rolled = false;
	    Document doc = getDocument(docKey);
	    if (doc != null) {
	    	doc.finishDocument(TX_NO);
	    	xddCache.set(docKey, doc);
	    	rolled = true;
	    }
		logger.trace("rollbackDocument.exit; rolled back: {}", rolled);
	}
	
	@Override
	public java.util.Collection<String> getCollections() throws BagriException {
		List<String> clNames = new ArrayList<>(repo.getSchema().getCollections().size());
		for (Collection cln: repo.getSchema().getCollections()) {
			clNames.add(cln.getName());
		}
		return clNames;
	}

	@Override
	public java.util.Collection<String> getCollectionDocumentUris(String collection, Properties props) throws BagriException {
		int pageSize = 100;
		if (props != null) {
			pageSize = Integer.valueOf(props.getProperty(pn_client_fetchSize, "100"));
		} 
		PagingPredicate pager = null;
		Predicate<DocumentKey, Document> query = new DocVisiblePredicate();
		((DocVisiblePredicate) query).setRepository(repo);
		if (collection == null) {
			if (pageSize > 0) {
				pager = new PagingPredicate(query, pageSize);
				query = pager;
			}
		} else {
			Collection cln = repo.getSchema().getCollection(collection);
			if (cln == null) {
				return null;
			}
			query = Predicates.and(query, new CollectionPredicate(cln.getId()));
			if (pageSize > 0) {
				pager = new PagingPredicate(query, pageSize);
				query = pager;
			}
		}
		
		List<String> result = new ArrayList<>(); 
		if (pager != null) {
			int size;
			do {
				size = result.size(); 
				fillUris(query, result);
				pager.nextPage();
			} while (result.size() > size);
		} else {
			fillUris(query, result);
		}
		
		// does not work because of a bug in HZ
		//Projection<Entry<DocumentKey, Document>, String> pro = Projections.singleAttribute(fnUri);
		//if (pager != null) {
		//	int size;
		//	do {
		//		size = result.size();  
		//		result.addAll(xddCache.project(pro, query));
		//		pager.nextPage();
		//	} while (result.size() > size);
		//} else {
		//	result.addAll(xddCache.project(pro, query));
		//}
		return result;
	}
	
	private void fillUris(Predicate query, java.util.Collection<String> uris) throws BagriException {
		java.util.Collection<Document> docs = xddCache.values(query);
		for (Document doc: docs) {
	    	uris.add(doc.getUri());
		}
	}

	Set<Long> getCollectionDocumentKeys(int collectId) {
		//
		Set<DocumentKey> docKeys;
		if (collectId == clnDefault) {
			// TODO: local or global keySet ?!
			docKeys = xddCache.keySet();
		} else {
			Predicate<DocumentKey, Document> clp = new CollectionPredicate(collectId);
			// TODO: local or global keySet ?!
			docKeys = xddCache.keySet(clp);
		}
		Set<Long> result = new HashSet<>(docKeys.size());
		for (DocumentKey key: docKeys) {
			result.add(key.getKey());
		}
		return result;
	}
	
	@Override
	public int removeCollectionDocuments(String collection) throws BagriException {
		logger.trace("removeCollectionDocuments.enter; collection: {}", collection);
		int cnt = 0;
		// remove local documents only?! yes!
		java.util.Collection<String> uris = getCollectionDocumentUris(collection, null);
		for (String uri: uris) {
			removeDocument(uri);
			cnt++;
		}
		logger.trace("removeCollectionDocuments.exit; removed: {}", cnt);
		return cnt;
	}
	
	@Override
	public int addDocumentToCollections(String uri, String[] collections) {
		logger.trace("addDocumentsToCollections.enter; got uri: {}; collectIds: {}", uri, Arrays.toString(collections));
		int addCount = 0;
		int unkCount = 0;
		Document doc = getDocument(uri);
		if (doc != null) {
			// TODO: cache size in the doc itself? yes, done
			// but must fix stats to account this size 
			int size = 0;
			for (Collection cln: repo.getSchema().getCollections()) {
				for (String collection: collections) {
					if (collection.equals(cln.getName())) {
						if (doc.addCollection(cln.getId())) {
							addCount++;
							updateStats(cln.getName(), true, size, doc.getFragments().length);
						}						
						break;
					}
				}
			}
			if (addCount > 0) {
				xddCache.set(factory.newDocumentKey(doc.getDocumentKey()), doc);
			}
		} else {
			unkCount++;
		}
		logger.trace("addDocumentsToCollections.exit; added: {}; unknown: {}", addCount, unkCount);
		return addCount;
	}

	@Override
	public int removeDocumentFromCollections(String uri, String[] collections) {
		logger.trace("removeDocumentsFromCollections.enter; got uri: {}; collectIds: {}", uri, Arrays.toString(collections));
		int remCount = 0;
		int unkCount = 0;
		Document doc = getDocument(uri);
		if (doc != null) {
			int size = 0;
			for (Collection cln: repo.getSchema().getCollections()) {
				for (String collection: collections) {
					if (collection.equals(cln.getName())) {
						if (doc.removeCollection(cln.getId())) {
							remCount++;
							updateStats(cln.getName(), false, size, doc.getFragments().length);
						}
						break;
					}
				}
			}
			if (remCount > 0) {
				xddCache.set(factory.newDocumentKey(doc.getDocumentKey()), doc);
			}
		} else {
			unkCount++;
		}
		logger.trace("removeDocumentsFromCollections.exit; removed: {}; unknown: {}", remCount, unkCount);
		return remCount;
	}
	
	private boolean lockDocument(DocumentKey docKey, long timeout) { //throws XDMException {
		
		boolean locked = false;
		if (timeout > 0) {
			try {
				locked = xddCache.tryLock(docKey, timeout, TimeUnit.MILLISECONDS);
			} catch (InterruptedException ex) {
				logger.error("lockDocument.error", ex);
				//throw new XDMException(ex);
			}
		} else {
			locked = xddCache.tryLock(docKey);
		}
		return locked;
	}

	private void unlockDocument(DocumentKey docKey) {

		xddCache.unlock(docKey);
	}

	private void updateStats(String name, boolean add, int elements, int fragments) {
		if (enableStats) {
			if (!queue.offer(new StatisticsEvent(name, add, new Object[] {fragments, elements}))) {
				logger.warn("updateStats; queue is full!!");
			}
		}
	}

}
