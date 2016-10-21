package com.quseit.texteditor;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.quseit.base.MyApp;
import com.quseit.base._WBase;
import com.quseit.util.NAction;
import com.quseit.util.NUtil;
import com.quseit.view.AdSlidShowView;
import com.quseit.view.AdSlidShowView.urlBackcall;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MSettingActivity extends BaseActivity {
	private static final String TAG = "setting";
    private static final int SCRIPT_EXEC_CODE = 1235;  

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarContentView(R.layout.m_setting);
        setTitle(R.string.m_title_3);
        //ArrayList<String> av = new ArrayList();
        //String k = av.get(1);
        
        //initWidgetTabItem(4);
        initAD(TAG);
        
        // alpha
        /*if (NAction.checkPluginNoAdEnable(getApplicationContext())) {
        	RelativeLayout tb = (RelativeLayout)findViewById(R.id.plugin_setting_box);
        	tb.setVisibility(View.VISIBLE);
        }*/
        // extend plugin
        if (NAction.checkIfScriptExtend(getApplicationContext())) {
        	RelativeLayout sb = (RelativeLayout)findViewById(R.id.plugin_script_box);
        	sb.setVisibility(View.VISIBLE);
        }
        
        if (NAction.getExtP(getApplicationContext(), "conf_is_pro").equals("1")) {
	        RelativeLayout fb = (RelativeLayout)findViewById(R.id.plugin_ftp_box);
	        fb.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.plugin_pro_box).setVisibility(View.VISIBLE);

        RelativeLayout rb = (RelativeLayout)findViewById(R.id.proxy_box);
        rb.setVisibility(View.GONE);
        
        //if (NAction.getExtP(getApplicationContext(), "conf_is_pro").equals("1")) {
        RelativeLayout fb = (RelativeLayout)findViewById(R.id.plugin_ftp_box);
        fb.setVisibility(View.VISIBLE);
        //}

        RelativeLayout pb = (RelativeLayout)findViewById(R.id.plugin_defaultroot_box);
        pb.setVisibility(View.VISIBLE);

        /*
        if (NAction.getExtP(this, "conf_is_pro").equals("0")) {
	        String notifyMsg = NAction.getExtP(getApplicationContext(), "conf_pro_msg");

            //RelativeLayout ab = (RelativeLayout)findViewById(R.id.plugin_adfree_box);
            //TextView at = (TextView)findViewById(R.id.plugin_adfree);
            //if (!notifyMsg.equals("")) {
            //	at.setText(notifyMsg);
            //}

            String adpkg = NAction.getExtP(getApplicationContext(), "conf_no_ad_pkg");
    		if  (!NUtil.checkAppInstalledByName(getApplicationContext(), adpkg)) {
                ab.setVisibility(View.VISIBLE);
    		}
        }*/	    
        //RelativeLayout pb = (RelativeLayout)findViewById(R.id.pylib_box);
        //pb.setVisibility(View.VISIBLE);


        //TextView ftpVal = (TextView)findViewById(R.id.plugin_ftp_value);
        //ftpVal.setText();
        
        
		//NAction.recordUserLog(getApplicationContext(), "setting", "");

        /*TextView host = (TextView)findViewById(R.id.proxy_host_value);
        host.setText(NAction.getProxyHost(getApplicationContext()));
        
        TextView port = (TextView)findViewById(R.id.proxy_port_value);
        port.setText(NAction.getProxyPort(getApplicationContext()));

        TextView username = (TextView)findViewById(R.id.proxy_username_value);
        username.setText(NAction.getProxyUsername(getApplicationContext()));

        TextView pwd = (TextView)findViewById(R.id.proxy_pwd_value);
        pwd.setText(NAction.getProxyPwd(getApplicationContext()));*/
        
        displayDefaultRoot();
        displayProxy();
        showRecommandAd();
        
        MNApp mnApp = (MNApp) this.getApplication();

        MyApp.getInstance().addActivity(this); 
    }
 
    public void showRecommandAd() {
        if (!NAction.checkIfPayIAP(getApplicationContext(), "ad")) {

		if (NAction.getExtP(getApplicationContext(), "adx_" + TAG)
				.equals("1")) {

		String ad = NAction.getExtAdConf(getApplicationContext());

		final List<String> ltImgLink=new ArrayList<String>();
		List<String> ltResImg=new ArrayList<String>();
			try {
				JSONObject jsonObj = new JSONObject(ad);
				
				JSONArray arrAd = jsonObj.getJSONArray("marquee");
				for(int i=0;i<arrAd.length();i++){
					JSONObject json=arrAd.getJSONObject(i);
					String ad_code=json.getString("ad_code");
					if(!NUtil.checkAppInstalledByName(getApplicationContext(), ad_code)){
						Log.d(TAG, "ad_code:"+ad_code);
						String link = confGetUpdateURL(3)+"&linkid="+json.getString("adLink_id");

						ltResImg.add(json.getString("ad_img"));
						ltImgLink.add(link);
					} else {
						Log.d(TAG, "!ad_code:"+ad_code);

					}
					
				}
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			

		    AdSlidShowView adSlid = (AdSlidShowView)findViewById(R.id.adSlid2);
			adSlid.setImagesFromUrl(ltResImg);
			adSlid.setOnUrlBackCall(new urlBackcall() {
				@Override
				public void onUrlBackCall(int i) {
					Intent intent = NAction.openRemoteLink(
							getApplicationContext(), ltImgLink.get(i));
					startActivity(intent);
				}
			});
			adSlid.setVisibility(View.VISIBLE);
			findViewById(R.id.adLine).setVisibility(View.VISIBLE);
			findViewById(R.id.adTitle).setVisibility(View.VISIBLE);
		}
        }
    }
    
    public void onNoAD(View v) {
    	String confProLink = NAction.getExtP(this, "conf_pro_link");
    	if (confProLink.equals("")) {
    		confProLink = "market://details?id=com.quseit.texteditorpro";
    	}
    	Intent intent = NAction.openRemoteLink(this, confProLink);
    	startActivity(intent);
    }

    public void onADFree(View v) {
        String adfreeUrl = NAction.getExtP(getApplicationContext(), "conf_no_ad_pkg_url");
        try {
			Intent intent = NAction.openRemoteLink(this, adfreeUrl);
			startActivity(intent);
        } catch (Exception e) {
    		Intent intent = NAction.openRemoteLink(this, "http://play.tubebook.net/adfree-tubeboook-app.html");
    		startActivity(intent);
        }
    }


    public void onPyLib(View v) {
    	String extPlgPlusName = com.quseit.config.CONF.EXT_PLG;

		String localPlugin = this.getPackageName();
		Intent intent = new Intent();
		intent.setClassName(localPlugin, extPlgPlusName+".MPyLibActivity");

		startActivity(intent);
    }

    public void onFtpSetting(View v) {
    	Intent intent = new Intent(this, MFTPSettingActivity.class);
    	startActivity(intent);
    }
    
    
    public void displayDefaultRoot() {
    	//String proxyHost = NAction.getProxyHost(getApplicationContext());
    	//String proxyPort = NAction.getProxyPort(getApplicationContext());
    	String root = NAction.getDefaultRoot(getApplicationContext());

    	if (root.equals("")) {
	    	root = Environment.getExternalStorageDirectory().getAbsolutePath().toString();
    	}
    	TextView rootValue = (TextView)findViewById(R.id.plugin_defaultroot_value);
    	rootValue.setText(root);
    }

    public void displayProxy() {
    	String proxyHost = NAction.getProxyHost(getApplicationContext());
    	String proxyPort = NAction.getProxyPort(getApplicationContext());
    	TextView proxyValue = (TextView)findViewById(R.id.proxy_value);
    	proxyValue.setText(proxyHost+":"+proxyPort);
    }
    
    @SuppressWarnings("deprecation")
	public void onSetProxy(View v) {
    	String proxyHost = NAction.getProxyHost(getApplicationContext());
    	String proxyPort = NAction.getProxyPort(getApplicationContext());
    	
		WBase.setTxtDialogParam2(0, R.string.proxy_setting, getString(R.string.proxy_host), getString(R.string.proxy_port), proxyHost, proxyPort,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
				        AlertDialog ad = (AlertDialog) dialog;  
				        EditText t1 = (EditText) ad.findViewById(R.id.editText_prompt1);
				        EditText t2 = (EditText) ad.findViewById(R.id.editText_prompt2);
				        String host = t1.getText().toString();
				        String port = t2.getText().toString();
				        boolean alert = false;
				        if (host!=null && !host.equals("")) {
				        	if (NUtil.isIP(host)) {
						        NAction.setProxyHost(getApplicationContext(), host);
								t1.setText(host);
								
				        	} else {
				        		alert = true;
					        	Toast.makeText(getApplicationContext(), R.string.err_ip_format, Toast.LENGTH_SHORT).show();

				        	}
				        } else {
					        NAction.setProxyHost(getApplicationContext(), "");
							t1.setText("");
				        }
				        if (!alert) {
					        if (port!=null && !port.equals("")) {
						        if (NUtil.isInt(port)) {
							        NAction.setProxyPort(getApplicationContext(), port);
									t2.setText(port);
						        } else {
						        	Toast.makeText(getApplicationContext(), R.string.err_need_int, Toast.LENGTH_SHORT).show();
						        }
					        } else {
						        NAction.setProxyPort(getApplicationContext(), "");
								t2.setText("");
					        }
				        }
				        
				        displayProxy();

					}
				},null);
		showDialog(_WBase.DIALOG_TEXT_ENTRY2+1);
    }

 
    @SuppressWarnings("deprecation")
	public void onDefaultRootSetting(View v) {
		final TextView rootText = (TextView)findViewById(R.id.plugin_defaultroot_value);
		String rootVal = rootText.getText().toString();
		WBase.setTxtDialogParam(0, R.string.plugin_defaultroot, rootVal,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
				        AlertDialog ad = (AlertDialog) dialog;  
				        EditText t = (EditText) ad.findViewById(R.id.editText_prompt);
				        String content = t.getText().toString();
				        
				        boolean failed = true;
				        String err = getString(R.string.root_need);
				        if (content!=null && !content.equals("")) {
				        	
				        	File r = new File(content);
				        	if (r.exists()) {
				        		if (r.isDirectory()) {
				        			failed = false;
				        		} else {
				        			err = getString(R.string.root_notdir);
				        		}
				        	} else {
			        			err = getString(R.string.root_noexist);

				        	}
				        	
				        } 
				        
				        if (failed) {
				        	Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
				        	onDefaultRootSetting(null);
				        } else {
				        	NAction.setDefaultRoot(getApplicationContext(), content);
				        	displayDefaultRoot();
				        	Toast.makeText(getApplicationContext(), R.string.set_root_ok, Toast.LENGTH_SHORT).show();
				        }
					}
				},null);
		showDialog(_WBase.DIALOG_TEXT_ENTRY+dialogIndex);
		dialogIndex++;
	}
    
    @SuppressWarnings("deprecation")
	public void onMediaCenterSetting(View v) {
		final TextView media = (TextView)findViewById(R.id.plugin_mediacenter_value);
		String mediaVal = media.getText().toString();
		WBase.setTxtDialogParam(R.drawable.ic_setting, R.string.plugin_mediacenter, mediaVal,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
				        AlertDialog ad = (AlertDialog) dialog;  
				        EditText t = (EditText) ad.findViewById(R.id.editText_prompt);
				        String content = t.getText().toString();
				        NAction.setMediCenter(getApplicationContext(), content);
				        media.setText(content);
					}
				},null);
		showDialog(_WBase.DIALOG_TEXT_ENTRY+dialogIndex);
		dialogIndex++;
    }
    
	public void setProxyPort(View v) {
		/*final TextView port = (TextView)findViewById(R.id.proxy_port_value);
		String portVal = port.getText().toString();
		WBase.setTxtDialogParam(0, R.string.proxy_port, portVal,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
				        AlertDialog ad = (AlertDialog) dialog;  
				        EditText t = (EditText) ad.findViewById(R.id.editText_prompt);
				        String content = t.getText().toString();
				        if (content!=null && !content.equals("")) {
					        if (NUtil.isInt(content)) {
						        NAction.setProxyPort(getApplicationContext(), content);
								port.setText(content);
					        } else {
					        	Toast.makeText(getApplicationContext(), R.string.err_need_int, Toast.LENGTH_SHORT).show();
					        }
				        } else {
					        NAction.setProxyPort(getApplicationContext(), "");
							port.setText("");
				        }
					}
				},null);
		showDialog(_WBase.DIALOG_TEXT_ENTRY+2);*/
	}

	/*public void setProxyUsername(View v) {
		final TextView username = (TextView)findViewById(R.id.proxy_username_value);
		String usernameVal = username.getText().toString();
		WBase.setTxtDialogParam(R.drawable.ic_setting, R.string.proxy_port, usernameVal,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
				        AlertDialog ad = (AlertDialog) dialog;  
				        EditText t = (EditText) ad.findViewById(R.id.editText_prompt);
				        String content = t.getText().toString();
				        NAction.setProxyUsername(getApplicationContext(), content);
						username.setText(content);
					}
				},null);
		showDialog(_WBase.DIALOG_TEXT_ENTRY+3);
	}
	
	public void setProxyPwd(View v) {
		final TextView pwd = (TextView)findViewById(R.id.proxy_pwd_value);
		String pwdVal = pwd.getText().toString();
		WBase.setTxtDialogParam(R.drawable.ic_setting, R.string.proxy_port, pwdVal,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
				        AlertDialog ad = (AlertDialog) dialog;  
				        EditText t = (EditText) ad.findViewById(R.id.editText_prompt);
				        String content = t.getText().toString();
				        NAction.setProxyPwd(getApplicationContext(), content);
						pwd.setText(content);
					}
				},null);
		showDialog(_WBase.DIALOG_TEXT_ENTRY+4);
	}*/
	

	public void onRate(View v){
		//Log.d(TAG, "exit1");
		//String rateUrl = NAction.getInstallLink(getApplicationContext());
		String rateUrl = NAction.getExtP(this, "conf_rate_url");
		if (rateUrl.equals("")) {
			rateUrl = "http://play.tubebook.net/";
		}
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(rateUrl));
		startActivity(i);
	}
	
	@Override  
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
		if (requestCode == SCRIPT_EXEC_CODE) {
			Log.d(TAG, "script exec:");
		}
        super.onActivityResult(requestCode, resultCode, data);  

	}

}
