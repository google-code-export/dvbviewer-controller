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

import java.util.Date;

import org.dvbviewer.controller.R;
import org.dvbviewer.controller.data.DbConsts.ChannelTbl;
import org.dvbviewer.controller.data.DbConsts.FavTbl;
import org.dvbviewer.controller.entities.Channel;
import org.dvbviewer.controller.entities.DVBViewerPreferences;
import org.dvbviewer.controller.ui.fragments.ChannelEpg.EpgDateInfo;
import org.dvbviewer.controller.utils.DateUtils;
import org.dvbviewer.controller.utils.UIUtils;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

/**
 * The Class EpgPager.
 *
 * @author RayBa
 * @date 07.04.2013
 */
public class EpgPager extends Fragment implements LoaderCallbacks<Cursor> {

	public static final String		KEY_CHANNELS	= EpgPager.class.getSimpleName() + "_KEY_CHANNELS";
	public static final String		KEY_POSITION	= EpgPager.class.getSimpleName() + "_KEY_POSITION";
	public static final String		KEY_GROUP_ID	= EpgPager.class.getSimpleName() + "_KEY_GROUP_ID";
	int								mPosition		= AdapterView.INVALID_POSITION;
	long							mGroupId		= AdapterView.INVALID_POSITION;
	ChannelEpg						mCurrent;
	private ViewPager				mPager;
	PagerAdapter					mAdapter;
	private OnPageChangeListener	mOnPageChangeListener;
	private boolean					showFavs		= false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.actionbarsherlock.app.SherlockFragment#onAttach(android.app.Activity)
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof OnPageChangeListener) {
			mOnPageChangeListener = (OnPageChangeListener) activity;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		showFavs = getArguments().getBoolean(DVBViewerPreferences.KEY_CHANNELS_USE_FAVS);
		mAdapter = new PagerAdapter(getChildFragmentManager());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mPosition = getArguments().containsKey(KEY_POSITION) ? getArguments().getInt(KEY_POSITION, mPosition) : mPosition;
		mGroupId = getArguments().containsKey(KEY_GROUP_ID) ? getArguments().getLong(KEY_GROUP_ID, mGroupId) : mGroupId;
		mAdapter.notifyDataSetChanged();
		mPager.setAdapter(mAdapter);
		mPager.setCurrentItem(mPosition);
		mPager.setPageMargin((int) UIUtils.dipToPixel(getActivity(), 25));
		mPager.setOnPageChangeListener(mOnPageChangeListener);
		mPager.setVisibility(View.VISIBLE);
		getLoaderManager().initLoader(0, savedInstanceState, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.Fragment#onViewCreated(android.view.View,
	 * android.os.Bundle)
	 */
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mPager = (ViewPager) view.findViewById(R.id.pager);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater,
	 * android.view.ViewGroup, android.os.Bundle)
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.pager, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.actionbarsherlock.app.SherlockFragment#onCreateOptionsMenu(android
	 * .view.Menu, android.view.MenuInflater)
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.channel_epg, menu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.actionbarsherlock.app.SherlockFragment#onOptionsItemSelected(android
	 * .view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		EpgDateInfo info = (EpgDateInfo) getActivity();
		int itemId = item.getItemId();
		switch (itemId) {
		case R.id.menuRefresh:
			break;
		case R.id.menuPrev:
			info.setEpgDate(DateUtils.substractDay(info.getEpgDate()));
			info.setEpgDate(DateUtils.substractDay(info.getEpgDate()));
		case R.id.menuNext:
			info.setEpgDate(DateUtils.addDay(info.getEpgDate()));
			break;
		case R.id.menuToday:
			info.setEpgDate(new Date());
			break;
		case R.id.menuNow:
			info.setEpgDate(DateUtils.setCurrentTime(info.getEpgDate()));
			break;
		case R.id.menuEvening:
			info.setEpgDate(DateUtils.setEveningTime(info.getEpgDate()));
			break;
		default:
			return false;
		}
		getActivity().supportInvalidateOptionsMenu();
		ChannelEpg mCurrent;
		mCurrent = (ChannelEpg) mAdapter.instantiateItem(mPager, mPager.getCurrentItem());
		mCurrent.refresh(true);
		return true;
	}

	/**
	 * The Class PagerAdapter.
	 *
	 * @author RayBa
	 * @date 07.04.2013
	 */
	class PagerAdapter extends FragmentStatePagerAdapter {
		
		private Cursor cursor;

		/**
		 * Instantiates a new pager adapter.
		 *
		 * @param fm the fm
		 * @author RayBa
		 * @date 07.04.2013
		 */
		public PagerAdapter(FragmentManager fm) {
			super(fm);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.support.v4.app.FragmentPagerAdapter#getItem(int)
		 */
		@Override
		public Fragment getItem(int position) {
			ChannelEpg channelEpg = (ChannelEpg) Fragment.instantiate(getActivity(), ChannelEpg.class.getName());
			cursor.moveToPosition(position);
			Channel chan = new Channel();
			chan.setId(cursor.getLong(cursor.getColumnIndex(ChannelTbl._ID)));
			chan.setName(cursor.getString(cursor.getColumnIndex(ChannelTbl.NAME)));
			chan.setEpgID(cursor.getLong(cursor.getColumnIndex(ChannelTbl.EPG_ID)));
			chan.setPosition(cursor.getInt(cursor.getColumnIndex(ChannelTbl.POSITION)));
			chan.setFavPosition(cursor.getInt(cursor.getColumnIndex(ChannelTbl.FAV_POSITION)));
			chan.setLogoUrl(cursor.getString(cursor.getColumnIndex(ChannelTbl.LOGO_URL)));
			channelEpg.setChannel(chan);
			return channelEpg;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.support.v4.view.PagerAdapter#getCount()
		 */
		@Override
		public int getCount() {
			if (cursor != null) {
				return cursor.getCount();
			} else {
				return 0;
			}
		}

		public void setCursor(Cursor cursor) {
			this.cursor = cursor;
		}
		
		@Override
		public int getItemPosition(Object object) {
			return POSITION_NONE;
		}

	}

	/**
	 * Sets the position.
	 *
	 * @param position the new position
	 * @author RayBa
	 * @date 07.04.2013
	 */
	public void setPosition(int position) {
		mPosition = position;
		if (mPager != null) {
			mPager.setCurrentItem(mPosition, false);
		}
	}

	public int getPosition() {
		int result = 0;
		if (mPager != null) {
			result = mPager.getCurrentItem();
		}
		return result;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		Loader<Cursor> loader = null;
		StringBuffer selection = new StringBuffer(showFavs ? ChannelTbl.FLAGS + " & " + Channel.FLAG_FAV + "!= 0" : ChannelTbl.FLAGS + " & " + Channel.FLAG_ADDITIONAL_AUDIO + "== 0");
		if (mGroupId > 0) {
			selection.append(" and ");
			if (showFavs) {
				selection.append(FavTbl.FAV_GROUP_ID + " = " + mGroupId);
			} else {
				selection.append(ChannelTbl.GROUP_ID + " = " + mGroupId);
			}
		}
		String orderBy = null;
		orderBy = showFavs ? ChannelTbl.FAV_POSITION : ChannelTbl.POSITION;
		loader = new CursorLoader(getActivity().getApplicationContext(), ChannelTbl.CONTENT_URI, null, selection.toString(), null, orderBy);
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mAdapter = new PagerAdapter(getChildFragmentManager());
		mAdapter.setCursor(cursor);
		mPager.setAdapter(mAdapter);
		mAdapter.notifyDataSetChanged();
		mPager.setCurrentItem(mPosition, false);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// TODO Auto-generated method stub

	}

	public void setGroupId(long groupId) {
		this.mGroupId = groupId;
	}
	
	public void refresh(){
		mPager.setAdapter(null);
		mAdapter.notifyDataSetChanged();
		mAdapter = new PagerAdapter(getChildFragmentManager());
		mPager.setAdapter(mAdapter);
		mPosition = 0;
		getLoaderManager().destroyLoader(0);
		getLoaderManager().restartLoader(0, getArguments(), this);
	}

}
