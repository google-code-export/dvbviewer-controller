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

import org.dvbviewer.controller.R;
import org.dvbviewer.controller.data.DbConsts.GroupTbl;
import org.dvbviewer.controller.entities.ChannelGroup;
import org.dvbviewer.controller.entities.DVBViewerPreferences;
import org.dvbviewer.controller.ui.base.DrawerActivity;
import org.dvbviewer.controller.ui.fragments.ChannelPager;
import org.dvbviewer.controller.ui.fragments.ChannelPager.onGroupTypeCHangedListener;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.AdapterView;

/**
 * The Class ChannelListActivity.
 *
 * @author RayBa
 * @date 07.04.2013
 */
public class ChannelListActivity extends DrawerActivity implements LoaderCallbacks<Cursor>, onGroupTypeCHangedListener, OnPageChangeListener {

	private boolean					showFavs;
	private DVBViewerPreferences	prefs;
	private SimpleCursorAdapter		drawerAdapter;
	private final String			pagerFragmentTag	= ChannelPager.class.getSimpleName();
	private ChannelPager			pager;
	public static int 				GROUP_INDEX;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dvbviewer.controller.ui.base.BaseSinglePaneActivity#onCreate(android
	 * .os.Bundle)
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		prefs = new DVBViewerPreferences(this);
		showFavs = prefs.getPrefs().getBoolean(DVBViewerPreferences.KEY_CHANNELS_USE_FAVS, false);
		drawerAdapter = new SimpleCursorAdapter(getApplicationContext(), R.layout.simple_list_item_1, null, new String[] { GroupTbl.NAME }, new int[] { android.R.id.text1 }, 0);
		mDrawerList.setAdapter(drawerAdapter);
		getSupportLoaderManager().initLoader(0, savedInstanceState, this);
	}

	
	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		String selection = showFavs ? GroupTbl.TYPE + " = " + ChannelGroup.TYPE_FAV : GroupTbl.TYPE + " = " + ChannelGroup.TYPE_CHAN;
		String orderBy = GroupTbl._ID;
		CursorLoader loader = new CursorLoader(getApplicationContext(), GroupTbl.CONTENT_URI, null, selection, null, orderBy);
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
		drawerAdapter.swapCursor(arg1);
		Fragment f = getSupportFragmentManager().findFragmentByTag(pagerFragmentTag);
		if (f == null) {
			FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
			pager = new ChannelPager();
			tran.add(R.id.content_frame, pager, pagerFragmentTag);
			tran.commitAllowingStateLoss();
		} else {
			pager = (ChannelPager) f;
		}
		mDrawerList.setItemChecked(pager.getPosition(), true);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {

	}

	@Override
	public void onTypeChanged(int type) {
		showFavs = prefs.getBoolean(DVBViewerPreferences.KEY_CHANNELS_USE_FAVS, false);
		getSupportLoaderManager().restartLoader(0, getIntent().getExtras(), this);
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
		
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		
	}

	@Override
	public void onPageSelected(int position) {
		GROUP_INDEX = position;
		for (int i = 0; i < mDrawerList.getAdapter().getCount(); i++) {
			mDrawerList.setItemChecked(i, false);
		}
		mDrawerList.setItemChecked(position, true);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		GROUP_INDEX = position;
		mDrawerLayout.closeDrawers();
		pager.setPosition(position);
	}

}