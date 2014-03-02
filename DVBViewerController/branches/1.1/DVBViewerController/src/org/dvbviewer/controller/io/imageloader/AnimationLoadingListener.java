package org.dvbviewer.controller.io.imageloader;

import android.graphics.Bitmap;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;

public class AnimationLoadingListener extends SimpleImageLoadingListener{
	
	@Override
	public void onLoadingStarted(String imageUri, View view) {
		super.onLoadingStarted(imageUri, view);
		if (view instanceof ImageView) {
			ImageView v = (ImageView) view;
			v.setImageDrawable(null);
		}
	}
	
	@Override
	public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
		super.onLoadingComplete(imageUri, view, loadedImage);
		Animation animation = new AlphaAnimation(0.0f, 1.0f);
		animation.setDuration(500);
		view.setAnimation(animation);
		animation.start();
	}

}
