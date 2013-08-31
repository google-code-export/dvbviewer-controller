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
package org.dvbviewer.controller.ui.fragments;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.dvbviewer.controller.R;
import org.dvbviewer.controller.data.DbConsts.ChannelTbl;
import org.dvbviewer.controller.data.DbConsts.EpgTbl;
import org.dvbviewer.controller.data.DbConsts.FavTbl;
import org.dvbviewer.controller.entities.Channel;
import org.dvbviewer.controller.entities.DVBViewerPreferences;
import org.dvbviewer.controller.entities.Timer;
import org.dvbviewer.controller.io.ResultReceiver.Receiver;
import org.dvbviewer.controller.io.ServerRequest.DVBViewerCommand;
import org.dvbviewer.controller.io.ServerRequest.RecordingServiceGet;
import org.dvbviewer.controller.service.SyncService;
import org.dvbviewer.controller.ui.base.BaseListFragment;
import org.dvbviewer.controller.ui.phone.StreamConfigActivity;
import org.dvbviewer.controller.ui.phone.TimerDetailsActivity;
import org.dvbviewer.controller.ui.tablet.ChannelListMultiActivity;
import org.dvbviewer.controller.ui.widget.CheckableLinearLayout;
import org.dvbviewer.controller.utils.DateUtils;
import org.dvbviewer.controller.utils.ServerConsts;
import org.dvbviewer.controller.utils.UIUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.rayba.imagecache.ImageCacher;

/**
 * The Class ChannelList.
 *
 * @author RayBa
 * @date 05.07.2012
 */
public class ChannelList extends BaseListFragment implements LoaderCallbacks<Cursor>, OnClickListener, Receiver {

	public static final String	KEY_SELECTED_POSITION		= "SELECTED_POSITION";
	public static final String	KEY_HAS_OPTIONMENU			= "HAS_OPTIONMENU";
	public static final String	KEY_GROUP_ID				= "GROUP_ID";
	ChannelAdapter				mAdapter;
	int							selectedPosition			= -1;
	boolean						hasOptionsMenu				= true;
	boolean						showFavs;
	public static final int		LOADER_CHANNELLIST			= 101;
	OnChannelSelectedListener	mCHannelSelectedListener;
	View						selectView;
	Context						mContext;
	private long				mGroupId						= -1;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = getActivity().getApplicationContext();

		showFavs = getArguments().getBoolean(DVBViewerPreferences.KEY_CHANNELS_USE_FAVS, false);
		mAdapter = new ChannelAdapter(getActivity());
		if (savedInstanceState == null) {
			if (getArguments() != null) {
				if (getArguments().containsKey(ChannelList.KEY_HAS_OPTIONMENU)) {
					hasOptionsMenu = getArguments().getBoolean(KEY_HAS_OPTIONMENU);
				}
				selectedPosition = getActivity().getIntent().getIntExtra(KEY_SELECTED_POSITION, selectedPosition);
				if (getArguments().containsKey(ChannelList.KEY_GROUP_ID)) {
					mGroupId = getArguments().getLong(KEY_GROUP_ID);
				}
			}
		}else {
			if (savedInstanceState.containsKey(KEY_SELECTED_POSITION)) {
				selectedPosition = savedInstanceState.getInt(KEY_SELECTED_POSITION);
				mGroupId = getArguments().getLong(KEY_GROUP_ID);
			}
		}
		
		setHasOptionsMenu(hasOptionsMenu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.actionbarsherlock.app.SherlockListFragment#onAttach(android.app.Activity
	 * )
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.i(ChannelList.class.getSimpleName(), "onAttach");
		if (activity instanceof OnChannelSelectedListener) {
			mCHannelSelectedListener = (OnChannelSelectedListener) activity;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setListAdapter(mAdapter);
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		registerForContextMenu(getListView());
		int loaderId = LOADER_CHANNELLIST;
//		/**
//		 * Pr�fung ob das EPG in der Senderliste angezeigt werden soll.
//		 */
//		if (!Config.CHANNELS_SYNCED) {
//			loaderId = LOADER_REFRESH_CHANNELLIST;
//		} else if ((showNowPlaying && !showNowPlayingWifi) || (showNowPlaying && showNowPlayingWifi && mNetworkInfo.isConnected())) {
//			loaderId = LOADER_EPG;
//		}
		setEmptyText(showFavs ? getResources().getString(R.string.no_favourites) : getResources().getString(R.string.no_channels));
		Loader<Cursor> loader = getLoaderManager().initLoader(loaderId, savedInstanceState, this);
		setListShown(!(!isResumed() || loader.isStarted()));
//		setSelection(selectedPosition);
	}
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.support.v4.app.LoaderManager.LoaderCallbacks#onCreateLoader(int,
	 * android.os.Bundle)
	 */
	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
		Loader<Cursor> loader = null;
		switch (loaderId) {
		case LOADER_CHANNELLIST:
			StringBuffer selection = new StringBuffer(showFavs ? ChannelTbl.FLAGS + " & " + Channel.FLAG_FAV + "!= 0" : new StringBuffer());
			String orderBy = null;
			if (mGroupId > 0) {
				if (showFavs) {
					selection.append(" and ");
					selection.append(FavTbl.FAV_GROUP_ID + " = "+mGroupId);
				}else {
					selection.append(ChannelTbl.GROUP_ID + " = "+mGroupId);
				}
			}
			orderBy = showFavs ? ChannelTbl.FAV_POSITION : ChannelTbl.POSITION;
			loader = new CursorLoader(getActivity(), ChannelTbl.CONTENT_URI_NOW, null, selection.toString(), null, orderBy);
			break;
		default:
			break;
		}
		return loader;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.support.v4.app.LoaderManager.LoaderCallbacks#onLoadFinished(android
	 * .support.v4.content.Loader, java.lang.Object)
	 */
	@SuppressLint("NewApi")
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mAdapter.swapCursor(cursor);
		if (selectedPosition != ListView.INVALID_POSITION) {
			getListView().setItemChecked(selectedPosition, true);
		}
		setListShown(true);
		getSherlockActivity().invalidateOptionsMenu();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.support.v4.app.LoaderManager.LoaderCallbacks#onLoaderReset(android
	 * .support.v4.content.Loader)
	 */
	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		if (isVisible()) {
			setListShown(true);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.support.v4.app.ListFragment#onCreateView(android.view.LayoutInflater
	 * , android.view.ViewGroup, android.os.Bundle)
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// View v = getActivity().getLayoutInflater().inflate(R.layout.list,
		// container);
		return super.onCreateView(inflater, container, savedInstanceState);
	}

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
	 * android.support.v4.app.Fragment#onCreateContextMenu(android.view.ContextMenu
	 * , android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		getActivity().getMenuInflater().inflate(R.menu.context_menu_channellist, menu);
		menu.findItem(R.id.menuSwitch).setVisible(URLUtil.isValidUrl(ServerConsts.DVBVIEWER_URL));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.actionbarsherlock.app.SherlockListFragment#onOptionsItemSelected(
	 * android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case R.id.menuChannelList:
		case R.id.menuFavourties:
			showFavs = !showFavs;
			refresh(LOADER_CHANNELLIST);
			return true;

		default:
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.support.v4.app.Fragment#onContextItemSelected(android.view.MenuItem
	 * )
	 */
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getMenuInfo() != null) {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			selectedPosition = info.position;
		}
		Cursor c = mAdapter.getCursor();
		c.moveToPosition(selectedPosition);
		Channel chan = cursorToChannel(c);
		Timer timer;
		switch (item.getItemId()) {
		case R.id.menuTimer:
			timer = cursorToTimer(c);
			if (UIUtils.isTablet(getActivity())) {
				TimerDetails timerdetails = TimerDetails.newInstance();
				Bundle args = new Bundle();
				args.putString(TimerDetails.EXTRA_TITLE, timer.getTitle());
				args.putString(TimerDetails.EXTRA_CHANNEL_NAME, timer.getChannelName());
				args.putLong(TimerDetails.EXTRA_CHANNEL_ID, timer.getChannelId());
				args.putLong(TimerDetails.EXTRA_START, timer.getStart().getTime());
				args.putLong(TimerDetails.EXTRA_END, timer.getEnd().getTime());
				args.putInt(TimerDetails.EXTRA_ACTION, timer.getTimerAction());
				timerdetails.setArguments(args);
				timerdetails.show(getSherlockActivity().getSupportFragmentManager(), TimerDetails.class.getName());
			} else {
				Intent timerIntent = new Intent(getActivity(), TimerDetailsActivity.class);
				timerIntent.putExtra(TimerDetails.EXTRA_TITLE, timer.getTitle());
				timerIntent.putExtra(TimerDetails.EXTRA_CHANNEL_NAME, timer.getChannelName());
				timerIntent.putExtra(TimerDetails.EXTRA_CHANNEL_ID, timer.getChannelId());
				timerIntent.putExtra(TimerDetails.EXTRA_START, timer.getStart().getTime());
				timerIntent.putExtra(TimerDetails.EXTRA_END, timer.getEnd().getTime());
				timerIntent.putExtra(TimerDetails.EXTRA_ACTION, timer.getTimerAction());
				startActivity(timerIntent);
			}
			return true;
		case R.id.menuStream:
			if (UIUtils.isTablet(getActivity())) {
				StreamConfig cfg = StreamConfig.newInstance();
				Bundle arguments = new Bundle();
				arguments.putInt(StreamConfig.EXTRA_FILE_ID, chan.getPosition());
				arguments.putInt(StreamConfig.EXTRA_FILE_TYPE, StreamConfig.FILE_TYPE_LIVE);
				arguments.putInt(StreamConfig.EXTRA_DIALOG_TITLE_RES, R.string.streamConfig);
				cfg.setArguments(arguments);
				cfg.show(getSherlockActivity().getSupportFragmentManager(), StreamConfig.class.getName());
			} else {
				Intent streamConfig = new Intent(getActivity(), StreamConfigActivity.class);
				streamConfig.putExtra(StreamConfig.EXTRA_FILE_ID, chan.getPosition());
				streamConfig.putExtra(StreamConfig.EXTRA_FILE_TYPE, StreamConfig.FILE_TYPE_LIVE);
				streamConfig.putExtra(StreamConfig.EXTRA_DIALOG_TITLE_RES, R.string.streamConfig);
				startActivity(streamConfig);
			}
			return true;
		case R.id.menuSwitch:
			String switchRequest = ServerConsts.URL_SWITCH_COMMAND+chan.getPosition();
			DVBViewerCommand command = new DVBViewerCommand(switchRequest);
			Thread exexuterTHread = new Thread(command);
			exexuterTHread.start();
			return true;
		case R.id.menuRecord:
			timer = cursorToTimer(c);
			String url = timer.getId() <= 0l ? ServerConsts.URL_TIMER_CREATE : ServerConsts.URL_TIMER_EDIT;
			String title = timer.getTitle();
			String days = String.valueOf(DateUtils.getDaysSinceDelphiNull(timer.getStart()));
			String start = String.valueOf(DateUtils.getMinutesOfDay(timer.getStart()));
			String stop = String.valueOf(DateUtils.getMinutesOfDay(timer.getEnd()));
			String endAction = String.valueOf(timer.getTimerAction());
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("ch", String.valueOf(timer.getChannelId())));
			params.add(new BasicNameValuePair("dor", days));
			params.add(new BasicNameValuePair("encoding", "255"));
			params.add(new BasicNameValuePair("enable", "1"));
			params.add(new BasicNameValuePair("start", start));
			params.add(new BasicNameValuePair("stop", stop));
			params.add(new BasicNameValuePair("title", title));
			params.add(new BasicNameValuePair("endact", endAction));
			if (timer.getId() > 0) {
				params.add(new BasicNameValuePair("id", String.valueOf(timer.getId())));
			}

			String query = URLEncodedUtils.format(params, "utf-8");
			String request = url + query;
			RecordingServiceGet rsGet = new RecordingServiceGet(request);
			Thread executionThread = new Thread(rsGet);
			executionThread.start();
			return true;

		default:
			break;
		}
		return false;
	}

	/**
	 * Refresh.
	 *
	 * @param id the id
	 * @author RayBa
	 * @date 05.07.2012
	 */
	public void refresh(long  groupId) {
		if (mGroupId != -1 && mGroupId != groupId) {
			mGroupId = groupId;
			getLoaderManager().restartLoader(LOADER_CHANNELLIST, getArguments(), this);
			setListShown(false);
		}
	}

	/**
	 * The Class ViewHolder.
	 *
	 * @author RayBa
	 * @date 05.07.2012
	 */
	private class ViewHolder {
		CheckableLinearLayout	v;
		ImageView				icon;
		TextView				position;
		TextView				channelName;
		TextView				epgTime;
		ProgressBar				progress;
		TextView				epgTitle;
		ImageView				contextMenu;
	}

	/**
	 * The Class ChannelAdapter.
	 *
	 * @author RayBa
	 * @date 05.07.2012
	 */
	public class ChannelAdapter extends CursorAdapter {

		Context		mContext;
		ImageCacher	imageChacher;

		/**
		 * Instantiates a new channel adapter.
		 *
		 * @param context the context
		 * @author RayBa
		 * @date 05.07.2012
		 */
		public ChannelAdapter(Context context) {
			super(context, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
			mContext = context;
			imageChacher = ImageCacher.getInstance(mContext);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * android.support.v4.widget.CursorAdapter#bindView(android.view.View,
		 * android.content.Context, android.database.Cursor)
		 */
		@Override
		public void bindView(View view, Context context, Cursor c) {
			ViewHolder holder = (ViewHolder) view.getTag();
			String channelName = c.getString(c.getColumnIndex(ChannelTbl.NAME));
			String logoUrl = c.getString(c.getColumnIndex(ChannelTbl.LOGO_URL));
			String epgTitle = c.getString(c.getColumnIndex(EpgTbl.TITLE));
			long epgStart = c.getLong(c.getColumnIndex(EpgTbl.START));
			long epgEnd = c.getLong(c.getColumnIndex(EpgTbl.END));
			Integer position = c.getInt(c.getColumnIndex(ChannelTbl.POSITION));
			Integer favPosition = c.getInt(c.getColumnIndex(ChannelTbl.FAV_POSITION));
			holder.channelName.setText(channelName);
			if (TextUtils.isEmpty(epgTitle)) {
				holder.epgTime.setVisibility(View.GONE);
				holder.epgTitle.setVisibility(View.GONE);
				holder.progress.setVisibility(View.GONE);
			} else {
				holder.epgTitle.setVisibility(View.VISIBLE);
				holder.epgTime.setVisibility(View.VISIBLE);
				holder.progress.setVisibility(View.VISIBLE);
				String start = DateUtils.formatDateTime(context, epgStart, DateUtils.FORMAT_SHOW_TIME);
				String end = DateUtils.formatDateTime(context, epgEnd, DateUtils.FORMAT_SHOW_TIME);
				float timeAll = epgEnd - epgStart;
				float timeNow = new Date().getTime() - epgStart;
				float progress = timeNow / timeAll;
				holder.progress.setProgress((int) (progress * 100));
				holder.epgTime.setText(start + " - " + end);
				holder.epgTitle.setText(epgTitle);
			}
			holder.position.setText(!showFavs ? position.toString() : favPosition.toString());
			holder.contextMenu.setTag(!showFavs ? position : favPosition - 1);
			holder.v.setChecked(getListView().isItemChecked(c.getPosition()));
			
			if (!TextUtils.isEmpty(logoUrl)) {
				StringBuffer url = new StringBuffer(ServerConsts.REC_SERVICE_URL);
				url.append("/");
				url.append(logoUrl);
				imageChacher.getImage(holder.icon, url.toString(), null, true);
			}else{
				holder.icon.setImageBitmap(null);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * android.support.v4.widget.CursorAdapter#newView(android.content.Context
		 * , android.database.Cursor, android.view.ViewGroup)
		 */
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			LayoutInflater vi = getActivity().getLayoutInflater();
			ViewHolder holder = new ViewHolder();
			View view = vi.inflate(R.layout.list_row_channel, null);
			holder.v = (CheckableLinearLayout) view;
			holder.icon = (ImageView) view.findViewById(R.id.icon);
			holder.position = (TextView) view.findViewById(R.id.position);
			holder.channelName = (TextView) view.findViewById(R.id.title);
			holder.epgTime = (TextView) view.findViewById(R.id.epgTime);
			holder.progress = (ProgressBar) view.findViewById(R.id.progress);
			holder.epgTitle = (TextView) view.findViewById(R.id.epgTitle);
			holder.contextMenu = (ImageView) view.findViewById(R.id.contextMenu);
			holder.contextMenu.setOnClickListener(ChannelList.this);
			view.setTag(holder);
			return view;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.support.v4.app.ListFragment#onListItemClick(android.widget.ListView
	 * , android.view.View, int, long)
	 */
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if (mCHannelSelectedListener != null) {
			selectedPosition = position;
			Cursor c = mAdapter.getCursor();
			c.moveToPosition(position);
			Channel chan = cursorToChannel(c);
			ArrayList<Channel> chans = cursorToChannellist(position);
			mCHannelSelectedListener.channelSelected(chans, chan, position);
			getListView().setItemChecked(position, true);
		} else {
			Intent epgPagerIntent = new Intent(getActivity(), org.dvbviewer.controller.ui.phone.EpgPagerActivity.class);
			// long[] feedIds = new long[data.getCount()];

			ArrayList<Channel> chans = cursorToChannellist(position);

			epgPagerIntent.putParcelableArrayListExtra(Channel.class.getName(), chans);
			epgPagerIntent.putExtra("position", position);
			startActivity(epgPagerIntent);
			selectedPosition = ListView.INVALID_POSITION;
		}
	}
	
	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onResume()
	 */
	@Override
	public void onResume() {
		super.onResume();
		Log.i(ChannelList.class.getSimpleName(), "onResume");
		if (!UIUtils.isTablet(getSherlockActivity())) {
			clearSelection();
		}
	}

	/**
	 * Cursor to channellist.
	 *
	 * @param position the position
	 * @return the array list�
	 * @author RayBa
	 * @date 07.04.2013
	 */
	private ArrayList<Channel> cursorToChannellist(int position) {
		Cursor c = (Cursor) mAdapter.getItem(position);
		ArrayList<Channel> chans = new ArrayList<Channel>();
		c.moveToPosition(-1);
		while (c.moveToNext()) {
			Channel channel = new Channel();
			channel.setId(c.getLong(c.getColumnIndex(ChannelTbl._ID)));
			channel.setEpgID(c.getLong(c.getColumnIndex(ChannelTbl.EPG_ID)));
			channel.setLogoUrl(c.getString(c.getColumnIndex(ChannelTbl.LOGO_URL)));
			String name = c.getString(c.getColumnIndex(ChannelTbl.NAME));
			channel.setName(name);
			channel.setPosition(c.getInt(c.getColumnIndex(ChannelTbl.POSITION)));
			chans.add(channel);
		}
		return chans;
	}

	/**
	 * Clears the selection of a ListView.
	 *
	 * @author RayBa
	 * @date 05.07.2012
	 */
	private void clearSelection() {
		for (int i = 0; i < getListAdapter().getCount(); i++) {
			getListView().setItemChecked(i, false);
		}
//		mAdapter.notifyDataSetChanged();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.support.v4.app.Fragment#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_SELECTED_POSITION, selectedPosition);
		outState.putLong(KEY_GROUP_ID, mGroupId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.contextMenu:
			selectedPosition = (Integer) v.getTag();
			getListView().showContextMenu();
			break;

		default:
			break;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dvbviewer.controller.io.ResultReceiver.Receiver#onReceiveResult(int,
	 * android.os.Bundle)
	 */
	@Override
	public void onReceiveResult(int resultCode, Bundle resultData) {
		switch (resultCode) {
		case SyncService.STATUS_RUNNING:
			setListShown(false);
			break;
		case SyncService.STATUS_ERROR:
			setListShown(false);
			break;
		case SyncService.STATUS_FINISHED:
			refresh(LOADER_CHANNELLIST);
			break;

		default:
			break;
		}

	}

	/**
	 * Reads the current cursorposition to a Channel.
	 *
	 * @param c the c
	 * @return the Channel
	 * @author RayBa
	 * @date 13.05.2012
	 */
	private Channel cursorToChannel(Cursor c) {
		Channel channel = new Channel();
		channel.setId(c.getLong(c.getColumnIndex(ChannelTbl._ID)));
		channel.setEpgID(c.getLong(c.getColumnIndex(ChannelTbl.EPG_ID)));
		String name = c.getString(c.getColumnIndex(ChannelTbl.NAME));
		channel.setName(name);
		channel.setPosition(c.getInt(c.getColumnIndex(ChannelTbl.POSITION)));
		return channel;
	}
	
	/**
	 * Cursor to timer.
	 *
	 * @param c the c
	 * @return the timer�
	 * @author RayBa
	 * @date 07.04.2013
	 */
	private Timer cursorToTimer(Cursor c) {
		String name = c.getString(c.getColumnIndex(ChannelTbl.NAME));
		long channelID = c.getLong(c.getColumnIndex(ChannelTbl._ID));
		String epgTitle = !c.isNull(c.getColumnIndex(EpgTbl.TITLE)) ? c.getString(c.getColumnIndex(EpgTbl.TITLE)) : name;
		long epgStart = c.getLong(c.getColumnIndex(EpgTbl.START));
		long epgEnd =  c.getLong(c.getColumnIndex(EpgTbl.END));
		DVBViewerPreferences prefs = new DVBViewerPreferences(getSherlockActivity());
		int epgBefore = prefs.getPrefs().getInt(DVBViewerPreferences.KEY_TIMER_TIME_BEFORE, 5);
		int epgAfter = prefs.getPrefs().getInt(DVBViewerPreferences.KEY_TIMER_TIME_AFTER, 5);
		Date start = epgStart > 0 ? new Date(epgStart) : new Date();
		Date end = epgEnd > 0 ? new Date(epgEnd) : new Date(start.getTime()+(1000*60*120));
		Log.i(ChannelList.class.getSimpleName(), "start: "+start.toString());
		Log.i(ChannelList.class.getSimpleName(), "end: "+end.toString());
		start = DateUtils.addMinutes(start, 0 - epgBefore);
		end = DateUtils.addMinutes(end, epgAfter);
		Timer timer = new Timer();
		timer.setTitle(epgTitle);
		timer.setChannelId(channelID);
		timer.setChannelName(name);
		timer.setStart(start);
		timer.setEnd(end);
		timer.setTimerAction(prefs.getPrefs().getInt(DVBViewerPreferences.KEY_TIMER_DEF_AFTER_RECORD, 0));
		return timer;
	}

	/**
	 * The listener interface for receiving onChannelSelected events.
	 * The class that is interested in processing a onChannelSelected
	 * event implements this interface, and the object created
	 * with that class is registered with a component using the
	 * component's <code>addOnChannelSelectedListener<code> method. When
	 * the onChannelSelected event occurs, that object's appropriate
	 * method is invoked.
	 *
	 * @see OnChannelSelectedEvent
	 * @author RayBa
	 * @date 05.07.2012
	 */
	public static interface OnChannelSelectedListener {

		/**
		 * Channel selected.
		 *
		 * @param chans the chans
		 * @param chan the chan
		 * @param position the position
		 * @author RayBa
		 * @date 05.07.2012
		 */
		public void channelSelected(List<Channel> chans, Channel chan, int position);

	}

	/**
	 * Sets the selected position.
	 *
	 * @param selectedPosition the new selected position
	 * @author RayBa
	 * @date 05.07.2012
	 */
	public void setSelectedPosition(int selectedPosition) {
		this.selectedPosition = selectedPosition;
	}

	/* (non-Javadoc)
	 * @see org.dvbviewer.controller.ui.base.BaseListFragment#setSelection(int)
	 */
	@Override
	public void setSelection(int position) {
		clearSelection();
		getListView().setItemChecked(position, true);
		setSelectedPosition(position);
		super.setSelection(position);
	}

	/**
	 * Checks if is show favs.
	 *
	 * @return true, if is show favs
	 * @author RayBa
	 * @date 07.04.2013
	 */
	public boolean isShowFavs() {
		return showFavs;
	}

}