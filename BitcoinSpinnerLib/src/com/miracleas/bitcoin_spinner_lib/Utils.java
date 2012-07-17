package com.miracleas.bitcoin_spinner_lib;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Hashtable;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.ClipboardManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bccapi.api.Network;
import com.bccapi.core.HashUtils;
import com.bccapi.core.Asynchronous.AsynchronousAccount;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class Utils {

	private static final QRCodeWriter QR_CODE_WRITER = new QRCodeWriter();

	public static void showConnectionAlert(Context context) {
		if (isConnected(context)) {
			showAlert(context, R.string.unexpected_error);
		} else {
			showAlert(context, R.string.no_network);
		}
	}

	public static void showAlert(Context context, String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(message).setCancelable(false).setNeutralButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
			}
		});
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
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
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] NI = cm.getAllNetworkInfo();
		for (int i = 0; i < NI.length; i++) {
			if (NI[i].isConnected()) {
				return true;
			}
		}
		return false;
	}

	private static String _lastQrAddressStringSmall;
	private static Bitmap _lastQrAddressBitmapSmall;

	public static synchronized Bitmap getPrimaryAddressAsSmallQrCode(AsynchronousAccount account) {
		String address = account.getPrimaryBitcoinAddress();
		if (address.equals(_lastQrAddressStringSmall)) {
			return _lastQrAddressBitmapSmall;
		}
		_lastQrAddressStringSmall = address;
		_lastQrAddressBitmapSmall = getSmallQRCodeBitmap("bitcoin:" + address);
		return _lastQrAddressBitmapSmall;
	}

	private static String _lastQrAddressStringLarge;
	private static Bitmap _lastQrAddressBitmapLarge;

	public static Bitmap getLargeQRCodeBitmap(final String url) {
		// make size 85% of display size
		SpinnerContext sc = SpinnerContext.getInstance();
		int size = Math.min(sc.getDisplayWidth(), sc.getDisplayWidth()) * 85 / 100;
		return getQRCodeBitmapX(url, size);
	}

	public static Bitmap getSmallQRCodeBitmap(final String url) {
		// make size 34% of display size
		SpinnerContext sc = SpinnerContext.getInstance();
		int size = Math.min(sc.getDisplayWidth(), sc.getDisplayWidth()) * 34 / 100;
		return getQRCodeBitmapX(url, size);
	}

	private static Bitmap getQRCodeBitmapX(final String url, final int size) {
		try {
			final Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
			hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
			final BitMatrix result = QR_CODE_WRITER.encode(url, BarcodeFormat.QR_CODE, size, size, hints);

			final int width = result.getWidth();
			final int height = result.getHeight();
			final int[] pixels = new int[width * height];

			for (int y = 0; y < height; y++) {
				final int offset = y * width;
				for (int x = 0; x < width; x++) {
					pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.WHITE;
				}
			}

			final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
			return bitmap;
		} catch (final WriterException x) {
			x.printStackTrace();
			return null;
		}
	}

	public static URL getBccapiUrl(Network network) {
		try {
			if (network.equals(Network.testNetwork)) {
				return Consts.USE_CLOSED_TESTNET ? new URL("https://testnet.bccapi.com:445") : new URL(
						"https://testnet.bccapi.com:444");
			}
			return new URL("https://prodnet.bccapi.com:443");
		} catch (Exception e) {
			// never happens
			return null;
		}
	}

	private static String getSeedFileName(Network network) {
		if (network.equals(Network.testNetwork)) {
			return Consts.USE_CLOSED_TESTNET ? Consts.CLOSED_TESTNET_FILE : Consts.TESTNET_FILE;
		}
		return Consts.PRODNET_FILE;
	}

	public static byte[] readSeed(Context context, Network network) {
		String seedFile = getSeedFileName(network);
		byte[] seed = new byte[Consts.SEED_SIZE];
		FileInputStream fis;
		try {
			fis = context.openFileInput(seedFile);
			fis.read(seed);
			fis.close();
			return seed;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean writeSeed(Context context, Network network, byte[] seed) {
		String seedFile = getSeedFileName(network);
		FileOutputStream fos = null;
		try {
			fos = context.openFileOutput(seedFile, Context.MODE_PRIVATE);
			fos.write(seed);
			fos.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static byte[] createAndWriteSeed(Context context, Network network) {
		try {
			SecureRandom random = new SecureRandom();
			byte genseed[] = random.generateSeed(Consts.SEED_GEN_SIZE);
			byte[] seed = HashUtils.sha256(genseed);
			if (writeSeed(context, network, seed)) {
				return seed;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void startScannerActivity(Activity parent, int requestCode) {
		final PackageManager pm = parent.getPackageManager();
		if (pm.resolveActivity(Consts.zxingIntent, 0) != null) {
			parent.startActivityForResult(Consts.zxingIntent, requestCode);
		} else if (pm.resolveActivity(Consts.gogglesIntent, 0) != null) {
			parent.startActivity(Consts.gogglesIntent);
		} else {
			showScannerMarketPage(parent);
			Toast.makeText(parent, R.string.install_qr_scanner, Toast.LENGTH_LONG).show();
		}

	}

	private static void showScannerMarketPage(Context context) {
		final Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(Consts.MARKET_APP_URL,
				Consts.PACKAGE_NAME_ZXING)));
		if (context.getPackageManager().resolveActivity(marketIntent, 0) != null) {
			context.startActivity(marketIntent);
		} else {
			context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(Consts.WEBMARKET_APP_URL,
					Consts.PACKAGE_NAME_ZXING))));
		}
	}

	public static AlertDialog showPrimaryAddressQrCode(final Context context, AsynchronousAccount account) {

		String address = account.getPrimaryBitcoinAddress();
		Bitmap qrCode = _lastQrAddressBitmapLarge;
		if (!address.equals(_lastQrAddressStringLarge)) {
			_lastQrAddressStringLarge = address;
			_lastQrAddressBitmapLarge = getLargeQRCodeBitmap("bitcoin:" + address);
			qrCode = _lastQrAddressBitmapLarge;
		}
		return showQrCode(context, R.string.bitcoin_address, qrCode, address);

	}

	public static AlertDialog showQrCode(final Context context, int titleMessageId, Bitmap qrCode, final String value) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.dialog_qr_address, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(context).setView(layout);
		final AlertDialog qrCodeDialog = builder.create();
		qrCodeDialog.setCanceledOnTouchOutside(true);
		TextView text = (TextView) layout.findViewById(R.id.tv_title_text);
		text.setText(titleMessageId);

		ImageView qrAdress = (ImageView) layout.findViewById(R.id.iv_qr_Address);
		qrAdress.setImageBitmap(qrCode);
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
				ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
				clipboard.setText(value);
				Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
			}
		});

		qrCodeDialog.show();
		return qrCodeDialog;
	}
	
	public static void runPinProtectedFunction(final Context context,
			final Runnable fun) {
		if (SpinnerContext.getInstance().isPinProtected()) {
			Dialog d = new PinDialog(context, true,
					new PinDialog.OnPinEntered() {

						@Override
						public void pinEntered(PinDialog dialog, String pin) {
							if (pin.equals(SpinnerContext.getInstance()
									.getPin())) {
								dialog.dismiss();
								fun.run();
							} else {
								Toast.makeText(context, R.string.pin_invalid_pin,
										Toast.LENGTH_LONG).show();
								dialog.dismiss();
							}
						}
					});
			d.setTitle(R.string.pin_enter_pin);
			d.show();
		} else {
			fun.run();
		}
	}

}
