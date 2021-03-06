package com.bagri.test.tpox.workload;

import static com.bagri.core.Constants.*;
import static com.bagri.support.util.PropUtils.setProperty;
import static com.bagri.support.util.XQUtils.*;
import static com.bagri.xqj.BagriXQDataSource.*;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.xquery.XQConnection;
import javax.xml.xquery.XQDataSource;
import javax.xml.xquery.XQDynamicContext;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQExpression;
import javax.xml.xquery.XQItemType;
import javax.xml.xquery.XQPreparedExpression;
import javax.xml.xquery.XQResultSequence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bagri.xqj.BagriXQDataSource;
import com.bagri.core.system.Parameter;
import com.bagri.xqj.BagriXQDataFactory;

public class BagriXQJPlugin extends BagriTPoXPlugin {

	private static final Logger logger = LoggerFactory.getLogger(BagriXQJPlugin.class);

	private static final XQDataSource xqds; 
	
	static {
		xqds = new BagriXQDataSource();
		try {
		    xqds.setProperty(ADDRESS, System.getProperty(pn_schema_address));
		    xqds.setProperty(SCHEMA, System.getProperty(pn_schema_name));
		    xqds.setProperty(USER, System.getProperty(pn_schema_user));
		    xqds.setProperty(PASSWORD, System.getProperty(pn_schema_password));
		    xqds.setProperty(XQ_PROCESSOR, "com.bagri.xquery.saxon.XQProcessorClient");
		    xqds.setProperty(XDM_REPOSITORY, "com.bagri.client.hazelcast.impl.SchemaRepositoryImpl");
		    String value = System.getProperty(pn_client_loginTimeout);
		    if (value != null) {
		    	xqds.setProperty(pn_client_loginTimeout, value);
		    }
		    value = System.getProperty(pn_client_bufferSize);
		    if (value != null) {
		    	xqds.setProperty(pn_client_bufferSize, value);
		    }
		    value = System.getProperty(pn_client_connectAttempts);
		    if (value != null) {
		    	xqds.setProperty(pn_client_connectAttempts, value);
		    }
		} catch (XQException ex) {
			logger.error("", ex);
		}
	}
	
    private static final ThreadLocal<XQConnection> xqc = new ThreadLocal<XQConnection>() {
		
    	@Override
    	protected XQConnection initialValue() {
    		try {
	    		XQConnection xqc = xqds.getConnection();
	    		setProperty(((BagriXQDataFactory) xqc).getProcessor().getProperties(), pn_client_fetchSize, null); 
	    		setProperty(((BagriXQDataFactory) xqc).getProcessor().getProperties(), pn_client_submitTo, null); 
	    		logger.info("initialValue.exit; XQC: {}", xqc);
	    		return xqc;
    		} catch (XQException ex) {
    			logger.error("", ex);
    			return null;
    		}
    	}
    };
    
    protected XQConnection getConnection() {
    	return xqc.get(); 
    }
    
    public BagriXQJPlugin() {
    	super();
    }
	
	@Override
	public void close() throws SQLException {
		XQConnection conn = getConnection();
		if (!conn.isClosed()) {
			logger.info("close; XQC: {}; stats: {}", conn, Arrays.toString(stats.get()));
			try {
				conn.close();
			} catch (XQException ex) {
				logger.error("close.error; " + ex, ex);
				throw new SQLException(ex);
			}
		} else {
			logger.debug("close; XQC is already closed: {}", conn);
		}
	}

	private void bindParams(Map<String, Parameter> params, XQDynamicContext xqe) throws XQException {
	    for (Map.Entry<String, Parameter> e: params.entrySet()) {
	    	Parameter param = e.getValue();
	    	QName typeName = new QName(xs_ns, param.getType(), xs_prefix);
			int baseType = getBaseTypeForTypeName(typeName);
			XQItemType type = getConnection().createAtomicType(baseType, typeName, null);
			//xqe.bindAtomicValue(new QName(e.getKey()), param.getName(), type);
			xqe.bindObject(new QName(e.getKey()), getAtomicValue(baseType, param.getName()), type);
	    }
	}
	
	@Override
	protected int execCommand(String query, Map<String, Parameter> params) throws XQException {
		
		XQExpression xqe = getConnection().createExpression();
		bindParams(params, xqe);
	    xqe.executeCommand(query);
	    // do next somehow!
	    xqe.close();
		return 1;
	}
	
	@Override
	protected int execQuery(String query, Map<String, Parameter> params) throws XQException {

		long stamp = System.currentTimeMillis();
	    XQPreparedExpression xqpe = getConnection().prepareExpression(query);
		bindParams(params, xqpe);
		long stamp2 = System.currentTimeMillis();
	    XQResultSequence xqs = xqpe.executeQuery();
	    stats.get()[3] += System.currentTimeMillis() - stamp2; 
	    int cnt = 0;
	    stamp2 = System.currentTimeMillis();
	    if (fetchSize > 0) {
	    	while (xqs.next() && cnt < fetchSize) {
	    		cnt++;
	    	}
	    } else {
	    	while (xqs.next()) {
	    		cnt++;
	    	}
	    }
	    stats.get()[4] += System.currentTimeMillis() - stamp2; 
	    xqs.close();
	    xqpe.close();
	    stats.get()[2] += System.currentTimeMillis() - stamp; 
	    return cnt;
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}
	
}
