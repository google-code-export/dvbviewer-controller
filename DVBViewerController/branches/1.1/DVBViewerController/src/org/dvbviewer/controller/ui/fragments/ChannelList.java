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
import org.dvbviewer.controller.ui.phone.EpgPagerActivity;
import org.dvbviewer.controller.ui.phone.StreamConfigActivity;
import org.dvbviewer.controller.ui.phone.TimerDetailsActivity;
import org.dvbviewer.controller.utils.DateUtils;
import org.dvbviewer.controller.utils.ServerConsts;
import org.dvbviewer.controller.utils.UIUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
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
public class ChannelList extends BaseListFragment implements LoaderCallbacks<Cursor>, OnClickListener, Receiver, OnMenuItemClickListener {

	public static final String	KEY_SELECTED_POSITION	= "SELECTED_POSITION";
	public static final String	KEY_HAS_OPTIONMENU		= "HAS_OPTIONMENU";
	public static final String	KEY_GROUP_ID			= "GROUP_ID";
	public static final String	KEY_SHOWFAVS			= "SHOWFAVS";
	public static final String	KEY_SEARCH_QUERY		= "SEARCHQUERY";
	ChannelAdapter				mAdapter;
	int							selectedPosition		= -1;
	boolean						hasOptionsMenu			= true;
	boolean						showFavs				= false;
	public static final int		LOADER_CHANNELLIST		= 101;
	OnChannelSelectedListener	mCHannelSelectedListener;
	View						selectView;
	Context						mContext;
	private long				mGroupId				= -1;


	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = getActivity().getApplicationContext();

		if (savedInstanceState == null && getArguments() != null) {
			if (getArguments().containsKey(ChannelList.KEY_HAS_OPTIONMENU)) {
				hasOptionsMenu = getArguments().getBoolean(KEY_HAS_OPTIONMENU);
			}
			selectedPosition = getActivity().getIntent().getIntExtra(KEY_SELECTED_POSITION, selectedPosition);
			if (getArguments().containsKey(ChannelList.KEY_GROUP_ID)) {
				mGroupId = getArguments().getLong(KEY_GROUP_ID);
			}
			showFavs = getArguments().getBoolean(DVBViewerPreferences.KEY_CHANNELS_USE_FAVS);
		} else {
			selectedPosition = savedInstanceState.getInt(KEY_SELECTED_POSITION, -1);
			mGroupId = savedInstanceState.getLong(KEY_GROUP_ID, -1);
			showFavs = savedInstanceState.getBoolean(DVBViewerPreferences.KEY_CHANNELS_USE_FAVS, false);
		}
		mAdapter = new ChannelAdapter(mContext);
		setHasOptionsMenu(false);
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
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		setListAdapter(mAdapter);
		setEmptyText(showFavs ? getResources().getString(R.string.no_favourites) : getResources().getString(R.string.no_channels));
		Loader<Cursor> loader = getLoaderManager().initLoader((int) LOADER_CHANNELLIST, savedInstanceState, this);
		setListShown(!(!isResumed() || loader.isStarted()));
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
		loader = new CursorLoader(getActivity().getApplicationContext(), ChannelTbl.CONTENT_URI_NOW, null, selection.toString(), null, orderBy);
		return loader;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.support.v4.app.LoaderManager.LoaderCallbacks#onLoadFinished(android
	 * .support.v4.content.Loader, java.lang.Object)
	 */
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (cursor != null && cursor.getCount() > 0) {
			mAdapter.changeCursor(cursor);
			if (selectedPosition != ListView.INVALID_POSITION) {
				getListView().setItemChecked(selectedPosition, true);
			}
		}
		setListShown(true);
		getActivity().supportInvalidateOptionsMenu();
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
		mAdapter = new ChannelAdapter(mContext);
		// mAdapter.swapCursor(null);
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



	/**
	 * Refresh.
	 *
	 * @param id the id
	 * @author RayBa
	 * @date 05.07.2012
	 */
	public void refresh(int loaderId) {
		getLoaderManager().restartLoader(LOADER_CHANNELLIST, getArguments(), this);

		setListShown(false);
	}

	/**
	 * The Class ViewHolder.
	 *
	 * @author RayBa
	 * @date 05.07.2012
	 */
	private class ViewHolder {
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
			holder.contextMenu.setTag(c.getPosition());

			if (!TextUtils.isEmpty(logoUrl)) {
				StringBuffer url = new StringBuffer(ServerConsts.REC_SERVICE_URL);
				url.append("/");
				url.append(logoUrl);
				imageChacher.getImage(holder.icon, url.toString(), null, true);
			} else {
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

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public boolean hasStableIds() {
			return true;
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
			ArrayList<Channel> chans = cursorToChannellist();
			mCHannelSelectedListener.channelSelected(chans, position);
			getListView().setItemChecked(position, true);
		} else {
			Intent epgPagerIntent = new Intent(getActivity(), EpgPagerActivity.class);
			ArrayList<Channel> chans = cursorToChannellist();
			EpgPagerActivity.channels = chans;
			epgPagerIntent.putExtra(EpgPager.KEY_POSITION, position);
			startActivity(epgPagerIntent);
			selectedPosition = ListView.INVALID_POSITION;
		}
	}

	/**
	 * Cursor to channellist.
	 *
	 * @param position the position
	 * @return the array list´
	 * @author RayBa
	 * @date 07.04.2013
	 */
	private ArrayList<Channel> cursorToChannellist() {
		Cursor c = (Cursor) mAdapter.getCursor();
		ArrayList<Channel> chans = new ArrayList<Channel>();
		c.moveToPosition(-1);
		while (c.moveToNext()) {
			Channel channel = cursorToChannel(c);
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
		// mAdapter.notifyDataSetChanged();
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
		selectedPosition = (Integer) v.getTag();
		PopupMenu popup = new PopupMenu(getActivity(), v);
		popup.getMenuInflater().inflate(R.menu.context_menu_channellist, popup.getMenu());
		popup.setOnMenuItemClickListener(this);
		popup.show();
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
		channel.setChannelID(c.getLong(c.getColumnIndex(ChannelTbl.CHANNEL_ID)));
		channel.setEpgID(c.getLong(c.getColumnIndex(ChannelTbl.EPG_ID)));
		channel.setName(c.getString(c.getColumnIndex(ChannelTbl.NAME)));
		channel.setPosition(c.getInt(c.getColumnIndex(ChannelTbl.POSITION)));
		channel.setLogoUrl(c.getString(c.getColumnIndex(ChannelTbl.LOGO_URL)));
		return channel;
	}

	/**
	 * Cursor to timer.
	 *
	 * @param c the c
	 * @return the timer´
	 * @author RayBa
	 * @date 07.04.2013
	 */
	private Timer cursorToTimer(Cursor c) {
		String name = c.getString(c.getColumnIndex(ChannelTbl.NAME));
		long channelID = c.getLong(c.getColumnIndex(ChannelTbl.CHANNEL_ID));
		String epgTitle = !c.isNull(c.getColumnIndex(EpgTbl.TITLE)) ? c.getString(c.getColumnIndex(EpgTbl.TITLE)) : name;
		long epgStart = c.getLong(c.getColumnIndex(EpgTbl.START));
		long epgEnd = c.getLong(c.getColumnIndex(EpgTbl.END));
		DVBViewerPreferences prefs = new DVBViewerPreferences(getActivity());
		int epgBefore = prefs.getPrefs().getInt(DVBViewerPreferences.KEY_TIMER_TIME_BEFORE, 5);
		int epgAfter = prefs.getPrefs().getInt(DVBViewerPreferences.KEY_TIMER_TIME_AFTER, 5);
		Date start = epgStart > 0 ? new Date(epgStart) : new Date();
		Date end = epgEnd > 0 ? new Date(epgEnd) : new Date(start.getTime() + (1000 * 60 * 120));
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
		public void channelSelected(List<Channel> chans, int position);

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

	/*
	 * (non-Javadoc)
	 * 
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

	@Override
	public boolean onMenuItemClick(MenuItem item) {
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
				args.putBoolean(TimerDetails.EXTRA_ACTIVE, true);
				timerdetails.setArguments(args);
				timerdetails.show(getActivity().getSupportFragmentManager(), TimerDetails.class.getName());
			} else {
				Intent timerIntent = new Intent(getActivity(), TimerDetailsActivity.class);
				timerIntent.putExtra(TimerDetails.EXTRA_TITLE, timer.getTitle());
				timerIntent.putExtra(TimerDetails.EXTRA_CHANNEL_NAME, timer.getChannelName());
				timerIntent.putExtra(TimerDetails.EXTRA_CHANNEL_ID, timer.getChannelId());
				timerIntent.putExtra(TimerDetails.EXTRA_START, timer.getStart().getTime());
				timerIntent.putExtra(TimerDetails.EXTRA_END, timer.getEnd().getTime());
				timerIntent.putExtra(TimerDetails.EXTRA_ACTION, timer.getTimerAction());
				timerIntent.putExtra(TimerDetails.EXTRA_ACTIVE, !timer.isFlagSet(Timer.FLAG_DISABLED));
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
				cfg.show(getActivity().getSupportFragmentManager(), StreamConfig.class.getName());
			} else {
				Intent streamConfig = new Intent(getActivity(), StreamConfigActivity.class);
				streamConfig.putExtra(StreamConfig.EXTRA_FILE_ID, chan.getPosition());
				streamConfig.putExtra(StreamConfig.EXTRA_FILE_TYPE, StreamConfig.FILE_TYPE_LIVE);
				streamConfig.putExtra(StreamConfig.EXTRA_DIALOG_TITLE_RES, R.string.streamConfig);
				startActivity(streamConfig);
			}
			return true;
		case R.id.menuSwitch:
			String switchRequest = ServerConsts.URL_SWITCH_COMMAND + chan.getPosition();
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

}
