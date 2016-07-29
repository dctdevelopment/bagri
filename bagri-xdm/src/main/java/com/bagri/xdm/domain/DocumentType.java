package com.bagri.xdm.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents XDM document type meta-data.
 *  
 * @author Denis Sukhoroslov
 * @since 05.2013 
 */
public class DocumentType { 
	
	private int typeId;
	private String rootPath;
	private boolean normalized = false;
	private Set<String> schemas = new HashSet<>();
	
	/**
	 * default constructor
	 */
	public DocumentType() {
		//
	}
	
	/**
	 * 
	 * @param typeId the type id
	 * @param rootPath the type's path
	 */
	public DocumentType(int typeId, String rootPath) {
		this.typeId = typeId;
		this.rootPath = rootPath;
	}

	/**
	 * @return the type Id
	 */
	public int getTypeId() {
		return typeId;
	}

	/**
	 * @return the root path
	 */
	public String getRootPath() {
		return rootPath;
	}
	
	/**
	 * @return is the type normalized
	 */
	public boolean isNormalized() {
		return normalized;
	}

	/**
	 * @param normalized: boolean; set type's normalized flag
	 */
	public void setNormalized(boolean normalized) {
		this.normalized = normalized;
	}
	
	/**
	 * @return Collection of schemas used by the type
	 */
	public Collection<String> getSchemas() {
		return Collections.unmodifiableCollection(schemas);
	}
	
	/**
	 * adds new schema
	 * 
	 * @param schemaUri: String; new schema uri
	 * @return true in case when schema registered, false otherwise
	 */
	public boolean addSchema(String schemaUri) {
		return schemas.add(schemaUri);
	}

	/**
	 * removes schema
	 * 
	 * @param schemaUri: String; the schema uri to remove
	 * @return true when schema removed, false otherwise
	 */
	public boolean removeSchema(String schemaUri) {
		return schemas.remove(schemaUri);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "DocumentType [typeId=" + typeId + ", rootPath=" + rootPath
				+ ", schemas=" + schemas + "]";
	}

}
