/**
 * Copyright(C) 2017 MassBot Co. Ltd. All rights reserved.
 *
 */
package com.bob.massabot.widget.dialog;

import com.bob.massabot.R;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * 生成弹出框的工具类
 * 
 * @since 2017年4月24日 上午9:08:37
 * @version $Id$
 * @author JiangJibo
 *
 */
public abstract class DialogUtils {

	/**
	 * 创建一个只带标题及两个按钮的Dialog
	 * 
	 * @param context
	 * @param titleText
	 * @return
	 */
	public static MassabotDialog createDialog0(Context context, String titleText) {
		return new NormalDialog(context, titleText);
	}

	/**
	 * 创建带一个EditText的Dialog
	 * 
	 * @param context
	 * @param titleText
	 * @param firHint
	 * @param listener
	 * @return
	 */
	public static MassabotDialog createDialog1(Context context, String titleText, String firHint) {
		return new NormalDialog(context, titleText, firHint);
	}

	/**
	 * 创建带两个EditText的Dialog
	 * 
	 * @param context
	 * @param titleText
	 * @param firHint
	 * @param secHint
	 * @param listener
	 * @return
	 */
	public static MassabotDialog createDialog2(Context context, String titleText, String firHint, String secHint) {
		return new NormalDialog(context, titleText, firHint, secHint);
	}

	/**
	 * 创建一个ProgressDialog
	 * 
	 * @param context
	 * @param title
	 * @param msg
	 * @return
	 */
	public static ProgressDialog createProgressDialog(Context context, String title, String msg) {
		ProgressDialog dialog = new ProgressDialog(context);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);// 设置进度条的形式为圆形转动的进度条
		dialog.setCancelable(false);// 设置是否可以通过点击Back键取消
		dialog.setCanceledOnTouchOutside(false);// 设置在点击Dialog外是否取消Dialog进度条
		if (title != null) {
			dialog.setTitle(title);
			dialog.setIcon(R.drawable.ic_launcher); // 设置提示的title的图标，默认是没有的，如果没有设置title的话只设置Icon是不会显示图标的
		}
		dialog.setMessage(msg);
		return dialog;
	}

}
