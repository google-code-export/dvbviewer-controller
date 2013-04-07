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
package org.dvbviewer.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpPostSender;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.dvbviewer.controller.entities.DVBViewerPreferences;
import org.dvbviewer.controller.io.ServerRequest;
import org.dvbviewer.controller.utils.Config;
import org.dvbviewer.controller.utils.ServerConsts;
import org.dvbviewer.controller.utils.URLUtil;
import org.json.JSONObject;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.text.TextUtils;

/**
 * The Class App.
 *
 * @author RayBa
 * @date 11.08.2012
 */
@ReportsCrashes(formKey = "", mode = ReportingInteractionMode.TOAST, resToastText = R.string.error_sending_report)
public class App extends Application {
	

	/* (non-Javadoc)
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate() {
		/**
		 * Acra initialisation
		 */
		boolean initAcra = getResources().getBoolean(R.bool.init_acra);
		String acraUrl = getResources().getString(R.string.url_acra_error);
		if (initAcra && !TextUtils.isEmpty(acraUrl)) {
			ACRA.init(this);
			Map<ReportField, String> mapping = new HashMap<ReportField, String>();
			mapping.put(ReportField.INSTALLATION_ID, "installationId");
			mapping.put(ReportField.PACKAGE_NAME, "package");
			mapping.put(ReportField.ANDROID_VERSION, "androidVersion");
			mapping.put(ReportField.BRAND, "brand");
			mapping.put(ReportField.PHONE_MODEL, "phoneModel");
			mapping.put(ReportField.APP_VERSION_CODE, "appVerCode");
			mapping.put(ReportField.APP_VERSION_NAME, "appVerName");
			mapping.put(ReportField.STACK_TRACE, "stackTrace");
			// remove any default report sender
			ErrorReporter.getInstance().removeAllReportSenders();
			// create your own instance with your specific mapping
			ErrorReporter.getInstance().addReportSender(new HttpPostSender(acraUrl, mapping));
		}
		

		/**
		 * Read preferences
		 */
		DVBViewerPreferences prefs = new DVBViewerPreferences(this);
		Config.IS_FIRST_START = prefs.getBoolean(DVBViewerPreferences.KEY_IS_FIRST_START, true);
		Config.CHANNELS_SYNCED = prefs.getBoolean(DVBViewerPreferences.KEY_CHANNELS_SYNCED, false);
		Config.SYNC_EPG = prefs.getBoolean(DVBViewerPreferences.KEY_SYNC_EPG, false);
		ServerConsts.DVBVIEWER_URL = prefs.getString(DVBViewerPreferences.KEY_DVBV_URL, "http://");
		ServerConsts.DVBVIEWER_PORT = prefs.getString(DVBViewerPreferences.KEY_DVBV_PORT, "80");
		ServerConsts.DVBVIEWER_URL = ServerConsts.DVBVIEWER_URL+":"+ServerConsts.DVBVIEWER_PORT;
		ServerConsts.DVBVIEWER_USER_NAME = prefs.getString(DVBViewerPreferences.KEY_DVBV_USERNAME, "");
		ServerConsts.DVBVIEWER_PASSWORD = prefs.getString(DVBViewerPreferences.KEY_DVBV_PASSWORD, "");
		
		String prefUrl = prefs.getString(DVBViewerPreferences.KEY_RS_URL, "http://");
		String prefPort = prefs.getString(DVBViewerPreferences.KEY_RS_PORT, "8089");
		URLUtil.setRecordingServicesAddress(prefUrl, prefPort);
		ServerConsts.REC_SERVICE_USER_NAME = prefs.getAppSharedPrefs().getString(DVBViewerPreferences.KEY_RS_USERNAME, "");
		ServerConsts.REC_SERVICE_PASSWORD = prefs.getAppSharedPrefs().getString(DVBViewerPreferences.KEY_RS_PASSWORD, "");
		ServerConsts.REC_SERVICE_LIVE_STREAM_PORT = prefs.getAppSharedPrefs().getString(DVBViewerPreferences.KEY_RS_LIVE_STREAM_PORT, ServerConsts.REC_SERVICE_LIVE_STREAM_PORT);
		ServerConsts.REC_SERVICE_MEDIA_STREAM_PORT = prefs.getAppSharedPrefs().getString(DVBViewerPreferences.KEY_RS_MEDIA_STREAM_PORT, ServerConsts.REC_SERVICE_MEDIA_STREAM_PORT);

		super.onCreate();
		
		/**
		 * Thread to check for Expiration
		 */
		Thread t = new Thread(new ExpirationChecker(prefs.getAppSharedPrefs()));
		t.start();
		
	}

	
	/**
	 * The Class ExpirationChecker.
	 *
	 * @author RayBa
	 * @date 11.08.2012
	 */
	class ExpirationChecker implements Runnable {
		
		SharedPreferences prefs;
		
		/**
		 * Instantiates a new expiration checker.
		 *
		 * @param prefs the prefs
		 * @author RayBa
		 * @date 11.08.2012
		 */
		public ExpirationChecker(SharedPreferences prefs) {
			this.prefs = prefs;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			String urlExpireDate = getString(R.string.url_expire_date);
			if (!TextUtils.isEmpty(urlExpireDate)) {
				try {
					PackageInfo pinfo = App.this.getPackageManager().getPackageInfo(getPackageName(), 0);
					List<NameValuePair> params = new ArrayList<NameValuePair>();
					String url = getString(R.string.url_expire_date);
					params.add(new BasicNameValuePair("ver", pinfo.versionName));
					String query = URLEncodedUtils.format(params, "utf-8");
					String jsonString = ServerRequest.getString(url + query);
					JSONObject jsonObject = new JSONObject(jsonString);
					String expireDate = jsonObject.getString("date");
					String expireMessage = jsonObject.getString("message");
					Editor editor = prefs.edit();
					editor.putString(DVBViewerPreferences.KEY_EXPIRE_DATE, expireDate);
					editor.putString(DVBViewerPreferences.KEY_EXPIRE_Message, expireMessage);
					editor.commit();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
