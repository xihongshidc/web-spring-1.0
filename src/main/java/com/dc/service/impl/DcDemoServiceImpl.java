package com.dc.service.impl;

import com.dc.annotation.DcService;
import com.dc.service.DcDemoService;

/**
 * Description:
 * Author: duancong
 * Date: 2023/9/14 14:58
 */
@DcService
public class DcDemoServiceImpl implements DcDemoService {
	@Override
	public String print(String name) {
		return  name + "*****";
	}
}
