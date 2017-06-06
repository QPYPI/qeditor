package com.quseit.texteditor;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.quseit.base.MyApp;
import com.quseit.util.NAction;

import java.text.MessageFormat;

public class OAboutActivity extends BaseActivity {
	//private static final String TAG = "OAboutActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarContentView(R.layout.o_about);
        setTitle(R.string.m_title_aboutus);
        initWidgetTabItem(3);
        
		//NAction.recordUserLog(getApplicationContext(), "about", "");

		//initAD();
        
        String[] appConf = NAction.getAppConf(getApplicationContext());
        String about = appConf[0];
        String link = appConf[1];
        String privacyTitle = appConf[2];
        String privacyUrl = appConf[3];

        //String selfcheck = appConf[4];
        //String selfCheckTitle = appConf[5];
        
        TextView aboutT = (TextView)findViewById(R.id.about);
        TextView marketLink = (TextView)findViewById(R.id.market_link);
        TextView feedT = (TextView)findViewById(R.id.privacy_title);
        TextView feedLink = (TextView)findViewById(R.id.privacy_link);
        //TextView updateT = (TextView)findViewById(R.id.update_content);

        //Button selfCheckBtn = (Button)findViewById(R.id.selfcheck_btn);
        
        if (!about.equals("")) {
        	aboutT.setText(about);
        } else {
        	aboutT.setText(getString(R.string.about_content));
        }
        if (!link.equals("")) {
        	marketLink.setText(link);
        	marketLink.setVisibility(View.VISIBLE);
        }
        
        if (!privacyTitle.equals("")) {
        	feedT.setText(privacyTitle);
        	feedT.setVisibility(View.VISIBLE);
        }
        if (!privacyUrl.equals("") && !privacyTitle.equals("")) {
        	feedLink.setText(privacyUrl);
        	feedLink.setVisibility(View.VISIBLE);
        }
        
        MNApp mnApp = (MNApp) this.getApplication();

        MyApp.getInstance().addActivity(this); 
    }
	
	public void checkUpdate(View v) {
		//if (NUtil.netCheckin(getApplicationContext())) {
			String[] conf = NAction.getAppConf(getApplicationContext());
			if (conf[6].equals("")) {
				checkConfUpdate(getApplicationContext());
				
			}  else {
				NAction.recordAdLog(getApplicationContext(), "feedback", "");
				Intent intent = NAction.getLinkAsIntent(this, conf[6]);
				this.startActivity(intent);	
			}
		/*} else {
			Toast.makeText(getApplicationContext(), R.string.net_error, Toast.LENGTH_SHORT).show();
		}*/
	}
	
    public void onShare(View v) {
		NAction.recordUseLog(getApplicationContext(), "ishare", "");

        String[] appConf = NAction.getAppConf(getApplicationContext());
        String about = appConf[0];
        //String link = appConf[1];
        //String feed = appConf[2];
        String feedUrl = appConf[3];
        
        if (feedUrl.equals("")) {
        	feedUrl = getString(R.string.app_url);
        }
		String shareContent = MessageFormat.format(getString(R.string.share_info), feedUrl);

        if (!about.equals("")) {
        	shareContent = about+" "+feedUrl;
        }  else {
        	shareContent = MessageFormat.format(getString(R.string.share_info), feedUrl);
        }
		Intent share = new Intent(Intent.ACTION_SEND);
		share.setType("text/plain");
		share.putExtra(Intent.EXTRA_TEXT, shareContent);

		startActivity(Intent.createChooser(share, getString(R.string.share)));
    }

}
