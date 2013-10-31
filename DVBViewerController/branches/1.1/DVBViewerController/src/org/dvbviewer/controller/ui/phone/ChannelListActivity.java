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
import org.dvbviewer.controller.entities.DVBViewerPreferences;
import org.dvbviewer.controller.ui.base.BaseActivity;
import org.dvbviewer.controller.ui.fragments.ChannelPager;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

/**
 * The Class ChannelListActivity.
 *
 * @author RayBa
 * @date 07.04.2013
 */
public class ChannelListActivity extends BaseActivity implements LoaderCallbacks<Cursor> {

	private DrawerLayout			mDrawerLayout;
	private ListView				mDrawerList;
	private boolean					showFavs;
	private DVBViewerPreferences	prefs;
	private SimpleCursorAdapter		mAdapter;
	private String					pagerFragmentTag	= ChannelPager.class.getSimpleName();
	private ChannelPager			pager;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dvbviewer.controller.ui.base.BaseSinglePaneActivity#onCreate(android
	 * .os.Bundle)
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.drawer_pager);
		prefs = new DVBViewerPreferences(this);
		showFavs = prefs.getPrefs().getBoolean(DVBViewerPreferences.KEY_CHANNELS_USE_FAVS, false);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		mDrawerList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView parent, View view, int position, long id) {
				pager.setPosition(position);
				mDrawerLayout.closeDrawers();
			}
		});
		mAdapter = new SimpleCursorAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, null, new String[] { GroupTbl.NAME }, new int[] { android.R.id.text1 }, 0);
		mDrawerList.setAdapter(mAdapter);
		getSupportLoaderManager().initLoader(0, savedInstanceState, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		String selection = showFavs ? GroupTbl.TYPE + " = " + 1 : GroupTbl.TYPE + " = " + 0;
		String orderBy = GroupTbl._ID;
		CursorLoader loader = new CursorLoader(getApplicationContext(), GroupTbl.CONTENT_URI, null, selection, null, orderBy);
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
		mAdapter.changeCursor(arg1);
		Fragment f = getSupportFragmentManager().findFragmentByTag(pagerFragmentTag);
		if (f == null) {
			FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
			pager = new ChannelPager();
			tran.add(R.id.content_frame, pager, pagerFragmentTag);
			tran.commitAllowingStateLoss();
		}else {
			pager = (ChannelPager) f;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// TODO Auto-generated method stub

	}

}