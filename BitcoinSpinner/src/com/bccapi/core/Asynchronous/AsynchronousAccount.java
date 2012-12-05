package com.bccapi.core.Asynchronous;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import com.bccapi.api.APIException;
import com.bccapi.api.AccountInfo;
import com.bccapi.api.AccountStatement;
import com.bccapi.api.BitcoinClientAPI;
import com.bccapi.api.Network;
import com.bccapi.api.SendCoinForm;
import com.bccapi.api.Tx;
import com.bccapi.api.TxInput;
import com.bccapi.api.TxOutput;
import com.bccapi.core.AddressUtil;
import com.bccapi.core.BccScriptStandardInput;
import com.bccapi.core.BitUtils;
import com.bccapi.core.ECKeyManager;
import com.bccapi.core.ECSigner;
import com.bccapi.core.HashUtils;
import com.bccapi.core.PublicECKey;

/**
 * This class manages the client side of a Bitcoin wallet including wallet private keys. It uses the BCCAPI for
 * accessing the server side wallet asynchronously, which is responsible of tracking wallet transactions using the
 * wallet public keys. All the 'request' functions are non-blocking, and are executing the BCCAPI call in the
 * background. For each 'request' function there is a corresponding interface with a call-back function that the caller
 * must implement. This function is called once the BCCAPI call has completed or failed.
 * <p>
 * Currently this class only supports one-key wallets as used with BitcoinSpinner.
 */
public abstract class AsynchronousAccount {

	private static final byte[] EMPTY_ARRAY = new byte[0];

	abstract protected CallbackRunnerInvoker createCallbackRunnerInvoker();

	abstract private class SynchronousFunctionCaller implements Runnable, AccountTask {

		private String _errorMessage = null;
		private boolean _canceled;

		@Override
		public void cancel() {
			_canceled = true;
		}

		@Override
		public void run() {
			synchronized (_lock) {
				try {
					if (_sessionId == null) {
						// Login if not yet logged in
						if (!doLogin()) {
							setError("Unable to login");
							return;
						}
					}
					if (_canceled) {
						return;
					}
					try {
						callFunction();
					} catch (IOException e) {
						setError(e.getMessage());
					} catch (APIException e) {
						// Session may have timed out, login again and try command once more
						if (_canceled) {
							return;
						}
						if (!doLogin()) {
							setError("Unable to login");
							return;
						}
						if (_canceled) {
							return;
						}
						try {
							callFunction();
						} catch (IOException e2) {
							setError(e2.getMessage());
						} catch (APIException e2) {
							setError(e2.getMessage());
						}

					}
				} finally {
					if (_canceled) {
						return;
					}
					callback();
				}
			}
		}

		abstract protected void callFunction() throws IOException, APIException;

		abstract protected void callback();

		protected void setError(String errorMessage) {
			_errorMessage = errorMessage;
		}

		protected String getError() {
			return _errorMessage;
		}

		/**
		 * Login to the BCCAPI server using the account public key
		 * 
		 * @return The {@link AccountInfo} for this account.
		 * @throws IOException
		 * @throws APIException
		 */
		private boolean doLogin() {
			PublicECKey publicKey = getAccountPublicKey();
			// Get challenge
			try {
				byte[] challenge = _api.getLoginChallenge(publicKey.getPubKeyBytes());
				// Calculate challenge response
				byte[] response = calculateLoginChallengeResponse(challenge);
				// Login
				_sessionId = _api.login(publicKey.getPubKeyBytes(), response);
				return true;
			} catch (Exception e) {
				synchronized (_lock) {
					_sessionId = null;
					_errorMessage = e.getMessage();
				}
			}
			return false;
		}

		/**
		 * Calculate login challenge response. The response is the signature on the SHA-256 of the public key and the
		 * challenge. The signature is made with the account private key.
		 * 
		 * @param challenge
		 *            The challenge to calculate a response for
		 * @return the login challenge response.
		 */
		private byte[] calculateLoginChallengeResponse(byte[] challenge) {
			byte[] accountPublicKeyBytes = _keyManager.getPublicKey(0).getPubKeyBytes();
			byte[] toSign = HashUtils.sha256(accountPublicKeyBytes, challenge);
			ECSigner accountSigner = new ECSigner(_keyManager, 0);
			return accountSigner.sign(toSign);
		}

	}

	private class GetAccountInfoCaller extends SynchronousFunctionCaller {

		private GetAccountInfoCallbackHandler _callbackHandler;
		private CallbackRunnerInvoker _callbackInvoker;
		private AccountInfo _info;

		private GetAccountInfoCaller(GetAccountInfoCallbackHandler callbackHandler) {
			_callbackHandler = callbackHandler;
			_callbackInvoker = createCallbackRunnerInvoker();
		}

		@Override
		protected void callFunction() throws IOException, APIException {
			_info = _api.getAccountInfo(_sessionId);
			if (_info.getKeys() == 0) {
				// This is the first time we get the account info, and we haven't added our key yet.
				// Do this now
				byte[] publicKeyBytes = getPrimaryBitcoinPublicKey().getPubKeyBytes();
				_api.addKeyToWallet(_sessionId, publicKeyBytes);
			}
			_accountCache.cacheAccountInfo(_info);
		}

		protected void callback() {
			_callbackInvoker.invoke(new GetAccountInfoCallbackRunner(_callbackHandler, _info, getError()));
		}
	}

	private static class GetAccountInfoCallbackRunner implements Runnable {
		private GetAccountInfoCallbackHandler _callbackHandler;
		private AccountInfo _info;
		private String _errorMessage;

		private GetAccountInfoCallbackRunner(GetAccountInfoCallbackHandler callbackHandler, AccountInfo info,
				String errorMessage) {
			_callbackHandler = callbackHandler;
			_info = info;
			_errorMessage = errorMessage;
		}

		@Override
		public void run() {
			_callbackHandler.handleGetAccountInfoCallback(_info, _errorMessage);

		}
	}

	private class RecentStatementsCaller extends SynchronousFunctionCaller {

		private RecentStatementsCallbackHandler _callbackHandler;
		private CallbackRunnerInvoker _callbackInvoker;
		private AccountStatement _statements;
		private int _count;

		private RecentStatementsCaller(int count, RecentStatementsCallbackHandler callbackHandler) {
			_callbackHandler = callbackHandler;
			_callbackInvoker = createCallbackRunnerInvoker();
			_count = count;
		}

		@Override
		protected void callFunction() throws IOException, APIException {
			_statements = _api.getRecentTransactionSummary(_sessionId, _count);
		}

		protected void callback() {
			_callbackInvoker.invoke(new RecentStatementsCallbackRunner(_callbackHandler, _statements, getError()));
		}
	}

	private static class RecentStatementsCallbackRunner implements Runnable {
		private RecentStatementsCallbackHandler _callbackHandler;
		private AccountStatement _statements;
		private String _errorMessage;

		private RecentStatementsCallbackRunner(RecentStatementsCallbackHandler callbackHandler,
				AccountStatement statements, String errorMessage) {
			_callbackHandler = callbackHandler;
			_statements = statements;
			_errorMessage = errorMessage;
		}

		@Override
		public void run() {
			_callbackHandler.handleRecentStatementsCallback(_statements, _errorMessage);

		}
	}

	private class GetSendCoinFormCaller extends SynchronousFunctionCaller {

		private GetSendCoinFormCallbackHandler _callbackHandler;
		private CallbackRunnerInvoker _callbackInvoker;
		private String _receivingAddress;
		private long _amount;
		private long _fee;
		private SendCoinForm _form;

		private GetSendCoinFormCaller(String receivingAddress, long amount, long fee,
				GetSendCoinFormCallbackHandler callbackHandler) {
			_callbackHandler = callbackHandler;
			_callbackInvoker = createCallbackRunnerInvoker();
			_receivingAddress = receivingAddress;
			_amount = amount;
			_fee = fee;
		}

		@Override
		protected void callFunction() throws IOException, APIException {
			_form = _api.getSendCoinForm(_sessionId, _receivingAddress, _amount, _fee);
		}

		protected void callback() {
			_callbackInvoker.invoke(new GetSendCoinFormCallbackRunner(_callbackHandler, _form, getError()));
		}
	}

	private static class GetSendCoinFormCallbackRunner implements Runnable {
		private GetSendCoinFormCallbackHandler _callbackHandler;
		private SendCoinForm _form;
		private String _errorMessage;

		private GetSendCoinFormCallbackRunner(GetSendCoinFormCallbackHandler callbackHandler, SendCoinForm form,
				String errorMessage) {
			_callbackHandler = callbackHandler;
			_form = form;
			_errorMessage = errorMessage;
		}

		@Override
		public void run() {
			_callbackHandler.handleGetSendCoinFormCallback(_form, _errorMessage);

		}
	}

	private class TransactionSubmitter extends SynchronousFunctionCaller {

		private TransactionSubmissionCallbackHandler _callbackHandler;
		private CallbackRunnerInvoker _callbackInvoker;
		private Tx _transaction;

		private TransactionSubmitter(Tx transaction, TransactionSubmissionCallbackHandler callbackHandler) {
			_callbackHandler = callbackHandler;
			_callbackInvoker = createCallbackRunnerInvoker();
			_transaction = transaction;
		}

		@Override
		protected void callFunction() throws IOException, APIException {
			_api.submitTransaction(_sessionId, _transaction);
		}

		protected void callback() {
			_callbackInvoker
					.invoke(new TransactionSubmissionCallbackRunner(_callbackHandler, _transaction, getError()));
		}
	}

	private static class TransactionSubmissionCallbackRunner implements Runnable {
		private TransactionSubmissionCallbackHandler _callbackHandler;
		private Tx _transaction;
		private String _errorMessage;

		private TransactionSubmissionCallbackRunner(TransactionSubmissionCallbackHandler callbackHandler,
				Tx transaction, String errorMessage) {
			_callbackHandler = callbackHandler;
			_transaction = transaction;
			_errorMessage = errorMessage;
		}

		@Override
		public void run() {
			_callbackHandler.handleTransactionSubmission(_transaction, _errorMessage);

		}
	}

	private ECKeyManager _keyManager;
	private BitcoinClientAPI _api;
	private String _sessionId;
	private String _lock = "AccountManager.Runnable.lock";
	AccountCache _accountCache;

	/**
	 * Create a new account instance.
	 * 
	 * @param keyManager
	 *            The manager of the private keys belonging to this account
	 * @param api
	 *            The BCCAPI instance used for communicating with the BCCAPI server.
	 * @param accountCache
	 *            The account cache instance used.
	 */
	public AsynchronousAccount(ECKeyManager keyManager, BitcoinClientAPI api, AccountCache accountCache) {
		_keyManager = keyManager;
		_api = api;
		_accountCache = accountCache;
		_accountCache.setAccountPublicKey(getAccountPublicKey().toString());
	}

	private synchronized void executeRequest(SynchronousFunctionCaller caller) {
		Thread thread = new Thread(caller);
		thread.start();
	}

	/**
	 * Execute {@link BitcoinClientAPI.getAccountInfo} in the background and do a callback to
	 * {@link GetAccountInfoCallbackHandler.handleGetAccountInfoCallback} once the function succeeds or fails.
	 * 
	 * @param callbackHandler
	 *            The callback handler to call
	 * @return an {@link AccountTask} instance that allows the caller to cancel the call back.
	 */
	public AccountTask requestAccountInfo(GetAccountInfoCallbackHandler callbackHandler) {
		GetAccountInfoCaller caller = new GetAccountInfoCaller(callbackHandler);
		executeRequest(caller);
		return caller;
	}

	/**
	 * Execute {@link BitcoinClientAPI.getSendCoinForm} in the background and do a callback to
	 * {@link GetSendCoinFormCallbackHandler.handleGetSendCoinFormCallback} once the function succeeds or fails.
	 * 
	 * @param callbackHandler
	 *            The callback handler to call
	 * @return an {@link AccountTask} instance that allows the caller to cancel the call back.
	 */
	public AccountTask requestSendCoinForm(String receivingAddress, long amount, long fee,
			GetSendCoinFormCallbackHandler callbackHandler) {
		GetSendCoinFormCaller caller = new GetSendCoinFormCaller(receivingAddress, amount, fee, callbackHandler);
		executeRequest(caller);
		return caller;
	}

	/**
	 * Execute {@link BitcoinClientAPI.submitTransaction} in the background and do a callback to
	 * {@link TransactionSubmissionCallbackHandler.handleTransactionSubmission} once the function succeeds or fails.
	 * 
	 * @param callbackHandler
	 *            The callback handler to call
	 * @return an {@link AccountTask} instance that allows the caller to cancel the call back.
	 */
	public AccountTask requestTransactionSubmission(Tx transaction, TransactionSubmissionCallbackHandler callbackHandler) {
		TransactionSubmitter caller = new TransactionSubmitter(transaction, callbackHandler);
		executeRequest(caller);
		return caller;
	}

	/**
	 * Execute {@link BitcoinClientAPI.getRecentTransactionSummary} in the background and do a callback to
	 * {@link RecentStatementsCallbackHandler.handleRecentStatementsCallback} once the function succeeds or fails.
	 * 
	 * @param callbackHandler
	 *            The callback handler to call
	 * @return an {@link AccountTask} instance that allows the caller to cancel the call back.
	 */
	public AccountTask requestRecentStatements(int count, RecentStatementsCallbackHandler callbackHandler) {
		RecentStatementsCaller caller = new RecentStatementsCaller(count, callbackHandler);
		executeRequest(caller);
		return caller;
	}

	/**
	 * Get the first Bitcoin address managed by this account.
	 * 
	 * @return The first Bitcoin address managed by this account.
	 */
	public String getPrimaryBitcoinAddress() {
		PublicECKey key = getPrimaryBitcoinPublicKey();
		return AddressUtil.publicKeyToStandardAddress(_api.getNetwork(), key.getPubKeyBytes());
	}

	/**
	 * Get the cached Bitcoin balance balance in satoshis. The cache is automatically updated whenever
	 * {@link requestAccountInfo} is called.
	 * 
	 * @return
	 */
	public long getCachedBalance() {
		return _accountCache.getBalance();
	}

	/**
	 * Get the unconfirmed number of coins being sent to your wallet from the cache. This is the sum of the transaction
	 * outputs sent to one of your Bitcoin addresses, but which have not been included in a block yet. The cache is
	 * automatically updated whenever {@link requestAccountInfo} is called.
	 * 
	 * @return The unconfirmed number of coins being sent to your wallet from the cache.
	 */
	public long getCachedCoinsOnTheWay() {
		return _accountCache.getConsOnTheWayToYou();
	}

	private PublicECKey getPrimaryBitcoinPublicKey() {
		// Our single Bitcoin public key is found on index 1
		return _keyManager.getPublicKey(1);
	}

	/**
	 * Get the public key corresponding to the private key used for authenticating the account when logging in to the
	 * BCCAPI server.
	 * 
	 * @return the public key of this account.
	 */
	private PublicECKey getAccountPublicKey() {
		return _keyManager.getPublicKey(0);
	}

	/**
	 * Get the {@link ECSigner} for a wallet key index. The {@link ECSigner} can be used for signing data with the
	 * private key.
	 * 
	 * @param index
	 *            The index of he private key to get an {@link ECSigner} for.
	 * @return The {@link ECSigner} for a wallet private key index.
	 */
	private ECSigner getWalletSignerByKeyIndex(int index) {
		return _keyManager.getSigner(index + 1);
	}

	/**
	 * Sign a {@link SendCoinForm} obtained using {@link requestSendCoinForm}.
	 * 
	 * @param form
	 * @param account
	 * @return the signed transaction of the {@link SendCoinForm}.
	 */
	public static Tx signSendCoinForm(SendCoinForm form, AsynchronousAccount account) {
		Tx tx = form.getTransaction();
		List<TxOutput> funding = form.getFunding();
		List<Integer> keyIndexes = form.getKeyIndexes();
		List<TxInput> inputs = tx.getInputs();
		byte[][] signatures = new byte[inputs.size()][];
		ECSigner[] signingKeys = new ECSigner[inputs.size()];
		// Clear all inputs
		for (TxInput input : inputs) {
			input.setScript(EMPTY_ARRAY);
		}

		// Generate signature for each input
		for (int i = 0; i < inputs.size(); i++) {
			TxInput input = inputs.get(i);

			// Set the input to the script of its output.
			input.setScript(funding.get(i).getScript());

			// Find the signing key to use.
			int index = keyIndexes.get(i);
			ECSigner signer = account.getWalletSignerByKeyIndex(index);
			if (signer == null) {
				throw new RuntimeException("Unable to find signer for key with index: " + index);
			}
			signingKeys[i] = signer;
			byte[] hash = hashTx(tx);
			// Set the script to empty again for the next input.
			input.setScript(EMPTY_ARRAY);

			// Sign for the output, and put the resulting signature in the
			// script along with the public key further downstream.
			try {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				bos.write(signer.sign(hash));
				bos.write((0 + 1) | 0);
				signatures[i] = bos.toByteArray();
			} catch (IOException e) {
				throw new RuntimeException(e); // Cannot happen.
			}
		}

		// Now we have calculated each signature, go through and create the
		// scripts. Reminder: the script consists of
		// a signature (over a hash of the transaction) and the complete public
		// key needed to sign for the connected
		// output.
		for (int i = 0; i < inputs.size(); i++) {
			TxInput input = inputs.get(i);
			PublicECKey key = signingKeys[i].getPublicKey();
			BccScriptStandardInput script = new BccScriptStandardInput(signatures[i], key.getPubKeyBytes());
			input.setScript(script.toByteArray());
		}
		return tx;
	}

	private static byte[] hashTx(Tx tx) {
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			tx.toStream(stream);
			int hashType = 1;
			BitUtils.uint32ToStream(hashType, stream);
			return HashUtils.doubleSha256(stream.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException(e); // Cannot happen.
		}
	}

	/**
	 * Get the network used, test network or production network.
	 * 
	 * @return The network used, test network or production network.
	 */
	public Network getNetwork() {
		return _api.getNetwork();
	}

}
