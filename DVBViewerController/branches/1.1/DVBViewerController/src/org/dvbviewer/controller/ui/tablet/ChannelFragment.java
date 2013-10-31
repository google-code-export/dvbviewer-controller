package org.dvbviewer.controller.ui.tablet;

import org.dvbviewer.controller.R;
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
		tran.add(R.id.left_container, new ChannelPager());
		tran.add(R.id.right_container, new EpgPager());
		tran.commit();
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_channel_multi, null);
		return view;
	}

}
