package com.android.kernellib.imageLoader;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Build;
import android.text.TextUtils;

import com.android.kernellib.utility.DebugLog;

public class ImgCacheMap<K, V>
{
	private static final String TAG = ImgCacheMap.class.getSimpleName();

	private final KiloByteBitmapCache<String, Bitmap> mLruMemCache;

	/**
	 * Creates a KiloByteBitmapCache instance initialized to hold (total
	 * available memory) / memoryFraction kilobytes worth of Bitmaps.
	 * 
	 * @param memoryFraction
	 */
	public ImgCacheMap(final int memoryFraction)
	{
		mLruMemCache = KiloByteBitmapCache.create(memoryFraction, false);
	}

	public ImgCacheMap(final int memoryFraction, boolean enableFraction)
	{
		mLruMemCache = KiloByteBitmapCache.create(memoryFraction, enableFraction);
	}

	public Bitmap put(String key, Bitmap value)
	{
		if (TextUtils.isEmpty(key) || value == null) {
			return value;
		}
		DebugLog.log(TAG, "Current LruMemCache size is : " + mLruMemCache.size()
                    + " , Max size: " + mLruMemCache.maxSize());
		return mLruMemCache.put(key, value);
	}

	public Bitmap get(String key) {
		if (TextUtils.isEmpty(key)) {
			return null;
		}
        DebugLog.log(TAG, "miss count: " + mLruMemCache.missCount() + " hit count: "
                + mLruMemCache.hitCount() + " put count: " + mLruMemCache.putCount());
		return mLruMemCache.get(key);
	}

	public void clear()
	{
		mLruMemCache.evictAll();
	}
	
	/**
	 * A Bitmap cache that measures the size in kilo-bytes and provides a
	 * factory method to adjust to available memory.
	 */
	static class KiloByteBitmapCache<K, V> extends LruCache<K, V> {

		private static final int KILOBYTE = 1024;

		private static final int DEFAULT_MEM_SIZE = 3 * KILOBYTE;

		private KiloByteBitmapCache(int maxSize) {
			super(maxSize);
		}

		@Override
		protected int sizeOf(K key, V bitmap) {
			// The cache size will be measured in kilobytes rather than
			// number of items. Add 1KB for overhead.
			if (bitmap instanceof Bitmap) {
				return bitmap == null ? 1
						: (getBitmapSize((Bitmap) bitmap) / KILOBYTE) + 1;
			} else {
				return 1;
			}
		}

		/**
		 * Creates a KiloByteBitmapCache instance initialized to hold (total
		 * available memory) / memoryFraction kilobytes worth of Bitmaps.
		 * 
		 * @param memoryFraction
		 *            The max size fraction of memory to use. 1 for 100% of the
		 *            available memory, 5 for 20% of the available memory etc.
		 * @return A new instance of KiloByteBitmapCache with a pre-calculated
		 *         max size.
		 * @throws java.lang.IllegalArgumentException
		 *             if memoryFraction is less or equal to 0.
		 */
		public static <K, V extends Object> KiloByteBitmapCache<K, V> create(
				int memoryFraction, boolean enableFraction) throws IllegalArgumentException {
			int maxMemory = (int) (Runtime.getRuntime().maxMemory() / KILOBYTE);
			if (maxMemory > DEFAULT_MEM_SIZE) {
			    maxMemory = DEFAULT_MEM_SIZE;
			}
			if (enableFraction) {
				if (memoryFraction <= 0) {
					throw new IllegalArgumentException(
							"Negative memory fractions are not allowed.");
				}
				if (memoryFraction < 2) {
					memoryFraction = 2;
				}
				int availableSize = (int) ((Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory()) / KILOBYTE);
				DebugLog.log("lincai", "Max available memory size:"
						+ availableSize);
				maxMemory = availableSize / memoryFraction;
				if (maxMemory < 1 * KILOBYTE) {
					maxMemory = 1 * KILOBYTE;
				}
				if (maxMemory > 12 * KILOBYTE) {
					maxMemory = 12 * KILOBYTE;
				}
			}
			DebugLog.log("lincai", "maxMemory:" + maxMemory);
			// Get max available VM memory in KB
			// Use 1/CACHE_MEMORY_FRACTION of the available memory for this
			// memory cache.
			return new KiloByteBitmapCache<K, V>(maxMemory);
		}

        @SuppressLint("NewApi")
		private static int getBitmapSize(Bitmap value) {

            int ret = 0;

            // VERSION_CODES.KITKAT 19
            if (Build.VERSION.SDK_INT >= 19) {
                try {
                    ret = value.getAllocationByteCount();
                } catch (Exception e) {
                   DebugLog.log("KiloByteBitmapCache", "exception in getBitmapSize: " + e.getMessage());
                    ret = value.getByteCount();
                }
            } else if (Build.VERSION.SDK_INT >= 12) {
                // VERSION_CODES.HONEYCOMB_MR1 12
                ret = value.getByteCount();
            } else {
                ret = value.getRowBytes() * value.getHeight();
            }

            return ret;
        }
	}
}
