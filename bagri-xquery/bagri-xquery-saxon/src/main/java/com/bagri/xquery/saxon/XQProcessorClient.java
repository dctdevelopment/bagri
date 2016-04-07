package com.bagri.xquery.saxon;

import static com.bagri.xdm.common.XDMConstants.cmd_get_document;
import static com.bagri.xdm.common.XDMConstants.cmd_remove_cln_documents;
import static com.bagri.xdm.common.XDMConstants.cmd_remove_document;
import static com.bagri.xdm.common.XDMConstants.cmd_store_document;
import static com.bagri.xdm.common.XDMConstants.pn_query_command;
import static com.bagri.xqj.BagriXQUtils.getXQException;
import static com.bagri.xquery.saxon.SaxonUtils.itemToXQItem;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQItem;
import javax.xml.xquery.XQStaticContext;

import com.bagri.xdm.api.XDMException;
import com.bagri.xdm.api.XDMQueryManagement;
import com.bagri.xdm.common.XDMDocumentId;
import com.bagri.xquery.api.XQProcessor;

import net.sf.saxon.expr.instruct.GlobalParameterSet;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;

public class XQProcessorClient extends XQProcessorImpl implements XQProcessor {
	
	@Override
    public void cancelExecution() throws XQException {
		try {
			getQueryManagement().cancelExecution();
		} catch (XDMException ex) {
    		throw getXQException(ex);
		}
    }
	
	@Override
	public Iterator<?> executeXCommand(String command, Map<QName, Object> params, XQStaticContext ctx) throws XQException {
		
		return executeXCommand(command, params, collectProperties(ctx));
	}
	
	private XDMDocumentId getDocumentId(Map<QName, Object> params) throws XQException {
		XQItem docId = (XQItem) params.remove(new QName("docId"));
		XQItem uri = (XQItem) params.remove(new QName("uri"));
		if (docId == null) {
			if (uri == null) {
				return null;
			}
			return new XDMDocumentId(uri.getAtomicValue());
		}
		if (uri == null) {
			// TODO: for update version should be 0!
			return new XDMDocumentId(docId.getLong(), 1);
		}
		return new XDMDocumentId(docId.getLong(), 1, uri.getAtomicValue());
	}
	
	private Properties fillProperties(Map<QName, Object> params, Properties props) {
		if (props == null) {
			props = new Properties();
		}
		for (Map.Entry<QName, Object> e: params.entrySet()) {
			props.put(e.getKey().getLocalPart(), e.getValue());
		}
		return props;
	}

	@Override
	public Iterator<?> executeXCommand(String command, Map<QName, Object> params, Properties props) throws XQException {
		
    	//logger.trace("executeXCommand.enter; command: {}", command);
    	try {
    		Object result;
			if (command.equals(cmd_store_document)) {
				String content = ((XQItem) params.remove(new QName("content"))).getAtomicValue();
				XDMDocumentId docId = getDocumentId(params);
				result = getDocumentManagement().storeDocumentFromString(docId, content, fillProperties(params, props));
			} else if (command.equals(cmd_get_document)) {
				result = getDocumentManagement().getDocumentAsString(getDocumentId(params));
			} else if (command.equals(cmd_remove_document)) {
				getDocumentManagement().removeDocument(getDocumentId(params));
				result = new Integer(1); 
			} else if (command.equals(cmd_remove_cln_documents)) {
				String collection = (String) params.get("collection");
				getDocumentManagement().removeCollectionDocuments(collection);
				result = new Integer(0); 
			} else {
				props = ensureProperty(props, pn_query_command, "true");
		    	XDMQueryManagement qMgr = getQueryManagement();
	    		return qMgr.executeQuery(command, params, props);
			}
			return Collections.singletonList(result).iterator();
    	} catch (XDMException ex) {
    		throw getXQException(ex);
    	}
	}

	@Override
	public Iterator<?> executeXQuery(String query, XQStaticContext ctx) throws XQException {

		return executeXQuery(query, collectProperties(ctx));
	}

	@Override
	public Iterator<?> executeXQuery(String query, Properties props) throws XQException {

    	//logger.trace("executeXQuery.enter; query: {}", query);
		props = ensureProperty(props, pn_query_command, "false");
    	XDMQueryManagement qMgr = getQueryManagement();
    	GlobalParameterSet params = dqc.getParameters();
    	Map<QName, Object> bindings = new HashMap<>(params.getNumberOfKeys());
    	
    	try {
	    	for (StructuredQName qName: params.getKeys()) {
	    		QName vName = new QName(qName.getURI(), qName.getLocalPart(), qName.getPrefix()); 
	    		//bindings.put(vName, itemToObject((Item) params.get(qName)));
	    		bindings.put(vName, itemToXQItem((Item) params.get(qName), this.getXQDataFactory()));
	    	}
	    	//logger.trace("executeXQuery; bindings: {}", bindings);
    	
    		return qMgr.executeQuery(query, bindings, props);
    	} catch (XPathException | XDMException ex) {
    		throw getXQException(ex);
    	}
	}

	@Override
    public Collection<QName> prepareXQuery(String query, XQStaticContext ctx) throws XQException {
    	XDMQueryManagement qMgr = getQueryManagement();
    	Collection<String> names = qMgr.prepareQuery(query);
    	if (names != null) {
    		return getParamNames(names);
    	}
    	return super.prepareXQuery(query, ctx);
	}


	@Override
	public Iterator<?> getResults() {
		return null;
	}

	@Override
	public void setResults(Iterator<?> itr) {
		// no-op
	}

	private Properties collectProperties(XQStaticContext ctx) throws XQException {
		Properties props = contextToProps(ctx);
		for (String name: properties.stringPropertyNames()) {
			if (!props.containsKey(name)) {
				props.setProperty(name, properties.getProperty(name));
			}
		}
		return props;
	}
	
	private Properties ensureProperty(Properties props, String key, String value) {
		if (props == null) {
			props = new Properties();
		}
		props.setProperty(key, value);
		return props;
	}
	
}
