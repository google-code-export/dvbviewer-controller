package org.dvbviewer.controller.ui.fragments;

import org.dvbviewer.controller.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

public class AboutFragment extends DialogFragment{
	
	private TextView	versionTextView;
	private ImageButton	payPalButton;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		String versionName = "";
		try {
			versionName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		versionTextView.setText(versionName);
		payPalButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				getActivity().startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=XBZT782XQV7AY")));
			}
		});
	}
	
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		View v = getActivity().getLayoutInflater().inflate(R.layout.fragment_about, null);
		payPalButton = (ImageButton) v.findViewById(R.id.paypalButton);
		versionTextView = (TextView) v.findViewById(R.id.versionTextView);
		Dialog d =  new AlertDialog.Builder(getActivity())
        .setTitle(R.string.about_app)
        .setView(v)
        .setPositiveButton("OK",
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                  getDialog().dismiss();
                }
            }
        )
        .create();
		return d;
	}

}
