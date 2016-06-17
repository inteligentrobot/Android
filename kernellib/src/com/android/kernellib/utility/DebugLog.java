package com.android.kernellib.utility;

import android.util.Log;


/**
 * @author zhuchengjin
 *
 * Debug configuration for app model you can enable debug by following method<br>
 * 1. Change code sIsDebug = false to true<br>
 * 2. Invoke setIsDebug(boolean enableDebug)<br>
 * 3. Change TAG's properties during the runtime by
 * "adb shell setprop log.tag.DebugLog VERBOSE" on the terminal
 */
public class DebugLog {

	public static final String TAG = "DebugLog";
	
	private  static boolean isDebug = false;
	
	public static void setIsDebug(boolean isDebug){
		DebugLog.isDebug = isDebug;
	}
	
	 /**
     * Check the debug configuration and check TAG with android.util.Log.isLoggable
     * @return
     */
	public static boolean isDebug(){
		return isDebug || android.util.Log.isLoggable(TAG, Log.VERBOSE);
	}
	
	
	/**
	 * @param TAG 
	 * @param msg log Message
	 * print log
	 */
	public static void log(String TAG ,Object msg){
		if(StringUtils.isEmpty(TAG) || null == msg) return;
		if(isDebug()){
			if(!StringUtils.isEmpty(TAG)){
				Log.d(TAG, String.valueOf(msg));
			}
		}
	}
	
}
