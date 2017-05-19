package com.bob.massabot.widget;

import java.util.ArrayList;
import java.util.List;

import com.bob.massabot.DemoActivity;
import com.bob.massabot.R;
import com.bob.massabot.widget.dialog.DialogOnClickListener;
import com.bob.massabot.widget.dialog.DialogUtils;
import com.bob.massabot.widget.dialog.MassabotDialog;

import android.app.Activity;
import android.content.Context;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @since 2017年3月13日 上午9:10:20
 * @version $Id$
 * @author JiangJibo
 *
 */
public class DropDownListView extends LinearLayout {

	private TextView editText;
	private PopupWindow popupWindow = null;
	private List<String> dataList = new ArrayList<String>();
	private Activity activity;

	public DropDownListView(Context context) {
		this(context, null);
	}

	public DropDownListView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DropDownListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		activity = (Activity) getContext();
		initView();
	}

	public void initView() {
		LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		switch (getId()) {
		case R.id.demo_dropdown: // 示教文件下拉框
			layoutInflater.inflate(R.layout.demo_dorpdownlist_view, this, true);
			editText = (TextView) findViewById(R.id.demoText);
			// 示教案列下拉框才配置长按事件
			this.setOnLongClickListener(new OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {
					doAddDemo();
					return true;
				}

			});
			break;
		default:
			break;
		}

		this.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (popupWindow == null) {
					showPopWindow();
				} else {
					closePopWindow();
				}
			}
		});

	}

	/**
	 * 添加示教案列
	 */
	private void doAddDemo() {
		final MassabotDialog dialog = DialogUtils.createDialog1(getContext(), "新增示教案列", "案列名称");
		dialog.setOnClickListener(new DialogOnClickListener() {

			@Override
			public void onLeftClicked() {
				String demoName = ((EditText) dialog.getEditText(0)).getText().toString().trim();
				if (TextUtils.isEmpty(demoName)) {
					Toast.makeText(getContext(), "示教案列的名称不能为空", Toast.LENGTH_SHORT).show();
					return;
				}
				demoName = demoName + DemoActivity.DEMO_FILE_EMPTY_FLAG;
				boolean result = ((DemoActivity) activity).createNewDemoCase(demoName);
				if (result) {
					dataList.add(demoName);
					editText.setText(demoName);
					Message msg = new Message();
					msg.obj = demoName;
					msg.what = 0;
					((DemoActivity) activity).getHandler().sendMessage(msg);
				}
				dialog.dismiss();

			}

			@Override
			public void onRightClicked() {
				dialog.dismiss();
			}

		});
		dialog.show();
	}

	/**
	 * 打开下拉列表弹窗
	 */
	@SuppressWarnings("deprecation")
	private void showPopWindow() {
		// 加载popupWindow的布局文件
		LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View contentView = layoutInflater.inflate(R.layout.dropdownlist_popupwindow, (ViewGroup) null, false);
		ListView listView = (ListView) contentView.findViewById(R.id.listView);

		listView.setAdapter(new XCDropDownListAdapter(getContext(), dataList));
		popupWindow = new PopupWindow(contentView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		popupWindow.setBackgroundDrawable(getResources().getDrawable(R.color.transparent));
		popupWindow.setOutsideTouchable(true);
		popupWindow.showAsDropDown(this);
	}

	/**
	 * 关闭下拉列表弹窗
	 */
	private void closePopWindow() {
		popupWindow.dismiss();
		popupWindow = null;
	}

	/**
	 * 设置数据
	 * 
	 * @param list
	 */
	public void setItemsData(List<String> list, Integer position) {
		dataList = list;
		if (list == null || list.isEmpty()) {
			editText.setText(null);
		} else {
			editText.setText(list.get(position).toString());
		}
	}

	/**
	 * 获取下拉框的数据
	 * 
	 * @return
	 */
	public List<String> getItemsData() {
		return dataList;
	}

	/**
	 * 数据适配器
	 * 
	 * @author caizhiming
	 *
	 */
	class XCDropDownListAdapter extends BaseAdapter {

		Context mContext;
		List<String> mData;
		LayoutInflater inflater;

		public XCDropDownListAdapter(Context ctx, List<String> data) {
			mContext = ctx;
			mData = data;
			inflater = LayoutInflater.from(mContext);
		}

		@Override
		public int getCount() {
			return mData.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// 自定义视图
			ListItemView listItemView = null;
			if (convertView == null) {
				// 获取list_item布局文件的视图
				convertView = inflater.inflate(R.layout.dropdown_list_item, (ViewGroup) null);

				listItemView = new ListItemView();
				// 获取控件对象
				listItemView.tv = (TextView) convertView.findViewById(R.id.tv);

				listItemView.layout = (LinearLayout) convertView.findViewById(R.id.layout_container);
				// 设置控件集到convertView
				convertView.setTag(listItemView);
			} else {
				listItemView = (ListItemView) convertView.getTag();
			}

			// 设置数据
			listItemView.tv.setText(mData.get(position).toString());
			final String text = mData.get(position).toString();
			listItemView.layout.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					editText.setText(text);
					closePopWindow();
					switch (getId()) {
					case R.id.demo_dropdown:
						Message msg = new Message();
						msg.what = 0;
						msg.obj = text;
						((DemoActivity) activity).getHandler().sendMessage(msg);
					default:
						break;
					}
				}
			});

			// 添加长按事件
			listItemView.layout.setOnLongClickListener(new OnLongClickListener() {

				public boolean onLongClick(View v) {
					switch (getId()) {
					case R.id.demo_dropdown:
						doDemoCaseDel(text);
						break;
					default:
						break;
					}
					return true;
				}

			});
			// TODO 设置是否可选
			// listItemView.layout.setClickable(false);
			return convertView;
		}

		/**
		 * 删除指定的演示案列
		 * 
		 * @param demoName
		 */
		private void doDemoCaseDel(final String demoName) {
			final MassabotDialog dialog = DialogUtils.createDialog0(getContext(), "删除此演示案列?");
			dialog.setOnClickListener(new DialogOnClickListener() {

				@Override
				public void onLeftClicked() {
					boolean result = ((DemoActivity) activity).deleteDemoCase(demoName);
					if (result) {
						mData.remove(demoName);
						if (mData.isEmpty()) {
							editText.setText("");
						} else {
							editText.setText(mData.get(0));
						}
					}
					dialog.dismiss();
					closePopWindow();
				}

				@Override
				public void onRightClicked() {
					dialog.dismiss();
				}

			});
			dialog.show();
		}

	}

	private static class ListItemView {

		TextView tv;
		LinearLayout layout;
	}

}
