package com.miracleas.bitcoin_spinner_lib;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

public class Utils {

	public static void showConnectionAlert(Context context) {
		if (isConnected(context)) {
			showAlert(context, R.string.no_network);
		} else {
			showAlert(context, R.string.unexpected_error);
		}
	}

	public static void showAlert(Context context, int messageId) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(messageId).setCancelable(false)
				.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
					}
				});
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}

	public static void showNoNetworkTip(Context context) {
		Toast.makeText(context, R.string.no_network, Toast.LENGTH_SHORT).show();
	}
	
	public static boolean isConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] NI = cm.getAllNetworkInfo();
		for (int i = 0; i < NI.length; i++) {
			if (NI[i].isConnected()) {
				return true;
			}
		}
		return false;
	}
	
}
