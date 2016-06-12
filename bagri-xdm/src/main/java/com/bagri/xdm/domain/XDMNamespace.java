package com.bagri.xdm.domain;

/**
 * Contains information about XDM namespace.
 *  
 * @author Denis Sukhoroslov
 * @since 05.2013 
 * @version 0.1
 */
public class XDMNamespace { 
	
	private String uri;
	private String prefix;
	private String location;
	
	/**
	 * default constructor
	 */
	public XDMNamespace() {
		//
	}

	/**
	 * 
	 * @param uri the namespace uri
	 * @param prefix the namespace prefix
	 * @param location the namespace location
	 */
	public XDMNamespace(String uri, String prefix, String location) {
		this.uri = uri;
		this.prefix = prefix;
		this.location = location;
	}

	/**
	 * @return the namespace uri
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * @return the namespace prefix
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * @return the namespace location
	 */
	public String getLocation() {
		return location;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		XDMNamespace other = (XDMNamespace) obj;
		if (uri == null) {
			if (other.uri != null) {
				return false;
			}
		} else if (!uri.equals(other.uri)) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "XDMNamespace [uri=" + uri + ", prefix=" + prefix
				+ ", location=" + location + "]";
	}
	
	

}
