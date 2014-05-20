package com.bagri.visualvm.manager.service;

import com.bagri.visualvm.manager.model.*;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class DefaultServiceProvider implements UserManagementService, ClusterManagementService, SchemaManagementService {
    private static final Logger LOGGER = Logger.getLogger(DefaultServiceProvider.class.getName());
    final MBeanServerConnection connection;

    public DefaultServiceProvider(MBeanServerConnection connection) {
        this.connection = connection;
    }

    @Override
    public List<User> getUsers() throws ServiceException {
        List<User> result;
        try {
            Object res = connection.invoke(new ObjectName("com.bagri.xdm:type=Management,name=UserManagement"), "getUserNames", null, null);
            String[] usersArray = (String[]) res;
            result = new ArrayList<User>(usersArray.length);
            for (String strUser : usersArray) {
                User u = new User(strUser);
                u.setActive(true);
                result.add(u);
            }
            return result;
        } catch (Exception e) {
            LOGGER.throwing(this.getClass().getName(), "getUserNames", e);
            throw new ServiceException(e);
        }
    }

    @Override
    public boolean addUser(String user, String password) throws ServiceException {
        try {
            Object res = connection.invoke(new ObjectName("com.bagri.xdm:type=Management,name=UserManagement"), "addUser",new Object[] {user, password}, new String[] {String.class.getName(), String.class.getName()});
            return (Boolean) res;
        } catch (Exception e) {
            LOGGER.throwing(this.getClass().getName(), "addUser", e);
            throw new ServiceException(e);
        }
    }

    @Override
    public boolean deleteUser(String user) throws ServiceException {
        try {
            Object res = connection.invoke(new ObjectName("com.bagri.xdm:type=Management,name=UserManagement"), "deleteUser",new Object[] {user}, new String[] {String.class.getName()});
            return (Boolean) res;
        } catch (Exception e) {
            LOGGER.throwing(this.getClass().getName(), "deleteUser", e);
            throw new ServiceException(e);
        }
    }

    @Override
    public boolean activateUser(String user, boolean activate) throws ServiceException {
        try {
            Object res = connection.invoke(new ObjectName("com.bagri.xdm:type=Management,name=UserManagement"), "activateUser",new Object[] {user, activate}, new String[] {String.class.getName(), boolean.class.getName()});
            return (Boolean) res;
        } catch (Exception e) {
            LOGGER.throwing(this.getClass().getName(), "activateUser", e);
            throw new ServiceException(e);
        }
    }

    @Override
    public boolean changePassword(String user, String password) throws ServiceException {
        try {
            Object res = connection.invoke(new ObjectName("com.bagri.xdm:type=Management,name=UserManagement"), "changePassword",new Object[] {user, password}, new String[] {String.class.getName(), String.class.getName()});
            return (Boolean) res;
        } catch (Exception e) {
            LOGGER.throwing(this.getClass().getName(), "changePassword", e);
            throw new ServiceException(e);
        }
    }

    @Override
    public Node getNode(ObjectName on) throws ServiceException {
        try {
            String nodeId = (String) connection.invoke(on, "getNodeId", null, null);
            String address = (String) connection.invoke(on, "getAddress", null, null);
            String[] deployedSchemas = new String[0];
            CompositeData optionsCd = null;
            try {
                deployedSchemas = (String[]) connection.invoke(on, "getDeployedSchemas",null, null);
            } catch (Exception e) {
                // Ignore it for now
            }
            try {
                optionsCd = (CompositeData) connection.invoke(on, "getOptions", null, null);
            } catch (Exception e) {
                // Ignore it for now
            }
            Node node = new Node(on, nodeId, address);
            node.setNodeOptions(convertCompositeToNodeOptions(optionsCd));
            node.setDeployedSchemas(Arrays.asList(deployedSchemas));
            return node;
        } catch (Exception e) {
            LOGGER.throwing(this.getClass().getName(), "getNodes", e);
            throw new ServiceException(e);
        }
    }

    @Override
    public List<Node> getNodes() throws ServiceException {
        try {
            Set<ObjectInstance> instances = connection.queryMBeans(new ObjectName("com.bagri.xdm:type=Node,name=*"), null);
            List<Node> nodes = new ArrayList<Node>();
            for (ObjectInstance instance : instances) {
                ObjectName on = instance.getObjectName();
                String nodeId = (String) connection.invoke(on, "getNodeId", null, null);
                String address = (String) connection.invoke(on, "getAddress", null, null);
                String[] deployedSchemas = new String[0];
                CompositeData optionsCd = null;
                try {
                    deployedSchemas = (String[]) connection.invoke(on, "getDeployedSchemas",null, null);
                } catch (Exception e) {
                    //Ignore it for now
                }
                try {
                    optionsCd = (CompositeData) connection.invoke(on, "getOptions", null, null);
                } catch (Exception e) {
                    //Ignore it for now
                }
                Node node = new Node(on, nodeId, address);
                node.setNodeOptions(convertCompositeToNodeOptions(optionsCd));
                node.setDeployedSchemas(Arrays.asList(deployedSchemas));
                nodes.add(node);
            }
            return nodes;
        } catch (Exception e) {
            LOGGER.throwing(this.getClass().getName(), "getNodes", e);
            throw new ServiceException(e);
        }
    }

    @Override
    public void saveNode(Node node) throws ServiceException {
        try {
            // TODO: What da hell, refactor it!!!. Define equals for NodeOption or inherit NodeOptions from Properties
            List<NodeOption> existing = convertCompositeToNodeOptions((CompositeData) connection.invoke(node.getObjectName(), "getOptions", null, null));
            List<String> toDelete = new ArrayList<String>();
            for (NodeOption option : existing) {
                String key = option.getOptionName();
                boolean found = false;
                for (NodeOption newOption : node.getNodeOptions()) {
                    if (key.equals(newOption.getOptionName())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    toDelete.add(key);
                }
            }
            for (String key : toDelete) {
                connection.invoke(node.getObjectName()
                        , "removeOption"
                        , new Object[] {key}
                        , new String[] {String.class.getName()});
            }
            for (NodeOption option : node.getNodeOptions()) {
                connection.invoke(node.getObjectName()
                        , "setOption", new Object[] {option.getOptionName()
                        , option.getOptionValue()}
                        , new String[]{String.class.getName()
                        , String.class.getName()});
            }
        } catch (Exception e) {
            LOGGER.throwing(this.getClass().getName(), "saveNode", e);
            throw new ServiceException(e);
        }
    }

    @Override
    public void addNode(Node node) throws ServiceException {
        String optionsStr = convertOptionsToString(node.getNodeOptions());
        try {
            connection.invoke(new ObjectName("com.bagri.xdm:type=Management,name=ClusterManagement")
                    , "addNode"
                    ,new Object[] {node.getAddress(), node.getNodeId(), optionsStr}
                    , new String[] {String.class.getName(), String.class.getName(), String.class.getName()});
        } catch (Exception e) {
            LOGGER.throwing(this.getClass().getName(), "addNode", e);
            throw new ServiceException(e);
        }
    }

    @Override
    public void deleteNode(Node node) throws ServiceException {
        try {
            connection.invoke(new ObjectName("com.bagri.xdm:type=Management,name=ClusterManagement")
                    , "deleteNode"
                    ,new Object[] {node.getAddress(), node.getNodeId()}
                    , new String[] {String.class.getName(), String.class.getName()});
        } catch (Exception e) {
            LOGGER.throwing(this.getClass().getName(), "addNode", e);
            throw new ServiceException(e);
        }
    }

    @Override
    public List<Schema> getSchemas() throws ServiceException {
        try {
            Set<ObjectInstance> instances = connection.queryMBeans(new ObjectName("com.bagri.xdm:type=Schema,name=*"), null);
            List<Schema> schemas = new ArrayList<Schema>();
            for (ObjectInstance instance : instances) {
                Schema schema = extractSchema(instance);
                schemas.add(schema);
            }
            return schemas;
        } catch (Exception e) {
            LOGGER.throwing(this.getClass().getName(), "getSchemas", e);
            throw new ServiceException(e);
        }
    }

    @Override
    public Properties getDefaultProperties() throws ServiceException {
        try {
            CompositeData cd = (CompositeData) connection.invoke(new ObjectName("com.bagri.xdm:type=Management,name=SchemaManagement")
                    , "getDefaultProperties"
                    , null
                    , null);
            return convertCompositeToProperties(cd);
        } catch (Exception e) {
            LOGGER.throwing(this.getClass().getName(), "getDefaultProperties", e);
            throw new ServiceException(e);
        }
    }

    @Override
    public void setDefaultProperty(Property property) throws ServiceException {
        try {
            connection.invoke(new ObjectName("com.bagri.xdm:type=Management,name=SchemaManagement")
                    , "setDefaultProperty"
                    , new Object[] {property.getPropertyName(), property.getPropertyValue()}
                    , new String[] {String.class.getName(), String.class.getName()});
        } catch (Exception e) {
            LOGGER.throwing(this.getClass().getName(), "setDefaultProperty", e);
            throw new ServiceException(e);
        }
    }

    @Override
    public void addSchema(Schema schema) throws ServiceException {
        try {
            connection.invoke(new ObjectName("com.bagri.xdm:type=Management,name=SchemaManagement")
                    , "createSchema"
                    , new Object[] {schema.getSchemaName(), schema.getDescription(), convertPropertiesToString(schema.getProperties())}
                    , new String[] {String.class.getName(), String.class.getName(), String.class.getName()});
        } catch (Exception e) {
            LOGGER.throwing(this.getClass().getName(), "addSchema", e);
            throw new ServiceException(e);
        }
    }

    @Override
    public Schema getSchema(String schemaName) throws ServiceException {
        try {
            ObjectInstance oi = connection.getObjectInstance(new ObjectName("com.bagri.xdm:type=Schema,name=" + schemaName));
            if (null != oi) {
                return extractSchema(oi);
            }
            return null;
        } catch (Exception e) {
            LOGGER.throwing(this.getClass().getName(), "getSchema", e);
            throw new ServiceException(e);
        }
    }

    @Override
    public Schema getSchema(ObjectName objectName) throws ServiceException {
        try {
            ObjectInstance oi = connection.getObjectInstance(objectName);
            if (null != oi) {
                return extractSchema(oi);
            }
            return null;
        } catch (Exception e) {
            LOGGER.throwing(this.getClass().getName(), "getSchema", e);
            throw new ServiceException(e);
        }
    }

    @Override
    public void saveSchema(Schema schema) throws ServiceException {
        try {
            if (null != schema.getDescription()) {
                connection.invoke(schema.getObjectName()
                        , "setDescription"
                        , new Object[] {schema.getDescription()}
                        , new String[] {String.class.getName()});
            }
            if (schema.isActive()) {
                connection.invoke(schema.getObjectName()
                        , "activateSchema"
                        , null
                        , null);
            } else {
                connection.invoke(schema.getObjectName()
                        , "deactivateSchema"
                        , null
                        , null);
            }
            Properties existing = convertCompositeToProperties((CompositeData) connection.invoke(schema.getObjectName(), "getProperties", null, null));
            existing.keySet().removeAll(schema.getProperties().keySet());
            Set<Object> toDelete = existing.keySet();
            for (Object key : toDelete) {
                connection.invoke(schema.getObjectName()
                        , "removeProperty"
                        , new Object[] {key.toString()}
                        , new String[] {String.class.getName()});
            }
            for (Object key : schema.getProperties().keySet()) {
                connection.invoke(schema.getObjectName()
                        , "setProperty"
                        , new Object[] {key.toString(), schema.getProperties().getProperty(key.toString())}
                        , new String[]{String.class.getName(), String.class.getName()});
            }
        } catch (Exception e) {
            LOGGER.throwing(this.getClass().getName(), "saveSchema", e);
            throw new ServiceException(e);
        }
    }

    @Override
    public void deleteSchema(Schema schema) throws ServiceException {
        try {
            connection.invoke(new ObjectName("com.bagri.xdm:type=Management,name=SchemaManagement")
                    , "destroySchema"
                    , new Object[] {schema.getSchemaName()}
                    , new String[] {String.class.getName()});
        } catch (Exception e) {
            LOGGER.throwing(this.getClass().getName(), "deleteSchema", e);
            throw new ServiceException(e);
        }
    }

    @Override
    public Object runQuery(Schema schema, String query) throws ServiceException {
        try {
            Object res = connection.invoke(new ObjectName("com.bagri.xdm:type=Schema,kind=QueryManagement,name=" + schema.getSchemaName())
                    , "runQuery"
                    , new Object[] {query}
                    , new String[] {String.class.getName()}
            );
            return res;
        } catch (Exception e) {
            LOGGER.throwing(this.getClass().getName(), "runQuery", e);
            throw new ServiceException(e);
        }
    }

    private Schema extractSchema(ObjectInstance oi) {
        ObjectName on = oi.getObjectName();
        String name = null;
        String description = null;
        String persistenceType =  null;
        String state = null;
        boolean isActive = false;
        int version = -1;
        String[] registeredTypes = new String[0];
        CompositeData propertiesCd = null;

        try {
            name = (String) connection.invoke(on, "getName", null, null);
        } catch (Exception e) {/* Ignore it for now */}
        try {
            description = (String) connection.invoke(on, "getDescription", null, null);
        } catch (Exception e) {/* Ignore it for now */}
        try {
            persistenceType = (String) connection.invoke(on, "getPersistenceType", null, null);
        } catch (Exception e) {/* Ignore it for now */}
        try {
            state = (String) connection.invoke(on, "getState", null, null);
        } catch (Exception e) {/* Ignore it for now */}
        try {
            isActive = (Boolean) connection.invoke(on, "isActive", null, null);
        } catch (Exception e) {/* Ignore it for now */}
        try {
            version = (Integer) connection.invoke(on, "getVersion", null, null);
        } catch (Exception e) {/* Ignore it for now */}
        try {
            registeredTypes = (String[]) connection.invoke(on, "getRegisteredTypes",null, null);
        } catch (Exception e) {/* Ignore it for now */}
        try {
            propertiesCd = (CompositeData) connection.invoke(on, "getProperties", null, null);
        } catch (Exception e) {/* Ignore it for now */}

        Schema schema = new Schema(name);
        schema.setObjectName(on);
        schema.setDescription(description);
        schema.setPersistenceType(persistenceType);
        schema.setState(state);
        schema.setActive(isActive);
        schema.setVersion(version);
        schema.setRegisteredTypes(registeredTypes);
        schema.setProperties(convertCompositeToProperties(propertiesCd));
        return schema;
    }

    private List<NodeOption> convertCompositeToNodeOptions(CompositeData cd) {
        List<NodeOption> options = new ArrayList<NodeOption>();
        if (null == cd) {
            return options;
        }
        Set<String> keys = cd.getCompositeType().keySet();
        for (String key : keys) {
            String value = (String) cd.get(key);
            NodeOption option = new NodeOption(key, value);
            options.add(option);
        }
        return options;
    }

    private String convertOptionsToString(List<NodeOption> options) {
        String result = "";
        for (NodeOption o : options) {
            result += o.getOptionName() + "=" + o.getOptionValue() + ";";
        }
        return result;
    }

    private String convertPropertiesToString(Properties properties) {
        String result = "";
        for (Object key : properties.keySet()) {
            result += key + "=" + properties.get(key) + ";";
        }
        return result;
    }

    private Properties convertCompositeToProperties(CompositeData cd) {
        Properties properties = new Properties();
        if (null == cd) {
            return properties;
        }
        Set<String> keys = cd.getCompositeType().keySet();
        for (String key : keys) {
            String value = (String) cd.get(key);
            properties.put(key, value);
        }
        return properties;
    }


}
