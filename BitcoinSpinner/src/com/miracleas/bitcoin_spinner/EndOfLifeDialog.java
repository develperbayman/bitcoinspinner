package com.miracleas.bitcoin_spinner;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

public class EndOfLifeDialog extends Dialog {

   private int _linkColor;

   public EndOfLifeDialog(final Activity activity) {
      super(activity);
      _linkColor = 0xFF8080FF; // bright blue

      this.setContentView(R.layout.end_of_life_dialog);
      this.setTitle(R.string.eol_title);

      ((TextView) findViewById(R.id.tvMessage1)).setText(activity.getResources().getString(
            R.string.eol_message1));
      ((TextView) findViewById(R.id.tvMessage2)).setText(activity.getResources().getString(
            R.string.eol_message2));

      findViewById(R.id.btOK).setOnClickListener(new android.view.View.OnClickListener() {

         @Override
         public void onClick(View v) {
            EndOfLifeDialog.this.dismiss();
         }

      });

      TextView link1 = (TextView) findViewById(R.id.tvLink1);
      link1.setOnClickListener(new android.view.View.OnClickListener() {

         @Override
         public void onClick(View v) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri
                  .parse("https://play.google.com/store/apps/details?id=com.mycelium.wallet"));
            activity.startActivity(browserIntent);
            EndOfLifeDialog.this.dismiss();
         }

      });

      setLinkText(link1, link1.getText().toString());

   }

   private void setLinkText(TextView tv, String text) {
      SpannableString link = new SpannableString(text);
      link.setSpan(new UnderlineSpan(), 0, text.length(), 0);
      tv.setText(link);
      tv.setTextColor(_linkColor);
   }

}
