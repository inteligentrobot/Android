package com.android.kernellib.http;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.android.kernellib.utility.NetworkHelper;
import com.android.kernellib.utility.StringUtils;

/**
 * Apn check
 * 
 * @author zhuchengjin
 * 
 */
public class ApnCheckor
{

	protected static final String TAG = ApnCheckor.class.getSimpleName();

	protected static final boolean debug = false;

	protected static void log(Object msg)
	{
		if (debug)
		{
			Log.d(TAG, String.valueOf(msg));
		}
	}

	/**
	 * APN (preferred) table name
	 */
	private static Uri PREFERRED_APN_URI = Uri.parse("content://telephony/carriers/preferapn");

	/**
	 * APN (all) table name
	 */
	// private static Uri ALL_APN_URI = Uri.parse("content://telephony/carriers");

	/**
	 * For China APN collection.
	 * 
	 * @author mayue
	 * 
	 */
	public enum ApnTag
	{
		CMWAP, CMNET, UNIWAP, UNINET, CTWAP, CTNET, _3GWAP, _3GNET, INTERNET, UNKNOW
	}

	/**
	 * APM tables count records, and each APN 17 properties with monitor(androi-8):
	 * 
	 * _id:1, name: Android, numeric: 310995, mcc: 310, mnc: 995, apn: internet, user: *, server: *,
	 * password: *, proxy: null, port: null, mmsproxy: null, mmsport: null, mmsc: null, authtype:
	 * -1, type: null, current: null
	 * 
	 * _id: 2, name: TelKila, numeric: 310260, mcc: 310, mnc: 260, apn: internet, user: *, server:
	 * *, password: *, proxy: null, port: null, mmsproxy: null, mmsport: null, mmsc: null, authtype:
	 * -1, type: null, current: 1
	 * 
	 * _id: 3, name: CMWAP, numeric: 310260, mcc: 310, mnc: 260, apn: CMWAP, user: , server: ,
	 * password: , proxy: 10.0.0.172, port: 80, mmsproxy: , mmsport: , mmsc: , authtype: -1, type: ,
	 * current:1
	 * 
	 * @param ctx
	 * @return
	 */

	@SuppressLint("DefaultLocale")
	private static ApnTag[] getAllAPNTag(Context ctx, Uri uri)
	{
		int[] r = NetCheckor.getNetworkType(ctx);
		if (null != r)
		{
			if (r[0] == ConnectivityManager.TYPE_WIFI)
			{
				return new ApnTag[] { ApnTag.INTERNET };
			}
		}

		ContentResolver cr = ctx.getContentResolver();
		Cursor cursor = cr.query(uri, new String[] { "name", "apn", "proxy", "port" }, null, null, null);
		int count = cursor.getCount();

		if (0 >= count)
		{
			return null;
		}

		ApnTag[] array = new ApnTag[count];
		String apn = null, name = null, proxy = null, port = null;
		int i = 0;
		while (cursor.moveToNext())
		{
			name = StringUtils.maskNull(cursor.getString(0));
			apn = StringUtils.maskNull(cursor.getString(1));
			proxy = StringUtils.maskNull(cursor.getString(2));
			port = StringUtils.maskNull(cursor.getString(3));

			log("cursor name:" + name + ",apn:" + apn + ",proxy:" + proxy + ",port:" + port);
			if (apn.toUpperCase().contains("CMWAP") || name.toUpperCase().contains("CMWAP"))
			{
				array[i] = (!TextUtils.isEmpty(proxy) && !TextUtils.isEmpty(port)) ? ApnTag.CMWAP : ApnTag.CMNET;
			}
			else if (apn.toUpperCase().contains("CMNET") || name.toUpperCase().contains("CMNET"))
			{
				array[i] = ApnTag.CMNET;
			}
			else if (apn.toUpperCase().contains("UNIWAP") || name.toUpperCase().contains("UNIWAP"))
			{
				array[i] = (!TextUtils.isEmpty(proxy) && !TextUtils.isEmpty(port)) ? ApnTag.CMWAP : ApnTag.CMNET;
			}
			else if (apn.toUpperCase().contains("UNINET") || name.toUpperCase().contains("UNINET"))
			{
				array[i] = ApnTag.UNINET;
			}
			else if (apn.toUpperCase().contains("CTWAP") || name.toUpperCase().contains("CTWAP"))
			{
				array[i] = (!TextUtils.isEmpty(proxy) && !TextUtils.isEmpty(port)) ? ApnTag.CTWAP : ApnTag.CTNET;
			}
			else if (apn.toUpperCase().contains("CTNET") || name.toUpperCase().contains("CTNET"))
			{
				array[i] = ApnTag.CTNET;
			}
			else if (apn.toUpperCase().contains("3GWAP") || name.toUpperCase().contains("3GWAP"))
			{
				array[i] = (!TextUtils.isEmpty(proxy) && !TextUtils.isEmpty(port)) ? ApnTag._3GWAP : ApnTag._3GNET;
			}
			else if (apn.toUpperCase().contains("3GNET") || name.toUpperCase().contains("3GNET"))
			{
				array[i] = ApnTag._3GNET;
			}
			else if ((apn.toUpperCase().contains("INTERNET") || name.toUpperCase().contains("INTERNET")))
			{
				array[i] = ApnTag.INTERNET;
			}
			else if (name.toUpperCase().contains("T-MOBILE US") || apn.toUpperCase().contains("epc.tmobile.com"))
			{
				array[i] = ApnTag.INTERNET;
			}
			else
			{
				array[i] = ApnTag.UNKNOW;
			}

			log("array[" + i + "]:" + array[i]);
			i++;
		}

		if (null != cursor)
		{
			cursor.close();
			cursor = null;
		}

		return array;
	}

	/**
	 * Get all APN Entity
	 * 
	 * @param ctx
	 * @return
	 */
	/*
	 * public static ApnTag[] getAllAPNTag(Context ctx) { Log.d(TAG,
	 * "getAllAPNType start..."); return getAllAPNTag(ctx, ALL_APN_URI); }
	 */

	/**
	 * Get preferred APN Entity
	 * 
	 * @param ctx
	 * @return
	 */
	public static ApnTag getCurrentUsedAPNTag(Context ctx)
	{
//		log("getCurrentUsedAPNType start");
		try {
			ApnTag[] array = getAllAPNTag(ctx, PREFERRED_APN_URI);
			if (null == array || array.length < 1)
			{
				return null;
			}

			return array[0];
		} catch (Exception e) {
			
			NetworkInfo networkinfo=NetworkHelper.getActiveNetworkInfo(ctx);
			if(networkinfo != null && networkinfo.isAvailable()){
				String netExtraInfo = networkinfo.getExtraInfo();
                if (netExtraInfo == null) {// jasontujun 加强null判断
                    return ApnTag.UNKNOW;
                }
				if(netExtraInfo.equalsIgnoreCase("3gwap")){
					return ApnTag._3GWAP;
				}else if(netExtraInfo.equalsIgnoreCase("3gnet")){
					return ApnTag._3GNET;
				}else if(netExtraInfo.equalsIgnoreCase("cmnet")){		//联通3G cmnet
					return ApnTag.CMNET;
				}else if(netExtraInfo.equalsIgnoreCase("cmwap")){
					return ApnTag.CMWAP;
				}else if(netExtraInfo.equalsIgnoreCase("uninet")){
					return ApnTag.UNINET;
				}else if(netExtraInfo.equalsIgnoreCase("uniwap")){
					return ApnTag.UNIWAP;
				}else if(netExtraInfo.equalsIgnoreCase("ctnet")){
					return ApnTag.CTNET;
				}else if(netExtraInfo.equalsIgnoreCase("ctwap")){
					return ApnTag.CTWAP;
				}else if(networkinfo.getTypeName().equalsIgnoreCase("WIFI")){
					return ApnTag.INTERNET;
				}
				else{
					return ApnTag.UNKNOW;
				}
			}
			return ApnTag.UNKNOW;
		}
	}
}
