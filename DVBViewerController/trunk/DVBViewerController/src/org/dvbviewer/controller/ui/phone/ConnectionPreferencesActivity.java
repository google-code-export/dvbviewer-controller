/*
 * Copyright � 2012 dvbviewer-controller Project
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

import java.net.MalformedURLException;
import java.net.URL;

import org.dvbviewer.controller.entities.DVBViewerPreferences;
import org.dvbviewer.controller.io.ServerRequest;
import org.dvbviewer.controller.utils.ServerConsts;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

/**
 * The Class ConnectionPreferencesActivity.
 * 
 * @author RayBa
 * @date 13.04.2012
 */
@SuppressWarnings("deprecation")
public class ConnectionPreferencesActivity extends SherlockPreferenceActivity implements OnSharedPreferenceChangeListener {

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
		prefMgr.setSharedPreferencesName(DVBViewerPreferences.APP_SHARED_PREFS);
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
		if (key.equals(DVBViewerPreferences.KEY_RS_URL)) {
			ServerConsts.REC_SERVICE_URL = sharedPreferences.getString(key, "http://");
			try {
				URL url = new URL(ServerConsts.REC_SERVICE_URL);
				ServerConsts.REC_SERVICE_HOST = url.getHost();
				ServerConsts.REC_SERVICE_PORT = sharedPreferences.getString(DVBViewerPreferences.KEY_RS_PORT, "8089");
				ServerConsts.REC_SERVICE_URL = ServerConsts.REC_SERVICE_URL+":"+ServerConsts.REC_SERVICE_PORT;
			} catch (MalformedURLException e) {
				Log.e(ConnectionPreferencesActivity.class.getSimpleName(), "MALFORMED RECORDING SERVICE URL");
			}
		} else if (key.equals(DVBViewerPreferences.KEY_RS_PORT)) {
			ServerConsts.REC_SERVICE_PORT = sharedPreferences.getString(key, "");
			ServerConsts.REC_SERVICE_URL = sharedPreferences.getString(DVBViewerPreferences.KEY_RS_URL, "http://");
			ServerConsts.REC_SERVICE_URL = ServerConsts.REC_SERVICE_URL+":"+ServerConsts.REC_SERVICE_PORT;
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
			ServerConsts.DVBVIEWER_PORT = sharedPreferences.getString(DVBViewerPreferences.KEY_DVBV_PORT, "80");
			ServerConsts.DVBVIEWER_URL = ServerConsts.DVBVIEWER_URL+":"+ServerConsts.DVBVIEWER_PORT;
		} else if (key.equals(DVBViewerPreferences.KEY_DVBV_PORT)) {
			ServerConsts.DVBVIEWER_PORT = sharedPreferences.getString(key, "80");
			ServerConsts.DVBVIEWER_URL = sharedPreferences.getString(DVBViewerPreferences.KEY_DVBV_URL, "http://");
			ServerConsts.DVBVIEWER_URL = ServerConsts.DVBVIEWER_URL+":"+ServerConsts.DVBVIEWER_PORT;
		} else if (key.equals(DVBViewerPreferences.KEY_DVBV_USERNAME)) {
			ServerConsts.DVBVIEWER_USER_NAME = sharedPreferences.getString(key, "");
		} else if (key.equals(DVBViewerPreferences.KEY_DVBV_PASSWORD)) {
			ServerConsts.DVBVIEWER_PASSWORD = sharedPreferences.getString(key, "");
		}
		ServerRequest.resetHttpCLient();
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