/**
 * Copyright(C) 2017 MassBot Co. Ltd. All rights reserved.
 *
 */
package com.bob.massabot.util;

import java.util.LinkedHashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.widget.Toast;

/**
 * @since 2017年3月12日 下午7:36:59
 * @version $Id$
 * @author JiangJibo
 *
 */
public class HostAdressConfigUtils {

	public static final String HOST_ADRESS_CONFIG_FILE_PATH = "host_adress";

	private static final Map<String, String> IP_CONFIG_CACHE = new LinkedHashMap<String, String>();

	private Activity activity;

	private SharedPreferences sp;

	private Editor editor;

	public HostAdressConfigUtils(Activity activity) {
		this.activity = activity;
		sp = activity.getSharedPreferences(HOST_ADRESS_CONFIG_FILE_PATH, Context.MODE_PRIVATE);
		editor = sp.edit();
	}

	/**
	 * 初始化默認的服務器IP地址配置文件
	 */
	public void initDefaultConfigFile() {
		init(HOST_ADRESS_CONFIG_FILE_PATH);
	}

	/**
	 * 初始化文件,删除内部原有的ip地址信息
	 */
	public void init(String fileName) {
		IP_CONFIG_CACHE.clear();
		editor.clear();
		editor.commit();
	}

	/**
	 * 获取默认文件夹下的服务器地址
	 * 
	 * @param activity
	 * @return
	 */
	public Map<String, String> getHostAdress() {
		if (!IP_CONFIG_CACHE.isEmpty()) {
			return IP_CONFIG_CACHE;
		}
		return getHostAdress(HOST_ADRESS_CONFIG_FILE_PATH);
	}

	/**
	 * 获取指定文件夹下的服务器地址
	 * 
	 * @param activity
	 * @param fileNamegetHostAdress
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Map<String, String> getHostAdress(String fileName) {
		IP_CONFIG_CACHE.putAll((Map<String, String>) sp.getAll());
		return IP_CONFIG_CACHE;
	}

	/**
	 * 向默认服务器地址文件内追加服务器地址
	 * 
	 * @param activity
	 * @param hostAdress
	 */
	public void addNewHostAdress(String hostName, String hostAdress) {
		IP_CONFIG_CACHE.put(hostName, hostAdress);
		editor.putString(hostName, hostAdress);
		editor.commit();
	}

	/**
	 * 删除默认服务器地址文件内的指定服务器地址
	 * 
	 * @param activity
	 * @param hostAdress
	 */
	public void deleteHostAdress(String hostName) {
		if (IP_CONFIG_CACHE.remove(hostName) == null) {
			return;
		}
		editor.remove(hostName);
		editor.commit();
	}

	public String getAdressByName(String hostName) {
		if (TextUtils.isEmpty(hostName)) {
			Toast.makeText(activity, "未知的服务器名称:[" + hostName + "]", Toast.LENGTH_SHORT).show();
			return null;
		}
		return IP_CONFIG_CACHE.get(hostName);
	}

}
