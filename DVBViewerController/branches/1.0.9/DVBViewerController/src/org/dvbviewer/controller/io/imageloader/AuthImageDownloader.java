/*
 * Copyright (C) 2012 dvbviewer-controller Project
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
package org.dvbviewer.controller.io.imageloader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import org.dvbviewer.controller.io.SSLUtil;
import org.dvbviewer.controller.io.ServerRequest;
import org.dvbviewer.controller.utils.ServerConsts;

import android.content.Context;
import android.util.Log;
import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.androidextra.Base64;

import com.nostra13.universalimageloader.core.assist.FlushedInputStream;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

/**
 * Imagedownloader which supports https connections and
 * downloading of protected Images through Basic Authentication.
 *
 * @author RayBa
 * @date 02.03.2014
 */
public class AuthImageDownloader extends BaseImageDownloader {
	
	/** The Constant TAG. */
	public static final String	TAG				= AuthImageDownloader.class.getName();

	/** The connect timeout. */
	private int					connectTimeout	= 20;
	
	/** The read timeout. */
	private int					readTimeout		= 10;

	/**
	 * Instantiates a new auth image downloader.
	 *
	 * @param context the context
	 * @author RayBa
	 * @date 02.03.2014
	 */
	public AuthImageDownloader(Context context) {
		super(context);
	}

	/* (non-Javadoc)
	 * @see com.nostra13.universalimageloader.core.download.BaseImageDownloader#getStreamFromNetwork(java.lang.String, java.lang.Object)
	 */
	@Override
	protected InputStream getStreamFromNetwork(String imageUri, Object extra) throws IOException {
		FlushedInputStream result = null;
		try {
			result = new FlushedInputStream(ServerRequest.getInputStream(imageUri));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	// always verify the host - dont check for certificate
	/** The Constant DO_NOT_VERIFY. */
	final static HostnameVerifier	DO_NOT_VERIFY	= new HostnameVerifier() {
														@Override
														public boolean verify(String hostname, SSLSession session) {
															return true;
														}
													};
													

	/**
	 * Encode credentials.
	 *
	 * @return the string´
	 * @author RayBa
	 * @date 02.03.2014
	 */
	public static String encodeCredentials() {
		try {
			String auth = Base64.encodeToString((ServerConsts.REC_SERVICE_USER_NAME + ":" + ServerConsts.REC_SERVICE_PASSWORD).getBytes("UTF-8"), Base64.NO_WRAP);
			return auth;
		} catch (Exception ignored) {
			Log.e(TAG, ignored.getMessage(), ignored);
		}
		return "";
	}

	/**
	 * Trust every server - dont check for any certificate.
	 *
	 * @author RayBa
	 * @date 02.03.2014
	 */
	private static void trustAllHosts() {
		// Create a trust manager that does not validate certificate chains
		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLUtil.getSSLContext();
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}