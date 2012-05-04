package com.miracleas.bitcoin_spinner_lib;

import java.math.BigDecimal;

import android.net.Uri;

public class BitcoinUri {

	private String _address;
	private long _amount;

	public BitcoinUri(String address, long amount) {
		_address = address;
		_amount = amount;
	}

	public String getAddress() {
		return _address;
	}

	public long getAmount() {
		return _amount;
	}

	public static BitcoinUri parse(String uri) {
		try {
			Uri u = Uri.parse(uri);
			String scheme = u.getScheme();
			if(!scheme.equals("bitcoin")) {
				// not a bitcoin URI
				return null;
			}
			String schemeSpecific = u.getSchemeSpecificPart();
			if(schemeSpecific.startsWith("//")){
				// Fix for invalid bitcoin URI on the form "bitcoin://"
				schemeSpecific = schemeSpecific.substring(2);
			}
			u = Uri.parse("bitcoin://" + schemeSpecific);
			if (u == null) {
				return null;
			}
			String host = u.getHost();
			if (host == null || host.length() < 1) {
				return null;
			}
			String amountStr = u.getQueryParameter("amount");
			long amount = 0;
			if (amountStr != null) {
				amount = new BigDecimal(amountStr).movePointRight(8).toBigIntegerExact().longValue();
			}
			return new BitcoinUri(host, amount);

		} catch (Exception e) {
			return null;
		}
	}

}
