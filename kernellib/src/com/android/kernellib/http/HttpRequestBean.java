package com.android.kernellib.http;

import java.util.Hashtable;

import org.apache.http.HttpEntity;

import com.android.kernellib.utility.StringUtils;

public class HttpRequestBean {

	private String requestUrl;

	private ApnCheckor.ApnTag apnTag;

	private String method;

	private Hashtable<String, String> headerTable;

	private HttpEntity httpEntity;

	private AbstractResponseHandler responseHandler;
	
	private int socketTimeout;
	
	private int connectionTimeout;
	
	private int socketBufferSize;
	
    private boolean isCustomSSLSocket = false;
	
	public void setCustomSSLSocketFlag(boolean flag)
	{
		isCustomSSLSocket = flag;
	}
	
	public boolean isCustomSSLSocketFlag()
	{
		return isCustomSSLSocket;
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}
	
	public int getConnectionTimeout() {
		return connectionTimeout;
	}
	
	public int getSocketBufferSize() {
		return socketBufferSize;
	}
	
	public void setSocketTimeout(int s) {
		this.socketTimeout = s;
	}
	
	public void setConnectionTimeout(int s) {
		this.connectionTimeout = s;
	}
	
	public void setSocketBufferSize(int s) {
		this.socketBufferSize = s;
	}
	
	public void setResponseHandler(AbstractResponseHandler responseHandler) {
		this.responseHandler = responseHandler;
	}

	public AbstractResponseHandler getResponseHandler() {
		return responseHandler;
	}

	public void setHttpEntity(HttpEntity httpEntity) {
		this.httpEntity = httpEntity;
	}

	public HttpEntity getHttpEntity() {
		return httpEntity;
	}

	public void setApnTag(ApnCheckor.ApnTag apnTag) {
		this.apnTag = apnTag;
	}

	public ApnCheckor.ApnTag getApnTag() {
		return apnTag;
	}

	public void addHeader(String key, String value) {
		if (headerTable == null) {
			headerTable = new Hashtable<String, String>();
		}
		
		this.headerTable.remove(key);
		this.headerTable.put(key, value);
	}

	public void setHeaders(Hashtable<String, String> table) {
		this.headerTable = table;
	}

	public Hashtable<String, String> getHeaders() {
		return headerTable;
	}

	public void setRequestUrl(String requestUrl) {
		this.requestUrl = StringUtils.maskUrl(requestUrl);
	}

	public String getRequestUrl() {
		return requestUrl;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getMethod() {
		return method;
	}

	public String toString() {
		return new StringBuffer().append("{requestUrl:")
				.append(StringUtils.maskNull(requestUrl)).append(",")
				.append("apnEntity:").append(apnTag).append(",")
				.append("method:").append(StringUtils.maskNull(method)).append(",")
				.append("Hashtable:")
				.append((headerTable == null ? "" : headerTable.toString()))
				.append("}").toString();
	}
}