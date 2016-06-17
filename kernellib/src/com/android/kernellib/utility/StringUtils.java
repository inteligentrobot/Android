package com.android.kernellib.utility;




/**
 * @author zhuchengjin
 */
public class StringUtils {

	
	/**
	 * @return
	 * check whether the String is empty
	 */
	public static boolean isEmpty(String str){
		if(null == str || "".equals(str)){
			return true;
		}else{
			if(str.length() > 4){
				return false;
			}else{
				return str.equalsIgnoreCase("null");
			}
		}
	}
	
	 /**
	  * change obj to string
	 * @param _obj
	 * @param _defaultValue
	 * @return
	 */
	public static String toStr(Object _obj, String _defaultValue) {
        if (isEmpty(String.valueOf(_obj))) {
            return _defaultValue;
        }
        return String.valueOf(_obj);
    }
	
	 public static final String maskUrl(String strUrl) {
        if (isEmpty(strUrl)) {
            return "";
        }
        String url = strUrl.trim().replaceAll("&amp;", "&");
        url = url.replaceAll(" ", "%20").trim();
        if (isEmpty(url))
            return "";
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
            return url;
        }
        return url;
	 }
	 
	 public static String maskNull(String str) {
	        return isEmpty(str) ? "" : str;
	 }
	 
	 public static int toInt(Object _obj, int _defaultValue) {
        if (isEmpty(String.valueOf(_obj))) {
            return _defaultValue;
        }

        try {
            return Integer.parseInt(String.valueOf(_obj));
        } catch (Exception e) {
        }

        return _defaultValue;
    }
}
