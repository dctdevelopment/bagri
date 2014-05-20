/**
 * 
 */
package com.bagri.xdm.cache.hazelcast.management;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.MalformedObjectNameException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.MBeanExportException;
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.bagri.common.manage.JMXUtils;
import com.bagri.xdm.process.hazelcast.schema.SchemaRemover;
import com.bagri.xdm.process.hazelcast.user.UserCreator;
import com.bagri.xdm.process.hazelcast.user.UserRemover;
import com.bagri.xdm.system.XDMSchema;
import com.bagri.xdm.system.XDMUser;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

/**
 * @author Denis Sukhoroslov
 *
 */
@ManagedResource(objectName="com.bagri.xdm:type=Management,name=UserManagement", 
	description="User Management MBean")
public class UserManagement implements EntryListener<String, XDMUser>, InitializingBean {

    private static final transient Logger logger = LoggerFactory.getLogger(UserManagement.class);
    
	//private Properties defaults; 
    private HazelcastInstance hzInstance;
    private IMap<String, XDMUser> userCache;
    private Map<String, UserManager> mgrCache = new HashMap<String, UserManager>();
    
    @Autowired
	private AnnotationMBeanExporter mbeanExporter;
    
    
	public UserManagement(HazelcastInstance hzInstance) {
		//super();
		this.hzInstance = hzInstance;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
        Set<String> names = userCache.keySet();
		logger.debug("afterPropertiesSet.enter; got users: {}", names); 
        for (String name: names) {
        	//XDMUser user = userCache.get(name);
       		UserManager uMgr = (UserManager) mgrCache.get(name);
       		if (uMgr == null) {
   				logger.debug("afterPropertiesSet; cannot get UserManager for user {}; initializing a new one", name); 
       			try {
       				uMgr = initUserManager(name);
       			} catch (MBeanExportException | MalformedObjectNameException ex) {
       				// JMX registration failed.
       				logger.error("afterPropertiesSet.error: ", ex);
       			}
       		}
   			//if (uMgr != null) {
   			//	sMgr.setState("Failed user initialization");
   			//}
        }
	}
	
	public void setUserCache(IMap<String, XDMUser> userCache) {
		this.userCache = userCache;
		userCache.addEntryListener(this, false);
}

	@ManagedAttribute(description="Registered User Names")
	public String[] getUserNames() {
		return userCache.keySet().toArray(new String[0]);
	}
	
	//@Override
	public Collection<XDMUser> getUsers() {
		return new ArrayList<XDMUser>(userCache.values());
	}

	@ManagedOperation(description="Create new User")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "login", description = "User login"),
		@ManagedOperationParameter(name = "password", description = "User password")})
	public boolean addUser(String login, String password) {

		if (!userCache.containsKey(login)) {
	    	Object result = userCache.executeOnKey(login, new UserCreator(JMXUtils.getCurrentUser(), password));
	    	logger.debug("addUser; execution result: {}", result);
			return true;
		}
		return false;
	}

	@ManagedOperation(description="Delete User")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "login", description = "User login")})
	public boolean deleteUser(String login) {
		//return userCache.remove(login) != null;
		XDMUser user = userCache.get(login);
		if (user != null) {
	    	Object result = userCache.executeOnKey(login, new UserRemover(user.getVersion(), JMXUtils.getCurrentUser()));
	    	logger.debug("deleteUser; execution result: {}", result);
	    	return result != null;
		}
		return false;
	}

	private UserManager initUserManager(String userName) throws MBeanExportException, MalformedObjectNameException {
		UserManager uMgr = null;
   	    if (!mgrCache.containsKey(userName)) {
			uMgr = new UserManager(userName);
			uMgr.setUserCache(userCache);
			mgrCache.put(userName, uMgr);
			mbeanExporter.registerManagedResource(uMgr, uMgr.getObjectName());
		}
   	    return uMgr;
	}

	@Override
	public void entryAdded(EntryEvent<String, XDMUser> event) {
		logger.trace("entryAdded; event: {}", event);
		String userName = event.getKey();
		try {
			initUserManager(userName);
		} catch (MBeanExportException | MalformedObjectNameException ex) {
			// JMX registration failed.
			logger.error("entryAdded.error: ", ex);
		}
	}

	@Override
	public void entryRemoved(EntryEvent<String, XDMUser> event) {
		logger.trace("entryRemoved; event: {}", event);
		String userName = event.getKey();
		if (mgrCache.containsKey(userName)) {
			UserManager uMgr = mgrCache.get(userName);
			mgrCache.remove(userName);
			try {
				mbeanExporter.unregisterManagedResource(uMgr.getObjectName());
			} catch (MalformedObjectNameException ex) {
				logger.error("entryRemoved.error: ", ex);
			}
		}
	}

	@Override
	public void entryUpdated(EntryEvent<String, XDMUser> event) {
		logger.trace("entryUpdated; event: {}", event);
	}

	@Override
	public void entryEvicted(EntryEvent<String, XDMUser> event) {
		logger.trace("entryEvicted; event: {}", event);
		// make user inactive ?
	}
	
	public boolean authenticate(String login, String password) {
		XDMUser user = userCache.get(login);
		if (user != null) {
			// @TODO: we'll NOT store passwords in open text,
			// will have to hash pwd and compare with original hash
			return password.equals(user.getPassword());
		}
		// throw NotFound exception?
		return false;
	}


}
