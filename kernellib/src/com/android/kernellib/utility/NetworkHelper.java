package com.android.kernellib.utility;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;

/** 
* @ClassName: NetWorkHelper 
* @Description: TODO(网络判断所使用的类) 
* @author zhuchengjin
*  
*/
public class NetworkHelper {

	public static final String NET_UNKNOW = "unknow";
	public static final String NET_SG = "2g";
	public static final String NET_TG = "3g";
	public static final String NET_FG = "4g";
	public static final String NET_WIFI = "wifi";

	// 3G补全
	private static final int NETWORK_TYPE_HSDPA = 8;
	private static final int NETWORK_TYPE_EVDO_B = 12;
	private static final int NETWORK_TYPE_HSPA = 10;
	private static final int NETWORK_TYPE_HSPAP = 15;
	private static final int NETWORK_TYPE_HSUPA = 9;
	// 2G补全
	private static final int NETWORK_TYPE_IDEN = 11;
	// 4G补全
	private static final int NETWORK_TYPE_LTE = 14;
	
	/** 
	* @Title: hasNetWork 
	* @Description: TODO(判断是否有可用的网络) 
	* @param @param context
	* @return void    返回类型 
	* @throws 
	*/
	public static boolean hasNetWork(Context context){
		try {
			NetworkInfo netInfo = getActiveNetworkInfo(context);
			if(null != netInfo && netInfo.getState() == NetworkInfo.State.CONNECTED){
				return true;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * @param ctx
	 * @return
	 * getActiveNetworkInfo 获取当前链接的网络
	 */
	public static NetworkInfo getActiveNetworkInfo(Context ctx){
		try {
			ConnectivityManager cm = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo netInfo = null;
			if(null != cm){
				netInfo = cm.getActiveNetworkInfo();
				if(null != netInfo && netInfo.isAvailable()){
					return netInfo;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/** 
	* @Title: isWifiConnected 
	* @Description: TODO(判断是否是wifi网络) 
	* @param @param context
	* @param @return
	* @return boolean    返回类型  true表示 wifi连接 ，false 表示非wifi状态
	* @throws 
	*/
	public static boolean isWifiConnected(Context context){
		
		try{
			
			NetworkInfo info = getActiveNetworkInfo(context);
			if(null != info && info.getType() == ConnectivityManager.TYPE_WIFI){
				return true;
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}
	
	/** 
	* @Title: getNetworkType 
	* @Description: TODO(获取手机网络类型) 
	* @param @param context
	* @param @return
	* @return boolean    返回类型 
	* @throws 
	*/
	public static String getMobileNetworkType(Context mContext) {
		if (hasNetWork(mContext)) {
			if (isWifiConnected(mContext)) {
				return NET_WIFI;
			} else {
				TelephonyManager tm = (TelephonyManager) mContext
						.getSystemService(Context.TELEPHONY_SERVICE);
				if (tm == null) {
					return NET_UNKNOW;
				} else if (tm.getNetworkType() == NETWORK_TYPE_LTE) {
					return NET_FG;
				} else if (tm.getNetworkType() == TelephonyManager.NETWORK_TYPE_EVDO_0
						|| tm.getNetworkType() == TelephonyManager.NETWORK_TYPE_EVDO_A
						|| tm.getNetworkType() == TelephonyManager.NETWORK_TYPE_UMTS
						|| tm.getNetworkType() == NETWORK_TYPE_HSDPA
						|| tm.getNetworkType() == NETWORK_TYPE_EVDO_B
						|| tm.getNetworkType() == NETWORK_TYPE_HSPA
						|| tm.getNetworkType() == NETWORK_TYPE_HSPAP
						|| tm.getNetworkType() == NETWORK_TYPE_HSUPA) {
					return NET_TG;
				} else if (tm.getNetworkType() == TelephonyManager.NETWORK_TYPE_1xRTT
						|| tm.getNetworkType() == TelephonyManager.NETWORK_TYPE_CDMA
						|| tm.getNetworkType() == TelephonyManager.NETWORK_TYPE_EDGE
						|| tm.getNetworkType() == TelephonyManager.NETWORK_TYPE_GPRS
						|| tm.getNetworkType() == NETWORK_TYPE_IDEN) {
					return NET_SG;
				} else if (tm.getNetworkType() == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
					return NET_UNKNOW;
				} else {
					return NET_UNKNOW;
				}
			}
		} else {
			return NET_UNKNOW;
		}
	}
	
	// 检查手机网络链接情况
	public static boolean checkInternet(String url) {
		HttpClient httpClient = null;
		HttpGet httpRequest = new HttpGet(url);
		httpClient = new DefaultHttpClient();
		try {
			HttpResponse httpResponse = httpClient.execute(httpRequest);
			int uRC = httpResponse.getStatusLine().getStatusCode();
			if (uRC == HttpStatus.SC_OK) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (httpClient != null) {
				httpClient.getConnectionManager().shutdown();
			}
			httpClient = null;
		}
		return false;
	}

		public static final int TYPE_CONNECT_NEVER = 0;
		public static final int TYPE_CONNECT_MOBILE = 1;
		public static final int TYPE_CONNECT_WIFI = 2;

		/**
		 * 0: 未连接 1: 有线连接 2: 无线连接
		 * 
		 * @param context
		 * @return
		 */
		public static int judgeConnectWifiOrMobile(Context context) {
			State wifiState = null;
			State mobileState = null;
			NetworkInfo wifiInfo = null;
			NetworkInfo mobileInfo = null;
			ConnectivityManager cm = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (cm == null) {
				return 0;
			}
			if (cm != null) {
				wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				mobileInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			}

			if (wifiInfo == null && mobileInfo == null) {
				return TYPE_CONNECT_NEVER;
			}
			if (wifiInfo != null) {
				wifiState = wifiInfo.getState();
			}
			if (mobileInfo != null) {
				mobileState = mobileInfo.getState();
			}
			if (wifiState == null && mobileState == null) {
				return TYPE_CONNECT_NEVER;
			}
			if (wifiState != null && mobileState != null) {
				if (State.CONNECTED != wifiState && State.CONNECTED == mobileState) {
					return TYPE_CONNECT_MOBILE;
				} else if (State.CONNECTED != wifiState
						&& State.CONNECTED != mobileState) {
					return TYPE_CONNECT_NEVER;
				} else if (State.CONNECTED != mobileState
						&& State.CONNECTED == wifiState) {
					return TYPE_CONNECT_WIFI;
				}
			} else if (wifiState != null && mobileState == null) {
				if (State.CONNECTED == wifiState) {
					return TYPE_CONNECT_WIFI;
				} else if (State.CONNECTED != mobileState) {
					return TYPE_CONNECT_NEVER;
				}

			} else if (wifiState == null && mobileState != null) {
				if (State.CONNECTED == mobileState) {
					return TYPE_CONNECT_MOBILE;
				} else if (State.CONNECTED != mobileState) {
					return TYPE_CONNECT_NEVER;
				}
			}
			return TYPE_CONNECT_NEVER;
		}

		// 获取用户的IPd
		public static int getIpAddress(Context context) {
			int ipAddress = 0;
			WifiManager wifiManager = (WifiManager) context
					.getSystemService(Context.WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			if (wifiInfo == null || wifiInfo.equals("")) {
				return ipAddress;
			} else {
				ipAddress = wifiInfo.getIpAddress();
			}
			return ipAddress;
		}

		// 获取用户设备IP
		@SuppressLint("DefaultLocale")
		public static String getUserIp(Context context) {
			int ipAddress = getIpAddress(context);
			return String.format("%d.%d.%d.%d", (ipAddress & 0xff),
					(ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff),
					(ipAddress >> 24 & 0xff));
		}

}
