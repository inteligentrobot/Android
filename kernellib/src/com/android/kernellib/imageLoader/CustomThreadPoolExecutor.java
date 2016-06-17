package com.android.kernellib.imageLoader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.android.kernellib.imageLoader.ImageLoader.DiskLoader;

/**
 * @author zhuchengjin
 * 为图片加载专用，可以过滤重复启动相同任务
 */
public class CustomThreadPoolExecutor extends ThreadPoolExecutor {
    public static final String TAG = CustomThreadPoolExecutor.class.getSimpleName();
    // 用于记录正在运行的任务
    private HashMap<Object, Object> mRunningTaskMap = new HashMap<Object, Object>();
    private Map<String, CustomRunnable> mSameIdentityTaskMap;

    public CustomThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
            TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
            RejectedExecutionHandler handler, Map<String, CustomRunnable> mSameIdentityTaskMap) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);

        this.mSameIdentityTaskMap = mSameIdentityTaskMap;
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        if (r != null && r instanceof CustomRunnable) {
            CustomRunnable finishedRunnable = (CustomRunnable) r;
            Object obj = finishedRunnable.getIdentity();
            if (obj != null) {
                mRunningTaskMap.remove(obj);

                // 找出相同key值的task，通知其执行结果
                synchronized (mSameIdentityTaskMap) {
                    for (Iterator<Map.Entry<String, CustomRunnable>> it =
                            mSameIdentityTaskMap.entrySet().iterator(); it.hasNext();) {
                        Map.Entry<String, CustomRunnable> pairs = it.next();
                        CustomRunnable unExecutedRunnable = pairs.getValue();
                        // 当finishedRunnable.getResult() == null
                        // 的时候，可能是DiskLoader没有取到本地，交给ImageDownloader来处理了，这种情况下，不应该onResult
                        if (obj.equals(unExecutedRunnable.getIdentity())
                                && !(finishedRunnable.getResult() == null && finishedRunnable instanceof DiskLoader)) {
                            unExecutedRunnable.onResult(finishedRunnable.getResult(), false);
                            it.remove();
                        }
                    }
                }
            }
        }
        super.afterExecute(r, t);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        if (r != null && r instanceof CustomRunnable) {
            Object obj = ((CustomRunnable) r).getIdentity();
            if (obj != null) {
                mRunningTaskMap.put(obj, obj);
                // 运行列表不可能多余线程池最大值
                if (mRunningTaskMap.size() > getMaximumPoolSize()) {
                    mRunningTaskMap.clear();
                }
            }
        }
        super.beforeExecute(t, r);
    }

    @Override
    public void execute(Runnable command) {
        if (command != null && command instanceof CustomRunnable) {
            CustomRunnable customRunnable = (CustomRunnable) command;
            Object obj = customRunnable.getIdentity();

            if (obj != null && mRunningTaskMap.containsKey(obj)) {
                String subIdentity = customRunnable.getSubIdentity();

                if (subIdentity != null) {
                    synchronized (mSameIdentityTaskMap) {
                        mSameIdentityTaskMap.put(subIdentity, customRunnable);
                    }
                }

                return;
            }
        }
        super.execute(command);
    }

    /**
     * 清除正在运行任务记录
     */
    public void resetRunningTaskMap() {
        mRunningTaskMap.clear();
    }
}
