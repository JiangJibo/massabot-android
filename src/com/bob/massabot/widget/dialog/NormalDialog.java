/**
 * Copyright(C) 2017 MassBot Co. Ltd. All rights reserved.
 *
 */
package com.bob.massabot.widget.dialog;

import com.bob.massabot.R;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * 自定义弹出框
 * 
 * @since 2017年3月14日 上午10:11:30
 * @version $Id$
 * @author JiangJibo
 *
 */
public class NormalDialog extends Dialog implements MassabotDialog {

	private EditText firstEdit;
	private EditText secEdit;
	private Button positiveButton, negativeButton;

	/**
	 * 定义没有输入框的Dialog
	 * 
	 * @param context
	 * @param titleText
	 *            标题
	 */
	NormalDialog(Context context, String titleText) {
		super(context, R.style.CustomDialog);
		createNormalDialog(titleText);
	}

	/**
	 * 定义有一个输入框的Dialog
	 * 
	 * @param context
	 * @param titleText
	 *            标题
	 * @param firHint
	 *            第一个EditText的hint
	 */
	NormalDialog(Context context, String titleText, String firHint) {
		super(context, R.style.CustomDialog);
		createOneEditDialog(titleText, firHint);
	}

	/**
	 * 定义有两个输入框的Dialog
	 * 
	 * @param context
	 * @param titleText
	 *            标题
	 * @param firHint
	 *            第一个EditText的hint
	 * @param secHint
	 *            第二个EditText的hint
	 */
	NormalDialog(Context context, String titleText, String firHint, String secHint) {
		super(context, R.style.CustomDialog);
		createTwoEditDialog(titleText, firHint, secHint);
	}

	private void createNormalDialog(String titleText) {
		View mView = LayoutInflater.from(getContext()).inflate(R.layout.massabot_dialog, (ViewGroup) null);
		((TextView) mView.findViewById(R.id.dialog_title)).setText(titleText);
		positiveButton = (Button) mView.findViewById(R.id.dialog_positiveButton);
		negativeButton = (Button) mView.findViewById(R.id.dialog_negativeButton);
		super.setContentView(mView);
	}

	private void createOneEditDialog(String titleText, String hint) {
		View mView = LayoutInflater.from(getContext()).inflate(R.layout.massabot_dialog, (ViewGroup) null);
		((TextView) mView.findViewById(R.id.dialog_title)).setText(titleText);
		firstEdit = (EditText) mView.findViewById(R.id.dialog_first_edit);
		firstEdit.setHint(hint);
		firstEdit.setVisibility(View.VISIBLE);
		positiveButton = (Button) mView.findViewById(R.id.dialog_positiveButton);
		negativeButton = (Button) mView.findViewById(R.id.dialog_negativeButton);
		super.setContentView(mView);
	}

	private void createTwoEditDialog(String titleText, String firHint, String secHint) {
		View mView = LayoutInflater.from(getContext()).inflate(R.layout.massabot_dialog, (ViewGroup) null);
		((TextView) mView.findViewById(R.id.dialog_title)).setText(titleText);
		firstEdit = (EditText) mView.findViewById(R.id.dialog_first_edit);
		firstEdit.setHint(firHint);
		firstEdit.setVisibility(View.VISIBLE);
		secEdit = (EditText) mView.findViewById(R.id.dialog_sec_edit);
		secEdit.setHint(secHint);
		secEdit.setVisibility(View.VISIBLE);
		positiveButton = (Button) mView.findViewById(R.id.dialog_positiveButton);
		negativeButton = (Button) mView.findViewById(R.id.dialog_negativeButton);
		super.setContentView(mView);
	}

	/**
	 * 设置监听器
	 * 
	 * @param listener
	 */
	public void setOnClickListener(DialogOnClickListener listener) {
		positiveButton.setOnClickListener(listener.getOnPositiveListener());
		negativeButton.setOnClickListener(listener.getOnNegativeListener());
	}

	/* (non-Javadoc)
	 * @see com.bob.massabot.widget.dialog.MassabotDialog#getEditText(int)
	 */
	@Override
	public View getEditText(int index) {
		if (index == 0) {
			return firstEdit;
		} else if (index == 1) {
			return secEdit;
		}
		return null;
	}

}
