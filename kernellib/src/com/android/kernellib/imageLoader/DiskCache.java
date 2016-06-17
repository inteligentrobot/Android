package com.android.kernellib.imageLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Environment;
import android.os.Process;
import android.text.TextUtils;
import android.util.SparseArray;

import com.android.kernellib.imageLoader.ImageLoader.ImageType;
import com.android.kernellib.utility.DebugLog;
import com.android.kernellib.utility.UITools;

/**
 * @author zhuchengjin 磁盘缓存图片
 */
public class DiskCache
{
	/**
	 * 通过缓存内容进行文件夹分类。
	 * 1. AD广告
	 * 2. TODO 可以通过文件夹分类，实现文件存放散裂化，提高IO读取效率
	 */
	public static final int DISK_CACHE_TYPE_COMMON = 0;
	public static final int DISK_CACHE_TYPE_AD = DISK_CACHE_TYPE_COMMON + 1;

	private final static String TAG = "DiskCache";
	private final static long MAX_SIZE = 20 * 1024 * 1024;//20M
	private final static long MAX_SIZE_AD = 10 * 1024 * 1024;//10M

	// 手机存储
	private SparseArray<File> mCacheDirMap = new SparseArray<File>(3);
	// 外部存储
	private SparseArray<File> mExternalCacheDirMap = new SparseArray<File>(3);

	//存储文件夹名(默认)
	private final static String CACHE_DIR_DEFAULT = "images" + File.separator + "default";
	private final static String CACHE_DIR_AD = "images" + File.separator + "ad";

	//正在写文件扩展名
	private final static String WRITING_FILE_EXTNAME = ".w";
	//正常显示文件扩展名
	private final static String READING_FILE_EXTNAME = ".r";
	//磁盘中图片大小统计
	private volatile long mSize = 0;
	//保存删除线程
	private Thread mDeleteThread = null;

	private static SparseArray<String> sDirNameTypePairs = new SparseArray<String>(3);
	private static SparseArray<Long> sDirMaxSizePairs = new SparseArray<Long>(3);
	static {
	    // default
		sDirNameTypePairs.put(DISK_CACHE_TYPE_COMMON, CACHE_DIR_DEFAULT);
		sDirMaxSizePairs.put(DISK_CACHE_TYPE_COMMON, MAX_SIZE);
		// ad
		sDirNameTypePairs.put(DISK_CACHE_TYPE_AD, CACHE_DIR_AD);
		sDirMaxSizePairs.put(DISK_CACHE_TYPE_AD, MAX_SIZE_AD);
	}

    public DiskCache() {
    }

    /**
     * update disk cache max size this will set the max size for diskCacheType
     * 
     * @param maxSize (MB) equals (maxsize * 1024 * 1024)
     * @param diskCacheType
     */
    public void updateMaxSize(int maxSize, int diskCacheType) {
        sDirMaxSizePairs.put(diskCacheType, maxSize * 1024L * 1024L);
    }

	/**
	 * 检查大小
	 * 
	 * @return
	 */
	public void checkSize(final Context context, final int diskCacheType)
	{
	    if(null == context) {
	        return;
	    }
		if (getDirMaxSizeByType(diskCacheType) < mSize || mSize == 0)
		{
		    DebugLog.log(TAG, "checkSize   size exceed");
			//异步删除老文件
			if (mDeleteThread == null)
			{
			    DebugLog.log(TAG, "checkSize   size exceed thread start");

				mDeleteThread = new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

						try
						{
							File dir = getDir(context, diskCacheType);
							ArrayList<File> list = new ArrayList<File>();
							//遍历所有文件
							if (dir != null)
							{
								File[] files = dir.listFiles();
								if (files != null)
								{
									mSize = 0;
									for (File temp : files)
									{
										if (temp != null && temp.exists() && temp.isFile())
										{
											mSize += temp.length();
											list.add(temp);
										}
									}
								}
							}
							//只有大小超大时执行。
							if (mSize > getDirMaxSizeByType(diskCacheType))
							{
								Collections.sort(list, new Sorter());
								File fd = null;
								int count = list.size() / 3;
								for (int i = 0; i < count; i++)
								{
									fd = list.get(i);
									if (fd != null && fd.exists() && fd.isFile())
									{
										long mod = fd.lastModified();
										DebugLog.log(TAG, "checkSize run mod:" + mod);
										mSize -= fd.length();
										fd.delete();
									}
								}
							}
						}
						catch (Exception e)
						{

						}

						mDeleteThread = null;
					}
				});
				mDeleteThread.start();
			}
			else
			{
			    DebugLog.log(TAG, "checkSize   size exceed thread has excute");
			}
		}
	}

	/**
	 * 添加图片到磁盘
	 * 
	 * @param context
	 * @param url
	 */
	public void putBitmapToDisk(Context context, String url, Bitmap bp,
			ImageType type, int diskCacheType) {
		if (bp == null || url == null || context == null)
		{
		    DebugLog.log(TAG, "putBitmapToDisk   null");
			return;
		}

		checkSize(context, diskCacheType);

		long temp = System.currentTimeMillis();
		DebugLog.log(TAG, "putBitmapToDisk " + url);

		String hash = hashKeyForDisk(url);

		DebugLog.log(TAG, "putBitmapToDisk " + hash);

		File bitmapfile = getWFile(context, hash, diskCacheType);

		DebugLog.log(TAG, "putBitmapToDisk getfile:" + (System.currentTimeMillis() - temp));
		temp = System.currentTimeMillis();

		if (bitmapfile != null)
		{
			if (bitmapfile.exists())
			{
			    DebugLog.log(TAG, "putBitmapToDisk bitmapfile.exists()");
				bitmapfile.delete();
			}
			FileOutputStream fos = null;
			try
			{
				bitmapfile.createNewFile();
				fos = new FileOutputStream(bitmapfile);

				switch(type)
				{
					case JPG:
						if (bp.hasAlpha()){
							bp.compress(Bitmap.CompressFormat.PNG, 100, fos);
						}else {
							bp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
						}
						break;
					case PNG:
						bp.compress(Bitmap.CompressFormat.PNG, 100, fos);
						break;
					default:
						bp.compress(Bitmap.CompressFormat.PNG, 100, fos);
						break;
				}

				DebugLog.log(TAG, "putBitmapToDisk commpress:" + (System.currentTimeMillis() - temp));

				temp = System.currentTimeMillis();

				File renamefile = getRFile(context, hash, diskCacheType);
				if (renamefile != null)
				{
					if (renamefile.exists())
					{
						renamefile.delete();
					}
					bitmapfile.renameTo(getRFile(context, hash, diskCacheType));

					DebugLog.log(TAG, "putBitmapToDisk rename:" + (System.currentTimeMillis() - temp));
					mSize += getRFile(context, hash, diskCacheType).length();
					temp = System.currentTimeMillis();
				}
			}
			catch (Exception e)
			{
			    DebugLog.log(TAG, "putBitmapToDisk e:" + e);
				if (bitmapfile != null && bitmapfile.exists())
				{
					bitmapfile.delete();
				}

            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e1) {
                        DebugLog.log(TAG, "putBitmapToDisk e1:" + e1);
                    }
                }
            }
			
		}

		DebugLog.log(TAG, "putBitmapToDisk end:" + (System.currentTimeMillis() - temp));
		temp = System.currentTimeMillis();
	}


	/**
	 * 从磁盘读取图片
	 * @param context 上下文
	 * @param url 图片地址
	 * @param type 图片类型
	 * @param isFullQuality 是否全品质加载图片
	 * @param diskCacheType 磁盘缓存类型
	 * @param isLoadLocalExistImg 是否是本地存储的图片，这个时候 url 就是一个本地文件夹路径，而不是网络地址
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public Bitmap getBitmapFromDisk(Context context, String url,ImageType type, boolean isFullQuality, int diskCacheType,boolean isLoadLocalExistImg)
	{
	    if(null == context) {
            return null;
        }
		Bitmap ret = null;

		DebugLog.log(TAG, "getBitmapFromDisk " + url);

		File bitmapfile = null;
		if(isLoadLocalExistImg)
		{
			bitmapfile = new File(url);
		} else
		{
			String hash = hashKeyForDisk(url);
			bitmapfile = getRFile(context, hash, diskCacheType);
		}
		if (bitmapfile != null && bitmapfile.exists())
		{
			final BitmapFactory.Options options = new BitmapFactory.Options();

			try
			{
				if (!isFullQuality) {
			        options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(bitmapfile.getAbsolutePath(), options);
                    options.inSampleSize = UITools.computeSampleSize(options, 480, 480 * 800);
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
				}
				options.inJustDecodeBounds = false;
				options.inDither = false;
				options.inPurgeable = true;
				options.inInputShareable = true;
				ret = BitmapFactory.decodeFile(bitmapfile.getAbsolutePath(), options);
				switch(type)
				{
					case CIRCLE:
					{
						Bitmap temp = toRoundBitmap(ret);

						if (temp != null)
						{
							ret.recycle();
							ret = temp;
						}
					}
						break;

					default:
						break;
				}
				
			}
			catch (Exception e)
			{
			    DebugLog.log(TAG, "getBitmapFromDisk " + e);
			}
			catch (OutOfMemoryError oe)
			{
			    DebugLog.log(TAG, "getBitmapFromDisk " + oe);
				System.gc();
			}
		}

		return ret;
	}
	
	/**
	 * 从磁盘读取图片
	 * 
	 * @param context
	 * @param url
	 * @return
	 */
	public Bitmap getBitmapFromDisk(Context context, String url,ImageType type, boolean isFullQuality, int diskCacheType)
	{
		return getBitmapFromDisk( context,  url, type,  isFullQuality,  diskCacheType,false);
	}

	/**
	 * 从磁盘读取图片
	 * 
	 * @param context
	 * @param url
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public Bitmap getBitmapFromDiskForExistImage(Context context, String url,ImageType type, boolean isFullQuality)
	{
	    if(null == context) {
            return null;
        }
		Bitmap ret = null;

		DebugLog.log(TAG, "getBitmapFromDisk " + url);

		File bitmapfile =new File(url);
		if (bitmapfile != null && bitmapfile.exists())
		{
			final BitmapFactory.Options options = new BitmapFactory.Options();

			try
			{
				if (!isFullQuality) {
			        options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(bitmapfile.getAbsolutePath(), options);
                    options.inSampleSize = UITools.computeSampleSize(options, 480, 480 * 800);
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
				}
				options.inJustDecodeBounds = false;
				options.inDither = false;
				options.inPurgeable = true;
				options.inInputShareable = true;
				ret = BitmapFactory.decodeFile(bitmapfile.getAbsolutePath(), options);
				switch(type)
				{
					case CIRCLE:
					{
						Bitmap temp = toRoundBitmap(ret);

						if (temp != null)
						{
							ret.recycle();
							ret = temp;
						}
					}
						break;

					default:
						break;
				}
				
			}
			catch (Exception e)
			{
			    DebugLog.log(TAG, "getBitmapFromDisk " + e);
			}
			catch (OutOfMemoryError oe)
			{
			    DebugLog.log(TAG, "getBitmapFromDisk " + oe);
				System.gc();
			}
		}

		return ret;
	}
	

	
	/**
	 * 转换图片成圆形
	 * 
	 * @param bitmap
	 *            传入Bitmap对象
	 * @return
	 */
	private Bitmap toRoundBitmap(Bitmap bitmap)
	{
		Bitmap ret = null;
		float roundPx;
		float left, top, right, bottom, dst_left, dst_top, dst_right, dst_bottom;

		int width = bitmap.getWidth();
		int height = bitmap.getHeight();

		if (width <= height)
		{
			roundPx = width / 2;
			top = 0;
			bottom = width;
			left = 0;
			right = width;
			height = width;
			dst_left = 0;
			dst_top = 0;
			dst_right = width;
			dst_bottom = width;
		}
		else
		{
			roundPx = height / 2;
			float clip = (width - height) / 2;
			left = clip;
			right = width - clip;
			top = 0;
			bottom = height;
			width = height;
			dst_left = 0;
			dst_top = 0;
			dst_right = height;
			dst_bottom = height;
		}
		Bitmap output = null;
		try
		{
			output = Bitmap.createBitmap(width, height, Config.ARGB_8888);
			Canvas canvas = new Canvas(output);

			final int color = 0xff424242;
			final Paint paint = new Paint();
			final Rect src = new Rect((int) left, (int) top, (int) right, (int) bottom);
			final Rect dst = new Rect((int) dst_left, (int) dst_top, (int) dst_right, (int) dst_bottom);
			final RectF rectF = new RectF(dst);

			paint.setAntiAlias(true);

			canvas.drawARGB(0, 0, 0, 0);
			paint.setColor(color);
			canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

			paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
			canvas.drawBitmap(bitmap, src, dst, paint);
		}
		catch (OutOfMemoryError oe)
		{
		    DebugLog.log(TAG, "toRoundBitmap " + oe);

			ret = null;
			
			if (output != null && !output.isRecycled())
			{
				output.recycle();
			}
			System.gc();
		}

		return ret;
	}

	/**
	 * 判断磁盘是否有图片
	 * 
	 * @param context
	 * @param url
	 * @return
	 */
	public boolean hasBitmap(Context context, String url, int diskCacheType)
	{
		String hash = hashKeyForDisk(url);

		DebugLog.log(TAG, "getBitmapFromDisk " + hash);

		File bitmapfile = getRFile(context, hash, diskCacheType);
		if (bitmapfile != null && bitmapfile.exists())
		{
			return true;
		}
		return false;
	}

	/**
	 * 判断是否正在写
	 * 
	 * @param context
	 * @param url
	 * @return
	 */
	public boolean isWritingDisk(Context context, String url, int diskCacheType)
	{
		String hash = hashKeyForDisk(url);

		DebugLog.log(TAG, "isWritingDisk " + hash);

		File bitmapfile = getWFile(context, hash, diskCacheType);
		if (bitmapfile != null && bitmapfile.exists())
		{
		    DebugLog.log(TAG, "isWritingDisk true");
			return true;
		}
		DebugLog.log(TAG, "isWritingDisk false");
		return false;
	}

	/**
	 * 获得图片保存目录
	 * 
	 * @param context
	 * @return
	 */
	private File getDir(Context context, int diskCacheType)
	{
		if (context == null)
		{
		    DebugLog.log(TAG, "getDir context == null");
			return null;
		}
		File cacheDir;
		//返回外存目录
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
		{
		    cacheDir = mExternalCacheDirMap.get(diskCacheType);
			if (cacheDir == null)
			{
				File dir = context.getExternalCacheDir();
				if(dir == null)
				{
					dir = context.getCacheDir();
				}
				cacheDir = new File(dir, getDirNameByType(diskCacheType));
				mExternalCacheDirMap.put(diskCacheType, cacheDir);
			}
		}
		//返回手机目录
		else
		{
		    cacheDir = mCacheDirMap.get(diskCacheType);
			if (cacheDir == null)
			{
			    cacheDir = new File(context.getCacheDir(), getDirNameByType(diskCacheType));
			    mCacheDirMap.put(diskCacheType, cacheDir);
			}
		}
        if (!cacheDir.exists())
        {
            cacheDir.mkdirs();
        }
        return cacheDir;
	}

	/**
	 * 通过diskcacheType 文件夹分类获取文件夹名字
	 * 
	 * @param diskCacheType
	 * @return
	 */
	private String getDirNameByType(int diskCacheType) {
	    String result = sDirNameTypePairs.get(diskCacheType);
	    if (TextUtils.isEmpty(result)) {
	        result = CACHE_DIR_DEFAULT;
	    }
	    return result;
	}

    /**
     * 通过diskcacheType 文件夹分类获取文件夹最大size
     * 
     * @param diskCacheType
     * @return
     */
    private long getDirMaxSizeByType(int diskCacheType) {
        Long result = sDirMaxSizePairs.get(diskCacheType);
        if (result == null || result <= 1000) {
            result = MAX_SIZE;
        }
        return result;
    }

	/**
	 * 获得可读文件
	 * 
	 * @param context
	 * @param url
	 * @return
	 */
	private File getRFile(Context context, String hash, int diskCacheType)
	{
		File file = getFile(context, hash + READING_FILE_EXTNAME, diskCacheType);

		return file;
	}

	/**
	 * 获得写文件
	 * 
	 * @param context
	 * @param url
	 * @return
	 */
	private File getWFile(Context context, String hash, int diskCacheType)
	{
		File file = getFile(context, hash + WRITING_FILE_EXTNAME, diskCacheType);

		return file;
	}

	/**
	 * 获得文件
	 * 
	 * @param context
	 * @param name
	 * @return
	 */
	private File getFile(Context context, String name, int diskCacheType)
	{
		File file = null;
		try
		{
			file = new File(getDir(context, diskCacheType), name);
		}
		catch (Exception e)
		{
		    DebugLog.log(TAG, "getFile e:" + e);
		}
		return file;
	}

	/**
	 * 将下载地址转换加密
	 * 
	 * @param key
	 * @return
	 */
	private static String hashKeyForDisk(String key)
	{
		String cacheKey;
		try
		{
			final MessageDigest mDigest = MessageDigest.getInstance("MD5");
			mDigest.update(key.getBytes());
			cacheKey = bytesToHexString(mDigest.digest());
		}
		catch (NoSuchAlgorithmException e)
		{
			cacheKey = String.valueOf(key.hashCode());
		}
		return cacheKey;
	}

	/**
	 * 字节与字符转换
	 * 
	 * @param bytes
	 * @return
	 */
	private static String bytesToHexString(byte[] bytes)
	{
		// http://stackoverflow.com/questions/332079
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++)
		{
			String hex = Integer.toHexString(0xFF & bytes[i]);
			if (hex.length() == 1)
			{
				sb.append('0');
			}
			sb.append(hex);
		}
		return sb.toString();
	}

	/**
	 * @author zhuchengjin 按照修改时间排序
	 */
	static class Sorter implements Comparator<File>
	{
		public int compare(File r1, File r2)
		{
			long time1 = r1.lastModified();
			long time2 = r2.lastModified();
			return (int) (time1 - time2);
		}
	}
}
