/**
 * Copyright(C) 2017 MassBot Co. Ltd. All rights reserved.
 *
 */
package com.bob.massabot.wifi;

import com.bob.massabot.IndexActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Message;

/**
 * @since 2017年5月8日 上午9:28:38
 * @version $Id$
 * @author JiangJibo
 *
 */
public class WifiReceiver extends BroadcastReceiver {

	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		// Toast.makeText(context, "WIFI接收器接受到了", Toast.LENGTH_SHORT).show();
		if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) { // WiFi信号强度改变
			// signal strength changed
		} else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {// wifi连接上与否

			NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
				//
			} else if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
				String ssid = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getSSID();
				if (ssid.startsWith("\"")) {
					ssid = ssid.substring(1, ssid.length() - 1);
				}
				Message msg = new Message();
				msg.what = 2;
				msg.obj = ssid;
				IndexActivity.getHandler().sendMessage(msg);
			}

		} else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {// wifi打开与否
			int wifistate = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);

			if (wifistate == WifiManager.WIFI_STATE_DISABLED) {
				System.out.println("系统关闭wifi");
			} else if (wifistate == WifiManager.WIFI_STATE_ENABLED) {
				System.out.println("系统开启wifi");
			}
		}
	}

}
