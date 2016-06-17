package com.android.kernellib.http;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;

public abstract class AbstractResponseHandler implements ResponseHandler<Object> {
	
	protected HttpResponse response;
	
	public HttpResponse getHttpResponse() {
		return response;
	}
	
	public Object handleResponse(HttpResponse response)
	throws ClientProtocolException, IOException {
		this.response = response;
		return handleResponseImpl(response);
	}
	
	public abstract Object handleResponseImpl(HttpResponse response)
		throws ClientProtocolException, IOException;
}