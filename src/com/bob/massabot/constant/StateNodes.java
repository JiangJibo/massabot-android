/**
 * Copyright(C) 2017 MassBot Co. Ltd. All rights reserved.
 *
 */
package com.bob.massabot.constant;

import android.util.SparseArray;

/**
 * 状态节点
 * 
 * @since 2017年3月16日 下午2:14:30
 * @version $Id$
 * @author JiangJibo
 *
 */
public enum StateNodes {

	DISCONNECTED(0, "断开连接"), CONNECTED(1, "正常连接"), RUNNING(10, "正常运行"), PAUSED(11, "暂停");

	public Integer code;
	public String label;

	/**
	 * @param code
	 * @param label
	 */
	private StateNodes(Integer code, String label) {
		this.code = code;
		this.label = label;
	}

	private static final SparseArray<StateNodes> valueMap = new SparseArray<StateNodes>();

	static {
		for (StateNodes node : StateNodes.values()) {
			valueMap.put(node.code, node);
		}
	}

	public static StateNodes valueOf(Integer code) {
		return valueMap.get(code);
	}

}
