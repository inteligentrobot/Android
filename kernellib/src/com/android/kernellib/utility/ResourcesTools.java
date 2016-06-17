package com.android.kernellib.utility;

import android.content.Context;
import android.content.res.Resources;

/**
 * @author zhuchengjin 
 * 		        资源加载的工具类 可以通过资源名称，资源id来进行查找资源 希望对android打包过程进行一定的了解
 *         对资源的管理有一定的了解 AssertManager 学习 打包过程分析
 *         http://blog.csdn.net/luoshengyang/article/details/8744683
 *         android资源的查找过程分析
 *         http://blog.csdn.net/luoshengyang/article/details/8806798
 * 
 *         我们不生产代码，我们只是代码的搬运工（Android 原理学习）
 *         但是我们要怀着对知识的渴望，探索知识，发现知识。
 */
public class ResourcesTools {

	private static Resources mResources;

	/** 资源标识 */
	static final String STRING = "String";
	static final String DRAWABLE = "drawable";
	static final String STYLE = "style";
	static final String ID = "id";
	static final String COLOR = "color";
	static final String RAW = "raw";
	static final String ANIM = "anim";
	static final String ATTR = "attr";
	static final String LAYOUT = "layout";

	/** 初始化锁 */
	static Object mInitLock = new Object();
	/** 应用程序的包名 资源加载时需要使用 */
	static String mPackageName;// 应用程序的包名

	/**
	 * @param mContext
	 *            application 进行初始化操作。全局公用resource 对象
	 */
	public static void init(Context mContext) {
		if (mContext == null)
			return;
		synchronized (mInitLock) {
			if (mResources == null && StringUtils.isEmpty(mPackageName)) {
				mPackageName = mContext.getPackageName();
				mResources = mContext.getResources();
			}

		}
	}

	/**
	 * 通过名称查找id的话，没有使用id查找来的更高效
	 * 
	 * @param sourceName
	 * @param sourceType
	 * @return
	 */
	private static int getResourcesId(String sourceName, String sourceType) {
		if (mResources == null && StringUtils.isEmpty(mPackageName)) {
			return -1;
		}
		return mResources.getIdentifier(sourceName, sourceType, mPackageName);
	}

	public static int getResourceIdForString(String sourceName) {
		if (StringUtils.isEmpty(sourceName)) {
			sourceName = "emptey_string_res";//设置默认为空时，显示空字符串
		}
		return getResourcesId(sourceName, STRING);
	}

	public static int getResourceIdForID(String sourceName) {
		return getResourcesId(sourceName, ID);
	}

	public static int getResourceIdForLayout(String sourceName) {
		return getResourcesId(sourceName, LAYOUT);
	}

	public static int getResourceIdForDrawable(String sourceName) {
		if (StringUtils.isEmpty(sourceName)) {
			sourceName = "default_empty_drawable_transparent";// 可以设置一个默认图片。
		}
		return getResourcesId(sourceName, DRAWABLE);
	}

	public static int getResourceIdForStyle(String sourceName) {
		return getResourcesId(sourceName, STYLE);
	}

	public static int getResourceIdForColor(String sourceName) {
		return getResourcesId(sourceName, COLOR);
	}

	public static int getResourceIdForRaw(String sourceName) {
		return getResourcesId(sourceName, RAW);
	}

	public static int getResourceForAnim(String sourceName) {
		return getResourcesId(sourceName, ANIM);
	}

	public static int getResourceForAttr(String sourceName) {
		return getResourcesId(sourceName, ATTR);
	}

}
