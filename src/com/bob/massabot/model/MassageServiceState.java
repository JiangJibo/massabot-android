/**
 * Copyright(C) 2017 MassBot Co. Ltd. All rights reserved.
 *
 */
package com.bob.massabot.model;

import java.io.Serializable;

/**
 * @since 2017年3月9日 下午3:35:06
 * @version $Id$
 * @author JiangJibo
 *
 */
public class MassageServiceState implements Serializable {

	private static final long serialVersionUID = 5218343821173959424L;

	private String currentPortName; // 当前使用的端口名称
	private int stateNodeCode; // 当前的状态节点
	private int currentType; // 服务类型
	private int progress; // 当前进度
	private String extraMsg; // 额外的信息
	private int fingerTemp; // 手指温度

	/**
	 * @return the currentPortName
	 */
	public String getCurrentPortName() {
		return currentPortName;
	}

	/**
	 * @param currentPortName
	 *            the currentPortName to set
	 */
	public void setCurrentPortName(String currentPortName) {
		this.currentPortName = currentPortName;
	}

	public MassageServiceState(int currentType) {
		this.currentType = currentType;
	}

	/**
	 * @return the stateNodeCode
	 */
	public int getStateNodeCode() {
		return stateNodeCode;
	}

	/**
	 * @param stateNodeCode
	 *            the stateNodeCode to set
	 */
	public void setStateNodeCode(int stateNodeCode) {
		this.stateNodeCode = stateNodeCode;
	}

	/**
	 * @return the currentType
	 */
	public int getCurrentType() {
		return currentType;
	}

	/**
	 * @param currentType
	 *            the currentType to set
	 */
	public void setCurrentType(int currentType) {
		this.currentType = currentType;
	}

	/**
	 * @return the progress
	 */
	public int getProgress() {
		return progress;
	}

	/**
	 * @param progress
	 *            the progress to set
	 */
	public void setProgress(int progress) {
		this.progress = progress;
	}

	/**
	 * @return the extraMsg
	 */
	public String getExtraMsg() {
		return extraMsg;
	}

	/**
	 * @param extraMsg
	 *            the extraMsg to set
	 */
	public void setExtraMsg(String extraMsg) {
		this.extraMsg = extraMsg;
	}

	/**
	 * @return the fingerTemp
	 */
	public int getFingerTemp() {
		return fingerTemp;
	}

	/**
	 * @param fingerTemp
	 *            the fingerTemp to set
	 */
	public void setFingerTemp(int fingerTemp) {
		this.fingerTemp = fingerTemp;
	}

	/**
	 * 服务状态的构造类
	 * 
	 * @since 2017年3月8日 下午1:52:06
	 * @version $Id$
	 * @author JiangJibo
	 *
	 *//*
		public static class StateBuilder {
		
		private int currentType;
		private int currentLine = 0;
		private boolean isActive = false;
		
		public static StateBuilder instance() {
			return new StateBuilder();
		}
		
		public StateBuilder currentType(int currentType) {
			this.currentType = currentType;
			return this;
		}
		
		public StateBuilder currentLine(int currentLine) {
			this.currentLine = currentLine;
			return this;
		}
		
		public StateBuilder isActive(boolean isActive) {
			this.isActive = isActive;
			return this;
		}
		
		public MassageServiceState build() {
			ServiceType type = ServiceType.valueOf(currentType);
			Assert.notNull(type, "必须指定按摩服务类型!");
			MassageServiceState state = new MassageServiceState();
			state.currentType = currentType;
			state.currentLine = currentLine;
			state.isActive = isActive;
			state.gcodeFilePath = type.filePath;
			state.totalLines = type.totalLines;
			return state;
		}
		
		}*/

}
