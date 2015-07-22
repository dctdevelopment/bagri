package com.bagri.common.util;

import java.lang.reflect.Constructor;

public class ReflectUtils {

	public static Class type2Class(String type) throws ClassNotFoundException {
		switch (type) {
			case "boolean": return boolean.class; 
			case "byte": return byte.class; 
			case "char": return char.class; 
			case "double": return double.class; 
			case "float": return float.class; 
			case "int": return int.class; 
			case "integer": return int.class; 
			case "long": return long.class; 
			case "short": return short.class; 
			case "string": return java.lang.String.class; 
		}
		return Class.forName(type);
	}
	
	public static Class type2Wrapper(String type) throws ClassNotFoundException {
		switch (type) {
			case "boolean": return Boolean.class; 
			case "byte": return Byte.class; 
			case "char": return Character.class; 
			case "double": return Double.class; 
			case "float": return Float.class; 
			case "int": return Integer.class; 
			case "long": return Long.class; 
			case "short": return Short.class; 
		}
		return Class.forName(type);
	}

	public static Object getValue(Class<?> cls, String value) throws Exception {
		Constructor<?> c = cls.getConstructor(String.class);
		return c.newInstance(value);
	}
	
}
