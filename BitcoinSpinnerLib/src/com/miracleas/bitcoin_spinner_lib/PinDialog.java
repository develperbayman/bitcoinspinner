package com.miracleas.bitcoin_spinner_lib;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PinDialog extends Dialog {

	public interface OnPinEntered {
		void pinEntered(PinDialog dialog, String pin);
	}

	private LinearLayout _pinButtons;
	private Button _b0;
	private Button _b1;
	private Button _b2;
	private Button _b3;
	private Button _b4;
	private Button _b5;
	private Button _b6;
	private Button _b7;
	private Button _b8;
	private Button _b9;
	private TextView _d1;
	private TextView _d2;
	private TextView _d3;
	private TextView _d4;
	private TextView _d5;
	private TextView _d6;
	private String _enteredPin;
	private OnPinEntered _onPinValid;
	private boolean _hidden;

	public PinDialog(Context context, boolean hidden, OnPinEntered onPinEntered) {
		super(context);
		setContentView(R.layout.dialog_enter_pin);
		_hidden = hidden;
		_onPinValid = onPinEntered;
		_pinButtons = (LinearLayout) findViewById(R.id.pin_buttons);
		_d1 = (TextView) findViewById(R.id.pin_char_1);
		_d2 = (TextView) findViewById(R.id.pin_char_2);
		_d3 = (TextView) findViewById(R.id.pin_char_3);
		_d4 = (TextView) findViewById(R.id.pin_char_4);
		_d5 = (TextView) findViewById(R.id.pin_char_5);
		_d6 = (TextView) findViewById(R.id.pin_char_6);
		_enteredPin = "";
		_b0 = ((Button) findViewById(R.id.pin_button0));
		_b0.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addDigit('0');
			}
		});
		_b1 = ((Button) findViewById(R.id.pin_button1));
		_b1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addDigit('1');
			}
		});
		_b2 = ((Button) findViewById(R.id.pin_button2));
		_b2.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addDigit('2');
			}
		});
		_b3 = ((Button) findViewById(R.id.pin_button3));
		_b3.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addDigit('3');
			}
		});
		_b4 = ((Button) findViewById(R.id.pin_button4));
		_b4.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addDigit('4');
			}
		});
		_b5 = ((Button) findViewById(R.id.pin_button5));
		_b5.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addDigit('5');
			}
		});
		_b6 = ((Button) findViewById(R.id.pin_button6));
		_b6.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addDigit('6');
			}
		});
		_b7 = ((Button) findViewById(R.id.pin_button7));
		_b7.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addDigit('7');
			}
		});
		_b8 = ((Button) findViewById(R.id.pin_button8));
		_b8.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addDigit('8');
			}
		});
		_b9 = ((Button) findViewById(R.id.pin_button9));
		_b9.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addDigit('9');
			}
		});

	}

	private void addDigit(char c) {
		_enteredPin = _enteredPin + c;
		_d1.setText(getPinDigitAsString(_enteredPin, 0));
		_d2.setText(getPinDigitAsString(_enteredPin, 1));
		_d3.setText(getPinDigitAsString(_enteredPin, 2));
		_d4.setText(getPinDigitAsString(_enteredPin, 3));
		_d5.setText(getPinDigitAsString(_enteredPin, 4));
		_d6.setText(getPinDigitAsString(_enteredPin, 5));
		checkPin();
	}

	private String getPinDigitAsString(String pin, int index) {
		if (pin.length() > index) {
			return _hidden ? "*" : pin.substring(index, index + 1);
		} else {
			return "";
		}
	}

	private void clearDigits() {
		_enteredPin = "";
		_d1.setText("");
		_d2.setText("");
		_d3.setText("");
		_d4.setText("");
		_d5.setText("");
		_d6.setText("");
	}

	private void enableButtons(boolean enabled) {
		_b0.setEnabled(enabled);
		_b1.setEnabled(enabled);
		_b2.setEnabled(enabled);
		_b3.setEnabled(enabled);
		_b4.setEnabled(enabled);
		_b5.setEnabled(enabled);
		_b6.setEnabled(enabled);
		_b7.setEnabled(enabled);
		_b8.setEnabled(enabled);
		_b9.setEnabled(enabled);
	}

	private void checkPin() {
		if (_enteredPin.length() >= 6) {
			enableButtons(false);
			delayhandler.sendMessage(delayhandler.obtainMessage());
		}
	}

	/**
	 * Trick to make the last digit update before the dialog is disabled
	 */
	final Handler delayhandler = new Handler() {
		public void handleMessage(Message msg) {
			_onPinValid.pinEntered(PinDialog.this, _enteredPin);
			enableButtons(true);
			clearDigits();
		}
	};

}
