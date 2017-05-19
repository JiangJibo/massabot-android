/**
 * Copyright(C) 2017 MassBot Co. Ltd. All rights reserved.
 *
 */
package com.bob.massabot.constant;

/**
 * @since 2017年4月15日 下午3:40:00
 * @version $Id$
 * @author JiangJibo
 *
 */
public interface MassabotConstant {

	/**
	 * 手指最大温度
	 */
	public static final Integer FINGER_TEMP_MAX = 60;

	/**
	 * 手指最小温度
	 */
	public static final Integer FINGER_TEMP_MIN = 25;

	/**
	 * 成功返回标识
	 */
	public static final String SUCCESS_FLAG = "success";

	/**
	 * wifi名称
	 */
	// public static final String WIFI_SSID = "massabot";
	public static final String WIFI_SSID = "Xbotpark_Guest";

	/**
	 * wifi密码
	 */
	// public static final String WIFI_PWD = "12345678";
	public static final String WIFI_PWD = "sslrobot123";

	/**
	 * 电机所在局域网的IP地址
	 */
	// public static final String HOST_ADRESS_IP = "192.168.1.113";
	public static final String HOST_ADRESS_IP = "192.168.200.76";

	/**
	 * wifi加密方式,WPA/WPA2 PSK加密类型
	 */
	public static final Integer WIFI_SSL = 3;

}
