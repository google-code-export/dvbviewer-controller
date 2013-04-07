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

import org.dvbviewer.controller.R;
import org.dvbviewer.controller.entities.DVBViewerPreferences;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

/**
 * The Class PreferencesActivity.
 * 
 * @author RayBa
 * @date 13.04.2012
 */
@SuppressWarnings("deprecation")
public class PreferencesActivity extends SherlockPreferenceActivity implements  OnPreferenceClickListener {

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		PreferenceManager prefMgr = getPreferenceManager();
		prefMgr.setSharedPreferencesName(DVBViewerPreferences.APP_SHARED_PREFS);
		prefMgr.setSharedPreferencesMode(MODE_WORLD_READABLE);

		addPreferencesFromResource(R.xml.preferences);
		Preference rsSettings = (Preference) findPreference(DVBViewerPreferences.KEY_RS_SETTINGS);
		rsSettings.setOnPreferenceClickListener(this);
		Preference dvbvSettings = (Preference) findPreference(DVBViewerPreferences.KEY_DVBV_SETTINGS);
		dvbvSettings.setOnPreferenceClickListener(this);
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

	/* (non-Javadoc)
	 * @see android.preference.Preference.OnPreferenceClickListener#onPreferenceClick(android.preference.Preference)
	 */
	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference.getKey().equals(DVBViewerPreferences.KEY_RS_SETTINGS)) {
			preference.getIntent().putExtra("id", R.xml.rs_preferences);
		}else if (preference.getKey().equals(DVBViewerPreferences.KEY_DVBV_SETTINGS)) {
			preference.getIntent().putExtra("id", R.xml.dvbv_preferences);
		}
		return false;
	}

}
