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

import android.os.Bundle;
import android.os.Handler;

/**
 * The Class ResultReceiver.
 *
 * @author RayBa
 * @date 07.04.2013
 */
public class ResultReceiver extends android.os.ResultReceiver {

	private Receiver	mReceiver;

	/**
	 * Instantiates a new result receiver.
	 *
	 * @param receiver the receiver
	 * @author RayBa
	 * @date 07.04.2013
	 */
	public ResultReceiver(Receiver receiver) {
		super(null);
		mReceiver = receiver;
	}
	
	/**
	 * Instantiates a new result receiver.
	 *
	 * @param handler the handler
	 * @author RayBa
	 * @date 07.04.2013
	 */
	public ResultReceiver(Handler handler) {
		super(handler);
	}

	/**
	 * Sets the receiver.
	 *
	 * @param receiver the new receiver
	 * @author RayBa
	 * @date 07.04.2013
	 */
	public void setReceiver(Receiver receiver) {
		mReceiver = receiver;
	}

	/* (non-Javadoc)
	 * @see android.os.ResultReceiver#onReceiveResult(int, android.os.Bundle)
	 */
	@Override
	protected void onReceiveResult(int resultCode, Bundle resultData) {
		if (mReceiver != null) {
			mReceiver.onReceiveResult(resultCode, resultData);
		}
	}

	/**
	 * The Interface Receiver.
	 *
	 * @author RayBa
	 * @date 07.04.2013
	 */
	public static interface Receiver {
		
		/**
		 * On receive result.
		 *
		 * @param resultCode the result code
		 * @param resultData the result data
		 * @author RayBa
		 * @date 07.04.2013
		 */
		public void onReceiveResult(int resultCode, Bundle resultData);
	}

}
