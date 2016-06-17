package com.android.kernellib.imageLoader;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

/**
 * Not suggest to use this class anymore, instead use
 * {@link org.qiyi.basecore.imageloader.ImageLoader}
 * 
 */
public class BitMapManager {
	//屏幕宽度
	public int mWindowsWidth = 0;
	//屏幕高度
	public int mWindowsHight = 0;
	
	public BitMapManager(Context context)
	{
		mWindowsWidth = context.getResources().getDisplayMetrics().widthPixels;
		mWindowsHight = context.getResources().getDisplayMetrics().heightPixels;
	}

	/**
	 * 加载普通图片
	 * 
	 * @param iv
	 * @param position
	 */
	public void loadImageForCat(ImageView iv, int id, ImgCacheMap<String, Bitmap> mmImageCacheMap)
	{	
		iv.setImageBitmap(null);
		ImageLoader.loadImageWithPNG(iv);
	}
	
	/**
	 * 加载有透明图图片
	 * 
	 * @param iv
	 * @param position
	 */
	public void loadImageForCat(ImageView iv, int id, ImgCacheMap<String, Bitmap> mmImageCacheMap, boolean isProcess)
	{	
		iv.setImageBitmap(null);
		ImageLoader.loadImageWithPNG(iv);
	}

	/**
	 * 加载有透明图图片
	 * 
	 * @param iv
	 * @param position
	 */
//	public void loadImageForCat(ImageView iv, int id)
//	{	
//		iv.setImageResource(id);
//		ImageLoader.loadImageWithPNG(iv);
//	}
	/**
	 * 加载需去圆角图片
	 * 
	 * @param iv
	 * @param position
	 */
//	public void loadImageForCat(ImageView iv, int id, ImgCacheMap mmImageCacheMap, int mType)
//	{	
//		iv.setImageBitmap(null);
//		ImageLoader.loadImage(iv);
//	}
	
	/**
	 * 加载图片
	 * 
	 * @param iv
	 * @param position
	 */
//	public void loadImageForCat(ImageView iv, int id, ImgCacheMap mmImageCacheMap, boolean isProcess, int mType)
//	{	
//		iv.setImageBitmap(null);
//		ImageLoader.loadImage(iv);
//	}
	
	/**
	 * 设置是否可以loadimage
	 * @param flag
	 */
	public void setCanLoadImage(boolean flag)
	{
		ImageLoader.setPauseWork(!flag);
	}
}
