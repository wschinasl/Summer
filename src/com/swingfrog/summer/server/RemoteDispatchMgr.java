package com.swingfrog.summer.server;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.swingfrog.summer.annotation.Optional;
import com.swingfrog.summer.ioc.ContainerMgr;
import com.swingfrog.summer.ioc.MehodParameterName;
import com.swingfrog.summer.protocol.SessionRequest;
import com.swingfrog.summer.protocol.SessionResponse;
import com.swingfrog.summer.server.exception.CodeException;
import com.swingfrog.summer.server.exception.SessionException;
import com.swingfrog.summer.web.WebRequest;
import com.swingfrog.summer.web.exception.WebSessionException;
import com.swingfrog.summer.web.view.WebView;

import javassist.NotFoundException;

public class RemoteDispatchMgr {
	
	private static Logger log = LoggerFactory.getLogger(RemoteDispatchMgr.class);
	private Map<String, RemoteClass> remoteClassMap;

	private static class SingleCase {
		public static final RemoteDispatchMgr INSTANCE = new RemoteDispatchMgr();
	}
	
	private RemoteDispatchMgr() {
		remoteClassMap = new HashMap<>();
	}
	
	public static RemoteDispatchMgr get() {
		return SingleCase.INSTANCE;
	}
	
	public void init() throws NotFoundException {
		Iterator<Class<?>> ite = ContainerMgr.get().iteratorRemoteList();
		while (ite.hasNext()) {
			Class<?> clazz = ite.next();
			log.info("server register remote {}", clazz.getSimpleName());
			remoteClassMap.put(clazz.getSimpleName(), new RemoteClass(clazz));
		}
	}
	
	public Method getMethod(SessionRequest req) {
		String remote = req.getRemote();
		String method = req.getMethod();
		RemoteClass remoteClass = remoteClassMap.get(remote);
		if (remoteClass != null) {
			RemoteMethod remoteMethod = remoteClass.getRemoteMethod(method);
			if (remoteMethod != null) {
				return remoteMethod.getMethod();
			}
		}
		return null;
	}
	
	private Object invoke(String remote, String method, JSONObject data, Map<Class<?>, Object> autoObj, Map<String, Object> autoNameObj) throws Exception {
		RemoteClass remoteClass = remoteClassMap.get(remote);
		if (remoteClass != null) {
			RemoteMethod remoteMethod = remoteClass.getRemoteMethod(method);
			if (remoteMethod != null) {
				Object remoteObj = ContainerMgr.get().getDeclaredComponent(remoteClass.getClazz());
				Method remoteMod = remoteMethod.getMethod();
				String[] params = remoteMethod.getParams();
				Type[] paramTypes = remoteMethod.paramTypes();
				Parameter[] parameters = remoteMethod.getParameters();
				boolean auto = ContainerMgr.get().isAutowiredParameter(remoteClass.getClazz());
				Object[] obj = new Object[params.length];
				try {
					for (int i = 0; i < params.length; i++) {
						String param = params[i];
						Type type = paramTypes[i];
						Parameter parameter = parameters[i];
						if (type == boolean.class) {
							obj[i] = data.getBooleanValue(param);
						} else if (type == byte.class) {
							obj[i] = data.getByteValue(param);
						} else if (type == short.class) {
							obj[i] = data.getShortValue(param);
						} else if (type == int.class) {
							obj[i] = data.getIntValue(param);
						} else if (type == long.class) {
							obj[i] = data.getLongValue(param);
						} else if (type == Boolean.class) {
							obj[i] = data.getBoolean(param);
						} else if (type == Byte.class) {
							obj[i] = data.getByte(param);
						} else if (type == Short.class) {
							obj[i] = data.getShort(param);
						} else if (type == Integer.class) {
							obj[i] = data.getInteger(param);
						} else if (type == Long.class) {
							obj[i] = data.getLong(param);
						} else if (type == String.class) {
							obj[i] = data.getString(param);
						} else {
							if (data.containsKey(param)) {
								try {
									obj[i] = JSON.parseObject(data.getString(param), type);
								} catch (Exception e) {
									log.error(e.getMessage(), e);
								}
							} else {
								if (auto) {
									if (autoObj != null && autoObj.containsKey(type)) {
										obj[i] = autoObj.get(type);
									} else if (autoNameObj != null && autoNameObj.containsKey(param)) {
										obj[i] = autoNameObj.get(param);
									} else {
										obj[i] = ContainerMgr.get().getComponent((Class<?>) type);
										if (obj[i] == null) {
											try {
												obj[i] = ((Class<?>) type).newInstance();
											} catch (Exception e) {
												log.error(e.getMessage(), e);
											}
										}
									}
								} 
							}
						}
						if (obj[i] == null) {
							if (!parameter.isAnnotationPresent(Optional.class)) {
								throw new CodeException(SessionException.PARAMETER_ERROR);
							}
						}
					}
				} catch (Exception e) {
					throw new CodeException(SessionException.PARAMETER_ERROR);
				}
				return remoteMod.invoke(remoteObj, obj);
			} else {
				throw new CodeException(SessionException.METHOD_NOT_EXIST);
			}
		} else {
			throw new CodeException(SessionException.REMOTE_NOT_EXIST);
		}
	}
	
	public SessionResponse process(SessionRequest req, SessionContext sctx) throws Exception {
		String remote = req.getRemote();
		String method = req.getMethod();
		JSONObject data = req.getData();
		Map<Class<?>, Object> autoObj = new HashMap<>();
		autoObj.put(SessionContext.class, sctx);
		return SessionResponse.buildMsg(req, invoke(remote, method, data, autoObj, null));
	}
	
	public WebView webProcess(WebRequest req, SessionContext sctx) throws Exception {
		String remote = req.getRemote();
		String method = req.getMethod();
		JSONObject data = req.getData();
		Map<Class<?>, Object> autoObj = new HashMap<>();
		autoObj.put(SessionContext.class, sctx);
		Map<String, Object> autoNameObj = new HashMap<>();
		for (String key : req.getFileUploadMap().keySet()) {
			autoNameObj.put(key, req.getFileUploadMap().get(key));
		}
		Object obj = invoke(remote, method, data, autoObj, autoNameObj);
		if (obj == null) {
			return null;
		}
		if (obj instanceof WebView) {
			return (WebView) obj;
		} else {
			throw new CodeException(WebSessionException.NOT_WEB_VIEW);
		}
	}
	
	private class RemoteClass {
		private Class<?> clazz;
		private Map<String, RemoteMethod> remoteMethodMap = new HashMap<>();
		public RemoteClass(Class<?> clazz) throws NotFoundException {
			this.clazz = clazz;
			Method[] methods = clazz.getDeclaredMethods();
			for (int i = 0; i < methods.length; i++) {
				Method method = methods[i];
				log.info("remote register {}.{}", clazz.getSimpleName(), method.getName());
				remoteMethodMap.put(method.getName(), new RemoteMethod(method, new MehodParameterName(clazz)));
			}
		}
		public Class<?> getClazz() {
			return clazz;
		}
		public RemoteMethod getRemoteMethod(String method) {
			return remoteMethodMap.get(method);
		}
	}
	
	private class RemoteMethod {
		private Method method;
		private String[] params;
		private Type[] paramTypes;
		private Parameter[] parameters;
		public RemoteMethod(Method method, MehodParameterName mpn) throws NotFoundException {
			this.method = method;
			paramTypes = method.getGenericParameterTypes();
			params = mpn.getParameterNameByMethod(method);
			parameters = method.getParameters();
		}
		public Method getMethod() {
			return method;
		}
		public String[] getParams() {
			return params;
		}
		public Type[] paramTypes() {
			return paramTypes;
		}
		public Parameter[] getParameters() {
			return parameters;
		}
	}
}
