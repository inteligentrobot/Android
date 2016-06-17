package com.android.kernellib.imageLoader;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CustomImageView extends ImageView
{
	private boolean mBlockRequestLayout = false;

	public CustomImageView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public CustomImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public CustomImageView(Context context)
	{
		super(context);
	}

	@Override
	public void requestLayout()
	{
		if (!mBlockRequestLayout)
		{
			super.requestLayout();
		}
	}

	@Override
	public void setImageBitmap(Bitmap bm)
	{
		mBlockRequestLayout = true;
		super.setImageBitmap(bm);
		mBlockRequestLayout = false;
	}
}
