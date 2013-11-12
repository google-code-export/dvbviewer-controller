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
import org.dvbviewer.controller.ui.base.BaseActivity;
import org.dvbviewer.controller.ui.fragments.ChannelPager;
import org.dvbviewer.controller.ui.fragments.ChannelPager.onGroupTypeCHangedListener;

import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.MenuItem;
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
public class ChannelListActivity extends BaseActivity implements LoaderCallbacks<Cursor>, onGroupTypeCHangedListener {

	private DrawerLayout			mDrawerLayout;
	private ListView				mDrawerList;
	private boolean					showFavs;
	private DVBViewerPreferences	prefs;
	private SimpleCursorAdapter		drawerAdapter;
	private final String			pagerFragmentTag	= ChannelPager.class.getSimpleName();
	private ChannelPager			pager;
	private ActionBarDrawerToggle	mDrawerToggle;
	private int	mGroupType;

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
		mDrawerLayout.setDrawerShadow(android.R.color.white, GravityCompat.RELATIVE_LAYOUT_DIRECTION);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		mDrawerList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView parent, View view, int position, long id) {
				mDrawerLayout.closeDrawers();
				pager.setPosition(position);
			}
		});
		
		mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_navigation_drawer,  /* nav drawer icon to replace 'Up' caret */
                R.string.stream_hours_hint,  /* "open drawer" description */
                R.string.stream_hours_hint  /* "close drawer" description */
                ) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
            }
        };
        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
		drawerAdapter = new SimpleCursorAdapter(getApplicationContext(), R.layout.simple_list_item_1, null, new String[] { GroupTbl.NAME }, new int[] { android.R.id.text1 }, 0);
		mDrawerList.setAdapter(drawerAdapter);
		getSupportLoaderManager().initLoader(0, savedInstanceState, this);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
	}

	@Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
          return true;
        }
        // Handle your other action bar items...
        return super.onOptionsItemSelected(item);
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
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTypeChanged(int type) {
		showFavs = prefs.getBoolean(DVBViewerPreferences.KEY_CHANNELS_USE_FAVS, false);
		getSupportLoaderManager().restartLoader(0, getIntent().getExtras(), this);
	}

}