package com.bob.massabot;

import static com.bob.massabot.constant.HttpRequestURLConfig.DEMOCASE_CON_URL;
import static com.bob.massabot.constant.HttpRequestURLConfig.DEMO_FILE_READ_URL;
import static com.bob.massabot.constant.HttpRequestURLConfig.DEMO_FILE_WRITE_URL;
import static com.bob.massabot.constant.HttpRequestURLConfig.PAUSE_URL;
import static com.bob.massabot.constant.HttpRequestURLConfig.PROGRESS_URL;
import static com.bob.massabot.constant.HttpRequestURLConfig.RESETTING_DEVICE;
import static com.bob.massabot.constant.HttpRequestURLConfig.RESUME_URL;
import static com.bob.massabot.constant.HttpRequestURLConfig.WEB_ROOT;
import static com.bob.massabot.constant.MassabotConstant.SUCCESS_FLAG;

import java.util.ArrayList;
import java.util.List;

import com.bob.massabot.model.BaseActivity;
import com.bob.massabot.util.HttpRequestUtils;
import com.bob.massabot.widget.DropDownListView;
import com.bob.massabot.widget.RoundProgressBar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 示教Activity
 * 
 * @since 2017年3月22日 上午10:51:02
 * @version $Id$
 * @author JiangJibo
 *
 */
public class DemoActivity extends BaseActivity implements OnClickListener {

	public static final String DEMO_FILE_EMPTY_FLAG = "(0)"; // 示教文件为空的标识
	public static final Integer DEMO_MODE_FLAG = 0; // 演示模式
	public static final String DEMO_FILE_NOT_EMPTY_FLAG = "(1)"; // 示教文件非空的标识
	public static final Integer REAPPER_MODE_FLAG = 1; // 复现模式

	private String curDemoCaseName; // 当前选中的示教案列名称

	private DropDownListView demoDropdown; // 示教下拉框

	private Integer currentMode = -1; // 当前选择的模式。0:示教模式;1:复现模式
	private static final Integer DEMO_MODE = 0;
	private static final Integer REAPPER_MODE = 1;

	private long rangeTime; // 计时器暂停时的时间差

	private volatile int progress = 0;// 当前进度
	private volatile boolean progressing; // 是否开始获取进度

	private RoundProgressBar roundProgressBar; // 进度环
	private Chronometer timer; // 计时器

	private Button startBtn, pauseBtn, modeBtn, reapperBtn;
	private String startText, finishText, pauseText, resumeText;

	private String hostAdress, requestPrefix;

	private Handler handler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_demo);
		demoDropdown = (DropDownListView) findViewById(R.id.demo_dropdown);

		roundProgressBar = (RoundProgressBar) findViewById(R.id.demoRoundProgressBar);
		timer = (Chronometer) this.findViewById(R.id.chronometer);

		findViewById(R.id.demo_back_to_main).setOnClickListener(DemoActivity.this);

		initButton();

		Intent intent = getIntent();
		this.hostAdress = intent.getStringExtra("hostAdress");
		this.requestPrefix = HttpRequestUtils.createRequestUrl(hostAdress, WEB_ROOT, DEMOCASE_CON_URL);

		handler = new Handler() {

			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				switch (msg.what) {
				case 0:
					resetModeBtn(); // 在新增/删除示教案列时,还原模式按钮
					String demoCaseName = msg.obj.toString();
					curDemoCaseName = demoCaseName;
					roundProgressBar.setProgress(0);
					setInVisiable();
					break;
				case 1:
					doResetAfterProgressed();// 进度已到达100%,复位按钮
					break;
				case 2:
					break;
				case 3:
					break;
				default:
					break;
				}
			}

		};

		addLongClickListenerToModBtn();
		initDemoDropdown();

	}

	/**
	 * 初始化4个Button
	 */
	private void initButton() {
		startBtn = (Button) findViewById(R.id.demo_start);
		startBtn.setOnClickListener(DemoActivity.this);
		pauseBtn = (Button) findViewById(R.id.demo_pause);
		pauseBtn.setOnClickListener(DemoActivity.this);
		modeBtn = (Button) findViewById(R.id.demo_mode);
		modeBtn.setOnClickListener(DemoActivity.this);
		reapperBtn = (Button) findViewById(R.id.reappear_mode);
		reapperBtn.setOnClickListener(DemoActivity.this);

		startText = getString(R.string.start_text);
		finishText = getString(R.string.finish_text);
		pauseText = getString(R.string.pause_text);
		resumeText = getString(R.string.resume_text);
	}

	/**
	 * 给演示/复现按钮添加单击事件
	 */
	private void addLongClickListenerToModBtn() {
		// 给复现按钮添加长按事件,长按后发送复位请求
		reapperBtn.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				try {
					HttpRequestUtils.doPutWithThread(requestPrefix + RESETTING_DEVICE, null, 1000);
				} catch (Exception e) {
					toast("复位电机时出现异常,请重新尝试");
				}
				return true;
			}
		});
	}

	/**
	 * 在演示模式下开始
	 */
	private void startDemoMode() {
		String result = null;
		try {
			result = HttpRequestUtils.doPutWithThread(precondition, requestPrefix + DEMO_FILE_WRITE_URL + "?demoName=" + curDemoCaseName, null, 3000);
			if (SUCCESS_FLAG.equals(result)) {
				timer.start();
				lockWidgetAfterStarted();
			} else {
				toast(result);
			}
		} catch (Exception e) {
			toast("开启案列:[" + curDemoCaseName + "]演示模式时出现异常,请重新尝试");
		}
	}

	/**
	 * 暂停演示模式运行
	 */
	private void pauseDemoMode() {
		String result = null;
		try {
			result = HttpRequestUtils.doPutWithThread(requestPrefix + DEMO_FILE_WRITE_URL + PAUSE_URL, null, 3000);
			if (SUCCESS_FLAG.equals(result)) {
				timer.stop();
				this.rangeTime = SystemClock.elapsedRealtime() - timer.getBase();
				pauseBtn.setText(resumeText);
			} else {
				toast(result);
			}
		} catch (Exception e) {
			toast("结束案列:[" + curDemoCaseName + "]演示模式时出现异常,请重新尝试");
		}
	}

	/**
	 * 恢复演示模式运行
	 */
	private void resumeDemoMode() {
		String result = null;
		try {
			result = HttpRequestUtils.doPutWithThread(precondition, requestPrefix + DEMO_FILE_WRITE_URL + RESUME_URL, null, 3000);
			if (SUCCESS_FLAG.equals(result)) {
				timer.setBase(SystemClock.elapsedRealtime() - rangeTime);
				timer.start();
				pauseBtn.setText(pauseText);
			} else {
				toast(result);
			}
		} catch (Exception e) {
			toast("结束案列:[" + curDemoCaseName + "]演示模式时出现异常,请重新尝试");
		}
	}

	/**
	 * 在演示模式下结束
	 */
	private void finishDemoMode() {
		String result = null;
		try {
			result = HttpRequestUtils.doPostWithThread(precondition, requestPrefix + DEMO_FILE_WRITE_URL, null, 3000);
			if (SUCCESS_FLAG.equals(result)) {
				processDemoNameAfterFinished();
			} else {
				toast(result);
			}
			timer.stop();
			lockWidgetAfterFinished();
		} catch (Exception e) {
			toast("结束案列:[" + curDemoCaseName + "]演示模式时出现异常,请重新尝试");
		}
	}

	/**
	 * 在复现模式下开始
	 * 
	 * @return
	 */
	private void startRepMode() {
		String result = null;
		try {
			result = HttpRequestUtils.doPostWithThread(precondition, requestPrefix + DEMO_FILE_READ_URL + "?demoName=" + curDemoCaseName, null, 3000);
			if (SUCCESS_FLAG.equals(result)) {
				this.progressing = true;
				lockWidgetAfterStarted();
				getProgress();
			} else {
				toast(result);
			}
		} catch (Exception e) {
			toast("开始案列:[" + curDemoCaseName + "]复现模式时出现异常,请重新尝试");
		}
	}

	/**
	 * 暂停复现模式
	 */
	private void pauseRepMode() {
		String result = null;
		try {
			result = HttpRequestUtils.doPutWithThread(precondition, requestPrefix + DEMO_FILE_READ_URL + PAUSE_URL, null, 3000);
			if (SUCCESS_FLAG.equals(result)) {
				this.progressing = false;
				pauseBtn.setText(resumeText);
			} else {
				toast(result);
			}
		} catch (Exception e) {
			toast("暂停案列:[" + curDemoCaseName + "]复现模式时出现异常,请重新尝试");
		}
	}

	/**
	 * 恢复复现模式的运行
	 */
	private void resumeRepMode() {
		String result = null;
		try {
			result = HttpRequestUtils.doPutWithThread(precondition, requestPrefix + DEMO_FILE_READ_URL + RESUME_URL, null, 3000);
			if (SUCCESS_FLAG.equals(result)) {
				this.progressing = true;
				pauseBtn.setText(pauseText);
				getProgress();
			} else {
				toast(result);
			}
		} catch (Exception e) {
			toast("恢复案列:[" + curDemoCaseName + "]复现模式时出现异常,请重新尝试");
		}
	}

	/**
	 * 中途结束示教复现模式
	 */
	private void abortRepMode() {
		String result = null;
		try {
			result = HttpRequestUtils.doDeleteWithThread(precondition, requestPrefix + DEMO_FILE_READ_URL, 3000);
			if (SUCCESS_FLAG.equals(result)) {
				this.progressing = false;
				roundProgressBar.setProgress(0);
				lockWidgetAfterFinished();
			} else {
				toast(result);
			}
		} catch (Exception e) {
			toast("结束案列:[" + curDemoCaseName + "]复现模式时出现异常,请重新尝试");
		}
	}

	/**
	 * 获取进度
	 */
	public void getProgress() {
		new Thread() {

			public void run() {
				int failtimes = 0;
				while (progress < 100) {
					if (!progressing) {
						return;
					}
					String result = HttpRequestUtils.doGet(requestPrefix + PROGRESS_URL, 2000);

					if (result == null) {
						Message msg = new Message();
						msg.obj = "进度更新出现异常";
						msg.what = 0;
						handler.sendMessage(msg);
						failtimes++;
						if (failtimes > 3) { // 连续3次未能取得进度,则终止继续请求
							msg.obj = "进度更新出现异常,终止进度更新请求";
							return;
						}
						continue;
					}
					failtimes = 0;

					progress = Integer.parseInt(result);
					roundProgressBar.setProgress(progress);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				handler.sendEmptyMessage(1); // 进度已到达100%
			}

		}.start();
	}

	/**
	 * 统一设置演示/复现模式按钮的可选性
	 * 
	 * @param clickable
	 */
	private void setModBtnClickable(boolean clickable) {
		setButtonClickable(modeBtn, clickable);
		setButtonClickable(reapperBtn, clickable);
		reapperBtn.setLongClickable(clickable);
	}

	/**
	 * 还原模式设定
	 */
	public void resetModeBtn() {
		modeBtn.setTextColor(0xff000000);
		reapperBtn.setTextColor(0xff000000);
		currentMode = -1;
	}

	/**
	 * 初始化示教文件下拉框
	 */
	private void initDemoDropdown() {
		ArrayList<String> data = null;
		String result = null;
		try {
			result = HttpRequestUtils.doGetWithThread(requestPrefix, 3000);
			if (result != null) {
				data = new Gson().fromJson(result, new TypeToken<ArrayList<String>>() {
				}.getType());
				setModBtnClickable(true);
				this.curDemoCaseName = data.get(0);
				demoDropdown.setItemsData(data, 0);
			} else {
				demoDropdown.setItemsData(new ArrayList<String>(), null);
			}
		} catch (Exception e) {
			toast(result);
		}
	}

	/**
	 * 创建新的示教案列
	 * 
	 * @param demoName
	 */
	public boolean createNewDemoCase(String demoName) {
		String result = null;
		try {
			result = HttpRequestUtils.doPostWithThread(requestPrefix + "?demoName=" + demoName, null, 3000);
			if (SUCCESS_FLAG.equals(result)) {
				return true;
			}
		} catch (Exception e) {
			toast("创建示教案列超时,请稍后再试");
			return false;
		}
		toast(result);
		return false;

	}

	/**
	 * 删除指定的示教案列
	 * 
	 * @param demoName
	 * @return
	 */
	public boolean deleteDemoCase(String demoName) {
		String result = null;
		try {
			result = HttpRequestUtils.doDeleteWithThread(requestPrefix + "?demoName=" + demoName, 3000);
			if (SUCCESS_FLAG.equals(result)) {
				return true;
			}
		} catch (Exception e) {
			toast("删除示教案列超时,请稍后再试");
			return false;
		}
		toast(result);
		return false;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onBackPressed()
	 */
	public void onBackPressed() {
		finishActivity();
	}

	private void finishActivity() {
		String text = startBtn.getText().toString();
		if (finishText.equals(text)) {
			toast("示教正在进行中,请结束运行后再返回");
			return;
		}
		finish();
	}

	/**
	 * 弹出消息提示框
	 * 
	 * @param text
	 */
	public void toast(String text) {
		Toast.makeText(DemoActivity.this, text, Toast.LENGTH_SHORT).show();
	}

	public Handler getHandler() {
		return handler;
	}

	/**
	 * @return the curDemoCaseName
	 */
	public String getCurDemoCaseName() {
		return curDemoCaseName;
	}

	/**
	 * @param curDemoCaseName
	 *            the curDemoCaseName to set
	 */
	public void setCurDemoCaseName(String curDemoCaseName) {
		this.curDemoCaseName = curDemoCaseName;
	}

	/**
	 * 在进度条运行结束之后
	 */
	private void doResetAfterProgressed() {
		progressing = false;
		progress = 0;
		lockWidgetAfterFinished();
	}

	/**
	 * 当切换示教案列时,将计时器及进度环均隐藏
	 */
	private void setInVisiable() {
		roundProgressBar.setVisibility(View.INVISIBLE);
		timer.setVisibility(View.INVISIBLE);
	}

	/**
	 * 当结束示教模式时,将示教案列的名称由"(0)"变为"(1)"
	 */
	private void processDemoNameAfterFinished() {
		List<String> itemsData = demoDropdown.getItemsData();
		int index = itemsData.indexOf(curDemoCaseName);
		int length = curDemoCaseName.length();
		curDemoCaseName = new StringBuffer(curDemoCaseName).replace(length - 2, length - 1, "1").toString();
		itemsData.add(index, curDemoCaseName);
		demoDropdown.setItemsData(itemsData, index);
		toast("案列:[" + curDemoCaseName + "]演示成功");
	}

	/**
	 * 当开始按钮被点击之后,锁定按钮下拉框等组件
	 */
	private void lockWidgetAfterStarted() {
		setModBtnClickable(false);
		setButtonClickable(pauseBtn, true);
		demoDropdown.setClickable(false);
		demoDropdown.setLongClickable(false);
		startBtn.setText(finishText);
	}

	/**
	 * 当结束按钮被点击或复现结束时,锁定/解锁指定的控件
	 */
	private void lockWidgetAfterFinished() {
		setModBtnClickable(true);
		resetModeBtn();
		setButtonClickable(pauseBtn, false);
		demoDropdown.setClickable(true);
		demoDropdown.setLongClickable(true);
		startBtn.setText(startText);
	}

	/**
	 * 设置按钮可选性,同时文字设置颜色
	 * 
	 * @param button
	 * @param clickable
	 */
	private void setButtonClickable(Button button, boolean clickable) {
		button.setClickable(clickable);
		if (clickable) {
			button.setTextColor(Color.parseColor("#000000"));
		} else {
			button.setTextColor(Color.parseColor("#3B3B3B"));
		}
	}

	/* (non-Javadoc)
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		String text = ((TextView) v).getText().toString();

		switch (v.getId()) {
		case R.id.demo_back_to_main: // 后退
			finishActivity();
			break;
		case R.id.demo_mode: // 示教按钮
			resetModeBtn();
			setButtonClickable(startBtn, false);
			if (curDemoCaseName.endsWith(DEMO_FILE_NOT_EMPTY_FLAG)) {
				toast("[" + curDemoCaseName + "]已含示教数据");
				return;
			}
			roundProgressBar.setVisibility(View.INVISIBLE);
			timer.setVisibility(View.VISIBLE);
			currentMode = DEMO_MODE_FLAG;
			((Button) v).setTextColor(Color.rgb(66, 229, 36));
			setButtonClickable(startBtn, true);
			break;
		case R.id.reappear_mode: // 复现按钮
			resetModeBtn();
			setButtonClickable(startBtn, false);
			if (curDemoCaseName.endsWith(DEMO_FILE_EMPTY_FLAG)) {
				toast("[" + curDemoCaseName + "]没有示教数据");
				return;
			}
			currentMode = REAPPER_MODE_FLAG;
			roundProgressBar.setVisibility(View.VISIBLE);
			roundProgressBar.setProgress(0);
			timer.setVisibility(View.INVISIBLE);
			((Button) v).setTextColor(Color.rgb(66, 229, 36));
			setButtonClickable(startBtn, true);
			break;
		case R.id.demo_start: // 开始按钮
			if (startText.equals(text)) { // 当按钮是开始时
				if (currentMode == DEMO_MODE) {
					startDemoMode();
				} else if (currentMode == REAPPER_MODE) {
					startRepMode();
				}
			} else { // 当按钮是结束时
				if (currentMode == DEMO_MODE) {
					finishDemoMode();
				} else if (currentMode == REAPPER_MODE) {
					abortRepMode();
				}
			}
			break;
		case R.id.demo_pause: // 暂停按钮
			if (pauseText.equals(text)) { // 当按钮是暂停时
				if (currentMode == DEMO_MODE) {
					pauseDemoMode();
				} else if (currentMode == REAPPER_MODE) {
					pauseRepMode();
				}
			} else { // 当按钮是继续时
				if (currentMode == DEMO_MODE) {
					resumeDemoMode();
				} else if (currentMode == REAPPER_MODE) {
					resumeRepMode();
				}
			}
			setButtonClickable(pauseBtn, true);
			break;
		default:
			break;
		}
	}

}
