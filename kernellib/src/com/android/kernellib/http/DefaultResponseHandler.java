package com.android.kernellib.http;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.BasicResponseHandler;

public class DefaultResponseHandler extends AbstractResponseHandler {

	private BasicResponseHandler defaultResponseHandler;
	
	public DefaultResponseHandler() {
		super();
		defaultResponseHandler = new BasicResponseHandler();
	}
	
	@Override
	public String handleResponseImpl(HttpResponse response) throws ClientProtocolException, IOException {
		return defaultResponseHandler.handleResponse(response);
	}
}