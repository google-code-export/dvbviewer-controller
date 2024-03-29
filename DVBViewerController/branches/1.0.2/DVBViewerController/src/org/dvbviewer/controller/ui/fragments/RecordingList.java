/*
 * Copyright � 2013 dvbviewer-controller Project
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.ClientProtocolException;
import org.dvbviewer.controller.R;
import org.dvbviewer.controller.entities.IEPG;
import org.dvbviewer.controller.entities.Recording;
import org.dvbviewer.controller.io.RecordingHandler;
import org.dvbviewer.controller.io.ServerRequest;
import org.dvbviewer.controller.ui.base.AsyncLoader;
import org.dvbviewer.controller.ui.base.BaseActivity.AsyncCallback;
import org.dvbviewer.controller.ui.base.BaseListFragment;
import org.dvbviewer.controller.ui.phone.IEpgDetailsActivity;
import org.dvbviewer.controller.ui.phone.StreamConfigActivity;
import org.dvbviewer.controller.ui.widget.CheckableLinearLayout;
import org.dvbviewer.controller.utils.ArrayListAdapter;
import org.dvbviewer.controller.utils.DateUtils;
import org.dvbviewer.controller.utils.ServerConsts;
import org.dvbviewer.controller.utils.UIUtils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * The Class RecordingList.
 *
 * @author RayBa
 * @date 07.04.2013
 */
public class RecordingList extends BaseListFragment implements AsyncCallback, LoaderCallbacks<List<Recording>>, OnClickListener, ActionMode.Callback, OnCheckedChangeListener, View.OnClickListener {

	RecordingAdapter	mAdapter;
	ActionMode			mode;
	int					selectedPosition;
	ProgressDialog		progressDialog;
	
	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAdapter = new RecordingAdapter(getActivity());
		setHasOptionsMenu(true);
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setListAdapter(mAdapter);
		getListView().setItemsCanFocus(false);
		registerForContextMenu(getListView());
		Loader<List<Recording>> loader = getLoaderManager().initLoader(0, savedInstanceState, this);
		setListShown(!(!isResumed() || loader.isStarted()));
		setEmptyText(getResources().getString(R.string.no_recordings));
		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		if (mode != null) {
			mode = getSherlockActivity().startActionMode(this);
		}
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onCreateLoader(int, android.os.Bundle)
	 */
	@Override
	public Loader<List<Recording>> onCreateLoader(int arg0, Bundle arg1) {
		AsyncLoader<List<Recording>> loader = new AsyncLoader<List<Recording>>(getActivity()) {

			@Override
			public List<Recording> loadInBackground() {
				List<Recording> result = null;
				try {
					String xml = ServerRequest.getRSString("/api/recordings.html?utf8=255");
					RecordingHandler hanler = new RecordingHandler();
					result = hanler.parse(xml);
					Collections.sort(result);
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
				return result;
			}
		};
		return loader;
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoadFinished(android.support.v4.content.Loader, java.lang.Object)
	 */
	@Override
	public void onLoadFinished(Loader<List<Recording>> arg0, List<Recording> arg1) {
		mAdapter.setItems(arg1);
		setListShown(true);
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoaderReset(android.support.v4.content.Loader)
	 */
	@Override
	public void onLoaderReset(Loader<List<Recording>> arg0) {
		if (isVisible()) {
			setListShown(true);
		}
	}

	/**
	 * The Class ViewHolder.
	 *
	 * @author RayBa
	 * @date 07.04.2013
	 */
	private class ViewHolder {
		CheckableLinearLayout	layout;
		TextView				title;
		TextView				subTitle;
		TextView				channelName;
		TextView				date;
		CheckBox				check;
		ImageView				contextMenu;
	}

	/**
	 * The Class RecordingAdapter.
	 *
	 * @author RayBa
	 * @date 07.04.2013
	 */
	public class RecordingAdapter extends ArrayListAdapter<Recording> {

		/**
		 * The Constructor.
		 *
		 * @param context the context
		 * @author RayBa
		 * @date 04.06.2010
		 * @description Instantiates a new recording adapter.
		 */
		public RecordingAdapter(Context context) {
			super();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.ArrayAdapter#getView(int, android.view.View,
		 * android.view.ViewGroup)
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				LayoutInflater vi = getActivity().getLayoutInflater();
				convertView = vi.inflate(R.layout.list_item_recording, null);
				holder = new ViewHolder();
				holder.layout = (CheckableLinearLayout) convertView;
				holder.title = (TextView) convertView.findViewById(R.id.title);
				holder.subTitle = (TextView) convertView.findViewById(R.id.subTitle);
				holder.channelName = (TextView) convertView.findViewById(R.id.channelName);
				holder.date = (TextView) convertView.findViewById(R.id.date);
				holder.check = (CheckBox) convertView.findViewById(R.id.checkIndicator);
				holder.contextMenu = (ImageView) convertView.findViewById(R.id.contextMenu);
				holder.check.setOnCheckedChangeListener(RecordingList.this);
				holder.check.setOnClickListener(RecordingList.this); 
				holder.contextMenu.setOnClickListener(RecordingList.this);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			Recording o = getItem(position);
			if (o != null) {
				holder.check.setTag(position);
				holder.layout.setChecked(getListView().isItemChecked(position));
				holder.title.setText(o.getTitle());
				if (TextUtils.isEmpty(o.getSubTitle())) {
					holder.subTitle.setVisibility(View.GONE);
				}else {
					holder.subTitle.setVisibility(View.VISIBLE);
					holder.subTitle.setText(o.getSubTitle());
				}
				holder.date.setText(DateUtils.formatDateTime(getActivity(), o.getStart().getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH));
				holder.channelName.setText(o.getChannel());
				holder.contextMenu.setTag(position);
			}
			return convertView;
		}
	}

	/* (non-Javadoc)
	 * @see org.dvbviewer.controller.ui.base.BaseListFragment#onListItemClick(android.widget.ListView, android.view.View, int, long)
	 */
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		getListView().setItemChecked(position, !getListView().isItemChecked(position));
		Intent details = new Intent(getActivity(), IEpgDetailsActivity.class);
		IEPG entry = mAdapter.getItem(position);
		details.putExtra(IEPG.class.getSimpleName(), entry);
		startActivity(details);
		if (mode == null) {
			clearSelection();
		}
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	/* (non-Javadoc)
	 * @see com.actionbarsherlock.view.ActionMode.Callback#onCreateActionMode(com.actionbarsherlock.view.ActionMode, com.actionbarsherlock.view.Menu)
	 */
	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		getSherlockActivity().getSupportMenuInflater().inflate(R.menu.actionmode_recording, menu);
		return true;
	}

	/* (non-Javadoc)
	 * @see com.actionbarsherlock.view.ActionMode.Callback#onPrepareActionMode(com.actionbarsherlock.view.ActionMode, com.actionbarsherlock.view.Menu)
	 */
	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.actionbarsherlock.view.ActionMode.Callback#onActionItemClicked(com.actionbarsherlock.view.ActionMode, com.actionbarsherlock.view.MenuItem)
	 */
	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuDelete:
			/**
			 * Alertdialog to confirm the delete of Recordings
			 */
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(getResources().getString(R.string.confirmDelete)).setPositiveButton(getResources().getString(R.string.yes), this).setNegativeButton(getResources().getString(R.string.no), this).show();
			break;

		default:
			break;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see com.actionbarsherlock.view.ActionMode.Callback#onDestroyActionMode(com.actionbarsherlock.view.ActionMode)
	 */
	@Override
	public void onDestroyActionMode(ActionMode mode) {
		clearSelection();
	}

	/**
	 * Clear selection.
	 *
	 * @author RayBa
	 * @date 07.04.2013
	 */
	private void clearSelection() {
		for (int i = 0; i < getListAdapter().getCount(); i++) {
			getListView().setItemChecked(i, false);
		}
		mAdapter.notifyDataSetChanged();
	}

	
	/* (non-Javadoc)
	 * @see android.widget.CompoundButton.OnCheckedChangeListener#onCheckedChanged(android.widget.CompoundButton, boolean)
	 */
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		getListView().setItemChecked((Integer) buttonView.getTag(), isChecked);
		int count = getCheckedItemCount();
		if (mode == null && count > 0) {
			mode = getSherlockActivity().startActionMode(RecordingList.this);
		} else if (count <= 0) {
			if (mode != null) {
				mode.finish();
				mode = null;
			}
		}
		if (mode != null) {
			mode.setTitle(count + " " + getResources().getString(R.string.selected));
		}
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		getActivity().getMenuInflater().inflate(R.menu.context_menu_recordinglist, menu);
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuStream:
			if (item.getMenuInfo() != null) {
				AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
				selectedPosition = info.position;
			}
			
			
			if (UIUtils.isTablet(getActivity())) {
				StreamConfig cfg = StreamConfig.newInstance();
				Bundle arguments = new Bundle();
				arguments.putInt(StreamConfig.EXTRA_FILE_ID, (int) mAdapter.getItem(selectedPosition).getId());
				arguments.putInt(StreamConfig.EXTRA_FILE_TYPE, StreamConfig.FILE_TYPE_RECORDING);
				arguments.putInt(StreamConfig.EXTRA_DIALOG_TITLE_RES, R.string.streamConfig);
				cfg.setArguments(arguments);
				cfg.show(getSherlockActivity().getSupportFragmentManager(), StreamConfig.class.getName());
			} else {
				Intent streamConfig = new Intent(getActivity(), StreamConfigActivity.class);
				streamConfig.putExtra(StreamConfig.EXTRA_FILE_ID, (int) mAdapter.getItem(selectedPosition).getId());
				streamConfig.putExtra(StreamConfig.EXTRA_FILE_TYPE, StreamConfig.FILE_TYPE_RECORDING);
				startActivity(streamConfig);
			}
			
			return true;

		default:
			break;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see com.actionbarsherlock.app.SherlockFragment#onCreateOptionsMenu(android.view.Menu, android.view.MenuInflater)
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.recording_list, menu);
	}

	/* (non-Javadoc)
	 * @see com.actionbarsherlock.app.SherlockFragment#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case R.id.menuRefresh:
			refresh();
			return true;

		default:
			return false;
		}
	}

	/**
	 * Refresh.
	 *
	 * @author RayBa
	 * @date 07.04.2013
	 */
	private void refresh() {
		getLoaderManager().restartLoader(0, getArguments(), this);
		setListShown(false);
	}

	/**
	 * The Class RecordingDeleter.
	 *
	 * @author RayBa
	 * @date 07.04.2013
	 */
	public static class RecordingDeleter extends AsyncTask<Recording, Void, Boolean> {

		AsyncCallback	callback;

		/**
		 * Instantiates a new recording deleter.
		 *
		 * @param callback the callback
		 * @author RayBa
		 * @date 07.04.2013
		 */
		public RecordingDeleter(AsyncCallback callback) {
			this.callback = callback;
		}
		
		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			callback.onAsyncActionStart();
		}

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				callback.onAsyncActionStop();
			}

		}

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected Boolean doInBackground(Recording... params) {
			int count = params.length;
			if (count <= 0) {
				return false;
			}
			for (int i = 0; i < count; i++) {
				try {
					ServerRequest.getRSString(ServerConsts.URL_DELETE_RECORDING + params[i].getId());
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (URISyntaxException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (AuthenticationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return true;
		}

	}

	/* (non-Javadoc)
	 * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		case DialogInterface.BUTTON_POSITIVE:
			Log.i(RecordingList.class.getSimpleName(), "onClick");
			SparseBooleanArray checkedPositions = getListView().getCheckedItemPositions();
			Log.i(RecordingList.class.getSimpleName(), "items selected: " + checkedPositions.size());
			if (checkedPositions != null && checkedPositions.size() > 0) {

				int size = checkedPositions.size();
				RecordingDeleter deleter = new RecordingDeleter(this);
				List<Recording> recordings = new ArrayList<Recording>();
				for (int i = 0; i < size; i++) {
					if (checkedPositions.valueAt(i)) {
						recordings.add(mAdapter.getItem(checkedPositions.keyAt(i)));
					}
				}
				Recording[] array = new Recording[recordings.size()];
				deleter.execute(recordings.toArray(array));
			}
			mode.finish();
			break;

		case DialogInterface.BUTTON_NEGATIVE:
			// No button clicked
			break;
		}
	}

	/* (non-Javadoc)
	 * @see org.dvbviewer.controller.ui.base.BaseListFragment#onDestroyView()
	 */
	@Override
	public void onDestroyView() {
		if (mode != null) {
			mode.finish();
		}
		super.onDestroyView();
	}

	/* (non-Javadoc)
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

	/* (non-Javadoc)
	 * @see org.dvbviewer.controller.ui.base.BaseActivity.AsyncCallback#onAsyncActionStart()
	 */
	@Override
	public void onAsyncActionStart() {
		progressDialog = new ProgressDialog(getActivity());
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setMessage(getResources().getString(R.string.busyDeleteRecordings));
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(false);
		progressDialog.show();
	}

	/* (non-Javadoc)
	 * @see org.dvbviewer.controller.ui.base.BaseActivity.AsyncCallback#onAsyncActionStop()
	 */
	@Override
	public void onAsyncActionStop() {
		progressDialog.dismiss();
		refresh();
	}

}
