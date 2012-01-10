package com.miracleas.bitcoin_spinner_lib;

import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.text.ClipboardManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bccapi.api.AccountInfo;
import com.bccapi.api.Network;
import com.bccapi.core.CoinUtils;
import com.bccapi.core.Asynchronous.AccountTask;
import com.bccapi.core.Asynchronous.GetAccountInfoCallbackHandler;
import com.miracleas.bitcoin_spinner_lib.SimpleGestureFilter.SimpleGestureListener;
import com.miracleas.bitcoin_spinner_lib.Ticker.BtcToUsdCallbackHandler;

public class MainActivity extends Activity implements SimpleGestureListener,
		GetAccountInfoCallbackHandler, BtcToUsdCallbackHandler {

	private Context mContext;
	private TextView tvAddress, tvBalance, tvUsdValue, tvEstimatedOnTheWay;
	private ImageView ivAddress;
	private Button btnSendMoney, btnTransactionHistory;
	private RelativeLayout rlBalance;
	private ProgressBar pbBalanceUpdateProgress;
	private View vBalanceUpdateView;
	private View vBalanceNoConnView;
	private AccountTask mGetInfoTask;

	private AlertDialog qrCodeDialog;

	private static final int REQUEST_CODE_SEND_MONEY = 10001;
	private static final int REQUEST_CODE_SETTINGS = 10002;

	private static final int ABOUT_DIALOG = 1001;
	private static final int THANKS_DIALOG = 1002;
	private static final int DONATE_DIALOG = 1003;

	private SimpleGestureFilter detector;

	private SharedPreferences preferences;
	private ConnectionWatcher mConnectionWatcher;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;

		Consts.applicationContext = getApplicationContext();

		preferences = getSharedPreferences(Consts.PREFS_NAME, MODE_PRIVATE);
	}

	@Override
	protected void onResume() {
		super.onResume();
		setContentView(R.layout.main);

		if (!preferences.getString(Consts.LOCALE, "").matches("")) {
			Locale locale = new Locale(preferences.getString(Consts.LOCALE,
					"en"));
			Locale.setDefault(locale);
			Configuration config = new Configuration();
			config.locale = locale;
			getBaseContext().getResources().updateConfiguration(config,
					getBaseContext().getResources().getDisplayMetrics());
		}

		detector = new SimpleGestureFilter(this, this);
		mConnectionWatcher = new ConnectionWatcher(mContext);
		new Thread(mConnectionWatcher).start();

		tvAddress = (TextView) findViewById(R.id.tv_address);
		ivAddress = (ImageView) findViewById(R.id.iv_address);
		tvBalance = (TextView) findViewById(R.id.tv_balance);
		tvUsdValue = (TextView) findViewById(R.id.tv_usd_value);
		tvEstimatedOnTheWay = (TextView) findViewById(R.id.tv_estimated_on_the_way);
		btnSendMoney = (Button) findViewById(R.id.btn_send_money);
		btnTransactionHistory = (Button) findViewById(R.id.btn_transaction_history);

		tvAddress.setOnClickListener(addressClickListener);

		ivAddress.setOnClickListener(qrClickListener);

		rlBalance = (RelativeLayout) findViewById(R.id.rl_balance_ref);
		btnSendMoney.setOnClickListener(sendMoneyClickListener);
		btnTransactionHistory
				.setOnClickListener(transactionHistoryClickListener);
		rlBalance.setOnClickListener(refreshMoneyClickListener);
		rlBalance.setOnLongClickListener(refreshMoneyLongClickListener);

		pbBalanceUpdateProgress = (ProgressBar) findViewById(R.id.pb_balance_update);
		vBalanceUpdateView = findViewById(R.id.v_balance_update);
		vBalanceNoConnView = findViewById(R.id.v_balance_no_conn);

		if (preferences.getBoolean("ShowQrCode", false)) {
			ivAddress.performClick();
		}
		UpdateInfo();
		updateAddress();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mConnectionWatcher.stop();
		Editor editor = preferences.edit();
		if (null != qrCodeDialog) {
			editor.putBoolean("ShowQrCode", qrCodeDialog.isShowing());
			qrCodeDialog.dismiss();
		} else {
			editor.putBoolean("ShowQrCode", false);
		}
		editor.commit();
	}

	private class ConnectionWatcher implements Runnable {
		private boolean _quit;
		private Context _context;
		private boolean _isCurrentlyConnected;
		private Handler _handler;

		public ConnectionWatcher(Context context) {
			_quit = false;
			_context = context;
			_handler = new Handler();
		}

		public void stop() {
			_quit = true;
		}

		@Override
		public void run() {
			try {
				_isCurrentlyConnected = Utils.isConnected(_context);
				while (!_quit) {
					Thread.sleep(1000);
					if (_quit) {
						return;
					}
					boolean connected = Utils.isConnected(_context);
					if (connected && !_isCurrentlyConnected) {
						// We went from not connected to connected
						// Post a handler that updates our balance
						_handler.post(new Runnable() {
							@Override
							public void run() {
								if (_quit) {
									return;
								}
								UpdateInfo();
							}
						});
					}
					_isCurrentlyConnected = connected;
				}
			} catch (InterruptedException e) {
				return;
			}

		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent me) {
		this.detector.onTouchEvent(me);
		return super.dispatchTouchEvent(me);
	}

	private final OnClickListener qrClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View layout = inflater.inflate(R.layout.dialog_qr_address, null);
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
					.setView(layout);
			qrCodeDialog = builder.create();
			qrCodeDialog.setCanceledOnTouchOutside(true);
			TextView text = (TextView) layout.findViewById(R.id.tv_title_text);
			text.setText(R.string.bitcoin_address);
			
			ImageView qrAdress = (ImageView) layout
					.findViewById(R.id.iv_qr_Address);
			qrAdress.setImageBitmap(Utils.getPrimaryAddressAsLargeQrCode(Consts.account));
			qrAdress.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					qrCodeDialog.dismiss();
				}
			});

			Button copy = (Button) layout.findViewById(R.id.btn_copy_to_clip);
			copy.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
					clipboard.setText(Consts.account.getPrimaryBitcoinAddress());
					Toast.makeText(mContext, R.string.clipboard_copy,
							Toast.LENGTH_SHORT).show();
				}
			});

			qrCodeDialog.show();
		}
	};

	private final OnClickListener addressClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TEXT,
					"bitcoin:" + Consts.account.getPrimaryBitcoinAddress());
			startActivity(Intent.createChooser(intent,
					getString(R.string.share_bitcoin_address)));
		}
	};

	private final OnClickListener sendMoneyClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent();
			intent.setClass(MainActivity.this, SendBitcoinsActivity.class);
			startActivityForResult(intent, REQUEST_CODE_SEND_MONEY);
		}
	};

	private final OnClickListener transactionHistoryClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent();
			intent.setClass(MainActivity.this, TransactionHistoryActivity.class);
			startActivity(intent);
		}
	};

	private final OnLongClickListener refreshMoneyLongClickListener = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			UpdateInfo();
			return true;
		}
	};

	private final OnClickListener refreshMoneyClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			UpdateInfo();
		}
	};

	@Override
	public void onActivityResult(final int requestCode, final int resultCode,
			final Intent intent) {
		if (Utils.isConnected(this)) {
			if (requestCode == REQUEST_CODE_SEND_MONEY) {
				// do nothing, balance is refreshed by onResume()
			} else if (requestCode == REQUEST_CODE_SETTINGS) {
				// do nothing, balance is refreshed by onResume()
			}
		}
	}

	@Override
	public void onSwipe(int direction) {
		Intent intent = new Intent();

		switch (direction) {

		case SimpleGestureFilter.SWIPE_LEFT:
			intent.setClass(this, SendBitcoinsActivity.class);
			startActivity(intent);
			break;

		case SimpleGestureFilter.SWIPE_RIGHT:
			intent.setClass(this, TransactionHistoryActivity.class);
			startActivity(intent);
			break;

		}
	}

	@Override
	public void onDoubleTap() {

	}

	@Override
	public void onBackPressed() {
		moveTaskToBack(true);
	};
	

	/** Called when menu button is pressed. */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	/** Callen when menu item is selected */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		if (item.getItemId() == R.id.about) {
			showDialog(ABOUT_DIALOG);
			return true;
		} else if (item.getItemId() == R.id.settings) {
			startActivityForResult(new Intent(this, SettingsActivity.class),
					REQUEST_CODE_SETTINGS);
			return true;
		} else if (item.getItemId() == R.id.credits) {
			showDialog(THANKS_DIALOG);
			return true;
		} else if (item.getItemId() == R.id.donate) {
			showDialog(DONATE_DIALOG);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		final Dialog dialog;
		switch (id) {
		case ABOUT_DIALOG:
			dialog = new Dialog(mContext);
			dialog.setTitle(R.string.about_title);

			dialog.setContentView(R.layout.dialog_about);

			try {
				String VersionName = getPackageManager().getPackageInfo(
						getPackageName(), 0).versionName;
				TextView tvAboutText = (TextView) dialog
						.findViewById(R.id.tv_about_text);
				tvAboutText.setText(String.format(
						getString(R.string.about_text), VersionName));
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}

			dialog.findViewById(R.id.btn_about_ok).setOnClickListener(
					new OnClickListener() {

						@Override
						public void onClick(View v) {
							dialog.dismiss();
						}
					});
			break;
		case THANKS_DIALOG:
			dialog = new Dialog(mContext);
			dialog.setTitle(R.string.about_title);

			dialog.setContentView(R.layout.dialog_about);

			TextView tvAboutText = (TextView) dialog
					.findViewById(R.id.tv_about_text);
			tvAboutText.setText(R.string.credits_text);

			dialog.findViewById(R.id.btn_about_ok).setOnClickListener(
					new OnClickListener() {

						@Override
						public void onClick(View v) {
							dialog.dismiss();
						}
					});
			break;
		case DONATE_DIALOG:
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setMessage(R.string.donations_appreciated)
					.setCancelable(false)
					.setPositiveButton(R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									Intent i = new Intent();
									i.setClass(MainActivity.this, SendBitcoinsActivity.class);
									if(Consts.account.getNetwork() == Network.testNetwork){
								        i.putExtra(Consts.BTC_ADDRESS_KEY, Consts.TESTNET_DONATION_ADDRESS);
									}else{
								        i.putExtra(Consts.BTC_ADDRESS_KEY, Consts.DONATION_ADDRESS);
									}
								startActivityForResult(i, REQUEST_CODE_SEND_MONEY);
								}
							})
					.setNegativeButton(R.string.no,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
								}
							});
			dialog = builder.create();
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	private void UpdateInfo() {
		// first update from cache
		updateBalances(Consts.account.getCachedBalance(),
				Consts.account.getCachedCoinsOnTheWay());
		if (mGetInfoTask != null) {
			// we already have a task for getting account info in progress
			return;
		}
		btnSendMoney.setEnabled(false);
		btnTransactionHistory.setEnabled(false);
		pbBalanceUpdateProgress.setVisibility(View.VISIBLE);
		vBalanceUpdateView.setVisibility(View.VISIBLE);
		mGetInfoTask = Consts.account.requestAccountInfo(this);
	}

	@Override
	public void handleGetAccountInfoCallback(AccountInfo info,
			String errorMessage) {
		mGetInfoTask = null;
		pbBalanceUpdateProgress.setVisibility(View.INVISIBLE);
		vBalanceUpdateView.setVisibility(View.INVISIBLE);
		if (info == null) {
			vBalanceNoConnView.setVisibility(View.VISIBLE);
			if (!Utils.isConnected(this)) {
				Utils.showNoNetworkTip(this);
			}
		} else {
			updateBalances(info.getAvailableBalance(),
					info.getEstimatedBalance() - info.getAvailableBalance());
			vBalanceNoConnView.setVisibility(View.INVISIBLE);
			btnSendMoney.setEnabled(true);
			btnTransactionHistory.setEnabled(true);
		}
	}

	private void updateBalances(long balance, long onTheWayToMe) {
		if (balance >= 0) {
			tvBalance.setText(CoinUtils.valueString(balance) + " BTC");
			Ticker.requestBtcToUsd(balance, this);
		} else {
			tvBalance.setText(R.string.unknown);
			tvUsdValue.setText("");
		}
		if (onTheWayToMe >= 0) {
			tvEstimatedOnTheWay.setText(CoinUtils.valueString(onTheWayToMe)
					+ " BTC");
		} else {
			tvEstimatedOnTheWay.setText(R.string.unknown);
		}
	}

	@Override
	public void handleBtcToUsdCallback(Double usdValue) {
		if(usdValue == null){
			tvUsdValue.setText("");
		}else{
			String value = String.format("%1$.2f", usdValue); 
			tvUsdValue.setText(getResources().getString(R.string.usd_value, value));
		}
	}

	private void updateAddress() {
		String address = Consts.account.getPrimaryBitcoinAddress();
		tvAddress.setText(address);
		ivAddress.setImageBitmap(Utils.getPrimaryAddressAsSmallQrCode(Consts.account));
	}



}