/**
 * Copyright(C) 2017 MassBot Co. Ltd. All rights reserved.
 *
 */
package com.bob.massabot.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import android.app.Activity;

/**
 * 管理控制所有的Activity
 * 
 * @since 2017年4月19日 上午10:32:45
 * @version $Id$
 * @author JiangJibo
 *
 */
public class ActivityCollector {

	/**
	 * 存放activity的列表
	 */
	public static HashMap<Class<?>, Activity> activities = new LinkedHashMap<Class<?>, Activity>();

	/**
	 * 添加Activity
	 *
	 * @param activity
	 */
	public static void addActivity(Activity activity, Class<?> clz) {
		activities.put(clz, activity);
	}

	/**
	 * 判断一个Activity 是否存在
	 *
	 * @param clz
	 * @return
	 */
	public static <T extends Activity> boolean isActivityExist(Class<T> clz) {
		boolean res;
		Activity activity = getActivity(clz);
		if (activity == null) {
			res = false;
		} else {
			if (activity.isFinishing() || activity.isDestroyed()) {
				res = false;
			} else {
				res = true;
			}
		}

		return res;
	}

	/**
	 * 获得指定activity实例
	 *
	 * @param clazz
	 *            Activity 的类对象
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Activity> T getActivity(Class<T> clazz) {
		return (T) activities.get(clazz);
	}

	/**
	 * 移除activity,代替finish
	 *
	 * @param activity
	 */
	public static void removeActivity(Activity activity) {
		if (activities.containsValue(activity)) {
			activities.remove(activity.getClass());
		}
	}

	/**
	 * 移除所有的Activity
	 */
	public static void removeAllActivity() {
		if (activities != null && activities.size() > 0) {
			for (Entry<Class<?>, Activity> s : activities.entrySet()) {
				if (!s.getValue().isFinishing()) {
					s.getValue().finish();
				}
			}
		}
		activities.clear();
	}
}
