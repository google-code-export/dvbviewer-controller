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
package org.dvbviewer.controller.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.ClientProtocolException;
import org.dvbviewer.controller.R;
import org.dvbviewer.controller.data.DbHelper;
import org.dvbviewer.controller.entities.Channel;
import org.dvbviewer.controller.entities.Channel.Fav;
import org.dvbviewer.controller.entities.DVBViewerPreferences;
import org.dvbviewer.controller.entities.EpgEntry;
import org.dvbviewer.controller.io.ServerRequest;
import org.dvbviewer.controller.io.data.ChannelListParser;
import org.dvbviewer.controller.io.data.EpgEntryHandler;
import org.dvbviewer.controller.io.data.FavouriteHandler;
import org.dvbviewer.controller.ui.fragments.ChannelEpg;
import org.dvbviewer.controller.utils.DateUtils;
import org.dvbviewer.controller.utils.ServerConsts;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.ViewDebug.FlagToString;


/**
 * The Class SyncService.
 *
 * @author RayBa
 * @date 07.04.2013
 */
public class SyncService extends Service {

	public static final int			NOTIFICATION_SYNC_EPG		= 0;
	public static final int			NOTIFICATION_SYNC_CHANNELS	= 1;

	public static final String		ACTION_EXTRA				= SyncService.class.getName() + ".action";
	public static final int			SYNC_CHANNELS				= 101;
	public static final int			SYNC_EPG					= 102;

	public static final int			STATUS_RUNNING				= 0x1;
	public static final int			STATUS_ERROR				= 0x2;
	public static final int			STATUS_FINISHED				= 0x3;

	public static final String		EXTRA_RESULT_RECEIVER		= "result_receiver";
	private NotificationManager		mNotificationManager;

	PendingIntent					contentIntent;

	SharedPreferences				prefs;

	private volatile Looper			mServiceLooper;
	private volatile ServiceHandler	mServiceHandler;
	private int						epgStartId;
	private int						channelStartId;

	/* (non-Javadoc)
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		Log.i(SyncService.class.getSimpleName(), "onCreate");
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		DVBViewerPreferences dvbPrefs = new DVBViewerPreferences(getApplication());
		prefs = dvbPrefs.getPrefs();
		HandlerThread thread = new HandlerThread(SyncService.class.getName(), Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
	}

	/* (non-Javadoc)
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			int intentFlags = intent.getIntExtra(ACTION_EXTRA, 0);
			Message msg;
			switch (intentFlags) {
			case SYNC_CHANNELS:
				msg = mServiceHandler.obtainMessage();
				msg.what = SYNC_CHANNELS;
				msg.arg1 = startId;
				msg.arg2 = flags;
				msg.setData(intent.getExtras());
				mServiceHandler.sendMessage(msg);
				Log.i("ServiceStartArguments", "Sending: " + msg);
				break;
			case SYNC_EPG:
				msg = mServiceHandler.obtainMessage();
				msg.what = SYNC_EPG;
				msg.arg1 = startId;
				msg.arg2 = flags;
				msg.setData(intent.getExtras());
				mServiceHandler.sendMessage(msg);
				Log.i("ServiceStartArguments", "Sending: " + msg);

				// EpgSyncronizer epgSyncer = new EpgSyncronizer();
				// epgSyncer.execute(intent);
				break;
			default:
				stopSelf();
				break;
			}
		} else {
			// switch (startId) {
			// case mChannelSyncId:
			//
			// break;
			//
			// default:
			// break;
			// }
		}
		return START_REDELIVER_INTENT;
	}

	/**
	 * The Class ServiceHandler.
	 *
	 * @author RayBa
	 * @date 07.04.2013
	 */
	private final class ServiceHandler extends Handler {

		/**
		 * Instantiates a new service handler.
		 *
		 * @param looper the looper
		 * @author RayBa
		 * @date 07.04.2013
		 */
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		/* (non-Javadoc)
		 * @see android.os.Handler#handleMessage(android.os.Message)
		 */
		@Override
		public void handleMessage(Message msg) {
			Log.i("ServiceStartArguments", "Message: " + msg + ", " + msg.arg1);
			Bundle arguments = msg.getData();
			boolean redeliverd = (msg.arg2 & Service.START_FLAG_REDELIVERY) != 0;

			switch (msg.what) {
			case SYNC_EPG:
				syncEpg(msg, redeliverd);
				break;
			case SYNC_CHANNELS:
				Bundle b = msg.getData();
				final ResultReceiver receiver = b.getParcelable(EXTRA_RESULT_RECEIVER);
				try {
					if (receiver != null) {
						post(new Runnable() {

							@Override
							public void run() {
								receiver.send(STATUS_RUNNING, null);
							}
						});
					}
					byte[] bytes = ServerRequest.getRSBytes(ServerConsts.URL_CHANNELS);
					List<Channel> chans = ChannelListParser.parseChannelList(getApplicationContext(), bytes);
					DbHelper dbHelper = new DbHelper(getApplicationContext());
					dbHelper.saveChannels(chans);

					String favXml = ServerRequest.getRSString(ServerConsts.URL_FAVS);
					FavouriteHandler handler = new FavouriteHandler();
					List<Fav> favs = handler.parse(getApplicationContext(), favXml);
					dbHelper.saveFavs(favs);
				} catch (AuthenticationException e) {
					e.printStackTrace();
					stopSelf();
				} catch (ClientProtocolException e) {
					e.printStackTrace();
					stopSelf();
				} catch (URISyntaxException e) {
					e.printStackTrace();
					stopSelf();
				} catch (IOException e) {
					e.printStackTrace();
					stopSelf();
				} catch (Exception e) {
					e.printStackTrace();
					stopSelf();
				} finally {
					if (receiver != null) {
						post(new Runnable() {

							@Override
							public void run() {
								receiver.send(STATUS_FINISHED, null);
							}
						});
						stopSelf();
					}

				}
				break;

			default:
				break;
			}

			mNotificationManager.cancel(NOTIFICATION_SYNC_EPG);
			Log.i("ServiceStartArguments", "Done with #" + msg.arg1);
			stopSelf(msg.arg1);
		}

		/**
		 * Sync epg.
		 *
		 * @param msg the msg
		 * @param redeliverd the redeliverd
		 * @author RayBa
		 * @date 07.04.2013
		 */
		private void syncEpg(Message msg, boolean redeliverd) {
			DbHelper dbHelper = new DbHelper(getApplicationContext());
			List<Channel> chans = null;
			if (!redeliverd) {
//				chans = prefs.getBoolean(DVBViewerPreferences.KEY_SYNC_ONLY_FAVS, false) ? dbHelper.loadFavourites() : dbHelper.loadChannellist();
				dbHelper.markChannelsForUpdate();
			} else {
				chans = dbHelper.loadPendingUpdateChannellist();
			}
			if (chans == null || chans.size() <= 0) {
				stopSelf(msg.arg1);
			}

			String notificationTickerText = getResources().getString(R.string.epg_update_started);
			long when = System.currentTimeMillis();

			Notification notification = new Notification(R.drawable.ic_stat_notification, notificationTickerText, when);

			Context context = getApplicationContext();
			String notificationTitle = getResources().getString(R.string.epg_update_running);
			PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
			notification.setLatestEventInfo(context, notificationTitle, "", contentIntent);
			// mNotificationManager.notify(NOTIFICATION_SYNC_EPG,
			// notification);
			int daysToSync = Integer.valueOf(prefs.getString(DVBViewerPreferences.KEY_DAYS_TO_SYNC, "2"));

			// dbHelper.deleteEPG();
			startForeground(NOTIFICATION_SYNC_EPG, notification);
			for (Channel channel : chans) {
				Date d = new Date();
				dbHelper.deleteEpgForChannel(channel.getEpgID());
				for (int i = 1; i <= daysToSync; i++) {
					StringBuffer notificationInfo = new StringBuffer();
					notificationInfo.append("Updating EPG for " + channel.getName());
					notificationInfo.append("\nTag " + i + " von " + daysToSync);
					notification.setLatestEventInfo(getBaseContext(), notificationTitle, notificationInfo.toString(), contentIntent);
					mNotificationManager.notify(NOTIFICATION_SYNC_EPG, notification);
					try {
						List<EpgEntry> result = null;
						// &start=40189.05&end=40190.05
						String nowFloat = org.dvbviewer.controller.utils.DateUtils.getFloatDate(d);
						Calendar cal = GregorianCalendar.getInstance();
						cal.setTime(d);
						cal.add(Calendar.DAY_OF_MONTH, 1);
						Date tommorrow = cal.getTime();
						String tommorrowFloat = org.dvbviewer.controller.utils.DateUtils.getFloatDate(tommorrow);
						String url = "/api/epg.html?lvl=2&channel=" + channel.getEpgID() + "&start=" + nowFloat + "&end=" + tommorrowFloat;
						Log.i(ChannelEpg.class.getSimpleName(), "url: " + url);
						EpgEntryHandler handler = new EpgEntryHandler();
						String xml = ServerRequest.getRSString(url);
						result = handler.parse(xml);
						dbHelper.saveEPG(result);
						if (result != null && result.size() > 0) {
							d = result.get(result.size() - 1).getEnd();
						}
					} catch (AuthenticationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ClientProtocolException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
						stopSelf();
					} finally {
						d = DateUtils.addMinutes(d, 2);
					}
				}
				dbHelper.markChannelUpdated(channel.getId());
			}
		}

	};

	/* (non-Javadoc)
	 * @see android.app.Service#onStart(android.content.Intent, int)
	 */
	@Override
	public void onStart(Intent intent, int startId) {
		Log.i(SyncService.class.getSimpleName(), "Service Started");
		super.onStart(intent, startId);
	}

	/* (non-Javadoc)
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		Log.i(SyncService.class.getSimpleName(), "Service Destroyed");
		mServiceLooper.quit();
		mNotificationManager.cancelAll();
	}

	/* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * The Class ChannelSyncronizer.
	 *
	 * @author RayBa
	 * @date 07.04.2013
	 */
	private class ChannelSyncronizer extends AsyncTask<Intent, Channel, Void> {

		// ResultReceiver receiver
		ResultReceiver	receiver;

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if (receiver != null) {
				receiver.send(STATUS_FINISHED, null);
			}
			stopSelf();
		}

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onProgressUpdate(Progress[])
		 */
		@Override
		protected void onProgressUpdate(Channel... values) {
			// TODO Auto-generated method stub
			super.onProgressUpdate(values);
		}

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected Void doInBackground(Intent... params) {
			try {
				Intent i = params[0];
				receiver = i.getParcelableExtra(EXTRA_RESULT_RECEIVER);
				if (receiver != null) {
					receiver.send(STATUS_RUNNING, null);
				}
				byte[] bytes = ServerRequest.getRSBytes(ServerConsts.URL_CHANNELS);
				List<Channel> chans = ChannelListParser.parseChannelList(getApplicationContext(), bytes);
				DbHelper dbHelper = new DbHelper(getApplicationContext());
				dbHelper.saveChannels(chans);

				String favXml = ServerRequest.getRSString(ServerConsts.URL_FAVS);
				FavouriteHandler handler = new FavouriteHandler();
				List<Fav> favs = handler.parse(getApplicationContext(), favXml);
				dbHelper.saveFavs(favs);
			} catch (AuthenticationException e) {
				e.printStackTrace();
				stopSelf();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				stopSelf();
			} catch (URISyntaxException e) {
				e.printStackTrace();
				stopSelf();
			} catch (IOException e) {
				e.printStackTrace();
				stopSelf();
			} catch (Exception e) {
				e.printStackTrace();
				stopSelf();
			} finally {
				if (receiver != null) {
					receiver.send(STATUS_FINISHED, null);
					stopSelf();
				}

			}
			return null;
		}
	}

}
