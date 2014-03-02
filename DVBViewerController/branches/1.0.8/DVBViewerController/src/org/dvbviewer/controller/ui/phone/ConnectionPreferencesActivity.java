/*
 * Copyright © 2012 dvbviewer-controller Project
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

import org.dvbviewer.controller.entities.DVBViewerPreferences;
import org.dvbviewer.controller.io.ServerRequest;
import org.dvbviewer.controller.utils.ServerConsts;
import org.dvbviewer.controller.utils.URLUtil;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * The Class ConnectionPreferencesActivity.
 * 
 * @author RayBa
 * @date 13.04.2012
 */
@SuppressWarnings("deprecation")
public class ConnectionPreferencesActivity extends SherlockPreferenceActivity implements OnSharedPreferenceChangeListener {
	
	boolean prefsChanged = false;
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		int id = getIntent().getExtras().getInt("id");
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		PreferenceManager prefMgr = getPreferenceManager();
		prefMgr.setSharedPreferencesName(DVBViewerPreferences.PREFS);
		prefMgr.setSharedPreferencesMode(MODE_WORLD_READABLE);
		addPreferencesFromResource(id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.actionbarsherlock.app.SherlockPreferenceActivity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.SharedPreferences.OnSharedPreferenceChangeListener#
	 * onSharedPreferenceChanged(android.content.SharedPreferences,
	 * java.lang.String)
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		prefsChanged = true;
		if (key.equals(DVBViewerPreferences.KEY_RS_URL)) {
			ServerConsts.REC_SERVICE_URL = sharedPreferences.getString(key, "http://");
		} else if (key.equals(DVBViewerPreferences.KEY_RS_PORT)) {
			ServerConsts.REC_SERVICE_PORT = sharedPreferences.getString(key, "");
		} else if (key.equals(DVBViewerPreferences.KEY_RS_USERNAME)) {
			ServerConsts.REC_SERVICE_USER_NAME = sharedPreferences.getString(key, "");
		} else if (key.equals(DVBViewerPreferences.KEY_RS_PASSWORD)) {
			ServerConsts.REC_SERVICE_PASSWORD = sharedPreferences.getString(key, "");
		} else if (key.equals(DVBViewerPreferences.KEY_RS_LIVE_STREAM_PORT)) {
			ServerConsts.REC_SERVICE_LIVE_STREAM_PORT = sharedPreferences.getString(key, ServerConsts.REC_SERVICE_LIVE_STREAM_PORT);
		} else if (key.equals(DVBViewerPreferences.KEY_RS_MEDIA_STREAM_PORT)) {
			ServerConsts.REC_SERVICE_MEDIA_STREAM_PORT =  sharedPreferences.getString(key, ServerConsts.REC_SERVICE_MEDIA_STREAM_PORT);
		} else if (key.equals(DVBViewerPreferences.KEY_DVBV_URL)) {
			ServerConsts.DVBVIEWER_URL = sharedPreferences.getString(key, "http://");
		} else if (key.equals(DVBViewerPreferences.KEY_DVBV_PORT)) {
			ServerConsts.DVBVIEWER_PORT = sharedPreferences.getString(key, "80");
		} else if (key.equals(DVBViewerPreferences.KEY_DVBV_USERNAME)) {
			ServerConsts.DVBVIEWER_USER_NAME = sharedPreferences.getString(key, "");
		} else if (key.equals(DVBViewerPreferences.KEY_DVBV_PASSWORD)) {
			ServerConsts.DVBVIEWER_PASSWORD = sharedPreferences.getString(key, "");
		}
		URLUtil.setRecordingServicesAddress(ServerConsts.REC_SERVICE_URL, ServerConsts.REC_SERVICE_PORT);
		URLUtil.setViewerAddress(ServerConsts.DVBVIEWER_URL, ServerConsts.DVBVIEWER_PORT);
		ServerRequest.resetHttpCLient();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
//		getSupportMenuInflater().inflate(R.menu.activity_connection_preference, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/* (non-Javadoc)
	 * @see com.actionbarsherlock.app.SherlockPreferenceActivity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		default:
			break;
		}
		return false;
	}

}
