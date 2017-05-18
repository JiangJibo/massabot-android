/**
 * Copyright(C) 2017 MassBot Co. Ltd. All rights reserved.
 *
 */
package com.bob.massabot.util;

import android.app.Application;
import android.content.Context;

/**
 * 全局获取ApplicationContext的工具类
 * 
 * @since 2017年5月18日 下午3:43:34
 * @version $Id$
 * @author JiangJibo
 *
 */
public class GlobalApplicationUtils extends Application {

	private static Context context;

	/* (non-Javadoc)
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate() {
		context = getApplicationContext();
	}

	public static Context getAppContext() {
		return context;
	}

}
