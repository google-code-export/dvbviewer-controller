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
package org.dvbviewer.controller.ui.phone;

import org.dvbviewer.controller.ui.base.BaseSinglePaneActivity;
import org.dvbviewer.controller.ui.fragments.VideoPlayer;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

/**
 * The Class VideoPlayerActivity.
 *
 * @author RayBa
 * @date 07.04.2013
 */
public class VideoPlayerActivity extends BaseSinglePaneActivity {

	public static final String	VIDEO_URL	= "video_url";
	
	VideoPlayer videoFragment;

	/* (non-Javadoc)
	 * @see org.dvbviewer.controller.ui.base.BaseSinglePaneActivity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}


	// this is called when the screen rotates.
	// (onCreate is no longer called when screen rotates due to manifest, see:
	// android:configChanges for this Activity in manifest)
	/* (non-Javadoc)
	 * @see com.actionbarsherlock.app.SherlockFragmentActivity#onConfigurationChanged(android.content.res.Configuration)
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.i(VideoPlayerActivity.class.getSimpleName(), "onConfigurationChanged");
		int orientation = getResources().getConfiguration().orientation;
		if (orientation == Configuration.ORIENTATION_LANDSCAPE || orientation == Configuration.ORIENTATION_PORTRAIT) {
			if (videoFragment != null) {
				videoFragment.resetLayout();
			}
		}
		super.onConfigurationChanged(newConfig);
	}

	/* (non-Javadoc)
	 * @see org.dvbviewer.controller.ui.base.BaseSinglePaneActivity#onCreatePane()
	 */
	@Override
	protected Fragment onCreatePane() {
		videoFragment = new VideoPlayer();
		return videoFragment;
	}

}
