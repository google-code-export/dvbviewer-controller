package org.dvbviewer.controller.entities;

import java.util.ArrayList;
import java.util.List;

import org.dvbviewer.controller.data.DbConsts.RootTbl;

import android.content.ContentValues;
import android.text.TextUtils;

public class ChannelRoot {

	private Long			id;

	private String			name;

	private List<ChannelGroup>	groups	= new ArrayList<ChannelGroup>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public List<ChannelGroup> getGroups() {
		return groups;
	}

	public void setGroups(List<ChannelGroup> categories) {
		this.groups = categories;
	}
	
	public ContentValues toContentValues() {
		ContentValues result = new ContentValues();
		if (this.id != null && !this.id.equals(0l)) {
			result.put(RootTbl._ID, id);
		}
		if (this.name != null && !TextUtils.isEmpty(this.name)) {
			result.put(RootTbl.NAME, this.name);
		}
		return result;
	}

}
