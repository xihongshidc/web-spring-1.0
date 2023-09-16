package com.dc.v3.servlet;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Description:
 * Author: duancong
 * Date: 2023/9/16 18:12
 */
public class HandlerMapping {
	//体现了类的单一指责原则, 避免代码过度冗余 ,同时可以提供多的功能,方便理解.

	private Method method;

	private String url;

	private Object controller;

	private Parameter[] parameter;

	public HandlerMapping(Method method, String url, Object controller) {
		this.method = method;
		this.url = url;
		this.controller = controller;
		parameter=method.getParameters();
	}

	public Method getMethod() {
		return method;
	}

	public String getUrl() {
		return url;
	}

	public Object getController() {
		return controller;
	}

	public Parameter[] getParameter() {
		return parameter;
	}
}
