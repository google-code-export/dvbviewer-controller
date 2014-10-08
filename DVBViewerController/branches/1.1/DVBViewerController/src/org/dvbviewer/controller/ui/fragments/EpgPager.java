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
import java.util.List;

import org.dvbviewer.controller.R;
import org.dvbviewer.controller.data.DbConsts.ChannelTbl;
import org.dvbviewer.controller.data.DbConsts.FavTbl;
import org.dvbviewer.controller.entities.Channel;
import org.dvbviewer.controller.entities.DVBViewerPreferences;
import org.dvbviewer.controller.ui.base.AsyncLoader;
import org.dvbviewer.controller.ui.fragments.ChannelEpg.EpgDateInfo;
import org.dvbviewer.controller.utils.DateUtils;
import org.dvbviewer.controller.utils.UIUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
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
public class EpgPager extends Fragment implements LoaderCallbacks<List<Channel>> {

	public static List<Channel>		CHANNELS;
	public static final String		KEY_POSITION	= EpgPager.class.getSimpleName() + "_KEY_POSITION";
	int								mPosition		= AdapterView.INVALID_POSITION;
	ChannelEpg						mCurrent;
	private ViewPager				mPager;
	PagerAdapter					mAdapter;
	private OnPageChangeListener	mOnPageChangeListener;
	private Boolean					showFavs;
	private DVBViewerPreferences	prefs;
	long							mGroupId		= AdapterView.INVALID_POSITION;

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
		prefs = new DVBViewerPreferences(getActivity());
		showFavs = prefs.getPrefs().getBoolean(DVBViewerPreferences.KEY_CHANNELS_USE_FAVS, false);
		mPosition = getArguments().containsKey("position") ? getArguments().getInt("position", mPosition) : mPosition;
		mPager.setAdapter(mAdapter);
		mPager.setCurrentItem(mPosition);
		mPager.setPageMargin((int) UIUtils.dipToPixel(getActivity(), 25));
		mPager.setOnPageChangeListener(mOnPageChangeListener);
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
		// menu.findItem(R.id.menuPrev).setEnabled(!DateUtils.isToday(epgDate.getTime()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.actionbarsherlock.app.SherlockFragment#onOptionsItemSelected(android
	 * .view.MenuItem)
	 */
	@SuppressLint("NewApi")
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

	@Override
	public Loader<List<Channel>> onCreateLoader(int arg0, Bundle arg1) {
		
		
		Loader<List<Channel>> chanLoader = new AsyncLoader<List<Channel>>(getActivity()) {

			@Override
			public List<Channel> loadInBackground() {
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
				Cursor cursor = getActivity().getContentResolver().query(ChannelTbl.CONTENT_URI, null, selection.toString(), null, orderBy);
				List<Channel> chans = ChannelList.cursorToChannellist(cursor);
				cursor.close();
				return chans;
			}
		};
		return chanLoader;
	}

	@Override
	public void onLoadFinished(Loader<List<Channel>> loader, List<Channel> chans) {
		EpgPager.CHANNELS = chans;
		mAdapter = new PagerAdapter(getChildFragmentManager());
		mPager.setAdapter(mAdapter);
		mAdapter.notifyDataSetChanged();
		Log.i(EpgPager.class.getSimpleName(), "Position:"+ mPosition);
		mPager.setCurrentItem(mPosition, false);
	}

	@Override
	public void onLoaderReset(Loader<List<Channel>> arg0) {
		// TODO Auto-generated method stub

	}

	public void setGroupId(long groupId) {
		this.mGroupId = groupId;
	}

	public void refresh() {
		refresh(0);
	}
	public void refresh(int selectedPosition) {
		mPager.setAdapter(null);
		EpgPager.CHANNELS = null;
		mAdapter.notifyDataSetChanged();
		mAdapter = new PagerAdapter(getChildFragmentManager());
		mPager.setAdapter(mAdapter);
		mPosition = selectedPosition;
		getLoaderManager().destroyLoader(0);
		getLoaderManager().restartLoader(0, getArguments(), this);
	}

	/**
	 * The Class PagerAdapter.
	 *
	 * @author RayBa
	 * @date 07.04.2013
	 */
	class PagerAdapter extends FragmentStatePagerAdapter {

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
			channelEpg.setChannel(CHANNELS.get(position));
			return channelEpg;
		}
		
		@Override
		public int getItemPosition(Object object) {
			// TODO Auto-generated method stub
			return POSITION_NONE;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.support.v4.view.PagerAdapter#getCount()
		 */
		@Override
		public int getCount() {
			return CHANNELS != null ? CHANNELS.size() : 0;
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
			mPager.setCurrentItem(mPosition);
		}
	}


}
