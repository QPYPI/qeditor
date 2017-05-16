package com.quseit.texteditor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.quseit.base.DialogBase;
import com.quseit.base.MyApp;
import com.quseit.common.QBaseActivity;
import com.quseit.util.NAction;
import com.quseit.util.NUtil;

//import greendroid.graphics.drawable.ActionBarDrawable;
//import greendroid.widget.ActionBarItem;
//import greendroid.widget.NormalActionBarItem;
//import greendroid.widget.QuickAction;

//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.AdView;

public class BaseActivity extends QBaseActivity {
    protected static final int SCRIPT_EXEC_PY = 2235;  
    protected static final int SCRIPT_EXEC_CODE = 1235;  

    @Override
    protected void onDestroy() {
        LinearLayout modBanner = (LinearLayout)findViewById(R.id.modbanner);

        if (modBanner!=null) {
            modBanner.removeAllViews();
        }

    	super.onDestroy();
    	
    }
	public void initAD(String pageId) {
		super.initAD(pageId);
		
        LinearLayout modBanner = (LinearLayout)findViewById(R.id.modbanner);

        if (NUtil.netCheckin(getApplicationContext()) && !NAction.checkIfPayIAP(getApplicationContext(), "ad")) {
        	if (NAction.getCode(getApplicationContext()).contains("qedit")) {
	        	if (!NAction.getExtP(getApplicationContext(), "ad_with_"+pageId).equals("0")) {
//	                adMob = (AdView) findViewById(R.id.adView);
//	                adMob.setVisibility(View.VISIBLE);
//	                if (NAction.getExtP(getApplicationContext(), "ad_conf_admob_np").equals("")) {
//	                    try {
//	                    	LinearLayout.LayoutParams adViewLayoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
//	                    	adMob.setLayoutParams(adViewLayoutParams);
//	                    } catch (Exception e) {
//
//	                    }
//	                }
//
//	                modBanner.removeAllViews();
//	                try {
//	                    AdRequest adr = new AdRequest.Builder().build();
//	                    adMob.loadAd(adr);
//
//	                    findViewById(R.id.modbanner_wrap).setVisibility(View.VISIBLE);
//	                } catch (NoSuchMethodError e){
//
//	                }

	               // }

	        	}
        	}
        }    

	}

	public void onNotify(View v) {
	}
	
	//@SuppressLint("NewApi")
	protected void initWidgetTabItem(int flag) {
		String code = NAction.getCode(getApplicationContext());
		if (code.startsWith("qpy") || code.startsWith("qlua5")) {
			if (flag == 5) {

//				addActionBarItem(getGDActionBar()
//			        		.newActionBarItem(NormalActionBarItem.class)
//			        		.setDrawable(new ActionBarDrawable(this, R.drawable.ic_folder_open_white)), 20);
//
//
//			    addActionBarItem(getGDActionBar()
//			        		.newActionBarItem(NormalActionBarItem.class)
//			        		.setDrawable(new ActionBarDrawable(this, R.drawable.ic_note_add_white)), 30);
//
//				addActionBarItem(getGDActionBar()
//						.newActionBarItem(NormalActionBarItem.class)
//						.setDrawable(new ActionBarDrawable(this, R.drawable.ic_action_overflow_dark)), 40);

			} else {
//			    addActionBarItem(getGDActionBar()
//		        		.newActionBarItem(NormalActionBarItem.class)
//		        		.setDrawable(new ActionBarDrawable(this, R.drawable.ic_action_overflow_dark)), 40);
			}
		    
		} else if (code.contains("qedit")) {
//            addActionBarItem(getGDActionBar()
//                    .newActionBarItem(NormalActionBarItem.class)
//                    .setDrawable(new ActionBarDrawable(this, R.drawable.ic_note_add_white)), 30);
//
//			addActionBarItem(getGDActionBar()
//					.newActionBarItem(NormalActionBarItem.class)
//					.setDrawable(new ActionBarDrawable(this, R.drawable.ic_folder_open_white)), 20);
//
////			if (flag == 5) {
////				addActionBarItem(getGDActionBar()
////		        		.newActionBarItem(NormalActionBarItem.class)
////		        		.setDrawable(new ActionBarDrawable(this, R.drawable.ic_save_white)), 25);
////			}
//			addActionBarItem(getGDActionBar()
//					.newActionBarItem(NormalActionBarItem.class)
//					.setDrawable(new ActionBarDrawable(this, R.drawable.ic_more_vert_white)), 50);
		} else {
			
			
		}
	}

	
//	@Override
//    public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {
//    	switch (item.getItemId()) {
//			case 40:
//				Intent intent = new Intent(this, TedSettingsActivity.class);
//				startActivity(intent);
//				break;
//    	}
//    	return 	super.onHandleActionBarItemClick(item, position);
//    }

    protected static class MyQuickAction /*extends QuickAction */{
        protected static final ColorFilter BLACK_CF = new LightingColorFilter(Color.BLACK, Color.BLACK);
        protected static final ColorFilter WHITE_CF = new LightingColorFilter(Color.WHITE, Color.WHITE);

        public MyQuickAction(Context ctx, int drawableId, int titleId) {
//            super(ctx, buildDrawable(ctx, drawableId), titleId);
        }
        
        protected static Drawable buildDrawable(Context ctx, int drawableId) {
            Drawable d = ctx.getResources().getDrawable(drawableId);
            d.setColorFilter(BLACK_CF);
            return d;
        }        
    }
    
    @Override
	public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    }
    
    @SuppressWarnings("deprecation")
	public void callLuaApi(String flag, String param, String luaCode) {
		String code = NAction.getCode(this);
		String luaApp = "com.quseit.qlua5pro2";
		// todo
		if (code.contains("lua")) {
			Intent intent = new Intent(".QLUAIndexActivity");
			intent.putExtra(CONF.EXTRA_CONTENT_URL0, param);
			sendBroadcast(intent);

		}  else {
    		WBase.setTxtDialogParam(0, R.string.pls_install_lua, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
    				String plgUrl = NAction.getExtP(getApplicationContext(), "ext_plugin_pkg4");
    				if (plgUrl.equals("")) {
    					plgUrl = CONF.EXT_PLG_URL4;
    				}
    				try {
						Intent intent = NAction.getLinkAsIntent(getApplicationContext(), plgUrl);
						startActivity(intent);
    				} catch (Exception e) {
    					plgUrl = CONF.EXT_PLG_URL4;
						Intent intent = NAction.getLinkAsIntent(getApplicationContext(), plgUrl);
						startActivity(intent);
    				}
				}
				}, null);
    		
    		showDialog(DialogBase.DIALOG_EXIT+dialogIndex);
    		dialogIndex++;


		}
    }

    
    /**
     * call the Qpython API
     * @param flag
     * @param param
     * @param pyCode is the python code to run 
     */
    @SuppressWarnings("deprecation")
	public void callPyApi(String flag, String param, String pyCode) {
    	String proxyCode = "";
    	String extPlgPlusName = com.quseit.config.CONF.EXT_PLG;
    	String extPlg3Name = CONF.EXT_PLG_3;
		String extPlgName = NAction.getExtP(getApplicationContext(), "ext_plugin");
		if (extPlgName.equals("")) {
			extPlgName = com.quseit.config.CONF.EXT_PLG;
		}
		
		String plgUrl = NAction.getExtP(getApplicationContext(), "ext_plugin_pkg");
		if (plgUrl.equals("")) {
			plgUrl = com.quseit.config.CONF.EXT_PLG_URL;
		}
		try {
			String localPlugin = this.getPackageName();
			Intent intent = new Intent();
			intent.setClassName(localPlugin, "org.qpython.qpylib.MPyApi");
			intent.setAction("org.qpython.qpylib.action.MPyApi");
			
			Bundle mBundle = new Bundle(); 
			mBundle.putString("root", MyApp.getInstance().getRoot());
	
			mBundle.putString("app", NAction.getCode(getApplicationContext()));
			mBundle.putString("act", "onPyApi");
			mBundle.putString("flag", flag);
			mBundle.putString("param", param);
			mBundle.putString("pycode", proxyCode+pyCode);
	
			intent.putExtras(mBundle);
			
			startActivityForResult(intent, SCRIPT_EXEC_PY);
		} catch (Exception e) {
			
			// qpython 3
			if (pyCode.contains("#qpy3\n")) {
				if (NUtil.checkAppInstalledByName(getApplicationContext(), extPlg3Name)) {
					Intent intent = new Intent();
					intent.setClassName(extPlg3Name, "org.qpython.qpylib.MPyApi");
					intent.setAction("org.qpython.qpylib.action.MPyApi");
					
					Bundle mBundle = new Bundle(); 
					mBundle.putString("app", NAction.getCode(getApplicationContext()));
					mBundle.putString("act", "onPyApi");
					mBundle.putString("flag", flag);
					mBundle.putString("param", param);
					mBundle.putString("pycode", proxyCode+pyCode);
		
					intent.putExtras(mBundle);
					
					startActivityForResult(intent, SCRIPT_EXEC_PY);
		
				} else {
					
		    		WBase.setTxtDialogParam(0, R.string.pls_install_qpy, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
		    				String plgUrl = NAction.getExtP(getApplicationContext(), "ext_plugin_pkg3");
		    				if (plgUrl.equals("")) {
		    					plgUrl = CONF.EXT_PLG_URL3;
		    				}
		    				try {
								Intent intent = NAction.getLinkAsIntent(getApplicationContext(), plgUrl);
								startActivity(intent);
		    				} catch (Exception e) {
		    					plgUrl = CONF.EXT_PLG_URL3;
								Intent intent = NAction.getLinkAsIntent(getApplicationContext(), plgUrl);
								startActivity(intent);
		    				}
						}
					}, null);
		    		showDialog(DialogBase.DIALOG_EXIT+dialogIndex);
		    		dialogIndex++;
		    		
				}
				
			} else { //
				
				if (NUtil.checkAppInstalledByName(getApplicationContext(), extPlgPlusName)) {
					Intent intent = new Intent();
					intent.setClassName(extPlgPlusName, "org.qpython.qpylib.MPyApi");
					intent.setAction("org.qpython.qpyplib.action.MPyApi");
					
					Bundle mBundle = new Bundle(); 
					mBundle.putString("app", NAction.getCode(getApplicationContext()));
					mBundle.putString("act", "onPyApi");
					mBundle.putString("flag", flag);
					mBundle.putString("param", param);
					mBundle.putString("pycode", proxyCode+pyCode);
		
					intent.putExtras(mBundle);
					
					startActivityForResult(intent, SCRIPT_EXEC_PY);
		
				} else if (NUtil.checkAppInstalledByName(getApplicationContext(), extPlgName)) {
					
					Intent intent = new Intent();
					intent.setClassName(extPlgName, "org.qpython.qpylib.MPyApi");
					intent.setAction("org.qpython.qpyplib.action.MPyApi");
					
					Bundle mBundle = new Bundle(); 
					mBundle.putString("app", NAction.getCode(getApplicationContext()));
					mBundle.putString("act", "onPyApi");
					mBundle.putString("flag", flag);
					mBundle.putString("param", param);
					mBundle.putString("pycode", proxyCode+pyCode);
		
					intent.putExtras(mBundle);
					
					startActivityForResult(intent, SCRIPT_EXEC_PY);
					
				}  else {
								
		    		WBase.setTxtDialogParam(0, R.string.pls_install_qpy, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
		
		    				String plgUrl = NAction.getExtP(getApplicationContext(), "ext_plugin_pkg");
		    				if (plgUrl.equals("")) {
		    					plgUrl = com.quseit.config.CONF.EXT_PLG_URL;
		    				}
							Intent intent = NAction.getLinkAsIntent(getApplicationContext(), plgUrl);
							startActivity(intent);
						}
					}, null);
		    		showDialog(DialogBase.DIALOG_EXIT+dialogIndex);
		    		dialogIndex++;
				}
			}

		}
    }

    public void onAbout(View v) {
		Intent intent2 = new Intent(getApplicationContext(), OAboutActivity.class);
		startActivity(intent2);
    	//overridePendingTransition(R.anim.push_down_in, R.anim.push_down_out);
	}
    
    public void onGSetting(View v) {
    	Intent intent = new Intent(this, MSettingActivity.class);
    	startActivity(intent);
    }

	@Override
	public Class<?> getUpdateSrv() {
		return null;
	}
	
    @SuppressWarnings("deprecation")
	@SuppressLint("DefaultLocale")
	public void playFromRemote(String link) {
    	String code = NAction.getCode(getApplicationContext());
        if (code.startsWith("mn")) {
        	
			String a8Name = NAction.getExtP(getApplicationContext(), "extend_a8_pkg");
			if (a8Name.equals("")) {
				a8Name = CONF.A8_PLAY;
			}
			
			String pluginName1 = NAction.getExtP(getApplicationContext(), "extend_plugin_pkg1");
			if (pluginName1.equals("")) {
				pluginName1 = CONF.PLAY_PLUGIN_1;
			}
			
			String pluginName2 = NAction.getExtP(getApplicationContext(), "extend_plugin_pkg2");
			if (pluginName2.equals("")) {
				pluginName2 = CONF.PLAY_PLUGIN_2;
			}
			if (NUtil.checkAppInstalledByName(getApplicationContext(), pluginName2)) {
				
	    		Intent intent = new Intent();
	    		intent.setClassName(pluginName2, "com.quseit.p.PLAPlayerAct");
	    		intent.setAction("android.intent.action.VIEW");
	    		Uri uri = Uri.parse(link);
	    		intent.setDataAndType(uri , "video/*");
	    		startActivity(intent);
	    		
			} else if (NUtil.checkAppInstalledByName(getApplicationContext(), pluginName1)) {
				
	    		Intent intent = new Intent();
	    		intent.setClassName(pluginName1, "com.quseit.p.PLAPlayerAct");
	    		intent.setAction("android.intent.action.VIEW");
	    		Uri uri = Uri.parse(link);
	    		intent.setDataAndType(uri , "video/*");
	    		startActivity(intent);
			} else {
				
				boolean useDefault = false;
				int indexOfDot = link.lastIndexOf('.');
				if (indexOfDot != -1) {
					String extension = link.substring(indexOfDot).toLowerCase();
					if (extension.compareTo(".mp4") == 0) {
						useDefault = true;
					}
				}
			
				if (!useDefault) {
			    	String a8VName = this.getPackageName();
			    	
					Intent intent = new Intent();
					intent.setClassName(a8VName, "com.quseit.p.FFMpegPlayer");
					intent.setAction("android.intent.action.VIEW");
					Uri uri = Uri.parse(link);
					intent.setDataAndType(uri , "video/*");
					startActivity(intent);
				} else {
			    	String a8VName = this.getPackageName();
			    	
					Intent intent = new Intent();
					intent.setClassName(a8VName, "com.quseit.m.PLAPlayerAct");
					intent.setAction("android.intent.action.VIEW");
					Uri uri = Uri.parse(link);
					intent.setDataAndType(uri , "video/*");
					startActivity(intent);
				}
			}
			
        } else {	// 不是MVP        	
			String pkgName = NAction.getExtP(getApplicationContext(), "extend_a8_pkg");
			if (pkgName.equals("")) {
				pkgName = CONF.A8_PLAY;
			}
			if (NUtil.checkAppInstalledByName(getApplicationContext(), pkgName)) {
	        	String a8Name = CONF.A8_PLAY;
	        	
	    		Intent intent = new Intent();
	    		intent.setClassName(a8Name, "com.quseit.mna8.PLAPlayerAct");
	    		intent.setAction("android.intent.action.VIEW");
	    		
	    		Uri uri = Uri.parse(link);
	    		intent.setDataAndType(uri , "video/*");
	    		startActivity(intent);
				
			} else {
				// 检查MVP是否在
				String mpvName = CONF.MPV_PLAY;
				if (NUtil.checkAppInstalledByName(getApplicationContext(), mpvName)) {
		    		Intent intent = new Intent();
		    		intent.setClassName(mpvName, "com.quseit.m.PLAPlayerAct");
		    		intent.setAction("android.intent.action.VIEW");
		    		
		    		Uri uri = Uri.parse(link);
		    		intent.setDataAndType(uri , "video/*");
		    		startActivity(intent);
				} else {
				
		    		WBase.setTxtDialogParam(0, R.string.pls_install_a8_play, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							String pkgUrl = NAction.getExtP(getApplicationContext(), "extend_a8_play_url");
							if (pkgUrl.equals("")) {
								pkgUrl = CONF.A8_PLAY_URL;
							}
							
							//if (pkgUrl.startsWith("http:")) {
							try {
								Intent intent = NAction.getLinkAsIntent(getApplicationContext(), pkgUrl);
								startActivity(intent);
							} catch (Exception e) {
								pkgUrl = CONF.A8_PLAY_URL2;
								Intent intent = NAction.getLinkAsIntent(getApplicationContext(), pkgUrl);
								startActivity(intent);
							}
						}
					}, null);
		    		showDialog(DialogBase.DIALOG_EXIT+dialogIndex);
		    		dialogIndex++;
				}
			}        	
        }
    }
    
    private static final int SCRIPT_CONSOLE_CODE = 1237;

    public void execInConsole(String[] args) {
    	Intent intent = new Intent();
		intent.setClassName(this.getPackageName(), "jackpal.androidterm.Term");
		intent.putExtra(CONF.EXTRA_CONTENT_URL0, "main");
		intent.putExtra("ARGS", args);
		startActivityForResult(intent,SCRIPT_CONSOLE_CODE);
    }

	@Override
	public String confGetUpdateURL(int flag) {
		if (flag == 2) {
			return CONF.LOG_URL+this.getPackageName()+"/"+NUtil.getVersinoCode(this);
		} else if (flag == 3) {
			return CONF.AD_URL+this.getPackageName()+"/"+NUtil.getVersinoCode(this)+"?"
					+ NAction.getUserUrl(getApplicationContext());

		} else {
			return CONF.UPDATE_URL+this.getPackageName()+"/"+NUtil.getVersinoCode(this);

		}
	}
	/////
}
