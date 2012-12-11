package com.miracleas.bitcoin_spinner;

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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bccapi.bitlib.model.Address;
import com.bccapi.bitlib.util.CoinUtil;
import com.bccapi.ng.api.ApiError;
import com.bccapi.ng.api.Balance;
import com.bccapi.ng.api.QueryBalanceResponse;
import com.bccapi.ng.async.AbstractCallbackHandler;
import com.bccapi.ng.async.AsyncTask;
import com.bccapi.ng.async.AsynchronousApi;
import com.miracleas.bitcoin_spinner.MultiTicker.TickerCallbackHandler;
import com.miracleas.bitcoin_spinner.SimpleGestureFilter.SimpleGestureListener;

public class MainActivity extends Activity implements SimpleGestureListener, TickerCallbackHandler,
    AbstractCallbackHandler<QueryBalanceResponse> {

  private static final int REQUEST_CODE_SEND_MONEY = 10001;
  private static final int REQUEST_CODE_SETTINGS = 10002;
  private static final int ABOUT_DIALOG = 1001;
  private static final int THANKS_DIALOG = 1002;
  private static final int DONATE_DIALOG = 1003;

  private Context mContext;
  private TextView tvAddress, tvBalance, tvCurrencyValue, tvEstimatedOnTheWay;
  private ImageView ivAddress;
  private Button btnSendMoney, btnTransactionHistory;
  private RelativeLayout rlBalance;
  private ProgressBar pbBalanceUpdateProgress;
  private View vBalanceUpdateView;
  private View vBalanceNoConnView;
  private AsyncTask mGetInfoTask;
  private CharSequence mRawAppName;
  private AlertDialog qrCodeDialog;
  private SimpleGestureFilter detector;
  private SharedPreferences preferences;
  private ConnectionWatcher mConnectionWatcher;
  private Balance mLatestBalance;
  private boolean mShowBackupWarning;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mRawAppName = this.getTitle();
    mContext = this;
    preferences = getSharedPreferences(Consts.PREFS_NAME, MODE_PRIVATE);
    if (!SpinnerContext.isInitialized()) {
      SpinnerContext.initialize(this, getWindowManager().getDefaultDisplay());
    }
    // Should we show the backup warning if there are coins in the account?
    mShowBackupWarning = !SpinnerContext.getInstance().isBackupWarningDisabled();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!preferences.getString(Consts.LOCALE, "").matches("")) {
      Locale locale = new Locale(preferences.getString(Consts.LOCALE, "en"));
      Locale.setDefault(locale);
      Configuration config = new Configuration();
      config.locale = locale;
      getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }
    setContentView(R.layout.main);
    mLatestBalance = null;
    detector = new SimpleGestureFilter(this, this);
    mConnectionWatcher = new ConnectionWatcher(mContext);
    new Thread(mConnectionWatcher).start();

    tvAddress = (TextView) findViewById(R.id.tv_address);
    ivAddress = (ImageView) findViewById(R.id.iv_address);
    tvBalance = (TextView) findViewById(R.id.tv_balance);
    tvCurrencyValue = (TextView) findViewById(R.id.tv_usd_value);
    tvEstimatedOnTheWay = (TextView) findViewById(R.id.tv_estimated_on_the_way);
    btnSendMoney = (Button) findViewById(R.id.btn_send_money);
    btnTransactionHistory = (Button) findViewById(R.id.btn_transaction_history);

    tvAddress.setOnClickListener(addressClickListener);

    ivAddress.setOnClickListener(qrClickListener);

    rlBalance = (RelativeLayout) findViewById(R.id.rl_balance_ref);
    btnSendMoney.setOnClickListener(sendMoneyClickListener);
    btnTransactionHistory.setOnClickListener(transactionHistoryClickListener);
    rlBalance.setOnClickListener(refreshMoneyClickListener);
    rlBalance.setOnLongClickListener(refreshMoneyLongClickListener);

    pbBalanceUpdateProgress = (ProgressBar) findViewById(R.id.pb_balance_update);
    vBalanceUpdateView = findViewById(R.id.v_balance_update);
    vBalanceNoConnView = findViewById(R.id.v_balance_no_conn);

    if (preferences.getBoolean("ShowQrCode", false)) {
      ivAddress.performClick();
    }
    updateInfo();
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
                updateInfo();
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
      Utils.showPrimaryAddressQrCode(mContext, SpinnerContext.getInstance().getAsyncApi());
    }
  };

  private final OnClickListener addressClickListener = new OnClickListener() {

    @Override
    public void onClick(View v) {
      Intent intent = new Intent(Intent.ACTION_SEND);
      intent.setType("text/plain");
      intent.putExtra(Intent.EXTRA_TEXT, "bitcoin:"
          + SpinnerContext.getInstance().getAsyncApi().getPrimaryBitcoinAddress());
      startActivity(Intent.createChooser(intent, getString(R.string.share_bitcoin_address)));
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
      updateInfo();
      return true;
    }
  };

  private final OnClickListener refreshMoneyClickListener = new OnClickListener() {

    @Override
    public void onClick(View v) {
      updateInfo();
    }
  };

  @Override
  public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
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

  /** Called when menu item is selected */
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    if (item.getItemId() == R.id.about) {
      showDialog(ABOUT_DIALOG);
      return true;
    } else if (item.getItemId() == R.id.settings) {
      startActivityForResult(new Intent(this, SettingsActivity.class), REQUEST_CODE_SETTINGS);
      return true;
    } else if (item.getItemId() == R.id.address_book) {
      startActivity(new Intent(this, AddressBookActivity.class));
      return true;
    } else if (item.getItemId() == R.id.donate) {
      showDialog(DONATE_DIALOG);
      return true;
    } else if (item.getItemId() == R.id.buy_coins) {
      startBuyCoins();
      return true;
    } else {
      return super.onOptionsItemSelected(item);
    }
  }

  private void startBuyCoins() {
    Address address = SpinnerContext.getInstance().getAsyncApi().getPrimaryBitcoinAddress();
    Uri uri = Uri.parse("http://www.bitinstant.com/mobile_deposit?addr=" + address.toString());
    Intent myIntent = new Intent(Intent.ACTION_VIEW, uri);
    startActivity(myIntent);
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
        String VersionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        TextView tvAboutText = (TextView) dialog.findViewById(R.id.tv_about_text);
        tvAboutText.setText(String.format(getString(R.string.about_text), VersionName));
      } catch (NameNotFoundException e) {
        e.printStackTrace();
      }

      dialog.findViewById(R.id.btn_about_ok).setOnClickListener(new OnClickListener() {

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

      TextView tvAboutText = (TextView) dialog.findViewById(R.id.tv_about_text);
      tvAboutText.setText(R.string.credits_text);

      dialog.findViewById(R.id.btn_about_ok).setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
          dialog.dismiss();
        }
      });
      break;
    case DONATE_DIALOG:
      AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
      builder.setMessage(R.string.donations_appreciated).setCancelable(false)
          .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              Intent i = new Intent();
              i.setClass(MainActivity.this, SendBitcoinsActivity.class);
              if (SpinnerContext.getInstance().getNewNetwork().isTestnet()) {
                i.putExtra(Consts.BTC_ADDRESS_KEY, Consts.TESTNET_DONATION_ADDRESS);
              } else {
                i.putExtra(Consts.BTC_ADDRESS_KEY, Consts.DONATION_ADDRESS);
              }
              startActivityForResult(i, REQUEST_CODE_SEND_MONEY);
            }
          }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
          });
      dialog = builder.create();
      break;
    default:
      dialog = null;
    }
    return dialog;
  }

  private void updateInfo() {
    // first update from cache
    AsynchronousApi api = SpinnerContext.getInstance().getAsyncApi();
    Balance balance = api.getCachedBalance();
    updateBalances(balance);
    btnSendMoney.setEnabled(false);
    btnTransactionHistory.setEnabled(false);
    pbBalanceUpdateProgress.setVisibility(View.VISIBLE);
    vBalanceUpdateView.setVisibility(View.VISIBLE);
    if (mGetInfoTask != null) {
      // we already have a task for getting account info in progress
      return;
    }
    mGetInfoTask = api.queryBalance(this);
  }

  private void showBackupWarning() {
    final Dialog dialog = new Dialog(mContext);
    dialog.setTitle(R.string.backup_warning_title);
    dialog.setContentView(R.layout.dialog_message_with_disable);
    dialog.findViewById(R.id.btn_ok).setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        CheckBox cb = (CheckBox) dialog.findViewById(R.id.cb_disable);
        if (cb.isChecked()) {
          SpinnerContext.getInstance().markBackupWarningDisabled();
        }
        dialog.dismiss();
      }
    });
    TextView tv = (TextView) dialog.findViewById(R.id.tv_text);
    tv.setText(R.string.backup_warning);
    dialog.show();
  }

  private void updateBalances(Balance balance) {
    mLatestBalance = balance;
    if (balance != null) {
      tvBalance.setText(CoinUtil.valueString(balance.unspent + balance.pendingChange) + " BTC");
      tvEstimatedOnTheWay.setText(CoinUtil.valueString(balance.pendingReceiving) + " BTC");
      String localCurrency = preferences.getString(Consts.LOCAL_CURRENCY, Consts.DEFAULT_CURRENCY);
      MultiTicker.requestTicker(localCurrency, this);
    } else {
      tvBalance.setText(R.string.unknown);
      tvEstimatedOnTheWay.setText(R.string.unknown);
      tvCurrencyValue.setText("");
    }
  }

  @Override
  public void handleTickerCallback(String currency, Double value) {
    if (value != null && mLatestBalance != null) {
      Double btc = Double.valueOf(mLatestBalance.unspent + mLatestBalance.pendingChange) / Consts.SATOSHIS_PER_BITCOIN;
      Double converted = btc * value;
      String text = String.format(Locale.US, "%1$.2f %2$s", converted, currency);
      tvCurrencyValue.setText(getResources().getString(R.string.worth_about, text));
      // Add the local currency value to the title
      String title = String.format(Locale.US, "        %1$.2f %2$s", value, currency);
      this.setTitle(mRawAppName + title);
    } else {
      tvCurrencyValue.setText("");
      this.setTitle(mRawAppName);
    }
  }

  private void updateAddress() {
    Address address = SpinnerContext.getInstance().getAsyncApi().getPrimaryBitcoinAddress();
    tvAddress.setText(address.toString());
    ivAddress.setScaleType(ScaleType.CENTER_CROP);
    ivAddress.setImageBitmap(Utils.getPrimaryAddressAsSmallQrCode(SpinnerContext.getInstance().getAsyncApi()));
    ivAddress.setVisibility(View.VISIBLE);
  }

  // @Override
  // public void handleGetAccountInfoCallback(AccountInfo info, String
  // errorMessage) {
  // mGetInfoTask = null;
  // pbBalanceUpdateProgress.setVisibility(View.INVISIBLE);
  // if (info == null) {
  // if (!Utils.isConnected(this)) {
  // Utils.showNoNetworkTip(this);
  // } else {
  // Utils.showNoServerConnectivityTip(this);
  // // This may also be because we cannot do first time login
  // // due to account creation disabled on server side
  // }
  // } else {
  // // We have had contact with the server, so we know our public key
  // // has been registered
  // if (!SpinnerContext.getInstance().isPublicKeyRegistered()) {
  // // This is the first time this installation has had server
  // // contact. The server now observes our public key
  // SpinnerContext.getInstance().markPublicKeyRegistered();
  // // Now we are ready to show our bitcoin address
  // updateAddress();
  // }
  // vBalanceUpdateView.setVisibility(View.INVISIBLE);
  // if (mShowBackupWarning && info.getAvailableBalance() > 0) {
  // mShowBackupWarning = false;
  // showBackupWarning();
  // }
  // updateBalances(info.getAvailableBalance(), info.getEstimatedBalance() -
  // info.getAvailableBalance());
  // vBalanceNoConnView.setVisibility(View.INVISIBLE);
  // btnSendMoney.setEnabled(true);
  // btnTransactionHistory.setEnabled(true);
  // }
  // }

  @Override
  public void handleCallback(QueryBalanceResponse response, ApiError error) {
    mGetInfoTask = null;
    pbBalanceUpdateProgress.setVisibility(View.INVISIBLE);
    if (response == null) {
      if (!Utils.isConnected(this)) {
        Utils.showNoNetworkTip(this);
      } else {
        Utils.showNoServerConnectivityTip(this);
        // This may also be because we cannot do first time login
        // due to account creation disabled on server side
      }
    } else {
      Balance balance = response.balance;
      vBalanceUpdateView.setVisibility(View.INVISIBLE);
      // Check whether we should show a backup warning
      if (mShowBackupWarning && balance.unspent + balance.pendingChange > 0) {
        mShowBackupWarning = false;
        showBackupWarning();
      }
      updateBalances(balance);
      vBalanceNoConnView.setVisibility(View.INVISIBLE);
      btnSendMoney.setEnabled(true);
      btnTransactionHistory.setEnabled(true);
    }
  }

}