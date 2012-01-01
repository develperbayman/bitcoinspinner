package com.bccapi.core.Asynchronous;

import com.bccapi.api.AccountInfo;

public interface AccountCache {
	public String getAccountPublicKey();

	public void setAccountPublicKey(String accountPublicKey);

	public void cacheAccountInfo(AccountInfo info);

	public long getBalance();

	public long getConsOnTheWayToYou();

	public int getBitcoinKeyCount();
}
