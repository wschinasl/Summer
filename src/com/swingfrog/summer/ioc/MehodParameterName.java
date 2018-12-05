package com.swingfrog.summer.ioc;

import java.lang.reflect.Method;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

public class MehodParameterName {

	private CtClass ct;
	
	public MehodParameterName(Class<?> c) throws NotFoundException {
		ct = ClassPool.getDefault().get(c.getName());
	}
	
	public String[] getParameterNameByMethod(Method method) throws NotFoundException {
		CtMethod ctm = ct.getDeclaredMethod(method.getName());
		MethodInfo methodInfo = ctm.getMethodInfo();
		CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
		LocalVariableAttribute attr = (LocalVariableAttribute)codeAttribute.getAttribute(LocalVariableAttribute.tag);
		String[] params = null;
		if (attr != null) {
			int len = ctm.getParameterTypes().length;
			params = new String[len];
			int pos = Modifier.isStatic(ctm.getModifiers()) ? 0 : 1;
			for (int i=0;i<len;i++) {
				params[i] = attr.variableName(i+pos);
			}
		}
		return params;
	}
	
}
