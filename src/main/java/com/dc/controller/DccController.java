package com.dc.controller;

import com.dc.annotation.DcAutowired;
import com.dc.annotation.DcController;
import com.dc.annotation.DcRequestMapping;
import com.dc.annotation.DcRequestParam;
import com.dc.service.DcDemoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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


	}

}

