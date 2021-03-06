/**
 * Copyright(C) 2017 MassBot Co. Ltd. All rights reserved.
 *
 */
package com.bob.massabot.model;

import static com.bob.massabot.constant.MassabotConstant.WIFI_SSID;

import com.bob.massabot.constant.MassabotConstant;
import com.bob.massabot.util.ActivityCollector;
import com.bob.massabot.util.http.HttpRequestFilter;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;

/**
 * @since 2017年5月3日 下午4:35:56
 * @version $Id$
 * @author JiangJibo
 *
 */
public class BaseActivity extends Activity {

	protected HttpRequestFilter requestFilter; // 发送Http请求之前的校验工作

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActivityCollector.addActivity(this, getClass());

		requestFilter = new HttpRequestFilter() {

			@Override
			public boolean doFilter() { // 校验当前的连接的是否是指定的wifi
				return ("\"" + WIFI_SSID + "\"").equals(getCurrentWifiSSID());
			}

			@Override
			public String getRejectionReason() {
				return "当前所连接的wifi节点不是[" + MassabotConstant.WIFI_SSID + "],请重新连接";
			}
		};
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		ActivityCollector.removeActivity(this);
	}

	/**
	 * 获取当前连接的wifi名称
	 * 
	 * @return
	 */
	private String getCurrentWifiSSID() {
		return ((WifiManager) getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getSSID();
	}

}
