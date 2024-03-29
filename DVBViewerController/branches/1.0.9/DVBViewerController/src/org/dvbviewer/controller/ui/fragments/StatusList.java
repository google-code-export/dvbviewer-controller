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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.dvbviewer.controller.R;
import org.dvbviewer.controller.entities.Status;
import org.dvbviewer.controller.entities.Status.Folder;
import org.dvbviewer.controller.entities.Status.StatusItem;
import org.dvbviewer.controller.io.ServerRequest;
import org.dvbviewer.controller.io.data.StatusHandler;
import org.dvbviewer.controller.io.data.VersionHandler;
import org.dvbviewer.controller.ui.base.AsyncLoader;
import org.dvbviewer.controller.ui.base.BaseListFragment;
import org.dvbviewer.controller.utils.ArrayListAdapter;
import org.dvbviewer.controller.utils.CategoryAdapter;
import org.dvbviewer.controller.utils.FileUtils;
import org.dvbviewer.controller.utils.ServerConsts;
import org.xml.sax.SAXException;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import ch.boye.httpclientandroidlib.ParseException;
import ch.boye.httpclientandroidlib.auth.AuthenticationException;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.conn.ConnectTimeoutException;

/**
 * The Class StatusList.
 *
 * @author RayBa
 * @date 05.07.2012
 */
public class StatusList extends BaseListFragment implements LoaderCallbacks<Status> {

	CategoryAdapter mAdapter;
	ProgressDialog	progressDialog;
	Resources mRes;
	private StatusAdapter	mStatusAdapter;

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		setRetainInstance(true);
		mStatusAdapter = new StatusAdapter(getActivity());
		mAdapter = new CategoryAdapter(getActivity());
	} 

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mRes = getResources();
		Loader<Status> loader = getLoaderManager().initLoader(0, savedInstanceState, this);
		setListShown(!(!isResumed() || loader.isStarted()));
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.ListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onCreateLoader(int, android.os.Bundle)
	 */
	@Override
	public Loader<Status> onCreateLoader(int arg0, Bundle arg1) {
		AsyncLoader<Status> loader = new AsyncLoader<Status>(getActivity().getApplicationContext()) {

			@Override
			public Status loadInBackground() {
				Status result = null;
				try {
					String statusXml = ServerRequest.getRSString(ServerConsts.URL_STATUS);
					StatusHandler statusHandler = new StatusHandler();
					result = statusHandler.parse(statusXml);
					String versionXml = ServerRequest.getRSString(ServerConsts.URL_VERSION);
					VersionHandler versionHandler = new VersionHandler();
					String version = versionHandler.parse(versionXml);
					StatusItem versionItem = new StatusItem();
					versionItem.setNameRessource(R.string.status_rs_version);
					versionItem.setValue(version);
					result.getItems().add(0, versionItem);
				} catch (AuthenticationException e) {
					e.printStackTrace();
					showToast(getStringSafely(R.string.error_invalid_credentials));
				} catch (UnknownHostException e) {
					e.printStackTrace();
					showToast(getStringSafely(R.string.error_unknonwn_host) + "\n\n" + ServerConsts.REC_SERVICE_URL);
				} catch (ConnectTimeoutException e) {
					e.printStackTrace();
					showToast(getStringSafely(R.string.error_connection_timeout));
				} catch (SAXException e) {
					e.printStackTrace();
					showToast(getStringSafely(R.string.error_parsing_xml));
				} catch (ParseException e) {
					e.printStackTrace();
					showToast(getStringSafely(R.string.error_common) + "\n\n" + e.getMessage());
				} catch (ClientProtocolException e) {
					e.printStackTrace();
					showToast(getStringSafely(R.string.error_common) + "\n\n" + e.getMessage());
				} catch (IOException e) {
					e.printStackTrace();
					showToast(getStringSafely(R.string.error_common) + "\n\n" + e.getMessage());
				} catch (URISyntaxException e) {
					e.printStackTrace();
					showToast(getStringSafely(R.string.error_invalid_url) + "\n\n" + ServerConsts.REC_SERVICE_URL);
				} catch (IllegalStateException e) {
					e.printStackTrace();
					showToast(getStringSafely(R.string.error_invalid_url) + "\n\n" + ServerConsts.REC_SERVICE_URL);
				} catch (IllegalArgumentException e) {
					showToast(getStringSafely(R.string.error_invalid_url) + "\n\n" + ServerConsts.REC_SERVICE_URL);
				} catch (Exception e) {
					e.printStackTrace();
					showToast(getStringSafely(R.string.error_common) + "\n\n" + e.getMessage());
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
	public void onLoadFinished(Loader<Status> loader, Status status) {
		if (status != null) {
			mStatusAdapter.setItems(status.getItems());
			FolderAdapter folderAdapter = new FolderAdapter(getActivity());
			folderAdapter.setItems(status.getFolders());
			mAdapter.addSection("Status", mStatusAdapter);
			mAdapter.addSection("Aufnahmeordner", folderAdapter);
			mAdapter.notifyDataSetChanged();
		}
		setListAdapter(mAdapter);
		setListShown(true);
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoaderReset(android.support.v4.content.Loader)
	 */
	@Override
	public void onLoaderReset(Loader<Status> arg0) {
		if (isVisible()) {
			setListShown(true);
		}
	}

	/**
	 * The Class StatusHolder.
	 *
	 * @author RayBa
	 * @date 05.07.2012
	 */
	private class StatusHolder {
		TextView	title;
		TextView	statusText;
		TextView	free;
		TextView	size;
	}


	/**
	 * The Class FolderAdapter.
	 *
	 * @author RayBa
	 * @date 05.07.2012
	 */
	public class FolderAdapter extends ArrayListAdapter<Folder> {

		/**
		 * Instantiates a new folder adapter.
		 *
		 * @param context the context
		 * @author RayBa
		 * @date 05.07.2012
		 */
		public FolderAdapter(Context context) {
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
			StatusHolder holder;
			if (convertView == null) {
				LayoutInflater vi = getActivity().getLayoutInflater();
				convertView = vi.inflate(R.layout.list_item_status, parent, false);
				holder = new StatusHolder();
				holder.title = (TextView) convertView.findViewById(R.id.title);
				holder.statusText = (TextView) convertView.findViewById(R.id.statusText);
				holder.size = (TextView) convertView.findViewById(R.id.size);
				holder.free = (TextView) convertView.findViewById(R.id.free);
				convertView.setTag(holder);
			} else {
				holder = (StatusHolder) convertView.getTag();
			}
			holder.title.setText(mItems.get(position).getPath());
			holder.statusText.setVisibility(View.GONE);
			holder.size.setVisibility(View.VISIBLE);
			holder.free.setVisibility(View.VISIBLE);
			holder.size.setText(mRes.getString(R.string.status_folder_total)+mRes.getString(R.string.common_colon)+FileUtils.byteToHumanString(mItems.get(position).getSize()));
			holder.free.setText(mRes.getString(R.string.status_folder_free)+mRes.getString(R.string.common_colon)+FileUtils.byteToHumanString(mItems.get(position).getFree()));
		super.getViewTypeCount();	
			return convertView;
		}
		
		
		
	}

	/**
	 * The Class StatusAdapter.
	 *
	 * @author RayBa
	 * @date 05.07.2012
	 */
	public class StatusAdapter extends ArrayListAdapter<StatusItem> {
		
		/**
		 * Instantiates a new status adapter.
		 *
		 * @param context the context
		 * @author RayBa
		 * @date 05.07.2012
		 */
		public StatusAdapter(Context context) {
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
			StatusHolder holder;
			if (convertView == null || !(convertView.getTag() instanceof StatusHolder)) {
				LayoutInflater vi = getActivity().getLayoutInflater();
				convertView = vi.inflate(R.layout.list_item_status, parent, false);
				holder = new StatusHolder();
				holder.title = (TextView) convertView.findViewById(R.id.title);
				holder.statusText = (TextView) convertView.findViewById(R.id.statusText);
				holder.size = (TextView) convertView.findViewById(R.id.size);
				holder.free = (TextView) convertView.findViewById(R.id.free);
				convertView.setTag(holder);
			} else {
				holder = (StatusHolder) convertView.getTag();
			}
			holder.title.setVisibility(View.VISIBLE);
			holder.title.setText(getResources().getString(mItems.get(position).getNameRessource()));
			holder.statusText.setVisibility(View.VISIBLE);
			switch (mItems.get(position).getNameRessource()) {
			case R.string.status_epg_update_running:
				holder.statusText.setText(Integer.valueOf(mItems.get(position).getValue()) == 0 ? R.string.no : R.string.yes);
				break;
			case R.string.status_epg_before:
				holder.statusText.setText(mItems.get(position).getValue()+" "+mRes.getString(R.string.minutes));
				break;
			case R.string.status_epg_after:
				holder.statusText.setText(mItems.get(position).getValue()+" "+mRes.getString(R.string.minutes));
				break;
			case R.string.status_timezone:
				int timezone = Integer.valueOf(mItems.get(position).getValue()) /60;
				holder.statusText.setText(mRes.getString(R.string.gmt) + (timezone > 0 ? " +" : "")+timezone);
				break;
			case R.string.status_def_after_record:
				holder.statusText.setText(mRes.getStringArray(R.array.postRecoridngActions)[Integer.valueOf(mItems.get(position).getValue())]);
				break;
			default:
				holder.statusText.setText(mItems.get(position).getValue());
				break;
			}
			holder.size.setVisibility(View.GONE);
			holder.free.setVisibility(View.GONE);
			return convertView;
		}
		
		
		
	}

	/**
	 * Refresh.
	 *
	 * @author RayBa
	 * @date 05.07.2012
	 */
	private void refresh() {
		getLoaderManager().restartLoader(0, getArguments(), this);
		setListShown(false);
	}

}
