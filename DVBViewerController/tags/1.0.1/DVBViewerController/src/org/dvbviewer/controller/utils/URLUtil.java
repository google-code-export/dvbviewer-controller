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
package org.dvbviewer.controller.utils;

import java.net.MalformedURLException;
import java.net.URL;



/**
 * The Class URLUtil.
 *
 * @author RayBa
 * @date 07.04.2013
 */
public class URLUtil {

	/**
	 * Sets the recording services address.
	 *
	 * @param url the url
	 * @param port the port
	 * @author RayBa
	 * @date 07.04.2013
	 */
	public static void setRecordingServicesAddress(String url, String port){
		try {
			String prefUrl = android.webkit.URLUtil.guessUrl(url);
			URL baseUrl = new URL(prefUrl);
			ServerConsts.REC_SERVICE_PROTOCOL = baseUrl.getProtocol();
			ServerConsts.REC_SERVICE_HOST = baseUrl.getHost();
			ServerConsts.REC_SERVICE_PORT = port;
			ServerConsts.REC_SERVICE_URL = ServerConsts.REC_SERVICE_PROTOCOL+"://"+ServerConsts.REC_SERVICE_HOST+":"+ServerConsts.REC_SERVICE_PORT;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sets the recording services address.
	 *
	 * @param url the new recording services address
	 * @author RayBa
	 * @date 07.04.2013
	 */
	public static void setRecordingServicesAddress(String url){
		try {
			String prefUrl = android.webkit.URLUtil.guessUrl(url);
			URL baseUrl = new URL(prefUrl);
			ServerConsts.REC_SERVICE_PROTOCOL = baseUrl.getProtocol();
			ServerConsts.REC_SERVICE_HOST = baseUrl.getHost();
			ServerConsts.REC_SERVICE_URL = ServerConsts.REC_SERVICE_PROTOCOL+"://"+ServerConsts.REC_SERVICE_HOST+":"+ServerConsts.REC_SERVICE_PORT;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sets the recording services address.
	 *
	 * @param scheme the scheme
	 * @param url the url
	 * @param port the port
	 * @author RayBa
	 * @date 07.04.2013
	 */
	public static void setRecordingServicesAddress(String scheme, String url, String port){
		setRecordingServicesAddress(scheme+"://"+url, port);
	}
	
	
}
