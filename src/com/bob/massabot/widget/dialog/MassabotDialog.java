/**
 * Copyright(C) 2017 MassBot Co. Ltd. All rights reserved.
 *
 */
package com.bob.massabot.widget.dialog;

import android.view.View;

/**
 * Dialog公共接口
 * 
 * @since 2017年4月24日 上午9:39:08
 * @version $Id$
 * @author JiangJibo
 *
 */
public interface MassabotDialog {

	/**
	 * 获取Dialog上EditText对象
	 * 
	 * @param index
	 *            序号,从上往下，从0开始
	 * @return
	 */
	public View getEditText(int index);

	/**
	 * 给确定/取消按钮添加事件
	 * 
	 * @param listener
	 */
	public void setOnClickListener(DialogOnClickListener listener);

	/**
	 * 消失
	 */
	public void dismiss();

	/**
	 * 显示
	 */
	public void show();

}
