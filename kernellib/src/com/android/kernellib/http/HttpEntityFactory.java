package com.android.kernellib.http;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;

import android.text.TextUtils;

public class HttpEntityFactory {

	public static HttpEntity createHttpEntity(String str) {
		if(TextUtils.isEmpty(str)) {
			return null;
		}
		
		try {
			return new StringEntity(str);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static HttpEntity createHttpEntity(byte[] buffer) {
		if(null == buffer || buffer.length < 1) {
			return null;
		}
		
		return new ByteArrayEntity(buffer);
	}
	
	public static HttpEntity createHttpEntity(List<? extends NameValuePair> list) {
		if(null == list || list.size() < 1) {
			return null;
		}
		
		try {
			return new UrlEncodedFormEntity(list, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
}