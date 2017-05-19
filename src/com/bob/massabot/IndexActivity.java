/**
 * Copyright(C) 2017 MassBot Co. Ltd. All rights reserved.
 *
 */
package com.bob.massabot;

import static com.bob.massabot.constant.HttpRequestURLConfig.CONNECT_URL;
import static com.bob.massabot.constant.HttpRequestURLConfig.INDEX_CON_URL;
import static com.bob.massabot.constant.HttpRequestURLConfig.WEB_ROOT;
import static com.bob.massabot.constant.MassabotConstant.SUCCESS_FLAG;
import static com.bob.massabot.constant.MassabotConstant.WIFI_PWD;
import static com.bob.massabot.constant.MassabotConstant.WIFI_SSID;
import static com.bob.massabot.constant.MassabotConstant.WIFI_SSL;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.bob.massabot.model.BaseActivity;
import com.bob.massabot.util.HttpRequestUtils;
import com.bob.massabot.widget.dialog.DialogUtils;
import com.bob.massabot.wifi.WifiConnectUtils;
import com.bob.massabot.wifi.WifiReceiver;
import com.zxing.activity.CaptureActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

/**
 * 首页根据主机IP地址及串口名称连接/断开
 * 
 * @since 2017年4月13日 下午1:59:13
 * @version $Id$
 * @author JiangJibo
 *
 */
public class IndexActivity extends BaseActivity implements OnClickListener {

	private WifiReceiver wifiReceiver;

	private static final Integer QR_SCAN_CODE = 2; // 二维码扫描时的请求code

	private Map<String, String> permissions; // 当前APP需要的权限集合

	private String requestPrefix; // http://IP:8080/项目地址/Controller路径

	private String androidId; // 手机唯一识别码

	private ProgressDialog progressDialog; // 切换wifi连接电机时的等待

	private static Handler handler;

	private String host_adress;
	private String serialPort;

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_index);

		findViewById(R.id.qr_img).setOnClickListener(this);
		progressDialog = DialogUtils.createProgressDialog(this, null, null);

		androidId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
		HttpRequestUtils.cacheHeader("androidId", androidId);

		handler = new Handler() {

			boolean wifiConnected; // wifi状态,是否切换到指定的wifi
			boolean wifiChecked = false; // 在ProgressDialog内延迟一秒连接设备

			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				switch (msg.what) {
				case 1:
					toast(msg.obj.toString());
					break;
				case 2: // 切换wifi有结果了
					if (!WIFI_SSID.equals((String) msg.obj)) {
						toast("当前网络状况不符合条件,请手动连接wifi:[" + WIFI_SSID + "]");
						progressDialog.dismiss();
						host_adress = null;
						wifiConnected = false;
					} else {
						wifiConnected = true;
					}
					break;
				case 3: // 一直未能连接到电机
					if (!progressDialog.isShowing()) { // 弹出框已经关闭
						return;
					}
					int secs = (Integer) msg.obj;
					if (secs > 0) {
						progressDialog.setMessage("连接推拿设备中,请稍等(" + secs + "秒)");
					} else {
						host_adress = null;
						progressDialog.dismiss();
						toast("连接推拿设备超时,请重新连接");
					}
					if (wifiConnected && wifiChecked) { // 在wifi切换成功后,可能网络初始化未完成,需要等待一点时间后再连接电机
						wifiConnected = false; // 只发送一次连接请求
						connectDevices(host_adress, serialPort);
					}
					wifiChecked = wifiConnected; // 在wifi切换成功的下一秒连接电机
					break;
				default:
					break;
				}
			}

		};

		registerWiFiReceiver();
		WifiConnectUtils.init();
		initPermissions();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case 1: // TODO 前一个页面返回,要不使用二维码扫描直接进Main页面
			if (resultCode == RESULT_OK) {
				host_adress = null;
			}
			break;
		case 2:
			// 使用设备ID获取IP,WIFI_SSID,WIFI_PWD
			String result = null;
			if (resultCode == RESULT_OK) { // 二维码扫描结果
				result = data.getExtras().getString("result");
				host_adress = result.substring(0, result.indexOf(","));
				serialPort = result.substring(result.indexOf(",") + 1);
			} else {
				toast("二维码扫描失败,请重新尝试");
				return;
			}
			showProgressDialog();
			WifiConnectUtils.init();
			String ssid = WifiConnectUtils.getSSID();
			if (WIFI_SSID.equals(ssid)) {
				connectDevices(host_adress, serialPort);
			} else {
				connectTargetWifi(WIFI_SSID, WIFI_PWD, WIFI_SSL);
			}
			break;
		default:
			break;
		}
	}

	/* (non-Javadoc)
	 * @see com.bob.massabot.model.BaseActivity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(wifiReceiver);
	}

	/**
	 * 获取跳转到下一个Activity的Intent对象,顺带传递IP和串口信息
	 */
	private Intent createNewActivity(Class<? extends Activity> activity) {
		Intent intent = new Intent(IndexActivity.this, activity);
		intent.putExtra("hostAdress", HttpRequestUtils.getHostAdress(requestPrefix));
		return intent;
	}

	/**
	 * 弹出消息提示框
	 * 
	 * @param text
	 */
	public void toast(String text) {
		Toast.makeText(IndexActivity.this, text, Toast.LENGTH_SHORT).show();
	}

	/* (non-Javadoc)
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.qr_img:
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA }, 1);
				return;
			}
			// 跳转到拍照界面扫描二维码
			startActivityForResult(new Intent(IndexActivity.this, CaptureActivity.class), QR_SCAN_CODE);
			break;
		default:
			break;
		}

	}

	/**
	 * 初始化权限信息,App需要哪些权限
	 */
	private void initPermissions() {
		permissions = new HashMap<String, String>();
		permissions.put(Manifest.permission.READ_PHONE_STATE, "电话");
		permissions.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, "存储");
		int wifiState = WifiConnectUtils.checkState();
		if (wifiState == WifiManager.WIFI_STATE_DISABLED || wifiState == WifiManager.WIFI_STATE_DISABLING) {
			permissions.put(Manifest.permission.CHANGE_WIFI_STATE, "WiFi");
		}
		// permissions.put(Manifest.permission.RECORD_AUDIO, "语音");
		gainingAccess(permissions.keySet().toArray(new String[permissions.size()]), 0);
	}

	/**
	 * 当android版本是6.0以上时,尝试获取权限信息
	 * 
	 * @param permissions
	 * @param requestCode
	 */
	private void gainingAccess(String[] permissions, int requestCode) {
		if (android.os.Build.VERSION.SDK_INT >= 23) {
			ActivityCompat.requestPermissions(this, permissions, requestCode);
		}
	}

	@TargetApi(23)
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode) {
		case 0:
			StringBuffer sb = new StringBuffer();
			for (String permission : permissions) {
				if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
					sb.append(this.permissions.get(permission)).append(",");
				}
			}
			String warn = sb.toString().trim();
			if (!warn.equals("")) {
				toast(warn.substring(0, warn.length() - 1) + " 权限授予失败");
			}
			break;
		case 1:
			for (String permission : permissions) {
				if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
					toast("摄像头权限授予失败");
					return;
				}
			}
			// 跳转到拍照界面扫描二维码
			startActivityForResult(new Intent(IndexActivity.this, CaptureActivity.class), QR_SCAN_CODE);
			break;
		default:
			break;
		}
	}

	/**
	 * 连接上指定wifi后开始连接电机
	 * 
	 * @param ssid
	 */
	private void connectDevices(String host_adress, final String serialPort) {
		requestPrefix = HttpRequestUtils.createRequestUrl(host_adress, WEB_ROOT, INDEX_CON_URL);

		new AsyncTask<Void, Void, String>() {

			protected String doInBackground(Void... params) {
				return HttpRequestUtils.doPost(requestPrefix + CONNECT_URL + "/" + serialPort, null, 3000);
			}

			@Override
			protected void onPostExecute(String result) {
				progressDialog.dismiss();
				if (SUCCESS_FLAG.equals(result)) {
					startActivityForResult(createNewActivity(MainActivity.class), 1);
				} else {
					processConnectResult(result);
				}
			}

		}.execute();

	}

	/**
	 * 根据连接请求的结果是否复位之前的页面按钮
	 * 
	 * @param connectResult
	 */
	public void processConnectResult(String connectResult) {
		if (!connectResult.startsWith("{")) {
			toast("连接的IP为:[" + requestPrefix + "]," + connectResult);
			return;
		}
		Intent intent = createNewActivity(MainActivity.class);
		intent.putExtra("msState", connectResult);
		startActivityForResult(intent, 1);
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
				WifiConnectUtils.retrieveWifiInfo();
				int state = WifiConnectUtils.checkState();
				if (state == WifiManager.WIFI_STATE_ENABLED || state == WifiManager.WIFI_STATE_ENABLING) {
					if (ssid.equals(WifiConnectUtils.getSSID())) {
						return;
					}
					WifiConnectUtils.disConnectWifi();
				}
				WifiConnectUtils.openWifi();
				WifiConnectUtils.addNetwork(WifiConnectUtils.CreateWifiInfo(ssid, pwd, ssl));
			}

		}.start();

	}

	/**
	 * 注册wifi广播接收器
	 */
	private void registerWiFiReceiver() {
		wifiReceiver = new WifiReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("android.net.wifi.STATE_CHANGE");
		intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
		registerReceiver(wifiReceiver, intentFilter);
	}

	/**
	 * 获取handler
	 * 
	 * @return
	 */
	public static Handler getHandler() {
		return handler;
	}

}
