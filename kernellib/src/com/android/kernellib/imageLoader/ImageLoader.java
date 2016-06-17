package com.android.kernellib.imageLoader;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpResponse;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.text.TextUtils;
import android.widget.ImageView;

import com.android.kernellib.http.ByteArrayResponseHandler;
import com.android.kernellib.http.HttpClientWrap;
import com.android.kernellib.utility.DebugLog;
import com.android.kernellib.utility.UITools;

public class ImageLoader {
    /**
     * Interface for the response handlers on image requests.<br>
     * 
     * The call flow is this:<br>
     * 1. Upon being attached to a request, onResponse(bitmap, true) will be invoked to reflect any
     * cached data that was already available. If the data was available.<br>
     * 
     * 2. After a network response returns, only one of the following cases will happen:<br>
     * - onResponse(bitmap, false) will be called if the image was loaded. or<br>
     * - onErrorResponse will be called if there was an error loading the image.
     */
    public interface ImageListener {
        public void onSuccessResponse(Bitmap bitmap,String url, boolean isCached);

        public void onErrorResponse(int errorCode);
    }

    private static final String TAG = "ImageLoader";

    /**
     * @author zhuchengjin 图片类型定义
     */
    enum ImageType {
        PNG, JPG, CIRCLE
    }

    // 线程工厂
    private final ThreadFactory sThreadFactoryDisk = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, TAG + ":disk:" + mCount.getAndIncrement());
        }
    };

    private final ThreadFactory sThreadFactoryNet = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, TAG + ":net:" + mCount.getAndIncrement());
        }
    };

    /**
     * 用于记录由于Identity相同，而没有得到执行的任务（因为这些任务有可能需要执行结果的回调） key 由 CustomRunnable 的getSubIdentity提供
     */
    private Map<String, CustomRunnable> mSameIdentityTaskMap =
            new LinkedHashMap<String, CustomRunnable>() {

                private static final long serialVersionUID = -3664050382241914314L;

                @Override
                protected boolean removeEldestEntry(Entry<String, CustomRunnable> eldest) {
                    return size() > 40;
                }
            };

    // 线程池
    private final CustomThreadPoolExecutor EXECUTOR_FOR_DISK = new CustomThreadPoolExecutor(2, 2,
            2, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(40), sThreadFactoryDisk,
            new ThreadPoolExecutor.DiscardOldestPolicy(), mSameIdentityTaskMap);

    private final CustomThreadPoolExecutor EXECUTOR_FOR_NETWORK = new CustomThreadPoolExecutor(10,
            10, 2, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1), sThreadFactoryNet,
            new ThreadPoolExecutor.DiscardOldestPolicy(), mSameIdentityTaskMap);
    // 下载队列监视器
    private MessageMonitor mMessageMonitor = new MessageMonitor();
    // 写磁盘队列监视器
    private BitmapToDiskMonitor mMessageMonitor2 = new BitmapToDiskMonitor();
    
    // 内存缓存
    private ImgCacheMap<String, Bitmap> mImageCacheMap = new ImgCacheMap<String, Bitmap>(5, true);
    // 磁盘缓存
    private DiskCache mDiskCache = new DiskCache();
    // //通知ui更新
    // private Handler mMainHandler = new Handler(Looper.getMainLooper());
    // 单例保存
    private static ImageLoader mImageLoader = null;
    // 初始化锁
    private static Object sInitLock = new Object();

    // 统计效率相关参数
    private static volatile long sTotalLoadImageCount = 0;
    private static volatile long sLoadImageFromNetCount = 0;
    private static volatile long sLoadImageFromDiskCount = 0;

    /**
     * 图片加载器构造函数
     */
    @SuppressLint("NewApi")
	private ImageLoader() {
        if (Build.VERSION.SDK_INT >= 9) {
            EXECUTOR_FOR_DISK.allowCoreThreadTimeOut(true);
            EXECUTOR_FOR_NETWORK.allowCoreThreadTimeOut(true);
        }

        EXECUTOR_FOR_NETWORK.execute(mMessageMonitor);
        EXECUTOR_FOR_NETWORK.execute(mMessageMonitor2);
    }

    /**
     * 获得单例对象
     * 
     * @return
     */
    private static ImageLoader getInstance() {
        synchronized (sInitLock) {
            if (mImageLoader == null) {
                mImageLoader = new ImageLoader();
            }
        }
        return mImageLoader;
    }

    /**
     * Update disk cache max size
     * 
     * @param maxSize (MB) equals (maxsize * 1024 * 1024)
     */
    public void updateDiskCacheMaxSize(int maxSize) {
        getInstance().mDiskCache.updateMaxSize(maxSize, DiskCache.DISK_CACHE_TYPE_COMMON);
    }

    /**
     * 加载图片，以JPG格式处理图片，透明部分压缩会变黑。
     * 
     * @param iv
     */
    public static void loadImage(ImageView iv) {
        if (iv != null && iv.getContext() != null) {
        	iv.setImageBitmap(null);
            loadImage(iv.getContext(), null, iv, ImageType.JPG, false, null,
                    DiskCache.DISK_CACHE_TYPE_COMMON);
        }
    }
    

	/**
	 * 从本地文件夹中加载图片，本地图片路径通过 view.setTag()方式随ImageView携带下来
	 * 
	 * @param iv
	 */
	public static void loadImageFromLocalExistImg(ImageView iv) {
		if (iv != null && iv.getContext() != null) {
			loadImage(iv.getContext(), null, iv, ImageType.JPG, false, null,
					DiskCache.DISK_CACHE_TYPE_COMMON, true);
		}
	}

    /**
     * 加载图片，以PNG格式处理图片，并设置默认图片。
     * 
     * @param iv
     * @param defaultResId
     */
    public static void loadImage(ImageView iv, int defaultResId) {
        if (iv != null) {
        	iv.setImageBitmap(null);
            iv.setImageResource(defaultResId);
            if (iv.getContext() != null) {
                loadImage(iv.getContext(), null, iv, ImageType.PNG, false, null,
                        DiskCache.DISK_CACHE_TYPE_COMMON);
            }
        }
    }

    /**
     * 加载图片，以PNG格式处理图片，以保留透明部分。
     * 
     * @param iv
     */
    public static void loadImageWithPNG(ImageView iv) {
        if (iv != null && iv.getContext() != null) {
        	iv.setImageBitmap(null);
            loadImage(iv.getContext(), null, iv, ImageType.PNG, false, null,
                    DiskCache.DISK_CACHE_TYPE_COMMON);
        }
    }

    /**
     * 加载图片，自动添加圆角处理
     * 
     * @param iv
     */
    public static void loadImageCircle(ImageView iv) {
        if (iv != null && iv.getContext() != null) {
        	iv.setImageBitmap(null);
            loadImage(iv.getContext(), null, iv, ImageType.CIRCLE, false, null,
                    DiskCache.DISK_CACHE_TYPE_COMMON);
        }
    }

    public static void loadImage(Context context, String url, ImageListener imgListener,
            boolean isFullQuality) {
        loadImage(context, url, null, ImageType.PNG, isFullQuality, imgListener,
                DiskCache.DISK_CACHE_TYPE_COMMON);
    }

    public static void loadImage(ImageView img, ImageListener imgListener, boolean isFullQuality) {
        if (img != null && img.getContext() != null) {
        	img.setImageBitmap(null);
            loadImage(img.getContext(), null, img, ImageType.PNG, isFullQuality, imgListener,
                    DiskCache.DISK_CACHE_TYPE_COMMON);
        }
    }

	private static void loadImage(Context context, String url, ImageView iv,
			ImageType type, boolean isFullQuality, ImageListener imgListener,
			int diskCacheType, boolean isLoadLocalExistImg) {
        Context finalContext = null;
        if (null != context) {
            finalContext = context.getApplicationContext();
        } else if (null != iv) {
            finalContext = iv.getContext().getApplicationContext();
        }
        if (null == finalContext) {
            return;
        }

        String finalUrl = null;
        if (!TextUtils.isEmpty(url)) {
            finalUrl = url;
        } else if (iv != null && (iv.getTag() instanceof String)) {
            finalUrl = (String) iv.getTag();
        } else {
            if (imgListener != null) {
                imgListener.onErrorResponse(-1);
            }
            return;
        }
        if (DebugLog.isDebug()) {
            sTotalLoadImageCount++;
            DebugLog.log(TAG, "Totally loadImage count: " + sTotalLoadImageCount);
        }

        // 取内存bitmap
        Bitmap bt = getInstance().getBitmapFromMemory(finalUrl);
        if (bt != null) {
            DebugLog.log(TAG, "loadImage memory: " + finalUrl);
            if (iv != null && finalUrl.equals(iv.getTag())) {
                iv.setImageBitmap(bt);
                if (imgListener != null) {
                    imgListener.onSuccessResponse(bt, finalUrl,true);
                }
            } else {
                if (imgListener != null) {
                    imgListener.onSuccessResponse(bt, finalUrl,true);
                }
            }
            return;
        }
        // 取磁盘bitmap
		if (iv != null) {
			getInstance().getBitmapFromDisk(finalContext, iv, type,
					isFullQuality, imgListener, diskCacheType,
					isLoadLocalExistImg);
		} else {
			getInstance().getBitmapFromDisk(finalContext, finalUrl, type,
					isFullQuality, imgListener, diskCacheType,
					isLoadLocalExistImg);
		}
    }
    
	private static void loadImage(Context context, String url, ImageView iv,
			ImageType type, boolean isFullQuality, ImageListener imgListener,
			int diskCacheType) {

		loadImage(context, url, iv, type, isFullQuality, imgListener,
				diskCacheType, false);
	}

    /**
     * 控制滑动时不添加网络下载任务
     * 
     * @param flag
     */
    public static void setPauseWork(boolean flag) {
        getInstance().mMessageMonitor.setPause(flag);
    }

    /**
     * 取得内存中bitmap
     * 
     * @param key
     * @return
     */
    private Bitmap getBitmapFromMemory(String key) {
        return mImageCacheMap.get(key);
    }

    /**
     * 添加图片到内存
     * 
     * @param key
     * @param bt
     */
    private void putBitmapToMemory(String key, Bitmap bt) {
        mImageCacheMap.put(key, bt);
    }

    /**
     * 获取disk图片 异步加载
     * 
     * @param iv
     */
	private void getBitmapFromDisk(Context appContext, ImageView iv,
			ImageType type, boolean isFullQuality, ImageListener callBack,
			int diskCacheType, boolean isLoadLocalExistImg) {
		EXECUTOR_FOR_DISK.execute(new DiskLoader(appContext, iv, type,
				isFullQuality, callBack, diskCacheType, isLoadLocalExistImg));
	}

	private void getBitmapFromDisk(Context appContext, String url, ImageType type,
			boolean isFullQuality, ImageListener callBack, int diskCacheType,
			boolean isLoadLocalExistImg) {
		EXECUTOR_FOR_DISK.execute(new DiskLoader(appContext, url, type, isFullQuality, callBack,
				diskCacheType, isLoadLocalExistImg));
	}

    /**
     * @author zhuchengjin 磁盘数据加载
     */
    class DiskLoader extends CustomRunnableImp {

		public DiskLoader(Context appContext, ImageView iv, ImageType type, boolean isFullQuality,
				ImageListener imgLis, int diskCacheType, boolean isLoadLocalExistImage) {
			super(appContext, iv, type, isFullQuality, imgLis, diskCacheType, isLoadLocalExistImage);
		}

		public DiskLoader(Context appContext, String url, ImageType type, boolean isFullQuality,
				ImageListener imgLis, int diskCacheType, boolean isLoadLocalExistImage) {
			super(appContext, url, type, isFullQuality, imgLis, diskCacheType,
					isLoadLocalExistImage);
		}

        @Override
        public void run() {
            if (TextUtils.isEmpty(mUrl)) {
                DebugLog.log("DiskLoader", "processDiskBitmap mUrl null: " + mUrl);
                return;
            }

            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            processDiskBitmap();
        }

        /**
         * 处理磁盘图片
         */
        private void processDiskBitmap() {
            ImageView iv = null;
            if (mImageView != null) {
                iv = mImageView.get();
                if (iv == null) {
                    DebugLog.log("DiskLoader", "DiskLoader run iv == null: " + mUrl);
                    return;
                }
            } else {
                if (mImgListener == null) {
                    DebugLog.log("DiskLoader",
                            "DiskLoader load picture with url mCallback == null: " + mUrl);
                    return;
                }
            }
            if (mAppContext == null) {
                DebugLog.log("DiskLoader", "DiskLoader run context is null: " + mUrl);
                return;
            }
            DebugLog.log("DiskLoader", "DiskLoader Start : " + mUrl);

			final Bitmap bt = mDiskCache.getBitmapFromDisk(mAppContext, mUrl, mImageType,
					mIsFullQuality, mDiskCacheType, mIsLoadLocalExistImage);
            
            // 取得磁盘图片
            if (bt != null) {
                DebugLog.log("DiskLoader", "DiskLoader disk data back :" + mUrl);

                // 添加到内存
                putBitmapToMemory(mUrl, bt);
                if (DebugLog.isDebug()) {
                    sLoadImageFromDiskCount++;
                    DebugLog.log(TAG, "LoadImage from disk count: " + sLoadImageFromDiskCount);
                }
                onResult(bt, true);
            } else {
				if (!mIsLoadLocalExistImage) {
					// 取网络图片
					DebugLog.log("DiskLoader", "DiskLoader load net : " + mUrl);

//					ImageListener listener = mImgListener == null ? null : mImgListener.get();
					if (iv != null) {
						mMessageMonitor.addRunnable(new ImageDownloader(mAppContext, iv, mImageType,
								mIsFullQuality, mImgListener, mDiskCacheType));
					} else {
						mMessageMonitor.addRunnable(new ImageDownloader(mAppContext, mUrl, mImageType,
								mIsFullQuality, mImgListener, mDiskCacheType));
					}
				}
            }
        }
    }

    private static class CustomRunnableImp extends CustomRunnable {

        // 保存图片view
        protected WeakReference<ImageView> mImageView = null;

        // 图片地址
        protected String mUrl = null;

        // 是否是下载广告
        protected ImageType mImageType = ImageType.JPG;

        // 执行完成结果
        private WeakReference<Bitmap> bitmapWR;

        protected boolean mIsFullQuality = false;

        // 可能会被设置的，执行完的回调
        protected ImageListener mImgListener;
        // 本地存储类型（分类存储）
        protected int mDiskCacheType;
        // Context
        protected Context mAppContext;

        //是否只是从本地已经存在的位置获取图片
        protected boolean mIsLoadLocalExistImage = false;
        
        // 通知ui更新
        private Handler mMainHandler = new Handler(Looper.getMainLooper());
        
		public CustomRunnableImp(Context appContext, ImageView iv,
				ImageType type, boolean isFullQuality, ImageListener imgLis,
				int diskCacheType, boolean isLoadLocalExistImage) {
			mImageView = new WeakReference<ImageView>(iv);
			if (iv != null && iv.getTag() != null
					&& (iv.getTag() instanceof String)) {
				mUrl = (String) iv.getTag();
			}
			mImageType = type;
			mIsFullQuality = isFullQuality;
            mImgListener = imgLis;
//			if (imgLis != null) {
//				mImgListener = new WeakReference<ImageListener>(imgLis);
//			} else {
//			}
            mDiskCacheType = diskCacheType;
			mAppContext = appContext;
			mIsLoadLocalExistImage = isLoadLocalExistImage;
		}

		public CustomRunnableImp(Context appContext, String url,
				ImageType type, boolean isFullQuality, ImageListener imgLis,
				int diskCacheType, boolean isLoadLocalExistImage) {
			if (!TextUtils.isEmpty(url)) {
				mUrl = url;
			}
			mImageType = type;
			mIsFullQuality = isFullQuality;
            mImgListener = imgLis;
//			if (imgLis != null) {
//				mImgListener = new WeakReference<ImageListener>(imgLis);
//			} else {
//			}
//            mImgListener = null;
            mDiskCacheType = diskCacheType;
			mAppContext = appContext;
			mIsLoadLocalExistImage = isLoadLocalExistImage;
		}
        
        @Override
        public Object getIdentity() {
            if (mUrl != null) {
                return mUrl;
            } else {
                return super.getIdentity();
            }
        }

        @Override
        String getSubIdentity() {
            return this.toString();
        }

		boolean isViewValide() {
			if (mImageView != null) {
				ImageView iv = mImageView.get();
				if (iv != null && (iv.getTag() instanceof String)
						&& mUrl.equals(iv.getTag())) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		}

        @Override
        void onResult(final Bitmap bt, final boolean isCached) {
            if (mImageView == null && (mImgListener == null/* || mImgListener.get() == null*/)) {
                // 通过url请求图片
                DebugLog.log("DiskLoader", "DiskLoader run null with url: " + mUrl);
                return;
            }
            if (mImageView != null) {
                // 请求图片并设置ImageView
                ImageView iv = mImageView.get();
                if (iv == null || !(iv.getTag() instanceof String) || !mUrl.equals(iv.getTag())) {
                    DebugLog.log("DiskLoader", "DiskLoader run null with ImageView: " + mUrl);
                    return;
                }
            }

            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mImageView == null) {
                        if (bt != null && mImgListener != null) {
                            ImageListener cb = mImgListener/*.get()*/;
                            if (cb != null) {
                                cb.onSuccessResponse(bt, mUrl,isCached);
                            }
                        } else if (mImgListener != null) {
                            ImageListener cb = mImgListener/*.get()*/;
                            if (cb != null) {
                                cb.onErrorResponse(-1);
                            }
                        }
                    } else {
                        ImageView iv = mImageView.get();

                        if (iv != null) {
                            if (bt != null) {
                                iv.setImageBitmap(bt);
                                if (mImgListener != null) {
                                    ImageListener cb = mImgListener/*.get()*/;
                                    if (cb != null) {
                                        cb.onSuccessResponse(bt, mUrl,isCached);
                                    }
                                }
                            } else {
                                if (mImgListener != null) {
                                    ImageListener cb = mImgListener/*.get()*/;
                                    if (cb != null) {
                                        cb.onErrorResponse(-1);
                                    }
                                }
                            }
                        }
                    }
                }
            });

            this.bitmapWR = new WeakReference<Bitmap>(bt);
        }

        @Override
        Bitmap getResult() {
            return bitmapWR == null ? null : bitmapWR.get();
        }
    }

    /**
     * @author zhuchengjin 网络加载图片
     */
    class ImageDownloader extends CustomRunnableImp {
        public ImageDownloader(Context appContext, ImageView iv, ImageType type,
                boolean isFullQuality, ImageListener imgLis, int diskCacheType) {
            super(appContext, iv, type, isFullQuality, imgLis, diskCacheType, false);
        }

        public ImageDownloader(Context appContext, String url, ImageType type,
                boolean isFullQuality, ImageListener imgLis, int diskCacheType) {
            super(appContext, url, type, isFullQuality, imgLis, diskCacheType, false);
        }

        @Override
        public void run() {
            if (TextUtils.isEmpty(mUrl)) {
                DebugLog.log("ImageDownloader", "processDownload mUrl null : " + mUrl);
                return;
            }

            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            processDownload();
        }

        /**
         * 下载图片处理
         * 
         */
        protected void processDownload() {
            ImageView iv = null;
            if (mImageView != null) {
                iv = mImageView.get();
                if (iv == null) {
                    DebugLog.log("ImageDownloader", "processDownload mImageView has released: " + mUrl);
                    return;
                }
            } else {
                if (mImgListener == null/* || mImgListener.get() == null*/) {
                    DebugLog.log("ImageDownloader",
                            "ImageDownloader load picture with url, mCallback == null: " + mUrl);
                    return;
                }
            }

            if (mAppContext == null) {
                DebugLog.log("ImageDownloader", "ImageDownloader run context is null: " + mUrl);
                return;
            }
            long time = 0;
            if (DebugLog.isDebug())
                time = System.currentTimeMillis();

            final Bitmap diskbt;
            // 判断磁盘图片是否存在
            if (mDiskCache.hasBitmap(mAppContext, mUrl, mDiskCacheType)) {
                DebugLog.log("ImageDownloader", "processDownload file has exits: " + mUrl);
                // 取出磁盘图片
                diskbt =
                        mDiskCache.getBitmapFromDisk(mAppContext, mUrl, mImageType, mIsFullQuality,
                                mDiskCacheType);
                if (DebugLog.isDebug()) {
                    sLoadImageFromDiskCount++;
                    DebugLog.log(TAG, "LoadImage from disk count: " + sLoadImageFromDiskCount);
                }
                onResult(diskbt, true);
            } else {
                Bitmap bt = getBitmapStream(mAppContext, mUrl);
                if (DebugLog.isDebug()) {
                    sLoadImageFromNetCount++;
                    DebugLog.log(TAG, "LoadImage from network count: "
                            + sLoadImageFromNetCount);
                }
                onResult(bt, false);
                if (bt != null) {
                    // 保存到磁盘
                    mMessageMonitor2.addRequest(mAppContext, mUrl, bt, mImageType, mDiskCacheType);
                    // 取出磁盘图片
                    diskbt = bt;
                          //mDiskCache.getBitmapFromDisk(mAppContext, mUrl, mImageType, mIsFullQuality,
                          //        mDiskCacheType);
                } else {
                    DebugLog.log("ImageDownloader", "processDownload download error: " + mUrl);
                    diskbt = null;
                }
            }

            DebugLog.log("ImageDownloader",
                    "processDownload download file time:" + (System.currentTimeMillis() - time)
                            + " : " + mUrl);

            // 添加到内存
            putBitmapToMemory(mUrl, diskbt);
        }

        /**
         * 获取图片数据,转换为bitmap
         * 
         * @param url
         * @return
         */
        private Bitmap getBitmapStream(Context context, String url) {
            if (TextUtils.isEmpty(url) || context == null) {
                DebugLog.log("ImageDownloader", "getBitmapStream para error: " + url);
                return null;
            }
            HttpClientWrap wrap = null;
            try {
                wrap = new HttpClientWrap(context);

                int errorCode = wrap.wrapHttpGet(url, new ByteArrayResponseHandler());
                HttpResponse response = wrap.getHttpResponse();
                if (null == response) {
                    DebugLog.log("ImageDownloader",
                            "getBitmapStream response null, errorCode: " + errorCode + " url: "
                                    + url);
                    return null;
                }

                int responseCode = response.getStatusLine().getStatusCode();
                if (responseCode != 200) {
                    DebugLog.log("ImageDownloader", "getBitmapStream null: " + url
                            + " responseCode: " + responseCode);
                    return null;
                }

                if (errorCode != 0) {
                    DebugLog.log("ImageDownloader", "getBitmapStream null: " + url
                            + " errorCode: " + errorCode);
                    return null;
                }

                byte[] _responseBuffer = (byte[]) wrap.getResponseData();
                if (null == _responseBuffer || _responseBuffer.length < 1) {
                    DebugLog.log("ImageDownloader", "getBitmapStream _responseBuffer null: " + url);
                    return null;
                }

                Bitmap mBitmap = UITools.byteArray2ImgBitmap(context.getApplicationContext(),_responseBuffer);

                if (null != mBitmap) {
                    return mBitmap;
                }
            } catch (Exception e) {
                DebugLog.log("ImageDownloader", "getBitmapStream " + url + " e:" + e);
            } finally {
                if (null != wrap) {
                    wrap.release();
                    wrap = null;
                }
            }

            return null;
        }
    }

    /**
     * @author zhuchengjin 监听消息使用
     */
    @SuppressLint("NewApi")
    private class MessageMonitor implements Runnable {
        private static final int MSG_QUEUE_SIZE = 10;
    	private static final int MSG_QUEUE_SIZE2 = 10;
        // 缓存网络下载任务
        private LinkedBlockingDeque<Runnable> mMsgQueue = new LinkedBlockingDeque<Runnable>(
                MSG_QUEUE_SIZE + 1);
		private LinkedBlockingDeque<Runnable> mMsgQueue2 = new LinkedBlockingDeque<Runnable>(
        		MSG_QUEUE_SIZE2 + 1);
        // 等待锁
        private Object mLockForWait = new Object();
        /**
         * true停止
         */
        private Boolean mStop = false;
        // 暂停从消息队列取消息
        private Boolean mPause = false;

        /**
         * 设置是否暂停消息处理
         * 
         * @param flag
         */
        private void setPause(Boolean flag) {
//        	flag = false;
            if (mPause.equals(flag)) {

                DebugLog.log("MessageMonitor", "setPause return flag:" + flag + "  mPause:"
                        + mPause);

                return;
            }
            DebugLog.log("MessageMonitor", "setPause flag:" + flag + "  mPause:" + mPause);

            mPause = flag;
            if (!mPause) {
                cancelWait();
            }
        }

        /**
         * 停止任务
         */
        /*
         * public void stop() { mStop = true; }
         */

        /**
         * 添加运行任务
         * 
         * @param r
         */
        public void addRunnable(Runnable r) {
            if (r != null) {
				try {
					while (mMsgQueue.size() >= MSG_QUEUE_SIZE) {
						Runnable rr = mMsgQueue.removeFirst();
						if (rr != null) {
							DebugLog.log("MessageMonitor",
									"remove runnable "
											+ ((ImageDownloader) rr).mUrl);
							while (mMsgQueue2.size() >= MSG_QUEUE_SIZE2) {
								Runnable rr2 = mMsgQueue2.removeLast();
								DebugLog.log("MessageMonitor", "remove runnable2 "
										+ ((ImageDownloader) rr2).mUrl);
							}
                            mMsgQueue2.offerFirst(rr);
						}
					}
					mMsgQueue.addLast(r);
					DebugLog.log("MessageMonitor", "Current size: "
							+ mMsgQueue.size() + " add runnable "
							+ ((ImageDownloader) r).mUrl);
				} catch (NoSuchElementException nsee) {
                    DebugLog.log("MessageMonitor", "addRunnable nsee:" + nsee);
                } catch (IllegalStateException ise) {
                    DebugLog.log("MessageMonitor", "addRunnable ise:" + ise);
                } catch (NullPointerException npe) {
                    DebugLog.log("MessageMonitor", "addRunnable npe:" + npe);
                }
            }
        }

        /**
         * 请求等待
         */
        public void requestWait() {
            synchronized (mLockForWait) {
                try {
                    mLockForWait.wait();
                } catch (Exception e) {
                    DebugLog.log("MessageMonitor", "requestWait e:" + e);
                }
            }
        }

        /**
         * 取消等待
         */
        public void cancelWait() {
            synchronized (mLockForWait) {
                try {
                    mLockForWait.notifyAll();
                } catch (Exception e) {
                    DebugLog.log("MessageMonitor", "cancelWait e:" + e);
                }
            }
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            Runnable runnable = null;
            while (!mStop) {
                runnable = null;

                // 暂停取消息
                if (mPause) {
                    DebugLog.log("MessageMonitor", "run wait pause cancel");
                    requestWait();
                    continue;
                }

                if (EXECUTOR_FOR_NETWORK.getQueue().remainingCapacity() < 1) {
                    try {
                        DebugLog.log("MessageMonitor", "run sleep 40ms");
                        Thread.sleep(40);
                    } catch (InterruptedException e) {
                        DebugLog.log("MessageMonitor", "run sleep :" + e);
                    }
                    continue;
                }
				try {
					int queue1size1 = mMsgQueue.size();
					int queue2size2 = mMsgQueue2.size();
					if (queue1size1 > 0) {
						runnable = mMsgQueue.takeFirst();
						if (!((ImageDownloader) runnable).isViewValide()) {
							while (mMsgQueue2.size() >= MSG_QUEUE_SIZE2) {
								mMsgQueue2.removeLast();
							}
                            mMsgQueue2.offerFirst(runnable);
							runnable = null;
						}
					} else if (queue2size2 > 0) {
						runnable = mMsgQueue2.takeFirst();
					} else {
						runnable = mMsgQueue.takeFirst();
						if (!((ImageDownloader) runnable).isViewValide()) {
							while (mMsgQueue2.size() >= MSG_QUEUE_SIZE2) {
								mMsgQueue2.removeLast();
							}
                            mMsgQueue2.offerFirst(runnable);
							runnable = null;
						}
					}
                    //Log.v("MessageMonitor", "mMsgQueue1.size:" + mMsgQueue.size());
                    //Log.v("MessageMonitor", "mMsgQueue2.size:" + mMsgQueue2.size());
				} catch (InterruptedException e) {
                    DebugLog.log("MessageMonitor", "run e:" + e.getStackTrace());
                } catch (IllegalStateException e) {
                    DebugLog.log("MessageMonitor", "run e:" + e.getStackTrace());
                } catch (Exception e) {
                    DebugLog.log("MessageMonitor", "run e:" + e.getStackTrace());
                }

                // 网络下载图片
                if (runnable != null) {
                    EXECUTOR_FOR_NETWORK.execute(runnable);
                }
            }
        }
    }

	@SuppressLint("NewApi")
	private class BitmapToDiskMonitor implements Runnable {

		class BitmapInfo {
			private Context mContext;
			private String mUrl;
			private Bitmap mBitmap;
			private ImageType mType;
			private int mDiskCacheType;

			public BitmapInfo(Context context, String url, Bitmap bitmap,
					ImageType type, int diskCacheType) {
				mContext = context;
				mUrl = url;
				mBitmap = bitmap;
				mType = type;
				mDiskCacheType = diskCacheType;
			}
		}

		private static final int MSG_QUEUE_SIZE = 20;
		// 图片保存请求队列
		private LinkedBlockingDeque<BitmapInfo> mMsgQueue = new LinkedBlockingDeque<BitmapInfo>(
				MSG_QUEUE_SIZE);
		/**
		 * true停止
		 */
		private Boolean mStop = false;

		/**
		 * 添加运行任务
		 * 
		 */
		public void addRequest(Context context, String url, Bitmap bitmap,
				ImageType type, int diskCacheType) {
			if (url != null && bitmap != null) {
				try {
					BitmapInfo info = new BitmapInfo(context, url, bitmap,
							type, diskCacheType);
					while (mMsgQueue.size() >= MSG_QUEUE_SIZE) {
						mMsgQueue.removeFirst();
					}
					mMsgQueue.addLast(info);
					DebugLog.log("BitmapToDiskMonitor", "Current size: "
							+ mMsgQueue.size() + " add runnable " + url);
				} catch (NoSuchElementException nsee) {
					DebugLog.log("BitmapToDiskMonitor",
							"addRunnable nsee:" + nsee);
				} catch (IllegalStateException ise) {
					DebugLog.log("BitmapToDiskMonitor", "addRunnable ise:"
							+ ise);
				} catch (NullPointerException npe) {
					DebugLog.log("BitmapToDiskMonitor", "addRunnable npe:"
							+ npe);
				}
			}
		}

		@Override
		public void run() {
			Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

			BitmapInfo runnable = null;
			while (!mStop) {
				runnable = null;
				try {
					runnable = mMsgQueue.takeFirst();
				} catch (InterruptedException e) {
					DebugLog.log("BitmapToDiskMonitor", "run e:" + e.getStackTrace());
				}

				// 网络下载图片
				if (runnable != null) {
					mDiskCache.putBitmapToDisk(runnable.mContext,
							runnable.mUrl, runnable.mBitmap, runnable.mType,
							runnable.mDiskCacheType);
				}
			}
		}
	}
}
