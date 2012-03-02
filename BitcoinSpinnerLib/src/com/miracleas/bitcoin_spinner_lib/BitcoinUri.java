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
			u = Uri.parse("bitcoin://" + u.getSchemeSpecificPart());

			String amountStr = u.getQueryParameter("amount");
			long amount = 0;
			if (amountStr != null) {
				amount = new BigDecimal(amountStr).movePointRight(8).toBigIntegerExact().longValue();
			}
			return new BitcoinUri(u.getHost(), amount);

		} catch (Exception e) {
			return null;
		}
	}

}
