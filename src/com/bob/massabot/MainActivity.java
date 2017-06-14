package com.bob.massabot;

import static com.bob.massabot.constant.MassabotConstant.FINGER_TEMP_MAX;
import static com.bob.massabot.constant.MassabotConstant.FINGER_TEMP_MIN;
import static com.bob.massabot.constant.MassabotConstant.SUCCESS_FLAG;
import static com.bob.massabot.constant.MassabotConstant.WIFI_SSID;
import static com.bob.massabot.util.http.HttpRequestURLConfig.CONNECT_URL;
import static com.bob.massabot.util.http.HttpRequestURLConfig.FINGER_TEMP_SETTING_URL;
import static com.bob.massabot.util.http.HttpRequestURLConfig.MAIN_CON_URL;
import static com.bob.massabot.util.http.HttpRequestURLConfig.MASSAGE_URL;
import static com.bob.massabot.util.http.HttpRequestURLConfig.PAUSE_URL;
import static com.bob.massabot.util.http.HttpRequestURLConfig.PROGRESS_URL;
import static com.bob.massabot.util.http.HttpRequestURLConfig.RESUME_URL;
import static com.bob.massabot.util.http.HttpRequestURLConfig.WEB_ROOT;

import com.bob.massabot.constant.MassageServiceType;
import com.bob.massabot.constant.StateNodes;
import com.bob.massabot.model.BaseActivity;
import com.bob.massabot.model.MassageServiceState;
import com.bob.massabot.util.ActivityCollector;
import com.bob.massabot.util.VoiceToTextProcessor;
import com.bob.massabot.util.http.HttpRequestUtils;
import com.bob.massabot.widget.RoundProgressBar;
import com.google.gson.Gson;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends BaseActivity implements OnClickListener {

	private volatile int progress = 0;// 当前进度

	private int indoorTemp; // 室内温度,手指难以调低到室温以下

	private Button startBtn, pauseBtn, voiceAdjustBtn, shoulderBtn, waistBtn, backBtn;

	private String startText, finishText, pauseText, resumeText;

	private volatile boolean progressing; // 是否开始获取进度

	private String hostAdress, requestPrefix; // 主机IP; http://IP:8080/项目地址/Controller路径

	private MassageServiceType massageType; // 当前选择的服务类型

	private RoundProgressBar roundProgressBar; // 进度环

	private MassageServiceState msState; // 传递过来的状态信息

	private static Handler handler;

	private TextView backToIndex, toDemoCase, tempReduce, tempAdd, tempText;

	private VoiceToTextProcessor voicer; // 语音解析对象

	private long exitTime;

	@Override
	@SuppressLint("HandlerLeak")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		roundProgressBar = (RoundProgressBar) findViewById(R.id.roundProgressBar);
		initButton();
		initTextView();

		handler = new Handler() {

			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				switch (msg.what) {
				case 0:
					toast(msg.obj.toString());
					break;
				case 1:
					doResetAfterProgressed(); // 进度已到达100%,复位按钮
					break;
				case 2:
					break;
				default:
					break;
				}
			}

		};

		retriveDataFromPreIntent();

		initFingerTemp();

		// voicer = new VoiceToTextProcessor(this, requestPrefix);
		// voicer.uploadUserWords();

		remindWifiDialog(WIFI_SSID);

	}

	/**
	 * 初始化TextView,主要是手指温度及后退等
	 */
	private void initTextView() {

		backToIndex = (TextView) findViewById(R.id.demo_to_index);
		backToIndex.setOnClickListener(MainActivity.this);

		toDemoCase = (TextView) findViewById(R.id.main_to_demo);
		toDemoCase.setOnClickListener(MainActivity.this);

		tempReduce = (TextView) findViewById(R.id.finger_temp_reduce);
		tempReduce.setOnClickListener(MainActivity.this);

		tempAdd = (TextView) findViewById(R.id.finger_temp_add);
		tempAdd.setOnClickListener(MainActivity.this);

		tempText = (TextView) findViewById(R.id.finger_temp_num);

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
				return HttpRequestUtils.doPut(requestFilter, requestPrefix + FINGER_TEMP_SETTING_URL + "/" + params[0], null, 1000);
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

	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case 1:
			if (resultCode == RESULT_OK) {
				// connectBtn.performClick();
			}
			break;
		default:
			break;
		}
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onBackPressed()
	 */
	public void onBackPressed() {
		if (!requestFilter.doFilter()) {
			toast(requestFilter.doAfterRejection());
			return;
		}
		long secTime = System.currentTimeMillis();
		if (secTime - exitTime <= 3000) {
			new AsyncTask<Void, Void, Void>() {

				protected Void doInBackground(Void... params) {
					HttpRequestUtils.doDelete(requestFilter, requestPrefix + CONNECT_URL, 5000);
					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					ActivityCollector.removeAllActivity();
				}

			}.execute();
		} else {
			String text = startBtn.getText().toString();
			if (finishText.equals(text)) {
				toast("请结束运行后再返回");
				return;
			}
			exitTime = secTime;
			toast("再按一次断开连接并退出");
		}
	}

	/**
	 * 弹出消息提示框
	 * 
	 * @param text
	 */
	public void toast(String text) {
		Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
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
	 * 在进度条运行结束之后
	 */
	private void doResetAfterProgressed() {
		progressing = false;
		progress = 0;
		startBtn.setText(startText);
		setButtonClickable(pauseBtn, false);
		setMassageServiceButtonClickable(true);
	}

	/**
	 * 统一设置按摩服务按钮是否可选
	 * 
	 * @param clickable
	 */
	private void setMassageServiceButtonClickable(boolean clickable) {
		setButtonClickable(shoulderBtn, clickable);
		setButtonClickable(backBtn, clickable);
		setButtonClickable(waistBtn, clickable);
	}

	/**
	 * 将按摩服务按钮复位,同时字体还原成黑色
	 */
	private void resetMassageButton() {
		shoulderBtn.setTextColor(Color.parseColor("#000000"));
		backBtn.setTextColor(Color.parseColor("#000000"));
		waistBtn.setTextColor(Color.parseColor("#000000"));
		massageType = null;
	}

	/**
	 * 判断是否选择了按摩服务类型
	 * 
	 * @return
	 */
	private boolean checkMassageButtonClicked() {

		return massageType != null;
	}

	public static Handler getHandle() {
		return handler;
	}

	private void initButton() {
		startText = getString(R.string.start_text);
		finishText = getString(R.string.finish_text);
		pauseText = getString(R.string.pause_text);
		resumeText = getString(R.string.resume_text);

		voiceAdjustBtn = (Button) findViewById(R.id.voice_adjust);
		voiceAdjustBtn.setOnClickListener(MainActivity.this);

		startBtn = (Button) findViewById(R.id.start);
		startBtn.setOnClickListener(MainActivity.this);

		pauseBtn = (Button) findViewById(R.id.pause);
		pauseBtn.setOnClickListener(MainActivity.this);

		shoulderBtn = (Button) findViewById(R.id.shoulder);
		shoulderBtn.setOnClickListener(MainActivity.this);
		shoulderBtn.setText(MassageServiceType.SHOULDER.label);

		backBtn = (Button) findViewById(R.id.back);
		backBtn.setOnClickListener(MainActivity.this);
		backBtn.setText(MassageServiceType.BACK.label);

		waistBtn = (Button) findViewById(R.id.waist);
		waistBtn.setOnClickListener(MainActivity.this);
		waistBtn.setText(MassageServiceType.WAIST.label);

	}

	/**
	 * 根据连接请求的结果是否复位之前的页面按钮
	 * 
	 * @param connectResult
	 */
	public void resetWithConnectResult(String connectResult) {
		if (!connectResult.startsWith("{")) {
			toast("连接的IP为:[" + requestPrefix + "]," + connectResult);
			return;
		}
		MassageServiceState msState = new Gson().fromJson(connectResult, MassageServiceState.class);

		/*toast("端口名称为:[" + msState.getCurrentPortName() + "]," + "当前状态为:[" + StateNodes.valueOf(msState.getStateNodeCode()).label + "],当前进度为:["
				+ msState.getProgress() + "]");*/
		doResetWithMsState(msState);
	}

	/**
	 * 根据连接返回的状态信息恢复之前的页面控件选择
	 */
	public void doResetWithMsState(MassageServiceState msState) {
		doResetWithStateNode(StateNodes.valueOf(msState.getStateNodeCode()));
		doResetWithMsType(msState.getCurrentType());
		doResetFingerTemp(msState.getFingerTemp());
	}

	/**
	 * 重设温度
	 * 
	 * @param fingerTemp
	 */
	private void doResetFingerTemp(int fingerTemp) {
		if (fingerTemp != 0) {
			tempText.setText(fingerTemp + "℃");
		}
	}

	/**
	 * 根据状态节点重设页面连接,开始,暂停等按钮的状态,APP左側按鈕
	 * 
	 * @param stateNodeCode
	 */
	private void doResetWithStateNode(StateNodes stateNode) {
		startBtn.setClickable(true);
		switch (stateNode) {
		case PAUSED: // 用户在暂停期间重新开启了APP,获取暂停时的进度信息,恢复进度环
			startBtn.setText(finishText);
			pauseBtn.setText(resumeText);
			setButtonClickable(pauseBtn, true);
			progress = msState.getProgress();
			roundProgressBar.setProgress(progress);
			progressing = false;
			break;
		case RUNNING:
			startBtn.setText(finishText);
			pauseBtn.setText(pauseText);
			setButtonClickable(pauseBtn, true);
			progressing = true;
			getProgress();
			break;
		case CONNECTED:
			setMassageServiceButtonClickable(true);
			break;
		default:
			break;
		}
	}

	/**
	 * 重设服务类型按钮
	 * 
	 * @param typeCode
	 */
	private void doResetWithMsType(Integer typeCode) {
		MassageServiceType msType = MassageServiceType.valueOf(typeCode);
		if (msType == null) {
			return;
		}
		switch (msType) {
		case SHOULDER:
			shoulderBtn.performClick();
			break;
		case BACK:
			backBtn.performClick();
			break;
		case WAIST:
			waistBtn.performClick();
			break;
		default:
			break;
		}
	}

	/**
	 * 获取从上一个Activity传递过来的数据
	 */
	private void retriveDataFromPreIntent() {
		Intent intent = getIntent();
		this.hostAdress = intent.getStringExtra("hostAdress");
		this.requestPrefix = HttpRequestUtils.createRequestUrl(hostAdress, WEB_ROOT, MAIN_CON_URL);
		String msStateString = intent.getStringExtra("msState");
		if (msStateString != null) {
			msState = new Gson().fromJson(msStateString, MassageServiceState.class);
			doResetWithMsState(msState);
		}
	}

	/**
	 * 设置TextView(Button)的可选性及文字颜色
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
	public void onClick(final View v) {
		TextView tv = (TextView) v;
		String text = tv.getText().toString();
		switch (v.getId()) {
		case R.id.start: // 开始按钮
			try {
				if (startText.equals(text)) {
					if (!checkMassageButtonClicked()) {
						toast("开始前请先选定按摩服务类型及力度!");
						return;
					}
					new AsyncTask<Void, Void, String>() {

						protected String doInBackground(Void... params) {
							return HttpRequestUtils.doPost(requestFilter, requestPrefix + MASSAGE_URL + "/" + massageType.code, null, 3000);
						}

						protected void onPostExecute(String result) {
							if (SUCCESS_FLAG.equals(result)) {
								((Button) v).setText(finishText);
								progressing = true;
								setButtonClickable(pauseBtn, true);
								pauseBtn.setText(pauseText);
								setMassageServiceButtonClickable(false);
								getProgress();
							} else {
								toast(result);
							}
						}

					}.execute();
				} else if (finishText.equals(text)) {

					new AsyncTask<Void, Void, String>() {

						protected String doInBackground(Void... params) {
							return HttpRequestUtils.doDelete(requestFilter, requestPrefix + MASSAGE_URL, 3000);
						}

						protected void onPostExecute(String result) {
							if (SUCCESS_FLAG.equals(result)) {
								progressing = false;
								((Button) v).setText(startText);
								setButtonClickable(pauseBtn, false);
								roundProgressBar.setProgress(0);
								resetMassageButton();
								setMassageServiceButtonClickable(true);
								pauseBtn.setText(pauseText);
							} else {
								toast(result);
							}
						}

					}.execute();
				}
			} catch (Exception e) {
				toast(text + "操作失败,请重试");
			}
			break;

		case R.id.pause: // 暂停按钮
			try {
				if (pauseText.equals(text)) {
					progressing = false;
					new AsyncTask<Void, Void, String>() {

						protected String doInBackground(Void... params) {
							return HttpRequestUtils.doPut(requestFilter, requestPrefix + MASSAGE_URL + PAUSE_URL, null, 3000);
						}

						protected void onPostExecute(String result) {
							if (SUCCESS_FLAG.equals(result)) {
								((Button) v).setText(resumeText);
							} else {
								toast(result);
							}
						}

					}.execute();
				} else if (resumeText.equals(text)) {
					progressing = true;
					new AsyncTask<Void, Void, String>() {

						protected String doInBackground(Void... params) {
							return HttpRequestUtils.doPut(requestFilter, requestPrefix + MASSAGE_URL + RESUME_URL, null, 3000);
						}

						protected void onPostExecute(String result) {
							if (SUCCESS_FLAG.equals(result)) {
								((Button) v).setText(pauseText);
								getProgress();
							} else {
								toast(result);
							}
						}

					}.execute();
				}
			} catch (Exception e) {
				toast(text + "操作失败,请重试");
			}
			break;

		case R.id.voice_adjust: // 语音调节按钮
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.RECORD_AUDIO }, 0);
				return;
			}
			voicer.processVoice();
			break;

		case R.id.shoulder:
			resetMassageButton();
			tv.setTextColor(Color.rgb(66, 229, 36));
			massageType = MassageServiceType.SHOULDER;
			break;

		case R.id.back: // 背部按摩
			resetMassageButton();
			tv.setTextColor(Color.rgb(66, 229, 36));
			massageType = MassageServiceType.BACK;
			break;

		case R.id.waist: // 腰部按摩
			resetMassageButton();
			tv.setTextColor(Color.rgb(66, 229, 36));
			massageType = MassageServiceType.BACK;
			break;

		case R.id.demo_to_index: // 返回首页
			if (finishText.equals(startBtn.getText().toString())) {
				toast("请结束运行后再返回");
				return;
			}
			finish();
			break;

		case R.id.main_to_demo: // 进入示教页
			resetMassageButton();
			Intent intent = new Intent(MainActivity.this, DemoActivity.class);
			intent.putExtra("hostAdress", HttpRequestUtils.getHostAdress(requestPrefix));
			startActivityForResult(intent, 1);
			break;

		case R.id.finger_temp_reduce: // -指温
			processFingerTemp(false);
			break;

		case R.id.finger_temp_add: // +指温
			processFingerTemp(true);
			break;

		default:
			break;
		}

	}

	/**
	 * 提醒用户在使用电机期间保持对控制wifi的连接
	 */
	private void remindWifiDialog(String ssid) {
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
		dialogBuilder.setTitle("重要提醒");
		dialogBuilder.setMessage("在进行推拿服务期间,请保持对wifi:[" + ssid + "]的连接,若[" + ssid + "]断开请手动重连");
		dialogBuilder.setCancelable(false);
		dialogBuilder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		dialogBuilder.create().show();
	}

	@TargetApi(23)
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode) {
		case 0: // 处理语音授权结果
			for (String permission : permissions) {
				if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
					toast("语音权限授予失败");
					return;
				}
				voicer.processVoice();
			}
			break;
		}
	}

	/**
	 * 初始化手指温度
	 */
	private void initFingerTemp() {

		new AsyncTask<Void, Void, String>() {

			protected String doInBackground(Void... params) {
				return HttpRequestUtils.doGet(requestFilter, requestPrefix + FINGER_TEMP_SETTING_URL, 3000);
			}

			protected void onPostExecute(String result) {
				try {
					int temp = Integer.parseInt(result);
					tempText.setText(result);
					indoorTemp = temp;
					setTextViewClickable(tempReduce, false);
				} catch (NumberFormatException e) {
					toast("温度为:" + result);
					// toast("初始化手指温度失败");
				}
			}

		}.execute();
	}

}
