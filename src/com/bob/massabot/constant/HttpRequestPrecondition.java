/**
 * Copyright(C) 2017 MassBot Co. Ltd. All rights reserved.
 *
 */
package com.bob.massabot.constant;

/**
 * 在进行Http请求之前的前置条件判断
 * 
 * @since 2017年5月4日 上午11:02:44
 * @version $Id$
 * @author JiangJibo
 *
 */
public interface HttpRequestPrecondition {

	/**
	 * 在发送Http请求之前做前置准备工作
	 * 
	 * @return
	 */
	public boolean checkBeforeRequest();

	/**
	 * 当前置校验位通过时,获取未通过的提示信息
	 * 
	 * @return
	 */
	public String getNotpassNotice();

}
