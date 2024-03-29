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
package org.dvbviewer.controller.io.data;

import java.util.ArrayList;
import java.util.List;

import org.dvbviewer.controller.entities.EpgEntry;
import org.dvbviewer.controller.utils.DateUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;

/**
 * The Class EpgEntryHandler.
 *
 * @author RayBa
 * @date 05.07.2012
 */
public class EpgEntryHandler extends DefaultHandler {

	List<EpgEntry>	epgList		= null;
	EpgEntry		currentEPG	= null;

	/**
	 * Parses the.
	 *
	 * @param xml the xml
	 * @return the list�
	 * @author RayBa
	 * @throws SAXException 
	 * @date 05.07.2012
	 */
	public List<EpgEntry> parse(String xml) throws SAXException {
		RootElement root = new RootElement("epg");
		Element programmeElement = root.getChild("programme");
		Element titles = programmeElement.getChild("titles");
		Element title = titles.getChild("title");
		Element descriptions = programmeElement.getChild("descriptions");
		Element description = descriptions.getChild("description");
		Element events = programmeElement.getChild("events");
		Element event = events.getChild("event");

		root.setStartElementListener(new StartElementListener() {

			@Override
			public void start(Attributes attributes) {
				epgList = new ArrayList<EpgEntry>();
			}
		});

		programmeElement.setStartElementListener(new StartElementListener() {
			public void start(Attributes attributes) {
				currentEPG = new EpgEntry();
				currentEPG.setEpgID(Long.valueOf(attributes.getValue("channel")));
				currentEPG.setStart(DateUtils.stringToDate(attributes.getValue("start"), DateUtils.DATEFORMAT_RS_EPG));
				currentEPG.setEnd(DateUtils.stringToDate(attributes.getValue("stop"), DateUtils.DATEFORMAT_RS_EPG));
			}
		});

		programmeElement.setEndElementListener(new EndElementListener() {

			@Override
			public void end() {
				epgList.add(currentEPG);
			}
		});

		title.setEndTextElementListener(new EndTextElementListener() {

			@Override
			public void end(String body) {
				currentEPG.setTitle(body);
			}
		});
		event.setEndTextElementListener(new EndTextElementListener() {

			@Override
			public void end(String body) {
				currentEPG.setSubTitle(body);
			}
		});

		description.setEndTextElementListener(new EndTextElementListener() {

			@Override
			public void end(String body) {
				currentEPG.setDescription(body);
			}
		});

		Xml.parse(xml, root.getContentHandler());
		return epgList;
	}

}
