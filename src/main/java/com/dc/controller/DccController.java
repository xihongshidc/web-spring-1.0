package com.dc.controller;

import com.dc.annotation.DcAutowired;
import com.dc.annotation.DcController;
import com.dc.annotation.DcRequestMapping;
import com.dc.annotation.DcRequestParam;
import com.dc.service.DcDemoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Description:
 * Author: duancong
 * Date: 2023/9/13 17:56
 */

@DcRequestMapping("/user")
@DcController
public class DccController {


	@DcAutowired
	private DcDemoService dcDemoService;

	@DcRequestMapping("/query")
	public void query(HttpServletRequest request, HttpServletResponse response , @DcRequestParam("name") String name){
		String print = dcDemoService.print(name);
		System.out.println(print);
		try {
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().print(print);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	@DcRequestMapping("/add")
	public void add(HttpServletRequest request, HttpServletResponse response , @DcRequestParam("add") Integer name){
		String print = dcDemoService.print(String.valueOf(name));
		System.out.println(print);
		try {
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().print(true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

