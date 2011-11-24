package com.miracleas.bitcoin_spinner;

import com.miracleas.bitcoin_spinner_lib.Consts;
import com.miracleas.bitcoin_spinner_lib.StartUpActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class Main extends Activity {
	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Intent i = new Intent(this, StartUpActivity.class);
        i.putExtra(Consts.EXTRA_NETWORK, Consts.PRODNET);
        startActivity(i);
        finish();
	}
}
