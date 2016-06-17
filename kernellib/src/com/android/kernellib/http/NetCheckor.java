package com.android.kernellib.http;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetCheckor
{
	protected static final String TAG = NetCheckor.class.getSimpleName();

	protected static final boolean debug = false;

	protected static void log(Object msg)
	{
		if (debug)
		{
			Log.d(TAG, String.valueOf(msg));
		}
	}

	public static int[] getNetworkType(Context ctx)
	{
		ConnectivityManager mConnectivity = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = mConnectivity.getActiveNetworkInfo();
		if (info == null)
		{
			log("NetworkInfo is null.");
			return null;
		}

		return new int[] { info.getType(), info.getSubtype() };
	}

	public static boolean checkNetworkType(Context ctx)
	{
		ConnectivityManager mConnectivity = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = mConnectivity.getActiveNetworkInfo();
		if (info == null)
		{
			log("NetworkInfo is null.");
			return false;
		}
		return true;
	}
}