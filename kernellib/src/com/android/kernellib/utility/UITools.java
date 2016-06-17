package com.android.kernellib.utility;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

/**
 * UI相关工具函数都放在此类中。 如：弹窗、toast 、view隐藏 显示
 * 
 */
public class UITools {
	private static Toast toast;

	/**
	 * toast提示
	 * 
	 * @param context
	 *            上下文
	 * @param toastResId
	 *            字符串资源id
	 */
	public static void showToast(Context context, int toastResId) {
		if (null == toast) {
			toast = Toast.makeText(context, ":)", Toast.LENGTH_SHORT);
		}
		toast.setText(toastResId);
		toast.show();
	}

	/**
	 * toast提示
	 * 
	 * @param context
	 *            上下文
	 * @param content
	 *            字符串内容
	 */
	public static void showToast(Context context, String content) {
		if (null == toast) {
			toast = Toast.makeText(context, ":)", Toast.LENGTH_SHORT);
		}
		toast.setText(content);
		toast.show();
	}

	/**
	 * 弹窗Dialog封装 单一按钮键alertDialog
	 * 
	 * @author WeiZheng
	 * 
	 * @param context
	 * @param titleId
	 * @param messageId
	 *            String资源Id
	 * @param buttonId
	 * @param listener
	 *            监听者
	 */
	public static void showSingleButtonDialogWithResId(final Context context,
			int titleId, int messageResId, int buttonId,
			OnClickListener listener) {
		new AlertDialog.Builder(context).setTitle(titleId)
				.setMessage(messageResId).setPositiveButton(buttonId, listener)
				.create().show();
	}

	/**
	 * 弹窗Dialog封装 单一按钮键alertDialog
	 * 
	 * @author dragon
	 * 
	 * @param context
	 * @param titleId
	 * @param messageId
	 *            String资源Id
	 * @param buttonId
	 * @param listener
	 *            监听者
	 */
	public static void showSingleButtonDialogWithResId(final Context context,
			int titleId, int messageResId, int buttonId,
			OnClickListener listener, DialogInterface.OnDismissListener dis) {
		Dialog d = new AlertDialog.Builder(context).setTitle(titleId)
				.setMessage(messageResId).setPositiveButton(buttonId, listener)
				.create();
		d.setOnDismissListener(dis);
		d.show();
	}

	/**
	 * 弹窗Dialog封装 单一按钮键alertDialog4String
	 * 
	 * @author WeiZheng
	 * 
	 * @param context
	 * @param titleId
	 * @param messageId
	 *            String类型
	 * @param buttonId
	 * @param listener
	 *            监听者
	 * 
	 */
	public static void showSingleButtonDialogWithStr(final Context context,
			int titleId, String messageStr, int buttonId,
			OnClickListener listener) {
		new AlertDialog.Builder(context).setTitle(titleId)
				.setMessage(messageStr).setPositiveButton(buttonId, listener)
				.create().show();
	}

	/**
	 * 两个按钮键alertDialog
	 * 
	 * @author WeiZheng
	 * 
	 * @param context
	 * @param titleId
	 * @param messageId
	 *            String资源Id
	 * @param positiveButtonId
	 * @param positiveListener
	 *            监听者
	 * @param negativeButtonId
	 * @param negativeListener
	 *            监听者
	 */
	public static void showDoubleButtonDialogWithResId(final Context context,
			final int titleId, final int messageId, final int positiveButtonId,
			OnClickListener positiveListener, final int negativeButtonId,
			OnClickListener negativeListener) {
		new AlertDialog.Builder(context).setTitle(titleId)
				.setMessage(messageId)
				.setPositiveButton(positiveButtonId, positiveListener)
				.setNegativeButton(negativeButtonId, negativeListener).create()
				.show();
	}

	/**
	 * 两个按钮键alertDialog
	 * 
	 * @author dragon
	 * 
	 * @param context
	 * @param titleId
	 * @param messageId
	 *            String资源Id
	 * @param positiveButtonId
	 * @param positiveListener
	 *            监听者
	 * @param negativeButtonId
	 * @param negativeListener
	 *            监听者
	 */
	public static void showDoubleButtonDialogWithResId(final Context context,
			final int titleId, final int messageId, final int positiveButtonId,
			OnClickListener positiveListener, final int negativeButtonId,
			OnClickListener negativeListener,
			DialogInterface.OnDismissListener dis) {
		Dialog d = new AlertDialog.Builder(context).setTitle(titleId)
				.setMessage(messageId)
				.setPositiveButton(positiveButtonId, positiveListener)
				.setNegativeButton(negativeButtonId, negativeListener).create();
		d.setOnDismissListener(dis);
		d.show();
	}

	/**
	 * 无标题栏的对话框
	 * 
	 * @date:2014-9-11
	 * @time:下午2:36:58
	 */

	public static void showDoubleButtonDialogWithNoTitle(final Context context,
			final int messageId, final int positiveButtonId,
			OnClickListener positiveListener, final int negativeButtonId,
			OnClickListener negativeListener,
			DialogInterface.OnDismissListener dis) {
		Dialog d = new AlertDialog.Builder(context).setMessage(messageId)
				.setPositiveButton(positiveButtonId, positiveListener)
				.setNegativeButton(negativeButtonId, negativeListener).create();
		d.setOnDismissListener(dis);
		d.show();
	}

	/**
	 * 强制收起键盘
	 * 
	 * @param mActivity
	 * @param now
	 */
	public static void hideSoftkeyboard(Activity mActivity, boolean now) {
		if (null != mActivity && null != mActivity.getCurrentFocus()) {
			InputMethodManager mInputMethodManager = (InputMethodManager) mActivity
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			if (null != mInputMethodManager) {
				mInputMethodManager.hideSoftInputFromWindow(mActivity
						.getCurrentFocus().getWindowToken(), 0);
			}
		}
	}

	public static int computeSampleSize(BitmapFactory.Options options,
			int minSideLength, int maxNumOfPixels) {
		int initialSize = computeInitialSampleSize(options, minSideLength,
				maxNumOfPixels);
		int roundedSize;
		if (initialSize <= 8) {
			roundedSize = 1;
			while (roundedSize < initialSize) {
				roundedSize <<= 1;
			}
		} else {
			roundedSize = (initialSize + 7) / 8 * 8;
		}

		return roundedSize;
	}

	private static int computeInitialSampleSize(BitmapFactory.Options options,
			int minSideLength, int maxNumOfPixels) {
		double w = options.outWidth;
		double h = options.outHeight;

		int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math
				.sqrt(w * h / maxNumOfPixels));
		int upperBound = (minSideLength == -1) ? 128 : (int) Math.min(
				Math.floor(w / minSideLength), Math.floor(h / minSideLength));

		if (upperBound < lowerBound) {
			// return the larger one when there is no overlapping zone.
			return lowerBound;
		}

		if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
			return 1;
		} else if (minSideLength == -1) {
			return lowerBound;
		} else {
			return upperBound;
		}
	}
	
    /**
     * liuzm
     * 获取边界压缩的bitmap流
     *
     * @param context
     * @param _b
     * @return
     */
    public static Bitmap byteArray2ImgBitmap(Context context, byte[] _b) {
        Bitmap b = zoomBitmap(context, _b);
        //Bitmap b = BitmapFactory.decodeByteArray(_b, 0, _b.length, getBitmapOption(context));
        return null == _b ? null : b;
    }
    
    @SuppressWarnings("deprecation")
	public static Bitmap zoomBitmap(Context context, byte[] _b) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int w = displayMetrics.widthPixels;
        int h = displayMetrics.heightPixels;
        int d = displayMetrics.densityDpi;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;

        try {
            BitmapFactory.decodeByteArray(_b, 0, _b.length, opts);

            //int x = 2;
            int x = computeSampleSize(opts, w > h ? w : h, w * h);
            opts.inTargetDensity = d;
            opts.inSampleSize = x;
            opts.inJustDecodeBounds = false;

            opts.inDither = false;
            opts.inPurgeable = true;

            return BitmapFactory.decodeByteArray(_b, 0, _b.length, opts);
        } catch (OutOfMemoryError ooe) {
            ooe.printStackTrace();
            System.gc();
            //			VMRuntime.getRuntime().setTargetHeapUtilization(0.75f);
        }
        return null;
    }
    
    public static Bitmap drawable2Bitmap(Drawable _d) {
        return null == _d ? null : ((BitmapDrawable) _d).getBitmap();
    }

    public static Bitmap byteArray2Bitmap(byte[] _b, Options option) {
        return null == _b ? null : BitmapFactory.decodeByteArray(_b, 0, _b.length, option);
    }

    public static Bitmap byteArray2Bitmap(byte[] _b) {
        return null == _b ? null : BitmapFactory.decodeByteArray(_b, 0, _b.length);
    }

    public static Bitmap byteArray2Bitmap(Context context, byte[] _b) {
        return null == _b ? null : BitmapFactory.decodeByteArray(_b, 0, _b.length, getBitmapOption(context));
    }
    
    public static BitmapFactory.Options getBitmapOption(Context mContext) {
        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inScaled = true;
        option.inDensity = DisplayMetrics.DENSITY_DEFAULT;
        option.inTargetDensity = mContext.getResources().getDisplayMetrics().densityDpi;

        return option;
    }
}
