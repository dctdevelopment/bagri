package com.bagri.xdm.cache.hazelcast.management;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.bagri.xdm.system.XDMFunction;
import com.bagri.xdm.system.XDMLibrary;
import com.bagri.xquery.api.XQCompiler;

@ManagedResource(description="Extension Library Manager MBean")
public class LibraryManager extends EntityManager<XDMLibrary> { 

	private XQCompiler xqComp;
	//private IExecutorService execService;

	public LibraryManager() {
		// default constructor
		super();
	}
    
	public LibraryManager(String libraryName) {
		super(libraryName);
		//execService = hzInstance.getExecutorService(PN_XDM_SYSTEM_POOL);
		//IMap<String, XDMNode> nodes = hzInstance.getMap("nodes"); 
		//setEntityCache(nodes);
	}

	public void setXQCompiler(XQCompiler xqComp) {
		this.xqComp = xqComp;
	}
	
	@ManagedAttribute(description="Returns Library functions")
	public String[] getDeclaredFunctions() {
		XDMLibrary library = getEntity();
		List<String> result = new ArrayList<>(library.getFunctions().size());
		for (XDMFunction func: library.getFunctions()) {
			result.add(func.toString());
		}
		Collections.sort(result);
		return result.toArray(new String[result.size()]);
	}
	
	@Override
	protected String getEntityType() {
		return "Library";
	}

	@ManagedAttribute(description="Returns Library description")
	public String getDescription() {
		return getEntity().getDescription();
	}

	@ManagedAttribute(description="Returns Library file name")
	public String getFileName() {
		return getEntity().getFileName();
	}

	@ManagedAttribute(description="Returns registered Library name")
	public String getName() {
		return entityName;
	}

	@ManagedAttribute(description="Returns registered Library namespace")
	public String getNamespace() {
		return getEntity().getNamespace();
	}

	@ManagedAttribute(description="Returns Library version")
	public int getVersion() {
		return super.getVersion();
	}
	
}