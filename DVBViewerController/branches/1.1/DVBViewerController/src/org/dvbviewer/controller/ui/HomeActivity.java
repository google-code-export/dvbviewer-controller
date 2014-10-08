/*
 * Copyright (C) 2012 dvbviewer-controller Project
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
package org.dvbviewer.controller.ui;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.dvbviewer.controller.R;
import org.dvbviewer.controller.entities.Channel;
import org.dvbviewer.controller.entities.DVBViewerPreferences;
import org.dvbviewer.controller.ui.base.BaseActivity;
import org.dvbviewer.controller.ui.fragments.AboutFragment;
import org.dvbviewer.controller.ui.fragments.ChannelList;
import org.dvbviewer.controller.ui.fragments.ChannelList.OnChannelSelectedListener;
import org.dvbviewer.controller.ui.fragments.ChannelPager;
import org.dvbviewer.controller.ui.fragments.Dashboard;
import org.dvbviewer.controller.ui.fragments.Dashboard.OnDashboardButtonClickListener;
import org.dvbviewer.controller.ui.fragments.RecordingList;
import org.dvbviewer.controller.ui.fragments.Remote;
import org.dvbviewer.controller.ui.fragments.TimerList;
import org.dvbviewer.controller.ui.phone.ChannelListActivity;
import org.dvbviewer.controller.ui.phone.PreferencesActivity;
import org.dvbviewer.controller.ui.phone.RecordinglistActivity;
import org.dvbviewer.controller.ui.phone.RemoteActivity;
import org.dvbviewer.controller.ui.phone.StatusActivity;
import org.dvbviewer.controller.ui.phone.TaskActivity;
import org.dvbviewer.controller.ui.phone.TimerlistActivity;
import org.dvbviewer.controller.ui.tablet.ChannelListMultiActivity;
import org.dvbviewer.controller.utils.Config;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

/**
 * The Class HomeActivity.
 *
 * @author RayBa
 * @date 01.07.2012
 */
public class HomeActivity extends BaseActivity implements OnClickListener, OnChannelSelectedListener, OnDashboardButtonClickListener {

	SimpleDateFormat				dateFormat	= new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
	private View					mRightContainer;
	private View					mLeftContainer;
	private TextView				multiContainerIndicator;
	boolean							expired		= false;
	private AlertDialog				expirationDialog;
	String							expirationMessage;
	private DVBViewerPreferences	prefs;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dvbviewer.controller.ui.base.BaseActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		prefs = new DVBViewerPreferences(this);
		if (savedInstanceState == null) {
			DVBViewerPreferences dvbprefs = new DVBViewerPreferences(this);
			String expirationString = dvbprefs.getString(DVBViewerPreferences.KEY_EXPIRE_DATE);
			try {

				Date now = new Date();
				expirationMessage = dvbprefs.getString(DVBViewerPreferences.KEY_EXPIRE_Message);
				Date d = dateFormat.parse(expirationString);
				if (now.after(d) && getResources().getBoolean(R.bool.check_expire_date)) {
					expired = true;
				}
			} catch (ParseException e1) {
				Log.d(HomeActivity.class.getSimpleName(), "Cant parse date: " + expirationString);
			}
		}

		mLeftContainer = findViewById(R.id.left_container);
		mRightContainer = findViewById(R.id.multi_container);
		multiContainerIndicator = (TextView) findViewById(R.id.multi_container_indicator);

		if (savedInstanceState == null) {
			FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
			Dashboard dashboard = new Dashboard();
			tran.add(mLeftContainer.getId(), dashboard);
			if (mRightContainer != null) {
				ChannelPager chanPager = new ChannelPager();
				Bundle args = new Bundle();
				args.putBoolean(ChannelList.KEY_HAS_OPTIONMENU, true);
				args.putBoolean(DVBViewerPreferences.KEY_CHANNELS_USE_FAVS, prefs.getBoolean(DVBViewerPreferences.KEY_CHANNELS_USE_FAVS, false));
				chanPager.setArguments(args);
				tran.add(mRightContainer.getId(), chanPager);
				multiContainerIndicator.setText(R.string.channelList);
			}
			tran.commit();
			if (Config.IS_FIRST_START) {
				Config.IS_FIRST_START = false;
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(getResources().getString(R.string.firstStartMessage)).setPositiveButton(R.string.yes, this).setTitle(getResources().getString(R.string.firstStartMessageTitle)).setNegativeButton(R.string.no, this).show();
				prefs.getPrefs().edit().putBoolean(DVBViewerPreferences.KEY_IS_FIRST_START, false).commit();
			}
		}

	}

	@Override
	protected void onPause() {
		super.onPause();
		if (expired) {
			expirationDialog.dismiss();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (expired) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setNeutralButton(R.string.ok, this);
			builder.setTitle(R.string.expiration_title);
			builder.setMessage(expirationMessage);
			builder.setCancelable(false);
			expirationDialog = builder.create();
			expirationDialog.show();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.content.DialogInterface.OnClickListener#onClick(android.content
	 * .DialogInterface, int)
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		case DialogInterface.BUTTON_POSITIVE:
			Intent settings = new Intent(HomeActivity.this, PreferencesActivity.class);
			startActivity(settings);
			break;

		case DialogInterface.BUTTON_NEUTRAL:
			finish();
			break;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dvbviewer.controller.ui.fragments.ChannelList.OnChannelSelectedListener
	 * #channelSelected(org.dvbviewer.controller.entities.Channel, int)
	 */
	@Override
	public void onChannelSelected(List<Channel> chans, int position) {
		Intent channelListIntent = new Intent(this, ChannelListMultiActivity.class);
		channelListIntent.putParcelableArrayListExtra(Channel.class.getName(), (ArrayList<Channel>) chans);
		channelListIntent.putExtra(ChannelList.KEY_SELECTED_POSITION, position);
		startActivity(channelListIntent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dvbviewer.controller.ui.fragments.Dashboard.
	 * OnDashboardButtonClickListener#onDashboarButtonClick(android.view.View)
	 */
	@Override
	public void onDashboarButtonClick(View v) {
		switch (v.getId()) {
		case R.id.home_btn_remote:
			if (mRightContainer != null) {
				FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
				tran.replace(mRightContainer.getId(), new Remote());
				tran.commit();
				multiContainerIndicator.setText(R.string.remote);
			} else {
				startActivity(new Intent(this, RemoteActivity.class));
			}
			break;
		case R.id.home_btn_channels:
			if (mRightContainer != null) {
				FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
				ChannelPager chanPager = new ChannelPager();
				Bundle args = new Bundle();
				args.putBoolean(ChannelList.KEY_HAS_OPTIONMENU, true);
				args.putBoolean(DVBViewerPreferences.KEY_CHANNELS_USE_FAVS, prefs.getBoolean(DVBViewerPreferences.KEY_CHANNELS_USE_FAVS, false));
				chanPager.setArguments(args);
				tran.replace(mRightContainer.getId(), chanPager);
				// ChannelList chans = new ChannelList();
				// chans.setHasOptionsMenu(true);
				// tran.replace(multiContainer.getId(), chans);
				tran.commit();
				multiContainerIndicator.setText(R.string.channelList);
			} else {
				startActivity(new Intent(this, ChannelListActivity.class));
			}
			break;
		case R.id.home_btn_timers:
			if (mRightContainer != null) {
				FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
				tran.replace(mRightContainer.getId(), new TimerList());
				tran.commit();
				multiContainerIndicator.setText(R.string.timer);
			} else {

				startActivity(new Intent(this, TimerlistActivity.class));
			}
			break;
		case R.id.home_btn_recordings:
			if (mRightContainer != null) {
				FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
				tran.replace(mRightContainer.getId(), new RecordingList());
				tran.commit();
				multiContainerIndicator.setText(R.string.recordings);
			} else {
				startActivity(new Intent(this, RecordinglistActivity.class));
			}
			break;
		case R.id.home_btn_settings:
			startActivity(new Intent(this, PreferencesActivity.class));
			break;
		case R.id.home_btn_tasks:
			startActivity(new Intent(this, TaskActivity.class));
			break;
		case R.id.home_btn_status:
			startActivity(new Intent(this, StatusActivity.class));
			break;

		default:
			break;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		expired = savedInstanceState.getBoolean("expired", false);
		expirationMessage = savedInstanceState.getString("expirationMessage");
		if (savedInstanceState != null && savedInstanceState.containsKey("indicatorText")) {
			multiContainerIndicator.setText(savedInstanceState.getString("indicatorText"));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.support.v4.app.FragmentActivity#onSaveInstanceState(android.os
	 * .Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("expired", expired);
		outState.putString("expirationMessage", expirationMessage);
		if (multiContainerIndicator != null) {
			outState.putString("indicatorText", multiContainerIndicator.getText().toString());
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.home, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuAbout:
			AboutFragment about = new AboutFragment();
			about.show(getSupportFragmentManager(), "ABOUT");
			return true;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

}
