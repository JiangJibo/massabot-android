/**
 * Copyright(C) 2017 MassBot Co. Ltd. All rights reserved.
 *
 */
package com.bob.massabot.util.http;

/**
 * @since 2017年3月8日 下午2:58:42
 * @version $Id$
 * @author JiangJibo
 *
 */
public interface HttpRequestURLConfig {

	public static final String WEB_ROOT = "/massabot";

	// java后端项目路径，robot问项目跟路径，index为首页的Controller路径
	public static final String INDEX_CON_URL = "/index";

	// java后端项目路径，robot问项目跟路径，main为主页的Controller路径
	public static final String MAIN_CON_URL = "/main";

	// java后端项目路径，robot问项目跟路径，main为首页的Controller路径
	public static final String DEMOCASE_CON_URL = "/democase";

	// 连接
	public static final String CONNECT_URL = "/connection";

	// 启动按摩服务URL
	public static final String MASSAGE_URL = "/massage";

	// 暂停
	public static final String PAUSE_URL = "/suspension";

	// 恢复
	public static final String RESUME_URL = "/restoration";

	// 进度查询
	public static final String PROGRESS_URL = "/progress";

	// 示教案列
	public static final String DEMO_CASE_URL = "/democase";

	// 获取可连接的设备列表
	public static final String PORT_NAMES = "/portnames";

	// 向指定的示教案列
	public static final String DEMO_FILE_WRITE_URL = "/writing";

	// 复现指定的示教案列的路径
	public static final String DEMO_FILE_READ_URL = "/reading";

	// 复位机械臂
	public static final String RESETTING_DEVICE = "/resetting";

	// 设置手指温度
	public static final String FINGER_TEMP_SETTING_URL = "/fingertemp";

	// 用户语音词表
	public static final String USER_VOICE_WORDS_URL = "/voicewords";

}
