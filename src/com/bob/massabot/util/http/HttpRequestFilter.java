/**
 * Copyright(C) 2017 MassBot Co. Ltd. All rights reserved.
 *
 */
package com.bob.massabot.util.http;

/**
 * 在进行Http请求之前的前置条件判断
 * 
 * @since 2017年5月4日 上午11:02:44
 * @version $Id$
 * @author JiangJibo
 *
 */
public interface HttpRequestFilter {

	/**
	 * 在发送Http请求之前做前置准备工作
	 * 
	 * @return false:终止执行Http请求
	 */
	public boolean doFilter();

	/**
	 * 在Http请求被时获取拒绝原因信息
	 */
	public String getRejectionReason();

}
