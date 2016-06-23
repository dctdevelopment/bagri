/**
 * 
 */
package com.bagri.xdm.system;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * Represents access permission that is assigned to some schema resource and can be granted to or revoked from schema accessor (User or Role). 
 * 
 * @author Denis Sukhoroslov
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = "http://www.bagridb.com/xdm/access", propOrder = {
		"resource",
		"perms"
})
public class XDMPermission {

	/**
	 * the distinct permission values 
	 */
	@XmlType(name = "Permission", namespace = "http://www.bagridb.com/xdm/access")
	@XmlEnum
	public enum Permission {

		/**
		 * allows read from resource
		 */
	    @XmlEnumValue("read")
		read,

		/**
		 * allows modify resource
		 */
	    @XmlEnumValue("modify")
		modify,
		
		/**
		 * allows to perform some batch actions on resource
		 */
	    @XmlEnumValue("execute")
		execute
	}
	
	@XmlAttribute(required = true)
	private String resource;
	  
	@XmlValue
	@XmlList
	private Set<Permission> perms = new HashSet<Permission>();
	
	/**
	 * default constructor
	 */
	public XDMPermission() {
		// for JAXB serialization
	}
	
	/**
	 * 
	 * @param resource the resource name
	 * @param permissions set of permissions granted on the resource
	 */
	public XDMPermission(String resource, Permission... permissions) {
		this.resource = resource;
		for (Permission p: permissions) {
			addPermission(p);
		}
	}

	/**
	 * 
	 * @param resource the resource name
	 * @param permissions set of permissions granted on the resource
	 */
	public XDMPermission(String resource, Set<Permission> permissions) {
		this.resource = resource;
		setPermissions(permissions);
	}

	/**
	 * 
	 * @return the resource name
	 */
	public String getResource() {
		return resource;
	}
	
	/**
	 * 
	 * @return set of permissions granted on the resource
	 */
	public Set<Permission> getPermissions() {
		return perms;
	}
	
	/**
	 * 
	 * @return string representation of the permissions granted on the resource. "read modify", for instance
	 */
	public String getPermissionsAsString() {
		StringBuffer buff = new StringBuffer();
		for (Permission p: perms) {
			buff.append(p.name());
			buff.append(" ");
		}
		buff.deleteCharAt(buff.length() - 1);
		return buff.toString();
	}
	
	/**
	 * 
	 * @return an array of permissions granted on the resource
	 */
	public String[] getPermissionsAsArray() {
		String[] pNames = new String[perms.size()];
		int i = 0;
		for (Permission p: perms) {
			pNames[i++] = p.name();
		}
		return pNames;
	}
	
	/**
	 * 
	 * @return true if no permissions granted, false otherwise
	 */
	public boolean isEmpty() {
		return perms.isEmpty();
	}
	
	/**
	 * 
	 * @return true if the resource contain willdcard ("*") character(-s), false otherwise 
	 */
	public boolean isWildcard() {
		return resource.indexOf("*") >= 0;
	}
	
	/**
	 * 
	 * @param permissions the set of permissions granted on the resource
	 */
	public void setPermissions(Set<Permission> permissions) {
		this.perms.clear();
		if (permissions != null) {
			perms.addAll(permissions);
		}
	}
	
	/**
	 * 
	 * @param permission the permission to grant on the resource 
	 * @return true if permission has been granted, false otherwise
	 */
	public boolean addPermission(Permission permission) {
		return perms.add(permission);
	}

	/**
	 * 
	 * @param permissions the permissions to grant on the resource 
	 * @return true if permissions has been granted, false otherwise
	 */
	public boolean addPermissions(Collection<Permission> permissions) {
		return perms.addAll(permissions);
	}
	
	/**
	 * 
	 * @param permission the permission to revoke from the resource
	 * @return true if permission has been revoked, false otherwise
	 */
	public boolean removePermission(Permission permission) {
		return perms.remove(permission);
	}
	
	/**
	 * 
	 * @param permission the permission to check
	 * @return true if permission is granted, false otherwise
	 */
	public boolean hasPermission(Permission permission) {
		return perms.contains(permission);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "XDMPermission [" + perms + "]";
	}

}


