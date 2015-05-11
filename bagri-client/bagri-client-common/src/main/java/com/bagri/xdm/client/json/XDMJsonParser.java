package com.bagri.xdm.client.json;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.json.stream.JsonParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bagri.xdm.api.XDMModelManagement;
import com.bagri.xdm.client.parser.XDMDataParser;
import com.bagri.xdm.domain.XDMData;
import com.bagri.xdm.domain.XDMElement;
import com.bagri.xdm.domain.XDMNodeKind;
import com.bagri.xdm.domain.XDMParser;
import com.bagri.xdm.domain.XDMPath;

public class XDMJsonParser extends XDMDataParser implements XDMParser {
	
	private static JsonParserFactory factory;
	static {
		//JsonProvider provider = JsonProvider.provider();
		//Map<String, Boolean> config = new HashMap<String, Boolean>();
		//config.put(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES.name(), true);
		//config.put(JsonParser.Feature.ALLOW_COMMENTS.name(), true);
		//config.put(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT.name(), true);
		//provider.createParserFactory(config); // Understands JsonFactory and JsonParser features
		
		Map<String, Object> params = new HashMap<String, Object>();
		//params.put("javax.json.spi.JsonProvider", "com.github.pgelinas.jackson.javax.json.spi.JacksonProvider");
		factory = Json.createParserFactory(params);
	}
	
	public static List<XDMData> parseDocument(XDMModelManagement dictionary, String json) throws IOException {
		XDMJsonParser parser = new XDMJsonParser(dictionary);
		return parser.parse(json);
	}
	
	public XDMJsonParser(XDMModelManagement dict) {
		super(dict);
	}

	@Override
	public List<XDMData> parse(String json) throws IOException { 
		try (Reader reader = new StringReader(json)) {
			return parse(reader);
		}
	}
	
	@Override
	public List<XDMData> parse(File file) throws IOException {
		try (Reader reader = new FileReader(file)) {
			return parse(reader);
		}
	}
	
	@Override
	public List<XDMData> parse(InputStream stream) throws IOException {

		try (JsonParser parser = factory.createParser(stream)) {
			return parse(parser);
		}
	}
	
	@Override
	public List<XDMData> parse(Reader reader) throws IOException {
		
		try (JsonParser parser = factory.createParser(reader)) {
			return parse(parser);
		}
	}

	public List<XDMData> parse(JsonParser parser) throws IOException {
		
		logger.trace("parse.enter; parser: {}", parser);
		
		init();
		while (parser.hasNext()) {
			processEvent(parser);
		}
		cleanup();

		List<XDMData> result = dataList;
		dataList = null;
		logger.trace("parse.exit; returning {} elements", result); //.size());
		return result;
	}
	
	private void processEvent(JsonParser parser) throws IOException { //, XMLStreamException {

		JsonParser.Event event = parser.next();
		if (event == Event.VALUE_STRING || event == Event.VALUE_NUMBER) {
			logger.trace("processEvent; got token: {}; value: {}", event.name(), parser.getString());
		} else if (event == Event.KEY_NAME) {
			logger.trace("processEvent; got token: {}; key: {}", event.name(), parser.getString());
		} else {
			logger.trace("processEvent; got token: {}", event.name()); 
		}
		
		switch (event) {
			
			case START_OBJECT:
				if (dataStack.size() == 0) {
					parser.next();
					processDocument(parser.getString());
					processStartElement(parser.getString());
				} else {
					processStartElement(false);
				}
				break;
			case START_ARRAY: 
				processStartElement(true);
				break;
			case KEY_NAME:
				processStartElement(parser.getString());
				break;
			case END_ARRAY: 
				processEndElement();
			case END_OBJECT:
				processEndElement();
				break;
			case VALUE_FALSE:
			case VALUE_NULL:
			case VALUE_NUMBER:
			case VALUE_TRUE:
			case VALUE_STRING:
				processValueElement(parser.getString());
				break;
			default: 
				logger.trace("processEvent; unknown event: {}", event);
		}			
	}
	
	private XDMData getTopData() {
		for (int i = dataStack.size() - 1; i >= 0; i--) {
			XDMData data = dataStack.elementAt(i);
			if (data != null && data.getElement() != null) {
				return data;
			}
		}
		return null;
	}

	private void processDocument(String name) {

		String root = "/" + (name == null ? "" : name);
		docType = dict.translateDocumentType(root);
		XDMPath path = dict.translatePath(docType, "", XDMNodeKind.document);
		XDMElement start = new XDMElement();
		start.setElementId(elementId++);
		//start.setParentId(0); // -1 ?
		XDMData data = new XDMData(path, start);
		dataStack.add(data);
		dataList.add(data);
	}

	private boolean isAttribute(String name) {
		return name.startsWith("-") || name.startsWith("@");
	}
	
	private void processStartElement(boolean isArray) {
		if (isArray) {
			dataStack.add(null);
		} else {
			XDMData current = dataStack.lastElement(); //getTopData(); 
			if (current == null || current.getNodeKind() != XDMNodeKind.element) {
				dataStack.add(null);
			}
		}
	}
	
	private void processStartElement(String name) {
		
		XDMData parent = getTopData();
		if (!name.equals(parent.getName())) {
			XDMData current = null;
			if (isAttribute(name)) {
				name = name.substring(1);
				if (name.startsWith("xmlns")) {
					current = addData(parent, XDMNodeKind.namespace, "/#" + name, null);
				} else {
					current = addData(parent, XDMNodeKind.attribute, "/@" + name, null);
				}
			} else if (name.equals("#text")) {
				//dataStack.add(null);
				current = new XDMData(null, null);  
			} else {
				current = addData(parent, XDMNodeKind.element, "/" + name, null); 
			}
			if (current != null) {
				dataStack.add(current);
			}
		}
	}

	private void processEndElement() {
		if (dataStack.size() > 0) {
			XDMData current = dataStack.pop();
			logger.trace("processEndElement; got current: {}", current);
		}
	}

	private void processValueElement(String value) {
		
		//value = value.replaceAll("&", "&amp;");
	
		XDMData current = dataStack.pop();
		boolean isArray = current == null;
		if (isArray || current.getElement() == null) {
			//current = dataStack.peek();
			current = getTopData();
		}
		if (current.getNodeKind() == XDMNodeKind.element) {
			addData(current, XDMNodeKind.text, "/text()", value);
		//} else if (current.getNodeKind() == XDMNodeKind.text) {
		//	current.getElement().setValue(value);
		} else {
			current.getElement().setValue(value);
		}
		if (isArray) {
			dataStack.add(null);
		}
	}	
}
