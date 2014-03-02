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

import java.util.Date;
import java.util.List;

import org.dvbviewer.controller.R;
import org.dvbviewer.controller.data.DbConsts.ChannelTbl;
import org.dvbviewer.controller.data.DbConsts.GroupTbl;
import org.dvbviewer.controller.entities.Channel;
import org.dvbviewer.controller.entities.ChannelGroup;
import org.dvbviewer.controller.entities.DVBViewerPreferences;
import org.dvbviewer.controller.io.imageloader.AnimationLoadingListener;
import org.dvbviewer.controller.ui.base.BaseActivity;
import org.dvbviewer.controller.ui.base.DrawerActivity;
import org.dvbviewer.controller.ui.fragments.ChannelEpg;
import org.dvbviewer.controller.ui.fragments.ChannelEpg.EpgDateInfo;
import org.dvbviewer.controller.ui.fragments.EpgPager;
import org.dvbviewer.controller.utils.ServerConsts;

import com.nostra13.universalimageloader.core.ImageLoader;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * The Class EpgPagerActivity.
 *
 * @author RayBa
 * @date 07.04.2013
 */
public class EpgPagerActivity extends DrawerActivity implements EpgDateInfo, LoaderCallbacks<Cursor> {

	/** The epg date. */
	Date							epgDate;
	
	/** The prefs. */
	private DVBViewerPreferences	prefs;
	
	/** The show favs. */
	private boolean					showFavs;
	
	/** The drawer adapter. */
	private SimpleCursorAdapter		drawerAdapter;
	
	/** The pager fragment tag. */
	private final String			pagerFragmentTag	= EpgPagerActivity.class.getSimpleName() + EpgPager.class.getSimpleName();
	
	/** The pager. */
	private EpgPager				pager;
	
	/** The channels. */
	public static List<Channel>		channels;
	
	/** The drawer position. */
	public static int				drawerPosition;

	/* (non-Javadoc)
	 * @see org.dvbviewer.controller.ui.base.DrawerActivity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		prefs = new DVBViewerPreferences(this);
		showFavs = prefs.getPrefs().getBoolean(DVBViewerPreferences.KEY_CHANNELS_USE_FAVS, false);
		epgDate = savedInstanceState != null && savedInstanceState.containsKey(ChannelEpg.KEY_EPG_DAY) ? new Date(savedInstanceState.getLong(ChannelEpg.KEY_EPG_DAY)) : new Date();
		drawerAdapter = new SimpleCursorAdapter(getApplicationContext(), R.layout.list_item_group, null, new String[] { GroupTbl.NAME }, new int[] { android.R.id.text1 }, 0);
		mDrawerList.setAdapter(drawerAdapter);
		getSupportLoaderManager().initLoader(0, savedInstanceState, this);
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putLong(ChannelEpg.KEY_EPG_DAY, epgDate.getTime());
		super.onSaveInstanceState(outState);
	}

	/* (non-Javadoc)
	 * @see org.dvbviewer.controller.ui.fragments.ChannelEpg.EpgDateInfo#setEpgDate(java.util.Date)
	 */
	@Override
	public void setEpgDate(Date epgDate) {
		this.epgDate = epgDate;
	}

	/* (non-Javadoc)
	 * @see org.dvbviewer.controller.ui.fragments.ChannelEpg.EpgDateInfo#getEpgDate()
	 */
	@Override
	public Date getEpgDate() {
		return epgDate;
	}

	/* (non-Javadoc)
	 * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Cursor c = drawerAdapter.getCursor();
		c.moveToPosition(position);
		long groupId = c.getLong(c.getColumnIndex(GroupTbl._ID));
		mDrawerLayout.closeDrawers();
		pager.setGroupId(groupId);
		pager.refresh();
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onCreateLoader(int, android.os.Bundle)
	 */
	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		String selection = showFavs ? GroupTbl.TYPE + " = " + ChannelGroup.TYPE_FAV : GroupTbl.TYPE + " = " + ChannelGroup.TYPE_CHAN;
		String orderBy = GroupTbl._ID;
		CursorLoader loader = new CursorLoader(getApplicationContext(), GroupTbl.CONTENT_URI, null, selection, null, orderBy);
		return loader;
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoadFinished(android.support.v4.content.Loader, java.lang.Object)
	 */
	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
		drawerAdapter.swapCursor(arg1);
		Fragment f = getSupportFragmentManager().findFragmentByTag(pagerFragmentTag);
		if (f == null) {
			FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
			pager = new EpgPager();
			pager.setArguments(BaseActivity.intentToFragmentArguments(getIntent()));
			tran.add(R.id.content_frame, pager, pagerFragmentTag);
			tran.commitAllowingStateLoss();
		} else {
			pager = (EpgPager) f;
		}
		mDrawerList.setItemChecked(ChannelListActivity.GROUP_INDEX, true);
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoaderReset(android.support.v4.content.Loader)
	 */
	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// TODO Auto-generated method stub

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
	
	public class ChannelAdapter extends CursorAdapter {

		Context		mContext;
		ImageLoader	imageChacher;

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
			imageChacher = ImageLoader.getInstance();
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
			imageChacher.cancelDisplayTask(holder.icon);
			String channelName = c.getString(c.getColumnIndex(ChannelTbl.NAME));
			String logoUrl = c.getString(c.getColumnIndex(ChannelTbl.LOGO_URL));
			Integer position = c.getInt(c.getColumnIndex(ChannelTbl.POSITION));
			Integer favPosition = c.getInt(c.getColumnIndex(ChannelTbl.FAV_POSITION));
			holder.channelName.setText(channelName);
			holder.position.setText(!showFavs ? position.toString() : favPosition.toString());
			if (!TextUtils.isEmpty(logoUrl)) {
				StringBuffer url = new StringBuffer(ServerConsts.REC_SERVICE_URL);
				url.append("/");
				url.append(logoUrl);
				imageChacher.displayImage(url.toString(), holder.icon, new AnimationLoadingListener());
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
			LayoutInflater vi = getLayoutInflater();
			ViewHolder holder = new ViewHolder();
			View view = vi.inflate(R.layout.list_item_channel, null);
			holder.icon = (ImageView) view.findViewById(R.id.icon);
			holder.position = (TextView) view.findViewById(R.id.position);
			holder.channelName = (TextView) view.findViewById(R.id.title);
			holder.epgTime = (TextView) view.findViewById(R.id.epgTime);
			holder.progress = (ProgressBar) view.findViewById(R.id.progress);
			holder.epgTitle = (TextView) view.findViewById(R.id.epgTitle);
			holder.contextMenu = (ImageView) view.findViewById(R.id.contextMenu);
			holder.epgTime.setVisibility(View.GONE);
			holder.epgTitle.setVisibility(View.GONE);
			holder.progress.setVisibility(View.GONE);
			holder.contextMenu.setVisibility(View.GONE);
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

}
