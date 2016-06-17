package com.android.kernellib.http;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.text.TextUtils;

import com.android.kernellib.utility.DebugLog;


public class HttpRequestAdapter {

    protected static final String TAG = HttpRequestAdapter.class.getSimpleName();

    protected static final int DEFAULT_SOCKET_TIMEOUT = 10000;

    protected static final int DEFAULT_SOCKET_BUF = 8192;

    protected static final int DEFAULT_CONNECTION_TIMEOUT = 10000;

    public static final String DEFAULT_PROXY = "10.0.0.172";

    public static final String CT_PROXY = "10.0.0.200";

    public static final int DEFAULT_PROXY_PORT = 80;

    protected HttpRequestBean bean;

    protected Object responseObject;

    protected HttpResponse httpResponse;

    protected HttpClient httpClient;

    public HttpRequestAdapter(HttpRequestBean bean) {
        this.bean = bean;
    }


    protected void setRequestHeaders(HttpRequestBase httpBase) {
        Hashtable<String, String> table = bean.getHeaders();
        if (null == table || table.size() < 1) {
            return;
        }

        Enumeration<?> e = table.keys();
        String key, value;
        while (e.hasMoreElements()) {
            key = (String) e.nextElement();
            value = (String) table.get(key);
            if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                httpBase.setHeader(key, value);
            }
        }
    }

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    public Object getResponseData() {
        return responseObject;
    }

    protected HttpClient getHttpClient(HttpParams httpParams) {
        HttpHost proxy = null;
        ApnCheckor.ApnTag apnTag = bean.getApnTag();
        if (ApnCheckor.ApnTag.CTWAP.equals(apnTag)) {
            proxy = new HttpHost(CT_PROXY, DEFAULT_PROXY_PORT);
        } else if (ApnCheckor.ApnTag.CMWAP.equals(apnTag)
                || ApnCheckor.ApnTag.UNIWAP.equals(apnTag)
                || ApnCheckor.ApnTag._3GWAP.equals(apnTag)) {
            proxy = new HttpHost(DEFAULT_PROXY, DEFAULT_PROXY_PORT);
        }

        if (null != proxy) {
            httpParams.setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }
        if (bean.isCustomSSLSocketFlag()) {
            DebugLog.log(TAG,"custom ssl socket custom ssl socket custom ssl socket custom ssl socket custom ssl socket");

            ThreadSafeClientConnManager cm =
                    new ThreadSafeClientConnManager(httpParams, getDefaultSchemeRegistry());

            return new DefaultHttpClient(cm, httpParams);
        } else {
            return new DefaultHttpClient(httpParams);
        }
    }

    private static SchemeRegistry getDefaultSchemeRegistry() {
        // Fix to SSL flaw in API < ICS 4.0
        // See https://code.google.com/p/android/issues/detail?id=13117

        SSLSocketFactory sslSocketFactory = MySSLSocketFactory.getFixedSocketFactory();

        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", sslSocketFactory, 443));

        return schemeRegistry;
    }

    /**
     * Error Code: 0, Success. -1, RequestUrl param error. -2, Request method param error. -3,
     * HttpEntity is null for post request. -4, Exception error.
     * 
     * @return
     */
    public int request() {
        String url = bean.getRequestUrl();
        if (TextUtils.isEmpty(url)) {
            return -1;
        }

        url = url.trim();
        HttpParams httpParams = new BasicHttpParams();

        int socketTimeout = bean.getSocketTimeout();
        if (socketTimeout < 1) {
            socketTimeout = DEFAULT_SOCKET_TIMEOUT;
        }
        int connectionTimeout = bean.getConnectionTimeout();
        if (connectionTimeout < 1) {
            connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
        }
        int socketBufferSize = bean.getSocketBufferSize();
        if (socketBufferSize < 1) {
            socketBufferSize = DEFAULT_SOCKET_BUF;
        }
        HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeout);
        HttpConnectionParams.setSoTimeout(httpParams, socketTimeout);
        HttpConnectionParams.setSocketBufferSize(httpParams, socketBufferSize);

        HttpClientParams.setRedirecting(httpParams, true);

        HttpRequestBase httpBase = null;
        String httpMethod = bean.getMethod().trim();
        if ("GET".equals(httpMethod)) {
            httpBase = new HttpGet(url);
        } else if ("POST".equals(httpMethod)) {
            httpBase = new HttpPost(url);
        }

        if (null == httpBase) {
            return -2;
        }

        setRequestHeaders(httpBase);
        if ("POST".equals(httpMethod)) {
            HttpEntity httpEntity = bean.getHttpEntity();
            if (null == httpEntity) {
                return -3;
            }

            ((HttpPost) httpBase).setEntity(httpEntity);
        }

        DebugLog.log(TAG,"request:" + bean);

        AbstractResponseHandler responseHandler = null;
        httpClient = getHttpClient(httpParams);
        try {
            responseHandler = (AbstractResponseHandler) bean.getResponseHandler();
            if (null == responseHandler) {
                httpResponse = httpClient.execute(httpBase);
                HttpEntity httpEntity = httpResponse.getEntity();
                if (null == httpEntity) {
                    return -4;
                }
                responseObject = (InputStream) httpEntity.getContent();
            } else {
                responseObject = httpClient.execute(httpBase, responseHandler);
                httpResponse = responseHandler.getHttpResponse();
            }

            int code = httpResponse.getStatusLine().getStatusCode();
            DebugLog.log(TAG,"request:" + (null != httpResponse ? code : "httpResponse null"));
            if (null != httpResponse && code != HttpStatus.SC_OK
                    && code != HttpStatus.SC_PARTIAL_CONTENT) {
                httpBase.abort();
            }
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -5;
        }
    }

    public void release() {
        if (null != httpClient) {
            httpClient.getConnectionManager().shutdown();
        }

        httpClient = null;
    }
}
