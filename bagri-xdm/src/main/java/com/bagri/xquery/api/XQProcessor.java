package com.bagri.xquery.api;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.xquery.XQDataFactory;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQStaticContext;

import com.bagri.xdm.api.XDMRepository;

public interface XQProcessor {
	
    XDMRepository getRepository();
    XQDataFactory getXQDataFactory();    
    void setRepository(XDMRepository xRepo);
	void setXQDataFactory(XQDataFactory xqFactory);    
	
    Properties getProperties();
	boolean isFeatureSupported(int feature);
    
    Collection<QName> prepareXQuery(String query, XQStaticContext ctx) throws XQException;
    Iterator<?> executeXQuery(String query, XQStaticContext ctx) throws XQException;
    Iterator<?> executeXQuery(String query, Properties props) throws XQException;
    void bindVariable(QName varName, Object var) throws XQException;
    void unbindVariable(QName varName) throws XQException;

	Iterator<?> executeXCommand(String command, Map<QName, Object> params, XQStaticContext ctx) throws XQException;
    Iterator<?> executeXCommand(String command, Map<QName, Object> params, Properties props) throws XQException;

    Iterator<?> getResults();
    void setResults(Iterator<?> itr);
    
    void cancelExecution() throws XQException;
    // Saxon specific conversion
    // TODO: move this out of the interface!
	String convertToString(Object item, Properties props) throws XQException;
	
}