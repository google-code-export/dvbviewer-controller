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
package org.dvbviewer.controller.io;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.ClientProtocolException;
import org.dvbviewer.controller.utils.ServerConsts;

import android.util.Log;

import com.github.kevinsawicki.http.HttpRequest;

/**
 * The Class ServerRequest.
 * 
 * @author RayBa
 * @date 06.04.2012
 */
public class ServerRequest {


	/**
	 * Sends an command to the DVBViewer Client. Every ActionID will be
	 * accepted.
	 *
	 * @param command ActionID
	 * @throws IllegalArgumentException the illegal argument exception
	 * @throws URISyntaxException the URI syntax exception
	 * @throws ClientProtocolException the client protocol exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws AuthenticationException the authentication exception
	 * @author RayBa
	 * @date 06.04.2012
	 */
	public static void sendCommand(String command) throws Exception {
		Log.d(ServerRequest.class.getSimpleName(), "executing DVBViewer command: " + command);
		HttpRequest request = getViewerRequest(command);

		switch (request.code()) {

		case HttpStatus.SC_OK:
			request.body();
			break;

		default:
			break;
		}

	}


	/**
	 * Gets the rS string.
	 *
	 * @param request the request
	 * @return the rS string
	 * @author RayBa
	 * @date 13.04.2012
	 */
	public static String getRSString(String url) throws Exception {
		return getServiceRequest(url).body();
	}
	
	public static byte[] getRSBytes(String url) throws Exception {
		return getServiceRequest(url).bytes();
	}
	
	private static HttpRequest getServiceRequest(String url) throws Exception {
		//Set Service Credentials
		return getRequest(ServerConsts.REC_SERVICE_URL+url, Type.RECORDING_SERVICE);
	}
	
	private static HttpRequest getViewerRequest(String url) throws Exception {
		//Set Viewer Credentials
		return getRequest(ServerConsts.DVBVIEWER_URL + url, Type.DVBVIEWER);
	}
	
	private static HttpRequest getRequest(String url, Type type) throws Exception {
		Log.d(ServerRequest.class.getSimpleName(), url);
		HttpRequest request = HttpRequest.get(url);
		//Tell server to gzip response and automatically uncompress
		request.acceptGzipEncoding().uncompress(true);
		//Accept all certificates
		request.trustAllCerts();
		// Accept all hostnames
		request.trustAllHosts();
		switch (type) {
		case DVBVIEWER:
			request.basic(ServerConsts.DVBVIEWER_USER_NAME, ServerConsts.DVBVIEWER_PASSWORD);
			break;
		case RECORDING_SERVICE:
			request.basic(ServerConsts.REC_SERVICE_USER_NAME, ServerConsts.REC_SERVICE_PASSWORD);
			break;

		default:
			break;
		}
		if (request.code() == HttpStatus.SC_FORBIDDEN) {
			throw new AuthenticationException();
		}
		
		return request;
	}
	

	
	enum Type {
		DVBVIEWER, RECORDING_SERVICE, UNKNOWN
	}




	/**
	 * The Class RecordingServiceGet.
	 *
	 * @author RayBa
	 * @date 05.07.2012
	 */
	public static class RecordingServiceGet implements Runnable {
		String	request;

		/**
		 * Instantiates a new recording service get.
		 *
		 * @param request the request
		 * @author RayBa
		 * @date 05.07.2012
		 */
		public RecordingServiceGet(String request) {
			this.request = request;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			try {
				getServiceRequest(request).body();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	
	
	
	/**
	 * The Class DVBViewerCommand.
	 *
	 * @author RayBa
	 * @date 07.04.2013
	 */
	public static class DVBViewerCommand implements Runnable {
		String	request;
		public DVBViewerCommand(String request) {
			this.request = request;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			try {
				ServerRequest.sendCommand(request);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}



	public static String getString(String string) throws Exception {
		return getRequest(string, Type.UNKNOWN).body();
	}
	
	

}