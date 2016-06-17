package com.android.kernellib.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.android.kernellib.utility.DebugLog;
import com.android.kernellib.utility.StringUtils;

/**
 * Wrap httpclient send request and revice respomse used params of (String requestUrl, String
 * method, HttpEntity entity, AbstractResponseHandler responseHandler)
 * 
 * requestUrl: request url. method: GET or POST entity:
 * 
 * HttpEntity entity = HttpEntityFactory.createHttpEntity(List or byte[] or String); (List for html
 * form, byte[] for binaray, String for simple string)
 * 
 * responseHandler:
 * 
 * AbstractResponseHandler responseHandler = new DefaultResponseHandler() or new
 * ByteArrayResponseHandler();
 * 
 * DefaultResponseHandler: return response data with string ByteArrayResponseHandler: return
 * response data with byte[]
 * 
 * Example:
 * 
 * HttpClientWrap wrap = null; try { new HttpClientWrap(Activity act); int errorCode =
 * wrap.wrapHttpGet(...); } finally { if(null != wrap) { wrap.release(); } }
 * 
 * @author zhuchengjin
 * 
 */
public class HttpClientWrap {

    protected static final String TAG = HttpClientWrap.class.getSimpleName();
    private static ThreadLocal<AtomicInteger> local = new ThreadLocal<AtomicInteger>();

    protected Activity activity;

    protected Object responseObject;

    protected HttpResponse httpResponse;

    protected HttpRequestAdapter adapter;

    protected int connectionTimeout = 10000;

    protected int socketTimeout = 10000;

    protected static final boolean debug = false;

    protected boolean isCustomSSLSocket = false;

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    public Object getResponseData() {
        return responseObject;
    }

    public void setConnectionTimeout(int mTimeOut) {
        connectionTimeout = mTimeOut;
    }

    public void setSocketTimeout(int aTimeOut) {
        socketTimeout = aTimeOut;
    }

    public HttpClientWrap(final Activity activity) {
        this.activity = activity;
    }

    protected Context _context;

    public HttpClientWrap(final Context _context) {
        this._context = _context;
    }

    public void setCustomSSLSocketFlag(boolean flag) {
        isCustomSSLSocket = flag;
    }

    public int wrapHttpGet(String requestUrl, AbstractResponseHandler responseHandler,
            Hashtable<String, String> header) {
        return wrapHttpRequest(requestUrl, "GET", null, responseHandler, header);
    }

    public int wrapHttpGet(String requestUrl, AbstractResponseHandler responseHandler) {
        return wrapHttpRequest(requestUrl, "GET", null, responseHandler);
    }

    public int wrapHttpPost(String requestUrl, HttpEntity entity,
            AbstractResponseHandler responseHandler) {
        return wrapHttpRequest(requestUrl, "POST", entity, responseHandler);
    }

    public int wrapHttpPost(String requestUrl, HttpEntity entity,
            AbstractResponseHandler responseHandler, Hashtable<String, String> header) {
        return wrapHttpRequest(requestUrl, "POST", entity, responseHandler, header);
    }

    /**
     * Error Code: 0, Success. -1, RequestUrl param error. -2, Request method param error. -3,
     * HttpEntity is null for post request. -4, Exception error. -11, Check network fail. -12, No
     * get correct apn value.
     * 
     * see org.inq.android.appstore.SimpleUtils
     * 
     * @return
     */
    public int wrapHttpRequest(String requestUrl, String method, HttpEntity entity,
            AbstractResponseHandler responseHandler) {
        return wrapHttpRequest(requestUrl, method, entity, responseHandler, null);
    }

    @SuppressLint("DefaultLocale")
	private int wrapHttpRequest(String requestUrl, String method, HttpEntity entity,
            AbstractResponseHandler responseHandler, Hashtable<String, String> headers) {
        Context ctx = null;
        if (null != activity) {
            ctx = activity.getApplicationContext();
        } else {
            ctx = this._context;
        }

        if (!NetCheckor.checkNetworkType(ctx)) {
            return -11;
        }

        ApnCheckor.ApnTag apnTag = ApnCheckor.getCurrentUsedAPNTag(ctx);
        // if (ApnCheckor.ApnTag.UNKNOW.equals(apnTag)) {
        // return -12;
        // }

        HttpRequestBean bean = new HttpRequestBean();
        if (isCustomSSLSocket) {
            bean.setCustomSSLSocketFlag(isCustomSSLSocket);
        }
        bean.setApnTag(apnTag);
        bean.setRequestUrl(requestUrl);
        bean.setMethod(method);
        if (null == headers || headers.size() < 1) {

        } else {

            Enumeration<?> e = headers.keys();
            String key, value;
            while (e.hasMoreElements()) {
                key = (String) e.nextElement();
                value = (String) headers.get(key);
                if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                    bean.addHeader(key, value);
                }
            }
        }
        //
        // bean.addHeader("Accept-Encoding", "gzip");

        if (connectionTimeout > 0) {
            bean.setConnectionTimeout(connectionTimeout);
        }
        if (socketTimeout > 0) {
            bean.setSocketTimeout(socketTimeout);
        }

        if ("POST".equals(method.toUpperCase())) {
            bean.setHttpEntity(entity);
        }

        bean.setResponseHandler(responseHandler);

        adapter = new HttpRequestAdapter(bean);

        int error = adapter.request();
        DebugLog.log(TAG,"errorCode:" + error);

        local.set(new AtomicInteger(3));
        while (error != 0 && local.get().decrementAndGet() >= 0) {
            DebugLog.log(TAG, "retry to get net data-----> " + local.get().get());
            error = adapter.request();
        }

        if (error == 0) {
            httpResponse = adapter.getHttpResponse();
            responseObject = adapter.getResponseData();
            //
            // //数据解压处理
            // String encodeing = getHeader(httpResponse, "Content-Encoding");
            // if(!StringUtils.isEmpty(encodeing) && encodeing.contains("gzip"))
            // {
            // responseObject = gunzip((byte [])responseObject);
            // }
        }
        local.remove();
        return error;
    }

    /**
     * gzip解压
     * 
     * @param bytes
     * @return
     * @throws IOException
     */
    public byte[] gunzip(byte[] bytes) {
        // 数据输入流
        InputStream in = null;
        // 二进制输出流
        ByteArrayOutputStream out = null;
        // 结果返回
        byte[] ret = null;
        try {
            out = new ByteArrayOutputStream();
            in = new GZIPInputStream(new ByteArrayInputStream(bytes));

            byte[] buffer = new byte[1024];
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
            ret = out.toByteArray();
        } catch (Exception e) {

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }

        return ret;
    }

    public String getHeader(HttpResponse response, String key) {
        if (null == response) {
            return "";
        }

        Header header = response.getFirstHeader(key);
        if (null == header) {
            return "";
        }

        return StringUtils.toStr(header.getValue(), "");
    }

    public void release() {
        if (null != adapter) {
            adapter.release();
            adapter = null;
        }
    }
}
