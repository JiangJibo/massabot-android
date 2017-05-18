/**
 * Copyright(C) 2017 MassBot Co. Ltd. All rights reserved.
 *
 */
package com.bob.massabot.wifi;

import java.util.List;

import com.bob.massabot.util.GlobalApplicationUtils;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;

/**
 * 自动连接WiFi的工具类
 * 
 * @since 2017年5月2日 上午9:16:58
 * @version $Id$
 * @author JiangJibo
 *
 */
public class WifiConnectUtils {

	// 定义WifiManager对象
	private static WifiManager mWifiManager;
	// 定义WifiInfo对象
	private static WifiInfo mWifiInfo;
	// 扫描出的网络连接列表
	private static List<ScanResult> mWifiList;
	// 网络连接列表
	private static List<WifiConfiguration> mWifiConfiguration;
	// 定义一个WifiLock
	private static WifiLock mWifiLock;

	static {
		// 取得WifiManager对象
		mWifiManager = (WifiManager) GlobalApplicationUtils.getAppContext().getSystemService(Context.WIFI_SERVICE);
		// 取得WifiInfo对象
		mWifiInfo = mWifiManager.getConnectionInfo();
	}

	// 打开WIFI
	public static void openWifi() {
		if (!mWifiManager.isWifiEnabled()) {
			mWifiManager.setWifiEnabled(true);
		}
	}

	// 关闭WIFI
	public static void closeWifi() {
		if (mWifiManager.isWifiEnabled()) {
			mWifiManager.setWifiEnabled(false);
		}
	}

	// 检查当前WIFI状态
	public static int checkState() {
		return mWifiManager.getWifiState();
	}

	// 锁定WifiLock
	public static void acquireWifiLock() {
		mWifiLock.acquire();
	}

	// 解锁WifiLock
	public static void releaseWifiLock() {
		// 判断时候锁定
		if (mWifiLock.isHeld()) {
			mWifiLock.acquire();
		}
	}

	public static void startScan() {
		mWifiManager.startScan();
		// 得到扫描结果
		mWifiList = mWifiManager.getScanResults();
		// 得到配置好的网络连接
		mWifiConfiguration = mWifiManager.getConfiguredNetworks();
	}

	// 创建一个WifiLock
	public static void creatWifiLock() {
		mWifiLock = mWifiManager.createWifiLock("Test");
	}

	// 得到配置好的网络
	public static List<WifiConfiguration> getConfiguration() {
		return mWifiConfiguration;
	}

	// 指定配置好的网络进行连接
	public static void connectConfiguration(int index) {
		// 索引大于配置好的网络索引返回
		if (index > mWifiConfiguration.size()) {
			return;
		}
		// 连接配置好的指定ID的网络
		mWifiManager.enableNetwork(mWifiConfiguration.get(index).networkId, true);
	}

	// 得到网络列表
	public static List<ScanResult> getWifiList() {
		return mWifiList;
	}

	// 查看扫描结果
	public static StringBuilder lookUpScan() {
		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < mWifiList.size(); i++) {
			stringBuilder.append("Index_" + Integer.valueOf(i + 1).toString() + ":");
			// 将ScanResult信息转换成一个字符串包
			// 其中把包括：BSSID、SSID、capabilities、frequency、level
			stringBuilder.append((mWifiList.get(i)).toString());
			stringBuilder.append("/n");
		}
		return stringBuilder;
	}

	// 得到MAC地址
	public static String getMacAddress() {
		return (mWifiInfo == null) ? "NULL" : mWifiInfo.getMacAddress();
	}

	// 得到接入点的BSSID
	public static String getBSSID() {
		return (mWifiInfo == null) ? "NULL" : mWifiInfo.getBSSID();
	}

	// 得到接入点的SSID
	public static String getSSID() {
		if (mWifiInfo == null) {
			return "NULL";
		}
		String ssid = mWifiInfo.getSSID();
		if (ssid.startsWith("\"")) {
			ssid = ssid.substring(1, ssid.length() - 1);
		}
		return ssid;
	}

	// 得到IP地址
	public static int getIPAddress() {
		return (mWifiInfo == null) ? 0 : mWifiInfo.getIpAddress();
	}

	// 得到连接的ID
	public static int getNetworkId() {
		return (mWifiInfo == null) ? 0 : mWifiInfo.getNetworkId();
	}

	// 得到WifiInfo的所有信息包
	public static String getWifiInfo() {
		return (mWifiInfo == null) ? "NULL" : mWifiInfo.toString();
	}

	/**
	 * 添加一个网络并连接
	 * 
	 * @param wcg
	 * @return 是否连接成功指定wifi
	 */
	public static boolean addNetwork(WifiConfiguration wcg) {
		int wcgID = mWifiManager.addNetwork(wcg);
		return mWifiManager.enableNetwork(wcgID, true);
	}

	// 断开当前连接
	public static void disConnectWifi() {
		disconnectWifi(getNetworkId());
	}

	// 断开指定ID的网络
	public static void disconnectWifi(int netId) {
		mWifiManager.disableNetwork(netId);
		mWifiManager.disconnect();
	}

	// 然后是一个实际应用方法，只验证过没有密码的情况：

	public static WifiConfiguration CreateWifiInfo(String SSID, String Password, int Type) {
		WifiConfiguration config = new WifiConfiguration();
		config.allowedAuthAlgorithms.clear();
		config.allowedGroupCiphers.clear();
		config.allowedKeyManagement.clear();
		config.allowedPairwiseCiphers.clear();
		config.allowedProtocols.clear();
		config.SSID = "\"" + SSID + "\"";

		// android 6.0以下
		if (android.os.Build.VERSION.SDK_INT < 23) {
			WifiConfiguration tempConfig = IsExsits(SSID);
			if (tempConfig != null) {
				mWifiManager.removeNetwork(tempConfig.networkId);
			}
		}

		if (Type == 1) { // 无密码的wifi
			// config.wepKeys[0] = "";
			// config.wepTxKeyIndex = 0;
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
		}
		if (Type == 2) { // WEP加密类型
			config.hiddenSSID = true;
			// config.wepKeys[0]= "\""+Password+"\"";
			config.wepKeys[0] = Password;
			config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			config.wepTxKeyIndex = 0;
		}
		if (Type == 3) { // WPA/WPA2 PSK加密类型,适合普通家庭用户和小型企业运用,在连接wifi时能看到wifi的加密类型
			config.preSharedKey = "\"" + Password + "\"";
			config.hiddenSSID = true;
			config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
			// config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
			config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			config.status = WifiConfiguration.Status.ENABLED;
		}
		return config;
	}

	private static WifiConfiguration IsExsits(String SSID) {
		List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
		for (WifiConfiguration existingConfig : existingConfigs) {
			if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
				return existingConfig;
			}
		}
		return null;
	}

}
