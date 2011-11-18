package com.miracle.bitcoin_spinner;

import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.bccapi.api.APIException;
import com.miracle.bitcoin_spinner.SimpleGestureFilter.SimpleGestureListener;
import com.bccapi.core.CoinUtils;

public class MainActivity extends Activity implements SimpleGestureListener {

	private Context mContext;
	private String mAddress;
	private String mBalance, mEstimatedOnTheWay;

	private TextView tvAddress, tvBalance, tvEstimatedOnTheWay;
	private ImageView ivAddress;
	private Button btnSendMoney;
	private RelativeLayout rlBalance;
	private ProgressBar pbBalanceUpdateProgress;
	private View vBalanceUpdateView;

	private AlertDialog qrCodeDialog;
	private static final QRCodeWriter QR_CODE_WRITER = new QRCodeWriter();

	private static final int REQUEST_CODE_SEND_MONEY = 10001;
	private static final int REQUEST_CODE_SETTINGS = 10002;

	private static final int ABOUT_DIALOG = 1001;

	private static final int UPDATE_BALANCES_MESSAGE = 101;
	private static final int NO_CONNECTION_MESSAGE = 102;
	protected static final int CONNECTION_MESSAGE = 103;

	private SimpleGestureFilter detector;

	private SharedPreferences preferences;
	private SharedPreferences.Editor editor;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		preferences = getSharedPreferences(Consts.PREFS_NAME, MODE_PRIVATE);
		editor = preferences.edit();

		mContext = this;

		detector = new SimpleGestureFilter(this, this);

		new Thread(ConnectionWatcher).start();

		for (String address : Consts.account.getAddresses()) {
			mAddress = address;
			break;
		}
		
		editor.putString(Consts.BITCOIN_ADDRESS, mAddress);
		editor.commit();

		tvAddress = (TextView) findViewById(R.id.tv_address);
		ivAddress = (ImageView) findViewById(R.id.iv_address);
		tvBalance = (TextView) findViewById(R.id.tv_balance);
		tvEstimatedOnTheWay = (TextView) findViewById(R.id.tv_estimated_on_the_way);
		btnSendMoney = (Button) findViewById(R.id.btn_send_money);

		tvAddress.setOnClickListener(addressClickListener);
		tvAddress.setOnLongClickListener(addressOnLongClickListener);

		ivAddress.setOnClickListener(qrClickListener);

		rlBalance = (RelativeLayout) findViewById(R.id.rl_balance_ref);
		mBalance = CoinUtils.valueString(Consts.info.getAvailableBalance());
		tvBalance.setText(mBalance + " BTC");
		mEstimatedOnTheWay = CoinUtils.valueString(Consts.info.getEstimatedBalance()
				- Consts.info.getAvailableBalance());
		tvEstimatedOnTheWay.setText(mEstimatedOnTheWay + " BTC");

		btnSendMoney.setOnClickListener(sendMoneyClickListener);
		rlBalance.setOnClickListener(refreshMoneyClickListener);
		rlBalance.setOnLongClickListener(balanceOnLongClickListener);

		pbBalanceUpdateProgress = (ProgressBar) findViewById(R.id.pb_balance_update);
		vBalanceUpdateView = findViewById(R.id.v_balance_update);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (preferences.getBoolean("ShowQrCode", false)) {
			ivAddress.performClick();
		}
		if (isConnected()) {
			logInAndUpdate();
		}
		updateAddress();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (null != qrCodeDialog) {
			editor.putBoolean("ShowQrCode", qrCodeDialog.isShowing());
			editor.commit();
			qrCodeDialog.dismiss();
		} else {
			editor.putBoolean("ShowQrCode", false);
			editor.commit();
		}
	}

	private Runnable ConnectionWatcher = new Runnable() {
		@Override
		public void run() {
			while (true) {
				Consts.isConnected(mContext);
				if (isConnected()) {
					Message message = handler.obtainMessage();
					message.arg1 = CONNECTION_MESSAGE;
					handler.sendMessage(message);
				} else {
					Message message = handler.obtainMessage();
					message.arg1 = NO_CONNECTION_MESSAGE;
					handler.sendMessage(message);
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
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
			ImageView qrAdress = (ImageView) layout
					.findViewById(R.id.iv_qr_Address);
			qrAdress.setImageBitmap(getQRCodeBitmap("bitcoin:" + mAddress, 320));
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
					clipboard.setText(mAddress);
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
			ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			clipboard.setText(mAddress);
			Toast.makeText(mContext, R.string.clipboard_copy,
					Toast.LENGTH_SHORT).show();
		}
	};

	private final OnClickListener sendMoneyClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent();
			intent.setClass(MainActivity.this, SendMoneyActivity.class);
			intent.putExtra("available_coins", mBalance);
			startActivityForResult(intent, REQUEST_CODE_SEND_MONEY);
		}
	};

	private final OnClickListener refreshMoneyClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			logInAndUpdate();
		}
	};

	@Override
	public void onActivityResult(final int requestCode, final int resultCode,
			final Intent intent) {
		if (isConnected()) {
			if (requestCode == REQUEST_CODE_SEND_MONEY) {
				logInAndUpdate(true);
				updateAddress();
			} else if (requestCode == REQUEST_CODE_SETTINGS) {
				logInAndUpdate();
				updateAddress();
			}
		}
	}

	@Override
	public void onSwipe(int direction) {
		Intent intent = new Intent();

		switch (direction) {

		case SimpleGestureFilter.SWIPE_LEFT:
			intent.setClass(this, SendMoneyActivity.class);
			startActivity(intent);
			break;

		}
	}

	@Override
	public void onDoubleTap() {

	}

	private final OnLongClickListener addressOnLongClickListener = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TEXT, "bitcoin:\n" + mAddress);
			startActivity(Intent.createChooser(intent,
					"Share your bitcoin address..."));
			return true;
		}
	};

	private final OnLongClickListener balanceOnLongClickListener = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			logInAndUpdate(true);
			return true;
		}
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
		switch (item.getItemId()) {
		case R.id.about:
			showDialog(ABOUT_DIALOG);
			return true;
		case R.id.settings:
			startActivityForResult(new Intent(this, SettingsActivity.class),
					REQUEST_CODE_SETTINGS);
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case ABOUT_DIALOG:
			dialog = new Dialog(mContext);
			dialog.setTitle(R.string.about_title);
			dialog.setContentView(R.layout.dialog_about);
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	private void logInAndUpdate() {
		logInAndUpdate(false);
	}
	
	private void logInAndUpdate(boolean force) {
		Date thisLogin = new Date();
		long loginDiff = thisLogin.getTime() - Consts.lastLogin.getTime();

		pbBalanceUpdateProgress.setVisibility(View.VISIBLE);
		vBalanceUpdateView.setVisibility(View.VISIBLE);

		if (loginDiff > 1140000 || force) {
			Consts.lastLogin = new Date();
			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						Consts.account.login();
						Consts.info = Consts.account.getInfo();
						Message message = handler.obtainMessage();
						message.arg1 = UPDATE_BALANCES_MESSAGE;
						handler.sendMessage(message);
					} catch (APIException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			t.start();
		} else {
			Message message = handler.obtainMessage();
			message.arg1 = UPDATE_BALANCES_MESSAGE;
			handler.sendMessage(message);
		}
	}

	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.arg1) {
			case UPDATE_BALANCES_MESSAGE:
				updateBalances();
				handler.post(new Runnable() {
					public void run() {
						pbBalanceUpdateProgress.setVisibility(View.INVISIBLE);
						vBalanceUpdateView.setVisibility(View.INVISIBLE);
					}
				});
				break;

			case NO_CONNECTION_MESSAGE:
				vBalanceUpdateView.setVisibility(View.VISIBLE);
				break;

			case CONNECTION_MESSAGE:
				vBalanceUpdateView.setVisibility(View.INVISIBLE);
				break;
			}
		}
	};
	

	private void updateBalances() {
		mBalance = CoinUtils.valueString(Consts.info.getAvailableBalance());
		tvBalance.setText(mBalance + " BTC");
		mEstimatedOnTheWay = CoinUtils.valueString(Consts.info.getEstimatedBalance()
				- Consts.info.getAvailableBalance());
		tvEstimatedOnTheWay.setText(mEstimatedOnTheWay + " BTC");
		
		editor.putString(Consts.AVAIABLE_BITCOINS, mBalance);
		editor.putString(Consts.BITCOINS_ON_THE_WAY, mEstimatedOnTheWay);
		editor.commit();
	}

	private void updateAddress() {
		mAddress = preferences.getString(Consts.BITCOIN_ADDRESS, "");

		tvAddress.setText(mAddress);
		ivAddress.setImageBitmap(getQRCodeBitmap("bitcoin:" + mAddress, 100));
	}
	
	private boolean isConnected() {
		return Consts.isConnected(mContext);
	}
}