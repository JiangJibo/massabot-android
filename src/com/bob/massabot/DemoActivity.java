package com.bob.massabot;

import static com.bob.massabot.constant.MassabotConstant.FINGER_TEMP_MAX;
import static com.bob.massabot.constant.MassabotConstant.FINGER_TEMP_MIN;
import static com.bob.massabot.constant.MassabotConstant.SUCCESS_FLAG;
import static com.bob.massabot.constant.MassabotConstant.WIFI_PWD;
import static com.bob.massabot.constant.MassabotConstant.WIFI_SSID;
import static com.bob.massabot.constant.MassabotConstant.WIFI_SSL;
import static com.bob.massabot.util.http.HttpRequestURLConfig.CONNECT_URL;
import static com.bob.massabot.util.http.HttpRequestURLConfig.DEMOCASE_CON_URL;
import static com.bob.massabot.util.http.HttpRequestURLConfig.DEMO_FILE_READ_URL;
import static com.bob.massabot.util.http.HttpRequestURLConfig.DEMO_FILE_WRITE_URL;
import static com.bob.massabot.util.http.HttpRequestURLConfig.FINGER_TEMP_SETTING_URL;
import static com.bob.massabot.util.http.HttpRequestURLConfig.INDEX_CON_URL;
import static com.bob.massabot.util.http.HttpRequestURLConfig.MAIN_CON_URL;
import static com.bob.massabot.util.http.HttpRequestURLConfig.PAUSE_URL;
import static com.bob.massabot.util.http.HttpRequestURLConfig.PROGRESS_URL;
import static com.bob.massabot.util.http.HttpRequestURLConfig.RESETTING_DEVICE;
import static com.bob.massabot.util.http.HttpRequestURLConfig.RESUME_URL;
import static com.bob.massabot.util.http.HttpRequestURLConfig.WEB_ROOT;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import com.bob.massabot.constant.MassabotConstant;
import com.bob.massabot.model.BaseActivity;
import com.bob.massabot.util.http.HttpRequestUtils;
import com.bob.massabot.widget.DropDownListView;
import com.bob.massabot.widget.RoundProgressBar;
import com.bob.massabot.widget.dialog.DialogUtils;
import com.bob.massabot.wifi.WifiConnectUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
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
public class DemoActivity extends BaseActivity implements OnClickListener, OnLongClickListener {

	public static final String DEMO_FILE_EMPTY_FLAG = "(0)"; // 示教文件为空的标识
	public static final Integer DEMO_MODE_FLAG = 0; // 演示模式
	public static final String DEMO_FILE_NOT_EMPTY_FLAG = "(1)"; // 示教文件非空的标识
	public static final Integer REAPPER_MODE_FLAG = 1; // 复现模式

	private String curDemoCaseName; // 当前选中的示教案列名称
	private volatile String curSerialPort; // 当前的串口名称

	private ProgressDialog progressDialog; // 切换wifi连接电机时的等待

	private DropDownListView demoDropdown; // 示教下拉框

	private Integer currentMode = -1; // 当前选择的模式。0:示教模式;1:复现模式
	private static final Integer DEMO_MODE = 0;
	private static final Integer REAPPER_MODE = 1;

	private long rangeTime; // 计时器暂停时的时间差

	private volatile int progress = 0;// 当前进度
	private volatile boolean progressing; // 是否开始获取进度

	private RoundProgressBar roundProgressBar; // 进度环
	private Chronometer timer; // 计时器

	private Button com5, com9, startBtn, pauseBtn, modeBtn, reapperBtn;
	private String startText, finishText, pauseText, resumeText;

	private TextView tempReduce, tempAdd, tempText;

	private String hostAdress, requestPrefix;

	private Handler handler;

	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_demo);
		demoDropdown = (DropDownListView) findViewById(R.id.demo_dropdown);

		roundProgressBar = (RoundProgressBar) findViewById(R.id.demoRoundProgressBar);
		timer = (Chronometer) this.findViewById(R.id.chronometer);

		progressDialog = DialogUtils.createProgressDialog(this, null, null);

		findViewById(R.id.demo_back_to_main).setOnClickListener(DemoActivity.this);

		initButton();

		initTextView();

		this.hostAdress = MassabotConstant.HOST_ADRESS_IP;
		this.requestPrefix = HttpRequestUtils.createRequestUrl(hostAdress, WEB_ROOT, "");

		handler = new Handler() {

			boolean wifiConnected; // wifi状态,是否切换到指定的wifi
			boolean wifiChecked = false; // 在ProgressDialog内延迟一秒连接设备

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
					if (!progressDialog.isShowing()) { // 弹出框已经关闭
						return;
					}
					int secs = (Integer) msg.obj;
					if (secs > 0) {
						progressDialog.setMessage("连接推拿设备中,请稍等(" + secs + "秒)");
					} else {
						progressDialog.dismiss();
						toast("连接推拿设备超时,请重新连接");
					}
					if (wifiConnected && wifiChecked) { // 在wifi切换成功后,可能网络初始化未完成,需要等待一点时间后再连接电机
						wifiConnected = false; // 只发送一次连接请求
						connectDevices(MassabotConstant.HOST_ADRESS_IP, curSerialPort);
					}
					wifiChecked = wifiConnected; // 在wifi切换成功的下一秒连接电机
					break;
				default:
					break;
				}
			}

		};

		initDemoDropdown();

	}

	/**
	 * 初始化4个Button
	 */
	private void initButton() {
		com5 = (Button) findViewById(R.id.com5);
		com5.setOnClickListener(DemoActivity.this);

		com9 = (Button) findViewById(R.id.com9);
		com9.setOnClickListener(DemoActivity.this);

		startBtn = (Button) findViewById(R.id.demo_start);
		startBtn.setOnClickListener(DemoActivity.this);
		pauseBtn = (Button) findViewById(R.id.demo_pause);
		pauseBtn.setOnClickListener(DemoActivity.this);
		modeBtn = (Button) findViewById(R.id.demo_mode);
		modeBtn.setOnClickListener(DemoActivity.this);
		setButtonClickable(modeBtn, false);
		reapperBtn = (Button) findViewById(R.id.reappear_mode);
		reapperBtn.setOnClickListener(DemoActivity.this);
		reapperBtn.setOnLongClickListener(DemoActivity.this);
		setButtonClickable(reapperBtn, false);
		setButtonLongClickable(reapperBtn, false);

		startText = getString(R.string.start_text);
		finishText = getString(R.string.finish_text);
		pauseText = getString(R.string.pause_text);
		resumeText = getString(R.string.resume_text);
	}

	/**
	 * 初始化TextView,主要是手指温度及后退等
	 */
	private void initTextView() {

		tempReduce = (TextView) findViewById(R.id.finger_temp_reduce);
		tempReduce.setOnClickListener(DemoActivity.this);

		tempAdd = (TextView) findViewById(R.id.finger_temp_add);
		tempAdd.setOnClickListener(DemoActivity.this);

		tempText = (TextView) findViewById(R.id.finger_temp_num);

	}

	/**
	 * 断开指定串口
	 * 
	 */
	private boolean disConnect() {

		Callable<String> callable = new Callable<String>() {

			@Override
			public String call() throws Exception {
				return HttpRequestUtils.doDelete(requestFilter, requestPrefix + INDEX_CON_URL + CONNECT_URL, 5000);
			}
		};

		Future<String> future = HttpRequestUtils.EXECUTOR.submit(callable);
		String result = null;
		try {
			result = future.get();
			if (SUCCESS_FLAG.equals(result)) {
				return true;
			}
		} catch (Exception e) {

		}
		toast(result);
		return false;
	}

	/**
	 * 连接指定的串口
	 * 
	 * @param serialPort
	 */
	private void connect(String serialPort) {
		showProgressDialog();
		String ssid = new WifiConnectUtils(this).getSSID();
		if (WIFI_SSID.equals(ssid)) {
			connectDevices(MassabotConstant.HOST_ADRESS_IP, serialPort);
		} else {
			connectTargetWifi(WIFI_SSID, WIFI_PWD, WIFI_SSL);
		}
		connectDevices(MassabotConstant.HOST_ADRESS_IP, serialPort);
	}

	/**
	 * 在切换wifi连接电机过程中,一直显示此弹框,直到连接成功/失败
	 */
	private void showProgressDialog() {
		progressDialog.show();
		final Timer timer = new Timer();
		timer.schedule(new TimerTask() {

			int second = 10;

			@Override
			public void run() {
				Message msg = new Message();
				msg.what = 3;
				msg.obj = second;
				handler.sendMessage(msg);
				second--;
				if (!progressDialog.isShowing()) {
					timer.cancel();
				}
			}
		}, 0, 1000);
	}

	/**
	 * 连接上指定wifi后开始连接电机
	 * 
	 * @param ssid
	 */
	private void connectDevices(String host_adress, final String serialPort) {

		new AsyncTask<Void, Void, String>() {

			protected String doInBackground(Void... params) {
				return HttpRequestUtils.doPost(requestPrefix + INDEX_CON_URL + CONNECT_URL + "/" + serialPort, null, 3000);
			}

			@Override
			protected void onPostExecute(String result) {
				progressDialog.dismiss();
				if (result != null && SUCCESS_FLAG.equals(result) || result.startsWith("{")) {
					curSerialPort = serialPort;
					if (isDemoMode()) {
						setButtonClickable(com5, false);
						setButtonClickable(reapperBtn, true);
						setButtonLongClickable(reapperBtn, true);
					} else {
						setButtonClickable(com9, false);
						setButtonClickable(modeBtn, true);
						initFingerTemp();
					}
				} else {
					toast("连接" + serialPort + "串口失败,結果为:[" + result + "]");
				}
			}

		}.execute();

	}

	/**
	 * 在演示模式下开始
	 */
	private void startDemoMode() {

		new AsyncTask<Void, Void, String>() {

			@Override
			protected String doInBackground(Void... params) {
				return HttpRequestUtils.doPut(requestFilter, requestPrefix + DEMOCASE_CON_URL + DEMO_FILE_WRITE_URL + "?demoName=" + curDemoCaseName, null,
						3000);
			}

			protected void onPostExecute(String result) {
				if (SUCCESS_FLAG.equals(result)) {
					timer.start();
					lockWidgetAfterStarted();
				} else {
					toast(result);
				}
			}

		}.execute();
	}

	/**
	 * 暂停演示模式运行
	 */
	private void pauseDemoMode() {

		new AsyncTask<Void, Void, String>() {

			@Override
			protected String doInBackground(Void... params) {
				return HttpRequestUtils.doPut(requestPrefix + DEMOCASE_CON_URL + DEMO_FILE_WRITE_URL + PAUSE_URL, null, 3000);
			}

			protected void onPostExecute(String result) {
				if (SUCCESS_FLAG.equals(result)) {
					timer.stop();
					rangeTime = SystemClock.elapsedRealtime() - timer.getBase();
					pauseBtn.setText(resumeText);
				} else {
					toast(result);
				}
			}

		}.execute();
	}

	/**
	 * 恢复演示模式运行
	 */
	private void resumeDemoMode() {

		new AsyncTask<Void, Void, String>() {

			protected String doInBackground(Void... params) {
				return HttpRequestUtils.doPut(requestFilter, requestPrefix + DEMOCASE_CON_URL + DEMO_FILE_WRITE_URL + RESUME_URL, null, 3000);
			}

			protected void onPostExecute(String result) {
				if (SUCCESS_FLAG.equals(result)) {
					timer.setBase(SystemClock.elapsedRealtime() - rangeTime);
					timer.start();
					pauseBtn.setText(pauseText);
				} else {
					toast(result);
				}
			}

		}.execute();
	}

	/**
	 * 在演示模式下结束
	 */
	private void finishDemoMode() {

		new AsyncTask<Void, Void, String>() {

			protected String doInBackground(Void... params) {
				return HttpRequestUtils.doPost(requestFilter, requestPrefix + DEMOCASE_CON_URL + DEMO_FILE_WRITE_URL, null, 3000);
			}

			protected void onPostExecute(String result) {
				if (SUCCESS_FLAG.equals(result)) {
					processDemoNameAfterFinished();
				} else {
					toast(result);
				}
				timer.stop();
				lockWidgetAfterFinished();
			}

		}.execute();
	}

	/**
	 * 在复现模式下开始
	 * 
	 * @return
	 */
	private void startRepMode() {
		new AsyncTask<Void, Void, String>() {

			protected String doInBackground(Void... params) {
				return HttpRequestUtils.doPost(requestFilter, requestPrefix + DEMOCASE_CON_URL + DEMO_FILE_READ_URL + "?demoName=" + curDemoCaseName, null,
						3000);
			}

			protected void onPostExecute(String result) {
				if (SUCCESS_FLAG.equals(result)) {
					progressing = true;
					lockWidgetAfterStarted();
					getProgress();
				} else {
					toast(result);
				}
			}

		}.execute();
	}

	/**
	 * 暂停复现模式
	 */
	private void pauseRepMode() {
		new AsyncTask<Void, Void, String>() {

			protected String doInBackground(Void... params) {
				return HttpRequestUtils.doPut(requestFilter, requestPrefix + DEMOCASE_CON_URL + DEMO_FILE_READ_URL + PAUSE_URL, null, 3000);
			}

			protected void onPostExecute(String result) {
				if (SUCCESS_FLAG.equals(result)) {
					progressing = false;
					pauseBtn.setText(resumeText);
				} else {
					toast(result);
				}
			}

		}.execute();
	}

	/**
	 * 恢复复现模式的运行
	 */
	private void resumeRepMode() {
		new AsyncTask<Void, Void, String>() {

			protected String doInBackground(Void... params) {
				return HttpRequestUtils.doPut(requestFilter, requestPrefix + DEMOCASE_CON_URL + DEMO_FILE_READ_URL + RESUME_URL, null, 3000);
			}

			protected void onPostExecute(String result) {
				if (SUCCESS_FLAG.equals(result)) {
					progressing = true;
					pauseBtn.setText(pauseText);
					getProgress();
				} else {
					toast(result);
				}
			}

		}.execute();
	}

	/**
	 * 中途结束示教复现模式
	 */
	private void abortRepMode() {
		new AsyncTask<Void, Void, String>() {

			protected String doInBackground(Void... params) {
				return HttpRequestUtils.doDelete(requestFilter, requestPrefix + DEMOCASE_CON_URL + DEMO_FILE_READ_URL, 3000);
			}

			protected void onPostExecute(String result) {
				if (SUCCESS_FLAG.equals(result)) {
					progressing = false;
					roundProgressBar.setProgress(0);
					lockWidgetAfterFinished();
				} else {
					toast(result);
				}
			}

		}.execute();
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
					String result = HttpRequestUtils.doGet(requestPrefix + DEMOCASE_CON_URL + PROGRESS_URL, 2000);

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
		new AsyncTask<Void, Void, String>() {

			protected String doInBackground(Void... params) {
				return HttpRequestUtils.doGet(requestPrefix + DEMOCASE_CON_URL, 3000);
			}

			protected void onPostExecute(String result) {
				if (result != null && !HttpRequestUtils.CONNECT_FAILED_RESULT.equals(result)) {
					ArrayList<String> data = new Gson().fromJson(result, new TypeToken<ArrayList<String>>() {
					}.getType());
					curDemoCaseName = data.get(0);
					demoDropdown.setItemsData(data, 0);
				} else {
					demoDropdown.setItemsData(new ArrayList<String>(), null);
					toast(result);
				}
			}

		}.execute();
	}

	/**
	 * 创建新的示教案列
	 * 
	 * @param demoName
	 */
	public boolean createNewDemoCase(final String demoName) {

		new AsyncTask<Void, Void, String>() {

			protected String doInBackground(Void... params) {
				return HttpRequestUtils.doPost(requestPrefix + DEMOCASE_CON_URL + "?demoName=" + demoName, null, 3000);
			}

			protected void onPostExecute(String result) {
				if (!SUCCESS_FLAG.equals(result)) {
					toast(result);
				}
			}

		}.execute();
		return true;
	}

	/**
	 * 删除指定的示教案列
	 * 
	 * @param demoName
	 * @return
	 */
	public boolean deleteDemoCase(final String demoName) {
		new AsyncTask<Void, Void, String>() {

			protected String doInBackground(Void... params) {
				return HttpRequestUtils.doDelete(requestPrefix + DEMOCASE_CON_URL + "?demoName=" + demoName, 3000);
			}

			protected void onPostExecute(String result) {
				if (!SUCCESS_FLAG.equals(result)) {
					toast(result);
				}
			}

		}.execute();
		return true;
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
		setButtonClickable(reapperBtn, true);
		demoDropdown.setClickable(true);
		demoDropdown.setLongClickable(true);
		startBtn.setText(startText);
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
		itemsData.set(index, curDemoCaseName);
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

	/**
	 * 設置是否可以長按
	 * 
	 * @param button
	 * @param clickable
	 */
	private void setButtonLongClickable(Button button, boolean clickable) {
		button.setLongClickable(clickable);
	}

	/**
	 * 提高/降低指温
	 * 
	 * @param operation
	 *            true:提高;false:降低
	 */
	private void processFingerTemp(final boolean operation) {
		int temp0 = Integer.parseInt(tempText.getText().toString().substring(0, 2));
		final int temp1 = operation ? temp0 + 1 : temp0 - 1;
		new AsyncTask<Integer, Void, String>() {

			protected String doInBackground(Integer... params) {
				return HttpRequestUtils.doPut(requestFilter, requestPrefix + MAIN_CON_URL + FINGER_TEMP_SETTING_URL + "/" + params[0], null, 1000);
			}

			@Override
			protected void onPostExecute(String result) {
				if (!SUCCESS_FLAG.equals(result)) {
					toast(result);
					return;
				}
				if (operation) {
					setTextViewClickable(tempReduce, true);
					tempText.setText(temp1 + "℃");
					if (temp1 == FINGER_TEMP_MAX) {
						setTextViewClickable(tempAdd, false);
					}
				} else {
					setTextViewClickable(tempAdd, true);
					tempText.setText(temp1 + "℃");
					if (temp1 == FINGER_TEMP_MIN) {
						setTextViewClickable(tempReduce, false);
					}
				}
			}
		}.execute(temp1);

	}

	/**
	 * 初始化手指温度
	 */
	private void initFingerTemp() {

		new AsyncTask<Void, Void, String>() {

			protected String doInBackground(Void... params) {
				return HttpRequestUtils.doGet(requestFilter, requestPrefix + MAIN_CON_URL + FINGER_TEMP_SETTING_URL, 3000);
			}

			protected void onPostExecute(String result) {
				try {
					Integer.parseInt(result);
					tempText.setText(result);
					setTextViewClickable(tempReduce, false);
				} catch (NumberFormatException e) {
					toast("温度为:" + result);
					// toast("初始化手指温度失败");
				}
			}

		}.execute();
	}

	/**
	 * 连接到电机控制的wifi
	 * 
	 * @param ssid
	 *            wifi名称
	 * @param pwd
	 *            wifi密码
	 * @param ssl
	 *            wifi加密类型
	 */
	private void connectTargetWifi(final String ssid, final String pwd, final int ssl) {

		new Thread() {

			public void run() {
				WifiConnectUtils wifiUtils = new WifiConnectUtils(DemoActivity.this);
				int state = wifiUtils.checkState();
				if (state == WifiManager.WIFI_STATE_ENABLED || state == WifiManager.WIFI_STATE_ENABLING) {
					if (ssid.equals(wifiUtils.getSSID())) {
						return;
					}
					wifiUtils.disConnectWifi();
				}
				wifiUtils.openWifi();
				wifiUtils.addNetwork(wifiUtils.CreateWifiInfo(ssid, pwd, ssl));
			}

		}.start();

	}

	/**
	 * 当前是否是示教模式
	 * 
	 * @return
	 */
	private boolean isDemoMode() {
		return curSerialPort != null && com5.getText().toString().equals(curSerialPort);
	}

	/**
	 * 设置TextView是否可选
	 * 
	 * @param tv
	 * @param clickable
	 */
	private void setTextViewClickable(TextView tv, boolean clickable) {
		tv.setClickable(clickable);
		if (clickable) {
			tv.setTextColor(Color.parseColor("#FFFFFF"));
		} else {
			tv.setTextColor(Color.parseColor("#000000"));
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

		case R.id.finger_temp_reduce: // -指温
			processFingerTemp(false);
			break;

		case R.id.finger_temp_add: // +指温
			processFingerTemp(true);
			break;
		case R.id.com5:
		case R.id.com9:
			if (curSerialPort != null && !text.equals(curSerialPort)) {
				boolean result = disConnect();
				if (result) {
					if (isDemoMode()) {
						setButtonClickable(com5, true);
						setButtonClickable(reapperBtn, false);
						setButtonLongClickable(reapperBtn, false);
					} else {
						setButtonClickable(com9, true);
						setButtonClickable(modeBtn, false);
					}
					connect(text);
				}
			} else {
				connect(text);
			}
			break;
		default:
			break;
		}
	}

	/* (non-Javadoc)
	 * @see android.view.View.OnLongClickListener#onLongClick(android.view.View)
	 */
	@Override
	public boolean onLongClick(View v) {
		switch (v.getId()) {
		case R.id.reappear_mode:
			new AsyncTask<Void, Void, String>() {

				@Override
				protected String doInBackground(Void... params) {
					return HttpRequestUtils.doPut(requestPrefix + DEMOCASE_CON_URL + RESETTING_DEVICE, null, 1000);
				}

				protected void onPostExecute(String result) {
					if (result == null) {
						toast(reapperBtn.getText().toString() + "操作失败");
					}
				}

			}.execute();
			return true;
		}
		return false;

	}

}
