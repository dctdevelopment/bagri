package com.bagri.core.api;

/**
 * XDM binding management interface; provided for the client side.
 * Allows to work with documents via JAXB entities. JAXB entities must be registered on the client side.
 * 
 * @author Denis Sukhoroslov
 */
public interface BindingManagement {

	/**
	 * 
	 * @param uri the document uri to be bound as JAXB entity
	 * @param type the java class to bind document into
	 * @param <T> type class
	 * @return the JAXB entity
	 * @throws BagriException in case of any binding error
	 */
	<T> T getDocumentBinding(String uri, Class<T> type) throws BagriException;
	
	/**
	 * 
	 * @param uri the document uri to be stored from the bound JAXB entity
	 * @param value the JAXB entity to bound into document
	 * @param <T> type class
	 * @throws BagriException in case of any binding error
	 */
	<T> void setDocumentBinding(String uri, T value) throws BagriException;
}


