package com.swingfrog.summer.config;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigMgr {

	private static final Logger log = LoggerFactory.getLogger(ConfigMgr.class);

	private ServerConfig serverConfig;
	private ServerConfig[] minorConfigs;
	private ClientConfig[] clientConfigs;

	private static class SingleCase {
		public static final ConfigMgr INSTANCE = new ConfigMgr();
	}
	
	private ConfigMgr() {
		serverConfig = new ServerConfig();
	}
	
	public static ConfigMgr get() {
		return SingleCase.INSTANCE;
	}

	public void loadConfig(String path) throws IOException {
		loadConfig(new FileInputStream(path));
	}

	public void loadConfig(InputStream in) throws IOException {
		Properties pro = new Properties();
		pro.load(in);
		loadDataWithBean(pro, "server.", serverConfig);
		String minorList = pro.getProperty("server.minorList");
		if (minorList != null && minorList.length() > 0) {
			String[] minors = getValueByTypeAndString(String[].class, minorList);
			minorConfigs = new ServerConfig[minors.length];
			for (int i = 0; i < minorConfigs.length; i ++) {
				minorConfigs[i] = new ServerConfig();
				loadDataWithBean(pro, String.format("minor.%s.", minors[i]), minorConfigs[i]);
			}
		}
		String clientList = pro.getProperty("server.clientList");
		if (clientList != null && clientList.length() > 0) {
			String[] clients = getValueByTypeAndString(String[].class, clientList);
			clientConfigs = new ClientConfig[clients.length];
			for (int i = 0; i < clientConfigs.length; i ++) {
				clientConfigs[i] = new ClientConfig();
				loadDataWithBean(pro, String.format("client.%s.", clients[i]), clientConfigs[i]);
			}
		}
		in.close();
		pro.clear();
	}

	private void loadDataWithBean(Properties pro, String prefix, Object dest) {
		Class<?> destClass = dest.getClass();
		Field[] destFields = destClass.getDeclaredFields();
		Field destField = null;
		Method[] destMethods = new Method[destFields.length];
		for (int i = 0; i < destFields.length; i++) {
			destField = destFields[i];
			if (destField != null) {
				try {
					destMethods[i] = new PropertyDescriptor(destField.getName(), destClass).getWriteMethod();
				} catch (IntrospectionException e) {
					log.error(e.getMessage(), e);
				}
			}
		}
		try {
			for (int i = 0; i < destFields.length; i++) {
				destMethods[i].invoke(dest, (Object)getValueByTypeAndString(destFields[i].getType(), pro.getProperty(prefix + destFields[i].getName())));
			}
		} catch (IllegalAccessException e) {
			log.error(e.getMessage(), e);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage(), e);
		} catch (InvocationTargetException e) {
			log.error(e.getMessage(), e);
		}
	}
	
	@SuppressWarnings("unchecked")
	private <T> T getValueByTypeAndString(Class<?> clazz, String value) {
		if (clazz == byte.class || clazz == short.class || clazz == int.class || clazz == long.class ||
				clazz == Byte.class || clazz == Short.class || clazz == Integer.class || clazz == Long.class) {
			return (T)Integer.valueOf(value);
		} else if (clazz == boolean.class || clazz == Boolean.class) {
			return (T)Boolean.valueOf(value);
		} else if (clazz == String[].class) {
			return (T)value.split(",");
		} else if (clazz == byte[].class || clazz == short[].class || clazz == int[].class || clazz == long[].class ||
				clazz == Byte[].class || clazz == Short[].class || clazz == Integer[].class || clazz == Long[].class) {
			String[] strs = value.split(",");
			Integer[] values = new Integer[strs.length];
			for (int i = 0; i < strs.length ; i ++) {
				values[i] = Integer.valueOf(strs[i]);
			}
			return (T)values;
		} else if (clazz == boolean[].class || clazz == Boolean[].class) {
			String[] strs = value.split(",");
			Boolean[] values = new Boolean[strs.length];
			for (int i = 0; i < strs.length ; i ++) {
				values[i] = Boolean.valueOf(strs[i]);
			}
			return (T)values;
		}
		return (T)value;
	}

	public ServerConfig getServerConfig() {
		return serverConfig;
	}
	
	public ServerConfig[] getMinorConfigs() {
		return minorConfigs;
	}
	
	public ClientConfig[] getClientConfigs() {
		return clientConfigs;
	}
	
}
