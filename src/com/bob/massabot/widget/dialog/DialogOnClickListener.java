/**
 * Copyright(C) 2017 MassBot Co. Ltd. All rights reserved.
 *
 */
package com.bob.massabot.widget.dialog;

import android.view.View;
import android.view.View.OnClickListener;

/**
 * @since 2017年4月24日 上午9:20:48
 * @version $Id$
 * @author JiangJibo
 *
 */
public abstract class DialogOnClickListener {

	/**
	 * 确定键监听器
	 * 
	 * @param listener
	 */
	OnClickListener getOnPositiveListener() {
		return new OnClickListener() {

			@Override
			public void onClick(View v) {
				onLeftClicked();
			}
		};
	}

	/**
	 * 取消键监听器
	 * 
	 * @param listener
	 */
	OnClickListener getOnNegativeListener() {
		return new OnClickListener() {

			@Override
			public void onClick(View v) {
				onRightClicked();
			}
		};
	}

	public abstract void onLeftClicked();

	public abstract void onRightClicked();

}
