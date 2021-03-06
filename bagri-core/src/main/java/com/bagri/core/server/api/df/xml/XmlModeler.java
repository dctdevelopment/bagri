package com.bagri.core.server.api.df.xml;

import com.bagri.core.api.BagriException;
import com.bagri.core.model.NodeKind;
import com.bagri.core.model.Occurrence;
import com.bagri.core.model.Path;
import com.bagri.core.server.api.ContentModeler;
import com.bagri.core.server.api.ModelManagement;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.xquery.XQItemType;

import org.apache.xerces.dom.DOMXSImplementationSourceImpl;
import org.apache.xerces.impl.xs.XSImplementationImpl;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSImplementation;
import org.apache.xerces.xs.XSLoader;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;

import static com.bagri.core.Constants.xs_ns;
import static com.bagri.support.util.XQUtils.getBaseTypeForTypeName;


public class XmlModeler implements ContentModeler {
	
    private static final transient Logger logger = LoggerFactory.getLogger(XmlModeler.class);
	
	protected ModelManagement modelMgr;
	
	/**
	 * 
	 * @param modelMgr the model management component
	 */
	XmlModeler(ModelManagement modelMgr) {
		//super(model);
		this.modelMgr = modelMgr;
	}
	
    /**
     * Lifecycle method. Invoked at initialization phase. 
     * 
     * @param properties the environment context
     */
    public void init(Properties properties) {
    	// set all internal props here..
    }

	/**
	 * registers bunch of node path's specified in the XML schema (XSD)
	 * 
	 * @param model String; schema in plain text  
	 * @throws BagriException in case of any error
	 */
	@Override
	public void registerModel(String model) throws BagriException {
		
		XSImplementation impl = (XSImplementation) new DOMXSImplementationSourceImpl().getDOMImplementation("XS-Loader LS");
		XSLoader schemaLoader = impl.createXSLoader(null);
		LSInput lsi = ((DOMImplementationLS) impl).createLSInput();
		lsi.setStringData(model);
		XSModel schema = schemaLoader.load(lsi);
		processModel(schema);
	}

	/**
	 * registers bunch of schemas located in the schemaUri folder   
	 * 
	 * @param modelsUri String; the folder containing schemas to register  
	 * @throws BagriException in case of any error
	 */
	//@Override
	public void registerModels(String modelsUri) throws BagriException {

		XSImplementation impl = (XSImplementation) new DOMXSImplementationSourceImpl().getDOMImplementation("XS-Loader LS");
		XSLoader schemaLoader = impl.createXSLoader(null);
		java.nio.file.Path catalog = Paths.get(modelsUri);
		List<String> files = new ArrayList<>(); 
		processCatalog(catalog, files);	
		if (files.size() > 0) {
			StringList schemas = impl.createStringList(files.toArray(new String[files.size()]));
			XSModel schema = schemaLoader.loadURIList(schemas);
			processModel(schema);
		}
	}
	
	private void processCatalog(java.nio.file.Path catalog, List<String> output) {
		logger.trace("processCatalog; got catalog: {}", catalog); 
		try (DirectoryStream<java.nio.file.Path> stream = Files.newDirectoryStream(catalog, "*.xsd")) {
		    for (java.nio.file.Path path: stream) {
		        if (Files.isDirectory(path)) {
		            processCatalog(path, output);
		        } else {
		            output.add(path.toString());
		        }
		    }
		} catch (IOException ex) {
			logger.error("processCatalog.error: " + ex.getMessage(), ex);
			throw new RuntimeException(ex.getMessage());
		}
	}

	/**
	 * registers bunch of schemas located in the schemaUri folder   
	 * 
	 * @param modelUri String; the folder containing schemas to register  
	 * @throws BagriException in case of any error
	 */
	@Override
	public void registerModelUri(String modelUri) throws BagriException {

		XSImplementation impl = new XSImplementationImpl(); 
		XSLoader schemaLoader = impl.createXSLoader(null);
		XSModel schema = schemaLoader.loadURI(modelUri);
		processModel(schema);
	}

	@SuppressWarnings("rawtypes")
	private List<Path> processModel(XSModel schema) throws BagriException {
		
		XSNamedMap elts = schema.getComponents(XSConstants.ELEMENT_DECLARATION);

		// collect substitutions 
		Map<String, List<XSElementDeclaration>> substitutions = new HashMap<String, List<XSElementDeclaration>>();
		for (Object o: elts.entrySet()) {
			Map.Entry e = (Map.Entry) o;
			XSElementDeclaration xsElement = (XSElementDeclaration) e.getValue();
			XSElementDeclaration subGroup = xsElement.getSubstitutionGroupAffiliation();
			if (subGroup != null) {
				List<XSElementDeclaration> subs = substitutions.get(subGroup.getName());
				if (subs == null) {
					subs = new ArrayList<XSElementDeclaration>();
					substitutions.put(subGroup.getName(), subs);
				}
				subs.add(xsElement);
			}
		}
		logger.trace("processModel; got substitutions: {}", substitutions.size());
		
		// process top-level elements
		for (Object o: elts.entrySet()) {
			Map.Entry e = (Map.Entry) o;
			XSElementDeclaration xsElement = (XSElementDeclaration) e.getValue();
			if (xsElement.getTypeDefinition().getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
				String root = xsElement.getNamespace() == null ? "/" + xsElement.getName() : "/{" + xsElement.getNamespace() + "}" + xsElement.getName();
				// register document root..
				Path xp = modelMgr.translatePath(root, "/", NodeKind.document, 0, XQItemType.XQBASETYPE_UNTYPED, Occurrence.onlyOne); 
				logger.trace("processModel; document root: {}; got XDMPath: {}", root, xp);
				
				//modelMgr.translatePath(root, "/#xmlns", NodeKind.namespace, xp.getPathId(), XQItemType.XQBASETYPE_QNAME, Occurrence.onlyOne); 

				// add these two??
				//xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				//xsi:schemaLocation="http://tpox-benchmark.com/security security.xsd">
				
				List<XSElementDeclaration> parents = new ArrayList<>(4);
				processElement(xp, "", xsElement, substitutions, parents, 1, 1);
				//normalizeDocumentType(docType);
			}
		}
		
		return new ArrayList<Path>();
	}

	private Path processElement(Path parent, String path, XSElementDeclaration xsElement, 
			Map<String, List<XSElementDeclaration>> substitutions,
			List<XSElementDeclaration> parents, int minOccurs, int maxOccurs) throws BagriException {
		
		Path element = parent;
		if (!xsElement.getAbstract()) {
			path += xsElement.getNamespace() == null ? "/" + xsElement.getName() : "/{" + xsElement.getNamespace() + "}" + xsElement.getName();
			element = modelMgr.translatePath(parent.getRoot(), path, NodeKind.element, parent.getPathId(), XQItemType.XQBASETYPE_ANYTYPE, 
					Occurrence.getOccurrence(minOccurs, maxOccurs));
			logger.trace("processElement; element: {}; type: {}; got XDMPath: {}", path, xsElement.getTypeDefinition(), element);
		}
		
		List<XSElementDeclaration> subs = substitutions.get(xsElement.getName());
		logger.trace("processElement; got {} substitutions for element: {}", subs == null ? 0 : subs.size(), xsElement.getName());
		if (subs != null) {
			for (XSElementDeclaration sub: subs) {
				processElement(parent, path, sub, substitutions, parents, minOccurs, maxOccurs);
			}
		}

		if (parents.contains(xsElement)) {
			return element;
		}
		parents.add(xsElement);
		
		Path text = null;
		if (xsElement.getTypeDefinition().getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {

			XSComplexTypeDefinition ctd = (XSComplexTypeDefinition) xsElement.getTypeDefinition();
			
			// TODO: process derivations..?

			// element's attributes
		    XSObjectList xsAttrList = ctd.getAttributeUses();
		    for (int i = 0; i < xsAttrList.getLength(); i ++) {
		        processAttribute(element, path, (XSAttributeUse) xsAttrList.item(i));
		    }
		      
			element = processParticle(element, path, ctd.getParticle(), substitutions, parents);

			if (ctd.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_SIMPLE || 
					ctd.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_MIXED) {
				path += "/text()";
				text = modelMgr.translatePath(element.getRoot(), path, NodeKind.text, element.getPathId(), getBaseType(ctd.getSimpleType()), 
						Occurrence.getOccurrence(minOccurs, maxOccurs));
				logger.trace("processElement; complex text: {}; type: {}; got XDMPath: {}", path, ctd.getBaseType(), text);
			}
		} else { //if (xsElementDecl.getTypeDefinition().getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
			XSSimpleTypeDefinition std = (XSSimpleTypeDefinition) xsElement.getTypeDefinition();
			path += "/text()";
			text = modelMgr.translatePath(element.getRoot(), path, NodeKind.text, element.getPathId(), getBaseType(std), 
					Occurrence.getOccurrence(minOccurs, maxOccurs));
			logger.trace("processElement; simple text: {}; type: {}; got XDMPath: {}", path, std, text); 
		}

		if (text != null) {
			element.setPostId(text.getPathId());
			modelMgr.updatePath(element);
		}
		if (parent.getPostId() < element.getPostId()) {
			parent.setPostId(element.getPostId());
			modelMgr.updatePath(parent);
		}
		
		parents.remove(xsElement);
		return element;
	}

	
    private void processAttribute(Path parent, String path, XSAttributeUse xsAttribute) throws BagriException {
    	
	    path += "/@" + xsAttribute.getAttrDeclaration().getName();
	    XSSimpleTypeDefinition std = xsAttribute.getAttrDeclaration().getTypeDefinition();
	    Occurrence occurrence = Occurrence.getOccurrence(
	    		xsAttribute.getRequired() ? 1 : 0,
	    		std.getVariety() == XSSimpleTypeDefinition.VARIETY_LIST ? -1 : 1);
		Path xp = modelMgr.translatePath(parent.getRoot(), path, NodeKind.attribute, parent.getPathId(), getBaseType(std), occurrence);
		logger.trace("processAttribute; attribute: {}; type: {}; got XDMPath: {}", path, std, xp); 
    }
	
	private Path processParticle(Path parent, String path, XSParticle xsParticle, Map<String, List<XSElementDeclaration>> substitutions,
			List<XSElementDeclaration> parents) throws BagriException {
		
		if (xsParticle == null) {
			return parent;
		}
		
	    XSTerm xsTerm = xsParticle.getTerm();
	    
	    Path particle = parent;
	    switch (xsTerm.getType()) {
	      case XSConstants.ELEMENT_DECLARATION:

	    	  particle = processElement(parent, path, (XSElementDeclaration) xsTerm, substitutions, parents, xsParticle.getMinOccurs(), xsParticle.getMaxOccurs());
	    	  break;

	      case XSConstants.MODEL_GROUP:

	    	  // this is one of the globally defined groups 
	    	  // (found in top-level declarations)

	    	  XSModelGroup xsGroup = (XSModelGroup) xsTerm;
	
		      // it also consists of particles
		      XSObjectList xsParticleList = xsGroup.getParticles();
		      for (int i = 0; i < xsParticleList.getLength(); i ++) {
		    	  XSParticle xsp = (XSParticle) xsParticleList.item(i);
		    	  particle = processParticle(parent, path, xsp, substitutions, parents);
		      }
	
		      //...
		      break;

	      case XSConstants.WILDCARD:

	          //...
	          break;
	    }
	    return particle;
	}
	
	private int getBaseType(XSTypeDefinition std) {
		if (std == null) {
			return XQItemType.XQBASETYPE_ANYSIMPLETYPE;
		}
		if (xs_ns.equals(std.getNamespace())) {
			QName qn = new QName(std.getNamespace(), std.getName());
			int type = getBaseTypeForTypeName(qn);
			logger.trace("getBaseType; returning {} for type {}", type, std.getName());
			return type;
		}
		return getBaseType(std.getBaseType());
	}
	
}
