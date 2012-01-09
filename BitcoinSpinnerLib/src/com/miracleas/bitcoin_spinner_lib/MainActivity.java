package com.miracleas.bitcoin_spinner_lib;

import java.util.Hashtable;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
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
import com.bccapi.core.CoinUtils;
import com.bccapi.core.Asynchronous.AccountTask;
import com.bccapi.core.Asynchronous.GetAccountInfoCallbackHandler;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.miracleas.bitcoin_spinner_lib.SimpleGestureFilter.SimpleGestureListener;

public class MainActivity extends Activity implements SimpleGestureListener,
		GetAccountInfoCallbackHandler {

	private Context mContext;

	private TextView tvAddress, tvBalance, tvEstimatedOnTheWay;
	private ImageView ivAddress;
	private Button btnSendMoney, btnTransactionHistory;
	private RelativeLayout rlBalance;
	private ProgressBar pbBalanceUpdateProgress;
	private View vBalanceUpdateView;
	private View vBalanceNoConnView;
	private AccountTask mGetInfoTask;

	private AlertDialog qrCodeDialog;
	private static final QRCodeWriter QR_CODE_WRITER = new QRCodeWriter();

	private static final int REQUEST_CODE_SEND_MONEY = 10001;
	private static final int REQUEST_CODE_SETTINGS = 10002;

	private static final int ABOUT_DIALOG = 1001;
	private static final int THANKS_DIALOG = 1002;

	private SimpleGestureFilter detector;

	private SharedPreferences preferences;

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

		// new Thread(ConnectionWatcher).start();

		// for (String address : Consts.account.getAddresses()) {
		// mAddress = address;
		// break;
		// }

		// editor.putString(Consts.BITCOIN_ADDRESS, mAddress);
		// editor.commit();

		tvAddress = (TextView) findViewById(R.id.tv_address);
		ivAddress = (ImageView) findViewById(R.id.iv_address);
		tvBalance = (TextView) findViewById(R.id.tv_balance);
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
		Editor editor = preferences.edit();
		if (null != qrCodeDialog) {
			editor.putBoolean("ShowQrCode", qrCodeDialog.isShowing());
			qrCodeDialog.dismiss();
		} else {
			editor.putBoolean("ShowQrCode", false);
		}
		editor.commit();
	}

	// private Runnable ConnectionWatcher = new Runnable() {
	// @Override
	// public void run() {
	// while (true) {
	// if (isConnected()) {
	// Message message = handler.obtainMessage();
	// message.arg1 = CONNECTION_MESSAGE;
	// handler.sendMessage(message);
	// } else {
	// Message message = handler.obtainMessage();
	// message.arg1 = NO_CONNECTION_MESSAGE;
	// handler.sendMessage(message);
	// }
	// try {
	// Thread.sleep(1000);
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// }
	// }
	// }
	// };

	@Override
	public boolean dispatchTouchEvent(MotionEvent me) {
		this.detector.onTouchEvent(me);
		return super.dispatchTouchEvent(me);
	}

	public static Bitmap getQRCodeBitmap(final String url, final int size) {
		try {
			final Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
			hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
			final BitMatrix result = QR_CODE_WRITER.encode(url,
					BarcodeFormat.QR_CODE, size, size, hints);

			final int width = result.getWidth();
			final int height = result.getHeight();
			final int[] pixels = new int[width * height];

			for (int y = 0; y < height; y++) {
				final int offset = y * width;
				for (int x = 0; x < width; x++) {
					pixels[offset + x] = result.get(x, y) ? Color.BLACK
							: Color.WHITE;
				}
			}

			final Bitmap bitmap = Bitmap.createBitmap(width, height,
					Bitmap.Config.ARGB_8888);
			bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
			return bitmap;
		} catch (final WriterException x) {
			x.printStackTrace();
			return null;
		}
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
			qrAdress.setImageBitmap(getQRCodeBitmap(
					"bitcoin:" + Consts.account.getPrimaryBitcoinAddress(), 320));
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
				UpdateInfo();
			} else if (requestCode == REQUEST_CODE_SETTINGS) {
				UpdateInfo();
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

	// private final OnLongClickListener addressOnLongClickListener = new
	// OnLongClickListener() {
	//
	// @Override
	// public boolean onLongClick(View v) {
	// return true;
	// }
	// };
	//

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
		} else {
			tvBalance.setText(R.string.unknown);
		}
		if (onTheWayToMe >= 0) {
			tvEstimatedOnTheWay.setText(CoinUtils.valueString(onTheWayToMe)
					+ " BTC");
		} else {
			tvEstimatedOnTheWay.setText(R.string.unknown);
		}
	}

	private void updateAddress() {
		String address = Consts.account.getPrimaryBitcoinAddress();
		tvAddress.setText(address);
		ivAddress.setImageBitmap(getQRCodeBitmap("bitcoin:" + address, 100));
	}

}