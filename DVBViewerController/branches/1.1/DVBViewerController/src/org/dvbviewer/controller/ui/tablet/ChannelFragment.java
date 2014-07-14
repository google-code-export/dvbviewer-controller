package org.dvbviewer.controller.ui.tablet;

import org.dvbviewer.controller.R;
import org.dvbviewer.controller.entities.DVBViewerPreferences;
import org.dvbviewer.controller.ui.fragments.ChannelList;
import org.dvbviewer.controller.ui.fragments.ChannelPager;
import org.dvbviewer.controller.ui.fragments.EpgPager;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ChannelFragment extends Fragment{
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		FragmentTransaction tran = getChildFragmentManager().beginTransaction();
		ChannelPager chanPager = new ChannelPager();
		Bundle chanPagerArgs = new Bundle();
		chanPager.setArguments(chanPagerArgs);;
		if (getArguments().containsKey(ChannelList.KEY_HAS_OPTIONMENU)) {
			chanPagerArgs.putBoolean(ChannelList.KEY_HAS_OPTIONMENU, getArguments().getBoolean(ChannelList.KEY_HAS_OPTIONMENU));
		}
		if (getArguments().containsKey(ChannelList.KEY_GROUP_ID)) {
			chanPagerArgs.putLong(ChannelList.KEY_GROUP_ID, getArguments().getLong(ChannelList.KEY_GROUP_ID));
		}
		chanPagerArgs.putBoolean(DVBViewerPreferences.KEY_CHANNELS_USE_FAVS, getArguments().getBoolean(DVBViewerPreferences.KEY_CHANNELS_USE_FAVS));
		chanPagerArgs.putInt(ChannelList.KEY_SELECTED_POSITION, getArguments().getInt(ChannelList.KEY_SELECTED_POSITION));
		EpgPager epgPager = new EpgPager();
		Bundle arguments = new Bundle();
		epgPager.setArguments(arguments);
//		epgPager.getArguments().putParcelableArrayList(EpgPager.KEY_CHANNELS, getArguments().getParcelableArrayList(EpgPager.KEY_CHANNELS));
		epgPager.getArguments().putInt(EpgPager.KEY_POSITION, getArguments().getInt(EpgPager.KEY_POSITION));
		tran.add(R.id.left_container, chanPager);
		tran.add(R.id.right_container, epgPager);
		tran.commit();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_channel_multi, null);
		return view;
	}

}
