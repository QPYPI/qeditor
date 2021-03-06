package com.quseit.texteditor.androidlib.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;

import com.quseit.texteditor.R;
import com.quseit.texteditor.androidlib.common.MiscUtils;

/**
 * It must contains two buttons : with the id buttonMail and buttonMarket. The
 * best way is to include the about_generic layout in the layout of children
 * activity
 */
@SuppressLint("Registered")
public class AboutActivity extends Activity implements OnClickListener {

	/**
	 * @see android.app.Activity#onResume()
	 */
	protected void onResume() {
		super.onResume();

		findViewById(R.id.buttonMail).setOnClickListener(this);
		findViewById(R.id.buttonMarket).setOnClickListener(this);
		findViewById(R.id.buttonRate).setOnClickListener(this);
	}

	/**
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	public void onClick(View view) {

		if (view.getId() == R.id.buttonMail) {
			CharSequence appName;
			appName = getPackageManager().getApplicationLabel(
					getApplicationInfo());
			MiscUtils.sendEmail(this, appName);
		} else if (view.getId() == R.id.buttonMarket) {
			MiscUtils.openMarket(this);
		} else if (view.getId() == R.id.buttonRate) {
			CharSequence appPackage;
			appPackage = getApplicationInfo().packageName;
			MiscUtils.openMarketApp(this, appPackage);
		}
	}
}
