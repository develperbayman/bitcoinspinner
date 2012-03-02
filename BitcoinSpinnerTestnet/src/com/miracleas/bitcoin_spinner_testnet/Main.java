package com.miracleas.bitcoin_spinner_testnet;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.bccapi.api.Network;
import com.miracleas.bitcoin_spinner_lib.BitcoinUri;
import com.miracleas.bitcoin_spinner_lib.Consts;
import com.miracleas.bitcoin_spinner_lib.SendBitcoinsActivity;
import com.miracleas.bitcoin_spinner_lib.StartUpActivity;

public class Main extends Activity {
	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
        Intent intent = getIntent();
        final String action = intent.getAction();
        final Uri intentUri = intent.getData();
        final String scheme = intentUri != null ? intentUri.getScheme() : null;
        if (Intent.ACTION_VIEW.equals(action) && intentUri != null && "bitcoin".equals(scheme)) {
        	BitcoinUri b = BitcoinUri.parse(intentUri.toString());
            Intent i = new Intent(this, SendBitcoinsActivity.class);
            i.putExtra(Consts.BTC_ADDRESS_KEY, b.getAddress());
            i.putExtra(Consts.BTC_AMOUNT_KEY, b.getAmount());
            startActivity(i);
            finish();
            return;
        }
        
        Intent i = new Intent(this, StartUpActivity.class);
        i.putExtra(Consts.EXTRA_NETWORK, Network.testNetwork);
        startActivity(i);
        finish();
	}
}
