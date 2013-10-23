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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.ClientProtocolException;
import org.dvbviewer.controller.R;
import org.dvbviewer.controller.data.DbConsts.GroupTbl;
import org.dvbviewer.controller.data.DbHelper;
import org.dvbviewer.controller.entities.Channel;
import org.dvbviewer.controller.entities.ChannelGroup;
import org.dvbviewer.controller.entities.ChannelRoot;
import org.dvbviewer.controller.entities.DVBViewerPreferences;
import org.dvbviewer.controller.entities.EpgEntry;
import org.dvbviewer.controller.entities.Status;
import org.dvbviewer.controller.io.ChannelHandler;
import org.dvbviewer.controller.io.ChannelListParser;
import org.dvbviewer.controller.io.EpgEntryHandler;
import org.dvbviewer.controller.io.FavouriteHandler;
import org.dvbviewer.controller.io.ServerRequest;
import org.dvbviewer.controller.io.StatusHandler;
import org.dvbviewer.controller.io.VersionHandler;
import org.dvbviewer.controller.ui.base.AsyncLoader;
import org.dvbviewer.controller.ui.base.BaseActivity.ErrorToastRunnable;
import org.dvbviewer.controller.ui.tablet.ChannelListMultiActivity;
import org.dvbviewer.controller.utils.Config;
import org.dvbviewer.controller.utils.NetUtils;
import org.dvbviewer.controller.utils.ServerConsts;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.actionbarsherlock.app.SherlockFragment;
import com.viewpagerindicator.TitlePageIndicator;

// TODO: Auto-generated Javadoc
/**
 * The Class EpgPager.
 *
 * @author RayBa
 * @date 07.04.2013
 */
public class ChannelPager extends SherlockFragment implements LoaderCallbacks<Cursor>{
	
	private static final String 	KEY_ADAPTER_POSITION = "KEY_ADAPTER_POSITION";

	/** The m channels. */
	List<Channel>					mChannels;
	
	/** The m position. */
	int								mPosition				= AdapterView.INVALID_POSITION;
	
	/** The m group cursor. */
	Cursor							mGroupCursor;
	
	/** The m current. */
	ChannelEpg						mCurrent;
	
	/** The m pager. */
	private ViewPager				mPager;
	
	private View					mProgress;
	
	/** The m adapter. */
	PagerAdapter					mAdapter;
	
	/** The m on page change listener. */
	private OnPageChangeListener	mOnPageChangeListener;
	
	/** The show favs. */
	private boolean					showFavs;
	private boolean					showGroups;
	private boolean					showExtraGroup;

	/** The prefs. */
	private DVBViewerPreferences	prefs;

	/** The synchronize channels. */
	private static final int		SYNCHRONIZE_CHANNELS	= 0;

	/** The load groups. */
	private static final int		LOAD_CHANNELS				= 1;

	private static final int		LOAD_CURRENT_PROGRAM				= 2;

	private TitlePageIndicator		titleIndicator;

	private NetworkInfo				mNetworkInfo;

	private boolean					showNowPlaying;

	private boolean					showNowPlayingWifi;

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

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		mAdapter = new PagerAdapter(getFragmentManager(), mGroupCursor);
		ConnectivityManager connManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
		mNetworkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		prefs = new DVBViewerPreferences(getActivity());
		showGroups = prefs.getPrefs().getBoolean(DVBViewerPreferences.KEY_CHANNELS_SHOW_GROUPS, false);
		showExtraGroup = prefs.getPrefs().getBoolean(DVBViewerPreferences.KEY_CHANNELS_SHOW_ALL_AS_GROUP, false);
		showFavs = prefs.getPrefs().getBoolean(DVBViewerPreferences.KEY_CHANNELS_USE_FAVS, false);
		showNowPlaying = prefs.getPrefs().getBoolean(DVBViewerPreferences.KEY_CHANNELS_SHOW_NOW_PLAYING, true);
		showNowPlayingWifi = prefs.getPrefs().getBoolean(DVBViewerPreferences.KEY_CHANNELS_SHOW_NOW_PLAYING_WIFI_ONLY, true);
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mPager.setAdapter(mAdapter);
		titleIndicator.setViewPager(mPager);
		titleIndicator.setOnPageChangeListener(mOnPageChangeListener);
		
		if (savedInstanceState != null) {
			Log.i(ChannelPager.class.getSimpleName(), "savedInstanceState != null");
			if (savedInstanceState.containsKey(KEY_ADAPTER_POSITION)) {
				Log.i(ChannelPager.class.getSimpleName(), "savedInstanceState.containsKey(KEY_ADAPTER_POSITION)");
				mPosition = savedInstanceState.getInt(KEY_ADAPTER_POSITION);
				Log.i(ChannelPager.class.getSimpleName(), "mPosition: "+mPosition);
			}
		}
		
		int loaderId = LOAD_CHANNELS;
		/**
		 * Prüfung ob das EPG in der Senderliste angezeigt werden soll.
		 */
//		if (!Config.CHANNELS_SYNCED) {
//			loaderId = SYNCHRONIZE_CHANNELS;
//		} else if ((showNowPlaying && !showNowPlayingWifi) || (showNowPlaying && showNowPlayingWifi && mNetworkInfo.isConnected())) {
//			loaderId = LOAD_CURRENT_PROGRAM;
//		}
		Loader<Cursor> loader = getLoaderManager().initLoader(loaderId, savedInstanceState, this);
//		showProgress(!isResumed() || loader.isStarted());
	}
	
	

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.pager, null);
		mProgress = view.findViewById(android.R.id.progress);
		mPager = (ViewPager) view.findViewById(R.id.pager);
		titleIndicator = (TitlePageIndicator)view.findViewById(R.id.titles);
		titleIndicator.setVisibility(showGroups ? View.VISIBLE : View.GONE);
		return view;
	}

	/* (non-Javadoc)
	 * @see com.actionbarsherlock.app.SherlockFragment#onCreateOptionsMenu(android.view.Menu, android.view.MenuInflater)
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.actionbarsherlock.app.SherlockListFragment#onCreateOptionsMenu(android
	 * .view.Menu, android.view.MenuInflater)
	 */
	@Override
	public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.channel_list, menu);
		for (int i = 0; i < menu.size(); i++) {
			if (menu.getItem(i).getItemId() == R.id.menuChannelList) {
				menu.getItem(i).setVisible(showFavs);
			} else if (menu.getItem(i).getItemId() == R.id.menuFavourties) {
				menu.getItem(i).setVisible(!showFavs);
			}
		}
		menu.findItem(R.id.menuChannelList).setVisible(showFavs);
		menu.findItem(R.id.menuFavourties).setVisible(!showFavs);
		if (getSherlockActivity() instanceof ChannelListMultiActivity) {
			menu.findItem(R.id.menu_refresh_now_playing).setVisible(false);
			menu.findItem(R.id.menuRefreshChannels).setVisible(false);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.actionbarsherlock.app.SherlockListFragment#onOptionsItemSelected(
	 * android.view.MenuItem)
	 */
	@SuppressLint("NewApi")
	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case R.id.menu_refresh_now_playing:
			refresh(LOAD_CHANNELS);
			return true;
		case R.id.menuRefreshChannels:
			refresh(SYNCHRONIZE_CHANNELS);
			return true;
		case R.id.menuChannelList:
		case R.id.menuFavourties:
			showFavs = !showFavs;
			persistChannelConfigConfig();
			refresh(LOAD_CHANNELS);
			getSherlockActivity().invalidateOptionsMenu();
			return true;

		default:
			return false;
		}
	}
	
	/**
	 * Persist channel config config.
	 *
	 * @author RayBa
	 * @date 25.08.2013
	 */
	public void persistChannelConfigConfig() {
		Editor editor = prefs.getPrefs().edit();
		editor.putBoolean(DVBViewerPreferences.KEY_CHANNELS_USE_FAVS, showFavs);
		editor.commit();
		super.onPause();
	}

	/**
	 * The Class PagerAdapter.
	 *
	 * @author RayBa
	 * @date 07.04.2013
	 */
	class PagerAdapter extends FragmentStatePagerAdapter {
		
		private Cursor mCursor;
		

		/**
		 * Instantiates a new pager adapter.
		 *
		 * @param fm the fm
		 * @author RayBa
		 * @date 07.04.2013
		 */
		public PagerAdapter(FragmentManager fm, Cursor cursor) {
			super(fm);
			mCursor = cursor;
		}

		/* (non-Javadoc)
		 * @see android.support.v4.app.FragmentPagerAdapter#getItem(int)
		 */
		@Override
		public Fragment getItem(int position) {
			long groupId = -1;
			if (showGroups) {
				if (showExtraGroup) {
					mCursor.moveToPosition(position-1);
					groupId = position > 0 ? mCursor.getLong(mCursor.getColumnIndex(GroupTbl._ID)) : -1;
				}else {
					mCursor.moveToPosition(position);
					groupId = mCursor.getLong(mCursor.getColumnIndex(GroupTbl._ID));
				}
			}
			ChannelList channelList = (ChannelList) Fragment.instantiate(getActivity(), ChannelList.class.getName());
			Bundle args = new Bundle();
			args.putLong(ChannelList.KEY_GROUP_ID, groupId);
			args.putBoolean(DVBViewerPreferences.KEY_CHANNELS_USE_FAVS, showFavs);
			args.putBoolean(ChannelList.KEY_HAS_OPTIONMENU, false);
			channelList.setArguments(args);
			return channelList;
		}
		
		@Override
		public int getItemPosition(Object object) {
			return POSITION_NONE;
		}

		/* (non-Javadoc)
		 * @see android.support.v4.view.PagerAdapter#getCount()
		 */
		@Override
		public int getCount() {
			if (mCursor != null) {
				if (showGroups) {
					if (showExtraGroup) {
						return mCursor.getCount()+1;
					}else {
						return mCursor.getCount();
					}
				}else {
					return 1;
				}
			}
			return 0;
		}
		
		@Override
		public CharSequence getPageTitle(int position) {
			String title = getString(R.string.common_all);
			if (showExtraGroup) {
				mCursor.moveToPosition(position - 1);
				if (position > 0) {
					title = mCursor.getString(mCursor.getColumnIndex(GroupTbl.NAME));
					return title;
				}
			}else {
				mCursor.moveToPosition(position);
				title = mCursor.getString(mCursor.getColumnIndex(GroupTbl.NAME));
				
			}
			return title;
		}

		public void setCursor(Cursor cursor) {
			this.mCursor = cursor;
			notifyDataSetChanged();
		}
		
		

	}
	
	/**
	 * Sets the position.
	 *
	 * @param position the new position
	 * @author RayBa
	 * @date 07.04.2013
	 */
	public void setPosition(int position){
		mPosition = position;
		if (mPager != null) {
			mPager.setCurrentItem(mPosition);
		}
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onCreateLoader(int, android.os.Bundle)
	 */
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
		Loader<Cursor> loader = null;
		switch (id) {
		case SYNCHRONIZE_CHANNELS:
			loader = new AsyncLoader<Cursor>(getActivity()) {
				
				@Override
				public Cursor loadInBackground() {

					try {
						/**
						 * Request the Status.xml to get some default Recordingservice Configs
						 */
						String statusXml = ServerRequest.getRSString(ServerConsts.URL_STATUS);
						StatusHandler statusHandler = new StatusHandler();
						Status s = statusHandler.parse(statusXml);
						String version = getVersionString();
					
						DbHelper mDbHelper = new DbHelper(getActivity());
						/**
						 * Request the Channels
						 */
						if (Config.isOldRsVersion(version)) {
							byte[] rawData = ServerRequest.getRsBytes(ServerConsts.URL_CHANNELS_OLD);
							List<Channel> chans = ChannelListParser.parseChannelList(getContext(), rawData);
							mDbHelper.saveChannels(chans);
						}else {
							String chanXml = ServerRequest.getRSString(ServerConsts.URL_CHANNELS);
							ChannelHandler channelHandler = new ChannelHandler();
							List<ChannelRoot> rootElements = channelHandler.parse(chanXml);
							mDbHelper.saveChannelRoots(rootElements);
//							List<Channel> chans = new ArrayList<Channel>();
//							for (ChannelRoot channelRoot : rootElements) {
//								for (ChannelGroup group : channelRoot.getGroups()) {
//									chans.addAll(group.getChannels());
//								}
//							}
//							mDbHelper.saveChannels(chans);
						}
						
						/**
						 * Request the Favourites
						 */
						String favXml = ServerRequest.getRSString(ServerConsts.URL_FAVS);
						if (!TextUtils.isEmpty(favXml)) {
							FavouriteHandler handler = new FavouriteHandler();
							List<ChannelGroup> favGroups = handler.parse(getActivity(), favXml);
							mDbHelper.saveFavGroups(favGroups);
//							List<Fav> favs = handler.parse(getActivity(), favXml);
//							mDbHelper.saveFavs(favs);
						}

						mDbHelper.close();
						
						/**
						 * Get the Mac Address for WOL
						 */
						String macAddress = NetUtils.getMacFromArpCache(ServerConsts.REC_SERVICE_HOST);
						/**
						 * Save the data in sharedpreferences
						 */
						Editor prefEditor = prefs.getPrefs().edit();
						if (s != null) {
							prefEditor.putInt(DVBViewerPreferences.KEY_TIMER_TIME_BEFORE, s.getEpgBefore());
							prefEditor.putInt(DVBViewerPreferences.KEY_TIMER_TIME_AFTER, s.getEpgAfter());
							prefEditor.putInt(DVBViewerPreferences.KEY_TIMER_DEF_AFTER_RECORD, s.getDefAfterRecord());
						}
						prefEditor.putString(DVBViewerPreferences.KEY_RS_MAC_ADDRESS, macAddress);
						prefEditor.putBoolean(DVBViewerPreferences.KEY_CHANNELS_SYNCED, true);
						prefEditor.commit();
						Config.CHANNELS_SYNCED = true;
					} catch (AuthenticationException e) {
						Log.e(ChannelEpg.class.getSimpleName(), "AuthenticationException");
						e.printStackTrace();
						showToast(getString(R.string.error_invalid_credentials));
					} catch (ParseException e) {
						Log.e(ChannelEpg.class.getSimpleName(), "ParseException");
						e.printStackTrace();
					} catch (ClientProtocolException e) {
						Log.e(ChannelEpg.class.getSimpleName(), "ClientProtocolException");
						e.printStackTrace();
					} catch (IOException e) {
						Log.e(ChannelEpg.class.getSimpleName(), "IOException");
						e.printStackTrace();
					} catch (URISyntaxException e) {
						Log.e(ChannelEpg.class.getSimpleName(), "URISyntaxException");
						e.printStackTrace();
						showToast(getString(R.string.error_invalid_url) + "\n\n" + ServerConsts.REC_SERVICE_URL);
					} catch (IllegalStateException e) {
						Log.e(ChannelEpg.class.getSimpleName(), "IllegalStateException");
						e.printStackTrace();
						showToast(getString(R.string.error_invalid_url) + "\n\n" + ServerConsts.REC_SERVICE_URL);
					} catch (IllegalArgumentException e) {
						Log.e(ChannelEpg.class.getSimpleName(), "IllegalArgumentException");
						showToast(getString(R.string.error_invalid_url) + "\n\n" + ServerConsts.REC_SERVICE_URL);
					} catch (Exception e) {
						Log.e(ChannelEpg.class.getSimpleName(), "Exception");
						e.printStackTrace();
					}
					return null;
				}

				private String getVersionString() throws Exception {
					String version = null;
					try {
						String versionXml = ServerRequest.getRSString(ServerConsts.URL_VERSION);
						VersionHandler versionHandler = new VersionHandler();
						version = versionHandler.parse(versionXml);
//						here is a regex required! 
						version = version.replace("DVBViewer Recording Service ", "");
						String[] arr = version.split(" ");
						version = arr[0];
//						Pattern pattern = Pattern.compile("(\\d([\\d.]*))\\s+\\(");
//						Matcher matcher = pattern.matcher(version);
//						matcher.find();
//						version = matcher.group();
						
					} catch (Exception e) {
						e.printStackTrace();
					}
					return version;
				}


			};
			break;
		case LOAD_CHANNELS:
//			if (showGroups) {
					String selection = showFavs  ? GroupTbl.TYPE + " = " + 1 : GroupTbl.TYPE + " = " + 0;
//			}else {
//				
//			}
			String orderBy = GroupTbl._ID;
			loader = new CursorLoader(getActivity(), GroupTbl.CONTENT_URI, null, selection, null, orderBy);
			break;
		case LOAD_CURRENT_PROGRAM:
			loader = new AsyncLoader<Cursor>(getActivity()) {

				@Override
				public Cursor loadInBackground() {
					List<EpgEntry> result = null;
					String nowFloat = org.dvbviewer.controller.utils.DateUtils.getFloatDate(new Date());
					String url = ServerConsts.URL_EPG + "&start=" + nowFloat + "&end=" + nowFloat;
					try {
						EpgEntryHandler handler = new EpgEntryHandler();
						String xml = ServerRequest.getRSString(url);
						result = handler.parse(xml);
						DbHelper helper = new DbHelper(getContext());
						helper.saveNowPlaying(result);
					} catch (AuthenticationException e) {
						Log.e(ChannelEpg.class.getSimpleName(), "AuthenticationException");
						e.printStackTrace();
						showToast(getString(R.string.error_invalid_credentials));
					} catch (ParseException e) {
						Log.e(ChannelEpg.class.getSimpleName(), "ParseException");
						e.printStackTrace();
					} catch (ClientProtocolException e) {
						Log.e(ChannelEpg.class.getSimpleName(), "ClientProtocolException");
						e.printStackTrace();
					} catch (IOException e) {
						Log.e(ChannelEpg.class.getSimpleName(), "IOException");
						e.printStackTrace();
					} catch (URISyntaxException e) {
						Log.e(ChannelEpg.class.getSimpleName(), "URISyntaxException");
						e.printStackTrace();
						showToast(getString(R.string.error_invalid_url) + "\n\n" + ServerConsts.REC_SERVICE_URL);
					} catch (IllegalStateException e) {
						Log.e(ChannelEpg.class.getSimpleName(), "IllegalStateException");
						e.printStackTrace();
						showToast(getString(R.string.error_invalid_url) + "\n\n" + ServerConsts.REC_SERVICE_URL);
					} catch (IllegalArgumentException e) {
						Log.e(ChannelEpg.class.getSimpleName(), "IllegalArgumentException");
						showToast(getString(R.string.error_invalid_url) + "\n\n" + ServerConsts.REC_SERVICE_URL);
					} catch (Exception e) {
						Log.e(ChannelEpg.class.getSimpleName(), "Exception");
						e.printStackTrace();
					}
					return null;
				}


			};
			break;
		default:
			break;
		}
		return loader;
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoadFinished(android.support.v4.content.Loader, java.lang.Object)
	 */
	@SuppressLint("NewApi")
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		switch (loader.getId()) {
		case LOAD_CURRENT_PROGRAM:
			refresh(LOAD_CHANNELS);
			break;
		case SYNCHRONIZE_CHANNELS:
			/**
			 * Prüfung ob das EPG in der Senderliste angezeigt werden soll.
			 */
			if ((showNowPlaying && !showNowPlayingWifi) || (showNowPlaying && showNowPlayingWifi && mNetworkInfo.isConnected())) {
				refresh(LOAD_CURRENT_PROGRAM);
			}else {
				refresh(LOAD_CHANNELS);
			}
			break;
		case LOAD_CHANNELS:
			Log.i(ChannelPager.class.getSimpleName(), "onLoadFinished");
			mGroupCursor = cursor;
//			mAdapter = new PagerAdapter(getFragmentManager(), cursor)
			mAdapter.setCursor(mGroupCursor);
			mAdapter.notifyDataSetChanged();
//			mPager.setAdapter(mAdapter);
			mPager.setCurrentItem(mPosition);
			getSherlockActivity().invalidateOptionsMenu();
			showProgress(false);
			break;

		default:
			showProgress(false);
			break;
		}
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoaderReset(android.support.v4.content.Loader)
	 */
	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_ADAPTER_POSITION, mPager.getCurrentItem());
	}
	
	/**
	 * Show toast.
	 *
	 * @param message the message
	 * @author RayBa
	 * @date 07.04.2013
	 */
	protected void showToast(String message) {
		if (getSherlockActivity() != null) {
			ErrorToastRunnable errorRunnable = new ErrorToastRunnable(getSherlockActivity(), message);
			getSherlockActivity().runOnUiThread(errorRunnable);
		}
	}
	
	/**
	 * Refresh.
	 *
	 * @param id the id
	 * @author RayBa
	 * @date 05.07.2012
	 */
	public void refresh(int id) {
		mGroupCursor = null;
		mPager.setAdapter(null);
		mAdapter.notifyDataSetChanged();
		mAdapter = new PagerAdapter(getFragmentManager(), mGroupCursor);
		mPager.setAdapter(mAdapter);
		titleIndicator.notifyDataSetChanged();
		getLoaderManager().destroyLoader(id);
		getLoaderManager().restartLoader(id, getArguments(), this);
	}
	
	private void showProgress(boolean show) {
		mProgress.setVisibility(show ? View.VISIBLE : View.GONE);
		mPager.setVisibility(show ? View.GONE : View.VISIBLE);
		if (showGroups) {
			titleIndicator.setVisibility(show ? View.GONE : View.VISIBLE);
		}

	}

}
