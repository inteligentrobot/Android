package com.android.kernellib.http;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.EntityUtils;

import android.text.TextUtils;

import com.android.kernellib.utility.DebugLog;
import com.android.kernellib.utility.StringUtils;

public class ByteArrayGZipResponseHandler extends AbstractResponseHandler {
    public static final String TAG = "ByteArrayGZipResponseHandler";

    public void setRequestUrl(String requestUrl) {}

    @Override
    public String handleResponseImpl(HttpResponse response) throws ClientProtocolException,
            IOException {
        if (null == response) {
            return null;
        }

        HttpEntity httpEntity = response.getEntity();

        if (null == httpEntity) {
            return null;
        }

        String encodeing = getHeader(response, "Content-Encoding");

        int lenth = StringUtils.toInt(getHeader(response, "Card-Length"), -1);

        DebugLog.log(TAG, "encodeing:" + encodeing);
        DebugLog.log(TAG, "lenth:" + lenth);

        try {
            // gzip压缩数据
            if (!TextUtils.isEmpty(encodeing) && encodeing.contains("gzip")) {
                return gunzip(httpEntity, lenth);
            } else {
                return new String(EntityUtils.toByteArray(httpEntity));
            }

        } catch (Throwable e) {
            DebugLog.log(TAG, "e:" + e);
        }
        return null;
    }

    /**
     * 分解头部信息
     * 
     * @param response
     * @param key
     * @return
     */
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

    /**
     * 解压数据liu
     * 
     * @param en
     * @param size
     * @return
     */
    private String gunzip(HttpEntity en, int size) {
        // 结果返回
        String ret = null;
        Reader reader = null;
        CharArrayBuffer out = null;

        if (size < 1024) {
            size = 1024;
            DebugLog.log(TAG, "size :" + size);
        }

        try {
            out = new CharArrayBuffer(size);
            reader = new InputStreamReader(new GZIPInputStream(en.getContent()), "UTF-8");

            char[] buffer = new char[1024];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                out.append(buffer, 0, count);
            }
            // 转换成字符
            ret = out.toString();
            // 释放buffer
            out = null;
        } catch (Exception e) {
            DebugLog.log(TAG, "e:" + e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                    reader = null;
                } catch (IOException e) {
                    DebugLog.log(TAG, "e:" + e);
                }
            }
        }

        return ret;
    }
}
