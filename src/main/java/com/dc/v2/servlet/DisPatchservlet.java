package com.dc.v2.servlet;

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
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Description: 代码改造
 * Author: duancong
 * Date: 2023/9/15 15:22
 */
public class DisPatchservlet extends HttpServlet {
	//读取默认配置文件的 信息
	private Properties properties = new Properties();

	private List<String> classpaths = new ArrayList<>();

	private HashMap<String, Object> ioc = new HashMap<>();

	private HashMap<String, Method> requestMapping = new HashMap<>();

	//  执行阶段入口，
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req,resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		System.out.println("请求进来了 ...");
		String requestURI = req.getRequestURI();
		String key = requestURI.replaceAll("/+", "/");
		if (!requestMapping.containsKey(key)){
			resp.getWriter().print("<html>\n" +
					"<body>\n" +
					"<h2>404 </h2>\n" +
					"<p>   Not Found !!!</p>\n" +
					"</body>\n" +
					"</html> ");
			resp.setStatus(404);
			return;
		}

		try {
			Method method = requestMapping.get(key);
			String simpleName = method.getDeclaringClass().getSimpleName();
			Object o = ioc.get(simpleName);
			Object[] params = getParams(req, resp, method);
			method.invoke(o,params);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}


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


	//初始化阶段
	@Override
	public void init(ServletConfig config) throws ServletException {
		//1.初始化配置，加载配置servlet的配置，
		doinit(config);
		String scanPackage = properties.getProperty("scanPackage");
		String replace = scanPackage.replaceAll("\\.", "/");

		//2.根据配置文件的路径，扫描包路径 获取需要加载的类路径
		doScan(replace);

		//3.实例化需要注入的类，注入ioc
		doInstance();

		//4.依赖注入
		doInject();

		//5.初始化组件，requestMapping
		registerRequestMapping();

		System.out.println("执行完成......");

	}

	private void registerRequestMapping() {
		try {
			for (String classpath : classpaths) {
				Class<?> aClass = Class.forName(classpath);
				String base = "";
				if (aClass.isAnnotationPresent(DcRequestMapping.class)){
					DcRequestMapping annotation = aClass.getAnnotation(DcRequestMapping.class);
					base = annotation.value()[0];
				}
				Method[] methods = aClass.getMethods();
				for (Method method : methods) {
					if (!method.isAnnotationPresent(DcRequestMapping.class)){
						continue;
					}
					DcRequestMapping annotation = method.getAnnotation(DcRequestMapping.class);
					String url =("/" + base +"/" +annotation.value()[0]).replaceAll("/+","/");
					requestMapping.put(url,method);

				}
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

	private void doInject() {
		try {
			for (String classpath : classpaths) {
				Class<?> aClass = Class.forName(classpath);
				Field[] declaredFields = aClass.getDeclaredFields();
				for (Field declaredField : declaredFields) {
					if (declaredField.isAnnotationPresent(DcAutowired.class)) {
						//获取到对象
						Object o = ioc.get(aClass.getSimpleName());
						Object o1 = ioc.get(declaredField.getType().getName());
						declaredField.setAccessible(true);
						declaredField.set(o, o1);
					}
				}
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	private void doInstance() {
		for (String classpath : classpaths) {
			try {
				Class<?> aClass = Class.forName(classpath);
				if (aClass.isAnnotationPresent(DcController.class)||aClass.isAnnotationPresent(DcRepository.class)){
					Object o = aClass.newInstance();
					ioc.put(aClass.getSimpleName(),o); //注册的时候可以根据驼峰命名发将首字符小写,
				}else if (aClass.isAnnotationPresent(DcService.class)){
					Class<?>[] interfaces = aClass.getInterfaces();
					Object o = aClass.newInstance();
					for (Class<?> anInterface : interfaces) {
						if (ioc.containsKey(aClass.getTypeName())){
							throw new IllegalArgumentException("接口类型 " +anInterface.getTypeName()+"已存在");
						}else{
							ioc.put(anInterface.getTypeName(),o);
						}
					}
					ioc.put(aClass.getSimpleName(),o);//注册的时候可以根据驼峰命名发将首字符小写,
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			}

		}
	}

	private void doScan(String scanPackage) {
		 //包名
		URL resource = this.getClass().getClassLoader().getResource(scanPackage);
		File file = new File(resource.getFile());
		for (File listFile : file.listFiles()) {
			String name = listFile.getName();
			System.out.println(name);
			if (listFile.isDirectory()){
				doScan(scanPackage+"/"+name);
			}else if (name.endsWith(".class")){
				//记录需要的类名字, 方便后面反射创建实例对象.
				classpaths.add((scanPackage+"/"+name.replace(".class","")).replaceAll("/+","\\."));
			}

		}
	}

	private void doinit(ServletConfig config) {
		String contextConfigLocation = config.getInitParameter("contextConfigLocation");
		try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation)){
			properties.load(resourceAsStream);      //加载配置信息.
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
