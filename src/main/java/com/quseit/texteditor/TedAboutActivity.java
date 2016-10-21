package com.quseit.texteditor;

import android.os.Bundle;
import com.quseit.texteditor.androidlib.ui.activity.AboutActivity;

public class TedAboutActivity extends AboutActivity {

	/**
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.layout_about);
	}
}
