package com.bagri.xdm.system;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = "http://www.bagri.com/xdm/access", propOrder = {
		"perms"
})
public class XDMPermissions {

	  @XmlElement(name = "permission")
	  private List<XDMPermission> perms = new ArrayList<>();

	  List<XDMPermission> permissions() {
		  return perms;
	  }
	 
	  void addPermission(XDMPermission perm) {
		  perms.add(perm);
	  }

}