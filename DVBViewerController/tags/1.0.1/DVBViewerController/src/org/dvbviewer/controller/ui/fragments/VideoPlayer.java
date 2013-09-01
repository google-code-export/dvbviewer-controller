/*
 * Copyright © 2013 dvbviewer-controller Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.dvbviewer.controller.ui.fragments;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnPreparedListener;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

import org.dvbviewer.controller.R;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * The Internal Videoplayer which is provided by the vitamio framework.
 *
 * @author RayBa
 * @date 07.04.2013
 */
public class VideoPlayer extends SherlockFragment implements OnPreparedListener, OnBufferingUpdateListener {

	private Uri			path;
	private VideoView	mVideoView;
	private ProgressBar	mProgress;
	private TextView	mProgressInfo;

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		path = getActivity().getIntent().getData();
		setRetainInstance(true);
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// mVideoView.setBufferSize(1024*1024);
		mVideoView.setMediaController(new MediaController(getActivity()));
		mVideoView.setOnPreparedListener(this);
		mVideoView.setOnBufferingUpdateListener(this);
		mVideoView.requestFocus();
		mVideoView.setVideoURI(path);
		mVideoView.start();
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_video_player, container, false);
		mVideoView = (VideoView) v.findViewById(R.id.VideoView);
		mProgress = (ProgressBar) v.findViewById(R.id.progress);
		mProgressInfo = (TextView) v.findViewById(R.id.progressInfo);
		return v;
	}

	/**
	 * Sets the path.
	 *
	 * @param path the new path
	 * @author RayBa
	 * @date 07.04.2013
	 */
	public void setPath(Uri path) {
		this.path = path;
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onConfigurationChanged(android.content.res.Configuration)
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		mVideoView.requestLayout();
		super.onConfigurationChanged(newConfig);
	}

	/**
	 * Je nachdem ob ein lokales File abgespielt wird, oder ein Stream gespielt
	 * wird<br>
	 * wird hier die videoSource für den VideoView gesetzt.
	 *
	 * @param source the new video source
	 * @author RayBa
	 * @date 22.02.2011
	 */
	private void setVideoSource(String source) {
		if (!URLUtil.isNetworkUrl(source)) {
			mVideoView.setVideoPath(source);
		} else {
			mVideoView.setVideoURI(Uri.parse(source));
		}
	}

	/**
	 * Reset layout.
	 *
	 * @author RayBa
	 * @date 07.04.2013
	 */
	public void resetLayout() {
		Log.i(VideoPlayer.class.getSimpleName(), "resetLayout");
		if (mVideoView != null) {
			mVideoView.setVideoLayout(VideoView.VIDEO_LAYOUT_SCALE, mVideoView.getVideoAspectRatio());
		}

	}

	/* (non-Javadoc)
	 * @see io.vov.vitamio.MediaPlayer.OnPreparedListener#onPrepared(io.vov.vitamio.MediaPlayer)
	 */
	@Override
	public void onPrepared(MediaPlayer arg0) {
		mVideoView.setVideoLayout(VideoView.VIDEO_LAYOUT_SCALE, mVideoView.getVideoAspectRatio());
		mVideoView.start();
		mProgress.setVisibility(View.GONE);
		mProgressInfo.setText("Buffering...");
	}

	/* (non-Javadoc)
	 * @see io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener#onBufferingUpdate(io.vov.vitamio.MediaPlayer, int)
	 */
	@Override
	public void onBufferingUpdate(MediaPlayer arg0, int arg1) {
		if (mVideoView.isPlaying() || arg1 == 100) {
			mProgressInfo.setVisibility(View.GONE);
			return;
		}
		mProgressInfo.setText("Buffering...  " + arg1 + " %");
		if (!mVideoView.isPlaying() && arg1 > 10) {
			mProgressInfo.setVisibility(View.GONE);
			Log.i(VideoPlayer.class.getSimpleName(), "Buffer Size: " + arg1);
			mVideoView.start();
		}

	}
}
