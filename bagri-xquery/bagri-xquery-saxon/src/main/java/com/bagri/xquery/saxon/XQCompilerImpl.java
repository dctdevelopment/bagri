package com.bagri.xquery.saxon;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.instruct.UserFunction;
import net.sf.saxon.expr.instruct.UserFunctionParameter;
import net.sf.saxon.functions.ExecutableFunctionLibrary;
import net.sf.saxon.functions.FunctionLibrary;
import net.sf.saxon.functions.FunctionLibraryList;
import net.sf.saxon.functions.IntegratedFunctionLibrary;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.lib.ModuleURIResolver;
import net.sf.saxon.lib.Validation;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;

import com.bagri.xdm.system.XDMFunction;
import com.bagri.xdm.system.XDMLibrary;
import com.bagri.xdm.system.XDMModule;
import com.bagri.xquery.api.XQCompiler;

public class XQCompilerImpl implements XQCompiler, ErrorListener {
	
	private static final Logger logger = LoggerFactory.getLogger(XQCompilerImpl.class);
	
	private Properties props = new Properties();
	
    private Configuration config;
    private StaticQueryContext sqc;
	private List<XDMLibrary> libraries = new ArrayList<>();
    private List<TransformerException> errors = new ArrayList<>();
	
    public XQCompilerImpl() {
        config = Configuration.newConfiguration();
        config.setHostLanguage(Configuration.XQUERY);
        config.setSchemaValidationMode(Validation.STRIP);
        //config.setConfigurationProperty(FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS, Boolean.TRUE);
        //list.
        //config.setConfigurationProperty(FeatureKeys.PRE_EVALUATE_DOC_FUNCTION, Boolean.TRUE);
        sqc = config.newStaticQueryContext();
        sqc.setErrorListener(this);
        //sqc.setCompileWithTracing(true);
    }

	@Override
	public Properties getProperties() {
		return props;
	}

	@Override
	public void setProperty(String name, Object value) {
		props.setProperty(name, value.toString());
	}

	@Override
	public void compileQuery(String query) {
		long stamp = System.currentTimeMillis();
		logger.trace("compileQuery.enter; got query: {}", query);
		clearErrors();
		try {
			XQueryExpression exp = sqc.compileQuery(query);
		} catch (XPathException ex) {
			StringBuffer buff = new StringBuffer();
			for (TransformerException tex: errors) {
				buff.append(tex.getMessageAndLocation()).append("\n");
			}
			String error = buff.toString();
			//logger.error("compileQuery.error", ex);
			logger.info("compileQuery.error; message: {}", error);
			throw new RuntimeException(error);
		}
		stamp = System.currentTimeMillis() - stamp;
		logger.trace("compileQuery.exit; time taken: {}", stamp); 
	}

	@Override
	public void compileModule(XDMModule module) {
		long stamp = System.currentTimeMillis();
		logger.trace("compileModule.enter; got module: {}", module);
		XQueryExpression exp = getModuleExpression(module);
		//lookupFunctions(exp.getExecutable().getFunctionLibrary());
		stamp = System.currentTimeMillis() - stamp;
		logger.trace("compileModule.exit; time taken: {}", stamp); 
	}
	
	private void clearErrors() {
		errors.clear();
	}
	
	@Override
	public List<String> getModuleFunctions(XDMModule module) {
		long stamp = System.currentTimeMillis();
		//module.
		logger.trace("getModuleFunctions.enter; got module: {}", module);
		XQueryExpression exp = getModuleExpression(module);
		// sqc.getExecutable().setFunctionLibrary(extLibrary);
		List<String> result = lookupFunctions(exp.getExecutable().getFunctionLibrary());
		stamp = System.currentTimeMillis() - stamp;
		logger.trace("getModuleFunctions.exit; time taken: {}; returning: {}", stamp, result);
		return result;
	}

	@Override
	public boolean getModuleState(XDMModule module) {
		try {
			String query = "import module namespace test=\"" + module.getNamespace() + 
					"\" at \"" + module.getName() + "\";\n\n";
			query += "1213";
			sqc.setModuleURIResolver(new LocalModuleURIResolver(module.getBody()));
			logger.trace("getModuleExpression; compiling query: {}", query);
			sqc.compileQuery(query);
			return true;
		} catch (XPathException ex) {
			return false;
		}
	}
	
	@Override
	public void setLibraries(Collection<XDMLibrary> libraries) {
		this.libraries.clear();
		this.libraries.addAll(libraries);
	}
	
	private XQueryExpression getModuleExpression(XDMModule module) {
		//logger.trace("getModuleExpression.enter; got namespace: {}, name: {}, body: {}", namespace, name, body);
		clearErrors();
		try {
			//sqc.compileLibrary(query); - works in EE only
			String query = "import module namespace test=\"" + module.getNamespace() + 
					"\" at \"" + module.getName() + "\";\n\n";
			query += "1213";
			sqc.setModuleURIResolver(new LocalModuleURIResolver(module.getBody()));
			logger.trace("getModuleExpression; compiling query: {}", query);
			//logger.trace("getModuleExpression.exit; time taken: {}", stamp);
			
			// TODO: register extension for each and every library/function..
			// then think how to cache this info..
			return sqc.compileQuery(query);
			//sqc.getCompiledLibrary("test")...
		} catch (XPathException ex) {
			//IntegratedFunctionLibrary ifl = config.getIntegratedFunctionLibrary();
			StringBuffer buff = new StringBuffer();
			for (TransformerException tex: errors) {
				buff.append(tex.getMessageAndLocation()).append("\n");
			}
			String error = buff.toString();
			//logger.error("compileQuery.error", ex);
			logger.info("getModuleExpression.error; message: {}", error);
			throw new RuntimeException(error);
		}
	}

	private List<String> lookupFunctions(FunctionLibraryList fll) {
		List<String> fl = new ArrayList<>();
		for (FunctionLibrary lib: fll.getLibraryList()) {
			logger.trace("lookupFunctions; function library: {}; class: {}", lib.toString(), lib.getClass().getName());
			if (lib instanceof FunctionLibraryList) {
				fl.addAll(lookupFunctions((FunctionLibraryList) lib));
			} else if (lib instanceof ExecutableFunctionLibrary) {
				ExecutableFunctionLibrary efl = (ExecutableFunctionLibrary) lib;
				Iterator<UserFunction> itr = efl.iterateFunctions();
				while (itr.hasNext()) {
					fl.add(getFunctionDeclaration(itr.next()));
				}
			}
		}
		return fl;
	}
	
	private String getFunctionDeclaration(UserFunction function) {
		//declare function hw:helloworld($name as xs:string)
		StringBuffer buff = new StringBuffer("function ");
		buff.append(function.getFunctionName());
		buff.append("(");
		int idx =0;
		for (UserFunctionParameter ufp: function.getParameterDefinitions()) {
			if (idx > 0) {
				buff.append(", ");
			}
			buff.append("$");
			buff.append(ufp.getVariableQName());
			buff.append(" as ");
			buff.append(ufp.getRequiredType().toString());
			idx++;
		}
		buff.append(") as ");
		// TODO: get rid of Q{} notation..
		buff.append(function.getDeclaredResultType().toString());
		return buff.toString();
	}

	@Override
	public void error(TransformerException txEx) throws TransformerException {
		errors.add(txEx);
	}

	@Override
	public void fatalError(TransformerException txEx) throws TransformerException {
		errors.add(txEx);
	}

	@Override
	public void warning(TransformerException txEx) throws TransformerException {
		errors.add(txEx);
	}
	
	private class LocalModuleURIResolver implements ModuleURIResolver {
		
		private String body;
		
		LocalModuleURIResolver(String body) {
			this.body = body;
		}

		@Override
		public StreamSource[] resolve(String moduleURI, String baseURI,	String[] locations) throws XPathException {
			logger.trace("resolve.enter; got moduleURI: {}, baseURI: {}, locations: {}", moduleURI, baseURI, locations);
			Reader reader = new StringReader(body);
			return new StreamSource[] {new StreamSource(reader)};
		}
	}

}
