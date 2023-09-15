package com.dc.v1.servlet;

import com.dc.annotation.DcAutowired;
import com.dc.annotation.DcController;
import com.dc.annotation.DcRepository;
import com.dc.annotation.DcRequestMapping;
import com.dc.annotation.DcRequestParam;
import com.dc.annotation.DcService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Description:
 * Author: duancong
 * Date: 2023/9/13 18:07
 */
//入口
public class DisPatchservlet extends HttpServlet {

	//存储bean
	private static HashMap<String, Object> map = new HashMap<>();
	//存储 url 和 method 的映射
	private static HashMap<String, Method> map1 = new HashMap<>();


	// 1. servlet 容器配置完成, 请求可以正常被DisPatchservlet 接受,
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String requestURI = req.getRequestURI();
		System.out.println(requestURI);
		System.out.println("进来了 ..");
		//RequestMaping
		String url = requestURI.replaceAll("/+", "/");
		if (!map1.containsKey(url)) {
			resp.setStatus(404);
			resp.getWriter().print("<html>\n" +
					"<body>\n" +
					"<h2>404 </h2>\n" +
					"<p>   Not Found !!!</p>\n" +
					"</body>\n" +
					"</html> ");
			return;
		}

		Method o = map1.get(url);
		String typeName = o.getDeclaringClass().getTypeName();
		Object o1 = map.get(typeName);
		try {
			Object[] objects = getParams(req, resp, o);
			o.invoke(o1,objects);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		resp.getWriter().print("结束  ... . ");
		resp.setStatus(200);
	}

	private Object[] getParams(HttpServletRequest req, HttpServletResponse resp, Method o) {
		ArrayList<Object> objects = new ArrayList<>();
		Parameter[] parameters = o.getParameters();
		for (Parameter parameter : parameters) {
			Class<?> type = parameter.getType();
			if (type == String.class) {
				String param = getParam(req, parameter);
				objects.add(param);
			} else if (type == HttpServletRequest.class) {
				objects.add(req);
			} else if (type == HttpServletResponse.class) {
				objects.add(resp);
			} else if (type == Integer.class){
				String param = getParam(req, parameter);
				if (param != null){
					Integer integer = Integer.valueOf(param);
					objects.add(integer);
				}else {
					objects.add(param);
				}
			}
		}

		Object[] objects2 = objects.toArray(new Object[0]);
		return objects2;
	}

	private String getParam(HttpServletRequest req, Parameter parameter) {
		String s = null;
		if (parameter.isAnnotationPresent(DcRequestParam.class)) {
			DcRequestParam annotation = parameter.getAnnotation(DcRequestParam.class);
			String value = annotation.value();
			Map<String, String[]> parameterMap = req.getParameterMap();

			if(parameterMap.containsKey(value)){//判断是否有值
				s = Arrays.toString(parameterMap.get(value)).replaceAll("\\[|\\]","");

			}
		}
		return s;
	}


	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//		super.doPost(req, resp);
		doGet(req, resp);
	}

	//2. 容器启动的时候需要初始化执行的逻辑

	@Override
	public void init(ServletConfig config) throws ServletException {
		//1. 读取配置文件

		Properties properties = new Properties();
		try {
			System.out.println(config.getInitParameter("contextConfigLocation"));
			InputStream contextConfigLocation = this.getClass().getClassLoader().getResourceAsStream(config.getInitParameter("contextConfigLocation"));
			properties.load(contextConfigLocation);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String scanPackage = properties.getProperty("scanPackage");

		//2. 加载注入bean (IOC)
		doScan(scanPackage);
		//3. DI
		doinject(map.keySet());

		//4. 初始化组件

	}

	private void doinject(Set<String> keySet) {
		for (String s : keySet) {
			Object o = map.get(s);
			Class<?> aClass = o.getClass();
			//获取到所有的字段， 包括private protected default
			for (Field declaredField : aClass.getDeclaredFields()) {
				if (declaredField.isAnnotationPresent(DcAutowired.class)) {
					DcAutowired annotation = declaredField.getAnnotation(DcAutowired.class);
					String name = declaredField.getType().getName();
					//注入 属性
					declaredField.setAccessible(true);
					try {
						declaredField.set(o, map.get(name));
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}

			}

		}


	}

	public static void main(String[] args) throws ClassNotFoundException {
		Class<?> aClass = Class.forName("com.dc.controller.DccController");
		Field[] declaredFields = aClass.getDeclaredFields();
		for (Field declaredField : declaredFields) {
			System.out.println(declaredField.getType().getName());
			System.out.println(declaredField.getName());
		}
		System.out.println(aClass.getName());
		DisPatchservlet disPatchservlet = new DisPatchservlet();
		disPatchservlet.doScan("com.dc");
//		System.out.println("1111");
//		Integer integer = Integer.valueOf(null);

		String[] st={"1","2","3"};
		System.out.println(Arrays.toString(st).replaceAll("\\[|\\]","").replaceAll(",","").replaceAll("\\s+",""));
		int i= 1;
		for (int i1 = 0; i1 < 10; i1++) {

			if (i==1){
				System.out.println(1);
				continue;
			} else if (i==2){
				System.out.println(2);
			}
		}

//		System.out.println(integer);
	}

	private void doScan(String path) {
		URL resource = this.getClass().getResource(("/" + path.replaceAll("\\.", "/")));
		System.out.println(resource);
		File file = new File(resource.getFile());
		for (File file1 : file.listFiles()) {
			if (file1.isDirectory()) {
				doScan(path + "." + file1.getName());
			} else {
				String name1 = file1.getName();
				if (!name1.endsWith(".class")) {
					continue;
				} else {
					name1 = name1.replace(".class", "");
					try {
						Class<?> aClass = Class.forName(path + "." + name1);
						System.out.println(aClass.getSimpleName());
						String typeName = aClass.getTypeName();
						if (aClass.isAnnotationPresent(DcService.class)) {
							Object o = aClass.newInstance();
							Class<?>[] interfaces = aClass.getInterfaces();
							for (int i = 0; i < interfaces.length; i++) {
								map.put(interfaces[i].getTypeName(),o);
							}
							DcService annotation = aClass.getAnnotation(DcService.class);
							if ("".equals(annotation.value())) {
//								map.put(path+"."+tolowFirst(name1), o);
								map.put(typeName, o);
							} else {
								map.put(annotation.value(), o);
							}
						} else if (aClass.isAnnotationPresent(DcController.class)) {
							String baseurl = "";
							if (aClass.isAnnotationPresent(DcRequestMapping.class)) {
								DcRequestMapping annotation = aClass.getAnnotation(DcRequestMapping.class);
								String[] value = annotation.value();
								baseurl = value[0];
							}
							Method[] methods = aClass.getMethods();
							for (Method method : methods) {
								if (method.isAnnotationPresent(DcRequestMapping.class)) {
									DcRequestMapping annotation = method.getAnnotation(DcRequestMapping.class);
									String url= baseurl +annotation.value()[0];
									map1.put(url, method);
								}
							}
							Object o = aClass.newInstance();
							DcController annotation = aClass.getAnnotation(DcController.class);
							if ("".equals(annotation.value())) {
//								map.put(path+"."+tolowFirst(name1), o);
								map.put(typeName, o);
							} else {
								map.put(annotation.value(), o);
							}
						} else if (aClass.isAnnotationPresent(DcRepository.class)) {
							Object o = aClass.newInstance();
							DcRepository annotation = aClass.getAnnotation(DcRepository.class);
							if ("".equals(annotation.value())) {
								map.put(typeName, o);
//								map.put(path+"."+tolowFirst(name1), o);
							} else {
								map.put(annotation.value(), o);
							}
						}

					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InstantiationException e) {
						e.printStackTrace();
					}
				}


				System.out.println(map.size());
				System.out.println(name1);
			}


		}


	}

	private String tolowFirst(String name) {
		char[] chars = name.toCharArray();
		chars[0] += 32;
		return String.valueOf(chars);
	}
}
