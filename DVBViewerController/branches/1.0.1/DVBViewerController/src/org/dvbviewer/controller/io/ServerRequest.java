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
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.dvbviewer.controller.utils.ServerConsts;

import android.util.Log;
import android.webkit.URLUtil;

/**
 * The Class ServerRequest.
 * 
 * @author RayBa
 * @date 06.04.2012
 */
public class ServerRequest {

	private static DefaultHttpClient	httpClient;
	private static Credentials			clientCredentials;
	private static Credentials			rsCredentials;
	private static AuthScope			clientAuthScope;
	private static AuthScope			rsAuthScope;

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
		URI uri = null;
		uri = new URI(ServerConsts.DVBVIEWER_URL + command);
		Log.d(ServerRequest.class.getSimpleName(), "executing DVBViewer command: " + uri);
		DefaultHttpClient client = getHttpClient();
		HttpGet request = new HttpGet(uri);
		HttpResponse response = executeGet(client, request, true);
		StatusLine status = response.getStatusLine();

		switch (status.getStatusCode()) {

		case HttpStatus.SC_OK:
			HttpEntity entity = response.getEntity();
			entity.consumeContent();
			break;

		default:
			break;
		}

	}

	/**
	 * Execute get.
	 *
	 * @param client the client
	 * @param request the request
	 * @return the http response´
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ClientProtocolException the client protocol exception
	 * @throws AuthenticationException the authentication exception
	 * @author RayBa
	 * @date 05.07.2012
	 */
	private static HttpResponse executeGet(DefaultHttpClient client, HttpGet request, boolean log) throws Exception{
		if (log) {
			Log.d(ServerRequest.class.getSimpleName(), "request: " + request.getRequestLine());
		}
		HttpResponse response = client.execute(request);
		StatusLine status = response.getStatusLine();
		Log.d(ServerRequest.class.getSimpleName(), "statusCode: " + status.getStatusCode());

		switch (status.getStatusCode()) {

		case HttpStatus.SC_UNAUTHORIZED:
			throw new AuthenticationException();

		default:
			break;
		}
		return response;
	}

	/**
	 * Gets the http client.
	 * 
	 * @return the http client
	 * @author RayBa
	 * @date 06.04.2012
	 */
	private static DefaultHttpClient getHttpClient() {
		if (httpClient == null) {
			HttpParams httpParams = new BasicHttpParams();
			// Set the timeout in milliseconds until a connection is
			// established.
			int timeoutConnection = 10000;
			HttpConnectionParams.setConnectionTimeout(httpParams, timeoutConnection);
			// Set the default socket timeout (SO_TIMEOUT)
			// in milliseconds which is the timeout for waiting for data.
			int timeoutSocket = 10000;
			HttpConnectionParams.setSoTimeout(httpParams, timeoutSocket);

			HttpProtocolParams.setContentCharset(httpParams, "UTF-8");

			ConnManagerParams.setMaxTotalConnections(httpParams, 10);

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
			HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setUseExpectContinue(httpParams, true);
			ThreadSafeClientConnManager connManager = new ThreadSafeClientConnManager(httpParams, registry);
			AuthRequestInterceptor preemptiveAuth = new AuthRequestInterceptor();
			httpClient = new DefaultHttpClient(connManager, httpParams);
			httpClient.addRequestInterceptor(preemptiveAuth, 0);
			if (getClientAuthScope() != null) {
				httpClient.getCredentialsProvider().setCredentials(getClientAuthScope(), getClientCredentials());
			}
			if (getRsAuthScope() != null) {
				httpClient.getCredentialsProvider().setCredentials(getRsAuthScope(), getRsCredentials());
			}
			
			httpClient.addRequestInterceptor(new GZipRequestInterceptor());
			httpClient.addResponseInterceptor(new GZipResponseInterceptor());
		}
		return httpClient;
	}

	/**
	 * Resets the HTTP client. This is necessary on preference changes.
	 * 
	 * @author RayBa
	 * @date 13.04.2012
	 */
	public static void resetHttpCLient() {
		if (httpClient != null && httpClient.getConnectionManager() != null) {
			httpClient.getConnectionManager().shutdown();
		}
		httpClient = null;
		rsAuthScope = null;
		clientAuthScope = null;
		rsCredentials = null;
		clientCredentials = null;
	}

	/**
	 * The Class AuthRequestInterceptor.
	 * 
	 * @author RayBa
	 * @date 06.04.2012
	 */
	private static class AuthRequestInterceptor implements HttpRequestInterceptor {

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.apache.http.HttpRequestInterceptor#process(org.apache.http.
		 * HttpRequest, org.apache.http.protocol.HttpContext)
		 */
		public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
			AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);
			CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
			HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);

			if (authState.getAuthScheme() == null) {
				AuthScope authScope = new AuthScope(targetHost.getHostName(), targetHost.getPort());
				Credentials creds = credsProvider.getCredentials(authScope);
				if (creds != null) {
					authState.setAuthScheme(new BasicScheme());
					authState.setCredentials(creds);
				}
			}
		}

	}

	/**
	 * Gets the rS bytes.
	 *
	 * @param request the request
	 * @return the rS bytes
	 * @throws AuthenticationException the authentication exception
	 * @throws URISyntaxException the URI syntax exception
	 * @throws ClientProtocolException the client protocol exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @author RayBa
	 * @date 13.04.2012
	 */
	public static byte[] getRSBytes(String request) throws Exception {
		byte[] result = null;
		result = EntityUtils.toByteArray(getRSEntity(request));
		return result;
	}

	/**
	 * Gets the rS string.
	 *
	 * @param request the request
	 * @return the rS string
	 * @throws AuthenticationException the authentication exception
	 * @throws ParseException the parse exception
	 * @throws ClientProtocolException the client protocol exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws URISyntaxException the URI syntax exception
	 * @author RayBa
	 * @date 13.04.2012
	 */
	public static String getRSString(String request) throws Exception {
//		URL url = new URL(ServerConsts.REC_SERVICE_URL + request);
//		URLConnection conn = url.openConnection();
//		String basicAuth = "Basic " + new String(Base64.encode((ServerConsts.REC_SERVICE_USER_NAME+":"+ServerConsts.REC_SERVICE_PASSWORD).getBytes(), Base64.NO_WRAP ));
//		conn.setRequestProperty ("Authorization", basicAuth);
//		conn.getContent();
		String result = null;
		result = EntityUtils.toString(getRSEntity(request));
		return result;
	}
	
	public static String getString(String request) throws Exception {
		String result = null;
		result = EntityUtils.toString(getEntity(request));
		return result;
	}

	/**
	 * Gets the rS entity.
	 *
	 * @param request the request
	 * @return the rS entity
	 * @throws IllegalStateException the illegal state exception
	 * @throws URISyntaxException the URI syntax exception
	 * @throws ClientProtocolException the client protocol exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws AuthenticationException the authentication exception
	 * @author RayBa
	 * @date 13.04.2012
	 */
	private static HttpEntity getRSEntity(String request) throws Exception {
		HttpEntity result = null;
		DefaultHttpClient client = getHttpClient();
		URI uri = null;
		uri = new URI(ServerConsts.REC_SERVICE_URL + request);
		HttpGet getMethod = new HttpGet(uri);
		HttpResponse res = executeGet(client, getMethod, true);

		StatusLine status = res.getStatusLine();
		switch (status.getStatusCode()) {

		case HttpStatus.SC_OK:
			result = res.getEntity();
			break;

		default:
			break;
		}
		return result;
	}
	
	/**
	 * Gets the rS entity.
	 *
	 * @param request the request
	 * @return the rS entity
	 * @throws IllegalStateException the illegal state exception
	 * @throws URISyntaxException the URI syntax exception
	 * @throws ClientProtocolException the client protocol exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws AuthenticationException the authentication exception
	 * @author RayBa
	 * @date 13.04.2012
	 */
	private static HttpEntity getEntity(String url) throws Exception {
		HttpEntity result = null;
		DefaultHttpClient client = getHttpClient();
		URI uri = null;
		uri = new URI(url);
		HttpGet getMethod = new HttpGet(uri);
		HttpResponse res = executeGet(client, getMethod, false);

		StatusLine status = res.getStatusLine();
		switch (status.getStatusCode()) {

		case HttpStatus.SC_OK:
			result = res.getEntity();
			break;

		default:
			break;
		}
		return result;
	}

	/**
	 * Executes a Recording Service Get request, with no return value.
	 * 
	 * For example used to send Timer to the Recording Service.
	 *
	 * @param request the request
	 * @throws URISyntaxException the URI syntax exception
	 * @throws ClientProtocolException the client protocol exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws AuthenticationException the authentication exception
	 * @throws IllegalArgumentException the illegal argument exception
	 * @author RayBa
	 * @date 13.04.2012
	 */
	public static void executeRSGet(String request) throws Exception {
		DefaultHttpClient client = getHttpClient();
		URI uri = null;
		Log.d(ServerRequest.class.getSimpleName(), "request: " + request);
		uri = new URI(ServerConsts.REC_SERVICE_URL + request);
		HttpGet getMethod = new HttpGet(uri);
		HttpResponse res = executeGet(client, getMethod, true);
		res.getEntity().consumeContent();
	}

	/**
	 * Gets the client credentials.
	 * 
	 * @return the client credentials
	 * @author RayBa
	 * @date 13.04.2012
	 */
	private static Credentials getClientCredentials() {
		if (clientCredentials == null) {
			clientCredentials = new UsernamePasswordCredentials(ServerConsts.DVBVIEWER_USER_NAME, ServerConsts.DVBVIEWER_PASSWORD);
		}
		return clientCredentials;
	}

	/**
	 * Gets the Recording service credentials.
	 * 
	 * @return the rs credentials
	 * @author RayBa
	 * @date 13.04.2012
	 */
	private static Credentials getRsCredentials() {
		if (rsCredentials == null) {
			rsCredentials = new UsernamePasswordCredentials(ServerConsts.REC_SERVICE_USER_NAME, ServerConsts.REC_SERVICE_PASSWORD);
		}
		return rsCredentials;
	}

	/**
	 * Gets the client auth scope.
	 * 
	 * @return the client auth scope
	 * @author RayBa
	 * @date 13.04.2012
	 */
	private static AuthScope getClientAuthScope() {
		if (clientAuthScope == null) {
			if (URLUtil.isValidUrl(ServerConsts.DVBVIEWER_URL)) {
				URI uri;
				try {
					uri = new URI(ServerConsts.DVBVIEWER_URL);
					clientAuthScope = new AuthScope(uri.getHost(), uri.getPort());
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
			}
		}
		return clientAuthScope;
	}

	/**
	 * Gets the Recording service auth scope.
	 * 
	 * @return the rs auth scope
	 * @author RayBa
	 * @date 13.04.2012
	 */
	private static AuthScope getRsAuthScope() {
		if (rsAuthScope == null) {
			if (URLUtil.isValidUrl(ServerConsts.REC_SERVICE_URL)) {
				URI uri;
				try {
					uri = new URI(ServerConsts.REC_SERVICE_URL);
					rsAuthScope = new AuthScope(uri.getHost(), uri.getPort()); 
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
			}
		}
		return rsAuthScope;
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
				ServerRequest.executeRSGet(request);
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
	
	static class GZipResponseInterceptor implements HttpResponseInterceptor{

		@Override
		public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
			 HttpEntity entity = response.getEntity();
             Header ceheader = entity.getContentEncoding();
             if (ceheader != null) {
                 HeaderElement[] codecs = ceheader.getElements();
                 for (int i = 0; i < codecs.length; i++) {
                     if (codecs[i].getName().equalsIgnoreCase("gzip")) {
                         response.setEntity(
                                 new GzipDecompressingEntity(response.getEntity())); 
                         return;
                     }
                 }
             }
		}

		
	}
	static class GZipRequestInterceptor implements HttpRequestInterceptor{
		
		public void process(
                final HttpRequest request, 
                final HttpContext context) throws HttpException, IOException {
            if (!request.containsHeader("Accept-Encoding")) {
                request.addHeader("Accept-Encoding", "gzip");
            }
        }
		
	}
	
	static class GzipDecompressingEntity extends HttpEntityWrapper {

        public GzipDecompressingEntity(final HttpEntity entity) {
            super(entity);
        }
    
        @Override
        public InputStream getContent()
            throws IOException, IllegalStateException {

            // the wrapped entity's getContent() decides about repeatability
            InputStream wrappedin = wrappedEntity.getContent();

            return new GZIPInputStream(wrappedin);
        }

        @Override
        public long getContentLength() {
            // length of ungzipped content is not known
            return -1;
        }

    } 

}
