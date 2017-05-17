/**
 * Copyright(C) 2017 MassBot Co. Ltd. All rights reserved.
 *
 */
package com.bob.massabot.constant;

import android.util.SparseArray;

/**
 * 按摩服務分类
 * 
 * @since 2017年3月12日 下午8:13:20
 * @version $Id$
 * @author JiangJibo
 *
 */
public enum MassageServiceType {

	DEMO(10, "示  教"), REAPPER(11, "复  现"), SHOULDER(1, "肩  部"), BACK(2, "背  部"), WAIST(3, "腰  部");

	public Integer code;
	public String label;

	MassageServiceType(Integer code, String label) {
		this.code = code;
		this.label = label;
	}

	private static final SparseArray<MassageServiceType> valueMap = new SparseArray<MassageServiceType>();

	static {
		for (MassageServiceType type : MassageServiceType.values()) {
			valueMap.put(type.code, type);
		}
	}

	public static MassageServiceType valueOf(Integer code) {
		return valueMap.get(code);
	}

}
