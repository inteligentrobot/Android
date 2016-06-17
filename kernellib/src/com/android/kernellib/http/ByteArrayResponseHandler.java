package com.android.kernellib.http;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.util.EntityUtils;

public class ByteArrayResponseHandler extends AbstractResponseHandler
{
	public static final String TAG = "ByteArrayResponseHandler";

//	private String requestUrl;
//
//	public void setRequestUrl(String requestUrl)
//	{
//		this.requestUrl = requestUrl;
//	}

	@Override
	public byte[] handleResponseImpl(HttpResponse response) throws ClientProtocolException, IOException
	{

		if (null == response)
		{
			return null;
		}

		HttpEntity httpEntity = response.getEntity();
		if (null == httpEntity)
		{
			return null;
		}
		try
		{
			return EntityUtils.toByteArray(httpEntity);
		}
		// OutOfMemoryError，崩溃保护，打印请求url
		catch (Throwable e)
		{
			//Log.e(TAG, "error : " + e + "  request url is : " + requestUrl + " response length is "+httpEntity.getContentLength());
			return null;
		}

	}
}