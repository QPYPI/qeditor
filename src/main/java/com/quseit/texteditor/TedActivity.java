package com.quseit.texteditor;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewAnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.quseit.base.MyApp;
import com.quseit.texteditor.common.Constants;
import com.quseit.texteditor.common.RecentFiles;
import com.quseit.texteditor.common.Settings;
import com.quseit.texteditor.common.TedChangelog;
import com.quseit.texteditor.common.TextFileUtils;
import com.quseit.texteditor.databinding.LayoutEditorBinding;
import com.quseit.texteditor.databinding.SearchTopBinding;
import com.quseit.texteditor.databinding.WidgetSaveBinding;
import com.quseit.texteditor.ui.view.EnterDialog;
import com.quseit.texteditor.ui.view.NewEditorPopUp;
import com.quseit.texteditor.undo.TextChangeWatcher;
import com.quseit.texteditor.widget.crouton.Crouton;
import com.quseit.texteditor.widget.crouton.Style;
import com.quseit.util.KeyboardUtils;
import com.quseit.util.NAction;
import com.quseit.util.NStorage;
import com.quseit.util.NUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;

import static com.quseit.texteditor.androidlib.data.FileUtils.deleteItem;
import static com.quseit.texteditor.androidlib.data.FileUtils.getCanonizePath;
import static com.quseit.texteditor.androidlib.data.FileUtils.renameItem;
import static com.quseit.texteditor.androidlib.ui.Toaster.showToast;
import static com.quseit.texteditor.androidlib.ui.activity.ActivityDecorator.addMenuItem;
import static com.quseit.texteditor.androidlib.ui.activity.ActivityDecorator.showMenuItemAsAction;

/**
 * @author River
 */

public class TedActivity extends Activity implements Constants, TextWatcher, OnClickListener {
    public static final  String TAG             = "TED";
    private static final String WEB_PROJECT     = "web";
    private static final String CONSOLE_PROJECT = "console";
    private static final String KIVY_PROJECT    = "kivy";

    private LayoutEditorBinding binding;
    private WidgetSaveBinding   widgetSaveBinding;
    private SearchTopBinding    searchTopBinding;

    protected String mCurrentFilePath;
    protected String mCurrentFileName;

    private   NewEditorPopUp editorPopUp;
    /**
     * the runnable to run after a save
     */
    protected Runnable       mAfterSave; // Mennen ? Axe ?
    protected boolean        mDirty;

    /**
     * is read only
     */
    protected boolean mReadOnly;

    /**
     * Undo watcher
     */
    protected TextChangeWatcher mWatcher;

    protected boolean mInUndo;

    protected boolean mWarnedShouldQuit;

    protected boolean mDoNotBackup;

    private   boolean isKeyboardShowing;
    /**
     * are we in a post activity result ?
     */
    protected boolean mReadIntent;

    final int DOC_FLAG = 10001;
    boolean IS_DOC_BACK = false;
    private Animator anim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyApp.getInstance().addActivity(this, CONF.BASE_PATH, "");

        binding = DataBindingUtil.setContentView(this, R.layout.layout_editor);
        switch (getIntent().getIntExtra(EXTRA_REQUEST_CODE, REQUEST_FILE)) {
            case REQUEST_PROJECT:
                break;
            case REQUEST_FILE:
            default:
                break;
        }
        initData();
        initListener();
        initFiles();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mReadIntent = false;
    }

    @Override
    protected void onStart() {
        super.onStart();

        TedChangelog changeLog;
        SharedPreferences prefs;

        changeLog = new TedChangelog();
        prefs = getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE);
        changeLog.saveCurrentVersion(this, prefs);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (CONF.DEBUG)
            Log.d(TAG, "onResume");

        if (mReadIntent) {
            readIntent();
        }
        mReadIntent = false;

        updateTitle();
        if (mCurrentFilePath != null && (mCurrentFilePath.endsWith(".py") || mCurrentFilePath.endsWith(".lua"))) {
            if (mCurrentFilePath.endsWith(".py")) {
                binding.editor.updateFromSettings("py");
            } else {
                binding.editor.updateFromSettings("lua");
            }
        } else {
            binding.editor.updateFromSettings("");

        }

        ImageButton pBtn = (ImageButton) findViewById(R.id.play_btn);
        pBtn.setVisibility(View.VISIBLE);

        if (mCurrentFilePath != null
                && (mCurrentFilePath.endsWith(".py") || mCurrentFilePath.endsWith(".md")
                || mCurrentFilePath.endsWith(".html") || mCurrentFilePath.endsWith(".htm")
                || mCurrentFilePath.endsWith(".lua") || mCurrentFilePath.endsWith(".sh"))) {
            if (mCurrentFilePath.endsWith(".py") || mCurrentFilePath.endsWith(".sh")
                    || mCurrentFilePath.endsWith(".lua")) {
                pBtn.setImageResource(R.drawable.ic_go);
            } else {
                pBtn.setImageResource(R.drawable.ic_from_website);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (CONF.DEBUG)
            Log.d(TAG, "onPause");

        if (Settings.FORCE_AUTO_SAVE && mDirty && (!mReadOnly)) {

            if ((mCurrentFilePath == null) || (mCurrentFilePath.length() == 0))
                doAutoSaveFile(true);
            else if (Settings.AUTO_SAVE_OVERWRITE)
                doSaveFile(mCurrentFilePath, true);
        }
    }

    @Override
    public void onDestroy() {
        // stopQPyService(this);
        super.onDestroy();
        String code = NAction.getCode(this);

        if (code.equals("qedit")) {

            MyApp.getInstance().exit();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        readIntent(intent);
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private void startAnim() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        anim = ViewAnimationUtils.createCircularReveal(searchTopBinding.getRoot(), width, 0, 0, width);
        anim.start();
    }

    private void initData() {
        Settings.updateFromPreferences(getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE));
//        if (NAction.getCode(this).contains("qedit")) {
//            initDrawerMenu(this);
//        }
        mReadIntent = true;
        mWatcher = new TextChangeWatcher();
        mWarnedShouldQuit = false;
        mDoNotBackup = false;
    }

    private void initSearchBarListener() {
        searchTopBinding.ibClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                searchTopBinding.textSearch.setText("");
            }
        });
        searchTopBinding.ibClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                searchTopBinding.llSearch.setVisibility(View.GONE);
                binding.searchBottom.rlSearchBottom.setVisibility(View.GONE);
            }
        });

        binding.searchBottom.ivSearch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                searchNext();
            }
        });
        binding.searchBottom.buttonSearchNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                searchNext();
            }
        });

        binding.searchBottom.buttonSearchPrev.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                searchPrevious();
            }
        });
    }

    private void initListener() {
        binding.ivBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finishEdit();
            }
        });
        binding.ivOpen.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TedLocalActivity.start(TedActivity.this);
            }
        });
        binding.ivSetting.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TedSettingsActivity.start(TedActivity.this);
            }
        });
        binding.ivAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editorPopUp == null) {
                    editorPopUp = new NewEditorPopUp(TedActivity.this);
                    editorPopUp.initListener(new NewEditorPopUp.AddClick() {
                        @Override
                        public void blankFile() {
                            newContent();
                        }

                        @Override
                        public void newScript() {

                        }

                        @Override
                        public void webApp() {
                            newProject(WEB_PROJECT);
                        }

                        @Override
                        public void consoleApp() {
                            newProject(CONSOLE_PROJECT);
                        }

                        @Override
                        public void kivyApp() {
                            newProject(KIVY_PROJECT);
                        }
                    });
                }
                editorPopUp.show(binding.rlTop);
            }
        });
        binding.ibMore.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard();
                if (!binding.vsSave.isInflated()) {
                    widgetSaveBinding = DataBindingUtil.bind(binding.vsSave.getViewStub().inflate());
                    initSaveWidgetListener();
                }

                if (view.isSelected()) {
                    // Widget visible
                    widgetSaveBinding.llRoot.setVisibility(View.GONE);
                    binding.ibMore.setImageResource(R.drawable.ic_editor_more_horiz);
                } else {
                    // Widget gone
                    widgetSaveBinding.llRoot.setVisibility(View.VISIBLE);
                    binding.ibMore.setImageResource(R.drawable.ic_editor_arrow_down);
                }
                view.setSelected(!view.isSelected());
            }
        });
        binding.ibKeyboard.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!v.isSelected()) {
                    binding.ibKeyboard.setImageResource(R.drawable.ic_editor_keyboardlock);
                    hideKeyboard();
                    binding.editor.setEnabled(false);
                } else {
                    binding.ibKeyboard.setImageResource(R.drawable.ic_editor_keyboard);
                    binding.editor.setEnabled(true);
                }
                v.setSelected(!v.isSelected());
            }
        });
        binding.ibLeftIndent.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                leftIndent();
            }
        });
        binding.ibRightIndent.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rightIndent();
            }
        });
        binding.ibJumpTo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                goToLine();
            }
        });
        binding.playBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCurrentFilePath != null
                        && (mCurrentFilePath.endsWith(".py") || mCurrentFilePath.endsWith(".md")
                        || mCurrentFilePath.endsWith(".html") || mCurrentFilePath.endsWith(".htm")
                        || mCurrentFilePath.endsWith(".lua") || mCurrentFilePath.endsWith(".sh"))) {
                    runScript();
                } else {
                    Toast.makeText(TedActivity.this, R.string.qedit_not_support, Toast.LENGTH_SHORT).show();
                }
            }
        });
        binding.editor.addTextChangedListener(this);
        binding.ibSearch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!binding.vsSearchTop.isInflated()) {
                    searchTopBinding = DataBindingUtil.bind(binding.vsSearchTop.getViewStub().inflate());
                    initSearchBarListener();
                }
                if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                    startAnim();
                }
                binding.searchBottom.rlSearchBottom.setVisibility(View.VISIBLE);
                searchTopBinding.llSearch.setVisibility(View.VISIBLE);
            }
        });
        binding.ibUndo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                undo();
            }
        });

        KeyboardUtils.addKeyboardToggleListener(this, new KeyboardUtils.SoftKeyboardToggleListener() {
            @Override
            public void onToggleSoftKeyboard(boolean isVisible) {
                isKeyboardShowing = isVisible;
            }
        });
    }

    private void initSaveWidgetListener() {
        widgetSaveBinding.rlSave.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                saveContent();
            }
        });

        widgetSaveBinding.rlSaveas.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                saveContentAs();
            }
        });

        widgetSaveBinding.rlRecent.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TedLocalActivity.start(TedActivity.this, REQUEST_RECENT);
            }
        });
    }


    private void initFiles() {
        String code = NAction.getCode(this);
        String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/" + CONF.BASE_PATH;
        String path = baseDir + (code.contains("3") ? "/snippets3" : "/snippets");
        File folder = new File(path);
        if (!(folder.exists() && folder.isDirectory())) {
            folder.mkdir();
        }

        File f = new File(path + "/Apache_License");
        if (!f.exists()) {
            String file1 = LoadDataFromAssets("Apache_License");
            writeToFile(path + "/Apache_License", file1);
        }
        f = new File(path + "/The_MIT_License");
        if (!f.exists()) {
            String file2 = LoadDataFromAssets("The_MIT_License");
            writeToFile(path + "/The_MIT_License", file2);
        }
//        if (code.startsWith("qpy")) {
        f = new File(path + "/QPy_WebApp");
        if (!f.exists()) {
            String file2 = LoadDataFromAssets("QPy_WebApp");
            writeToFile(path + "/QPy_WebApp", file2);
        }

        f = new File(path + "/QPy_KivyApp");
        if (!f.exists()) {
            String file2 = LoadDataFromAssets("QPy_KivyApp");
            writeToFile(path + "/QPy_KivyApp", file2);
        }

        f = new File(path + "/QPy_ConsoleApp");
        if (!f.exists()) {
            String file2 = LoadDataFromAssets("QPy_ConsoleApp");
            writeToFile(path + "/QPy_ConsoleApp", file2);
        }
//        }
        String lastFile = NStorage.getSP(this, "qedit.last_filename");
        if (!lastFile.equals("")) {
            File f2 = new File(lastFile);
            if (f2.exists()) {
                //Log.d(TAG, "OPEN LAST:" + lastFile);

                doOpenFile(f2, false);
            }

        }
    }

    public static void start(Context context) {
        Intent starter = new Intent(context, TedActivity.class);
        context.startActivity(starter);
    }

    public static void start(Context context, String text) {
        Intent starter = new Intent(context, TedActivity.class);
        starter.putExtra("TEXT", text);
        context.startActivity(starter);
    }

    public static void start(Context context, String action, Uri path) {
        Intent starter = new Intent(context, TedActivity.class);
        starter.setAction(action);
        starter.setData(path);
        context.startActivity(starter);
    }

    /**
     * Create a list of snippet
     */
    public void SnippetsList(boolean isProject) {
        String code = NAction.getCode(this);
        List<String> listItems = new ArrayList<String>();
        String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/" + CONF.BASE_PATH;
        String path = baseDir + (code.contains("3") ? "/snippets3/" : "/snippets/");
        String files;
        File folder = new File(path);
        if (folder.exists()) {
            File[] listOfFiles = folder.listFiles();
            if (listOfFiles != null) {
                for (int i = 0; i < listOfFiles.length; i++) {
                    if (listOfFiles[i].isFile()) {
                        files = listOfFiles[i].getName();
                        listItems.add(files);
                    }
                }
            }
        }

        final CharSequence colors[] = listItems.toArray(new CharSequence[listItems.size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.info_snippets);

        builder.setItems(colors, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Toast.makeText(getApplicationContext(), colors[which],
                // Toast.LENGTH_SHORT).show();
                try {
                    insertSnippet("" + colors[which]);
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), R.string.fail_to_insert, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
        builder.show();
    }


    /**
     * @param snippetName WEB_PROJECT/CONSOLE_PROJECT/KIVY_PROJECT
     */
    public void insertSnippet(String snippetName) throws IOException {
        String code = NAction.getCode(this);
        String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/" + CONF.BASE_PATH;
        String path = baseDir + (code.contains("3") ? "/snippets3/" : "/snippets/");
        String s;
        switch (snippetName) {
            case WEB_PROJECT:
                s = readFile(path + "QPy_WebApp");
                break;
            case CONSOLE_PROJECT:
                s = readFile(path + "QPy_ConsoleApp");
                break;
            case KIVY_PROJECT:
                s = readFile(path + "QPy_KivyApp");
                break;
            default:
                s = readFile(path + "QPy_WebApp");
        }
        binding.editor.getText().insert(0, s);
        // int start = binding.editor.getSelectionStart(); //this is to get the the cursor position

		/*
         * Kyle Kersey March 24 2014 If the template file contains the extension ".qpy.html" a WebView will pop up, and
		 * will load the file contents, The template file contains a HTML form that is used to configure the code
		 * template, the template is written in JavaScript and is used format the values in the code template. When the
		 * submit button is clicked the WebView will load a URL containing the URL encoded template data with the
		 * paramater of "?template=" Changes in the URL of the WebView are listened for and if it conatins the paramater
		 * "?template=" the WebView closes and the URL content is decoded and the template content is inserted into the
		 * EditText.
		 */
//        if (snippetName.endsWith(".qpy.html")) {
//            LayoutInflater inflater = getLayoutInflater();
//            View dialoglayout = inflater.inflate(R.layout.template_webview, (ViewGroup) getCurrentFocus());
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setView(dialoglayout);
//            builder.show();
//            final AlertDialog optionDialog = builder.create();
//            WebView lWebView = (WebView) dialoglayout.findViewById(R.id.template_webview);
//            lWebView.getSettings().setJavaScriptEnabled(true);
//            lWebView.loadUrl("file://" + path); /* Load the template into the WebView */
//            lWebView.setWebViewClient(new WebViewClient() {
//                @Override
//                // Listen for URL change
//                public WebResourceResponse shouldInterceptRequest(final WebView view, String url) {
//                    if (url.contains("?template=")) {
//                        optionDialog.dismiss(); /* close the popup dialog */
//                        Uri uri = Uri.parse(url);
//                        final String encoded_template_data = uri.getQueryParameter("template");
//                        String result;
//                        try {
//                            // Decode the template data
//                            result = URLDecoder.decode(encoded_template_data, "UTF-8");
//                            // Insert the result into the EditText
//                            binding.editor.setText(result);
//                        } catch (UnsupportedEncodingException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    return null;
//                }
//            });
//
//        } else {
//            String s = readFile(path + snippetName);
//            binding.editor.getText().insert(0, s);
//        }
    }

    /**
     * Save code snippet
     */
    public void getInfo() {
        // int startSelection = binding.editor.getSelectionStart();
        // int endSelection = binding.editor.getSelectionEnd();
        // final String selectedText = binding.editor.getText().toString().substring(startSelection, endSelection);

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getString(R.string.save_as_snippets));

        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(getString(R.string.info_snippets));
        input.setSelection(input.getText().length());
        alert.setView(input);
        // input.addTextChangedListener(new TextWatcher() {
        // public void afterTextChanged(Editable s) {}
        // public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        //
        // public void onTextChanged(CharSequence s, int start, int before, int count) {
        // String baseDir =
        // Environment.getExternalStorageDirectory().getAbsolutePath().toString()+"/org.qpython.qpy";
        // String path = baseDir + "/snippets/";
        // File file = new File("" + path + s);
        // if (file.exists() && !file.isDirectory()) {
        // input.setBackgroundResource(R.drawable.red);
        // } else {
        // input.setBackgroundResource(R.drawable.green);
        // }
        // }
        // });

        alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            int startSelection = binding.editor.getSelectionStart();

            int endSelection = binding.editor.getSelectionEnd();

            final String selectedText = binding.editor.getText().toString().substring(startSelection, endSelection);

            public void onClick(DialogInterface dialog, int whichButton) {
                String code = NAction.getCode(getApplicationContext());

                String value = input.getText().toString();
                String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath().toString()
                        + "/" + CONF.BASE_PATH;
                final String saveName = baseDir + (code.contains("3") ? "/snippets3/" : "/snippets/") + value;

                File f = new File(saveName);
                if (f.exists()) {

////                    WBase.setTxtDialogParam(R.drawable.alert_dialog_icon, R.string.snippet_confirm,
//                            new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    writeToFile(saveName, selectedText);
//                                    Toast.makeText(getApplicationContext(),
//                                            getString(R.string.save_as_snippets_1) + saveName, Toast.LENGTH_SHORT)
//                                            .show();
//
//                                }
//                            });
//                    showDialog(DialogBase.DIALOG_YES_NO_MESSAGE + dialogIndex);
//                    dialogIndex++;

                } else {
                    writeToFile(saveName, selectedText);
                    Toast.makeText(getApplicationContext(), getString(R.string.save_as_snippets_1) + saveName,
                            Toast.LENGTH_SHORT).show();
                }

            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        alert.show();
    }

    /**
     * The contextual action bar (CAB)
     *
     * @author kyle kersey
     */
    /*
    class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			// TODO Auto-generated method stub
			if (item.getItemId() == R.id.shareText) {
				shareData();
			}// else if (item.getItemId() == R.id.findText) {
				// setSearch();
			// }
			return false;
		}


		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Change the button image
			mode.getMenuInflater().inflate(R.menu.action_bar_menu, menu);
			ImageButton helpButton = (ImageButton) findViewById(R.id.help_button);
			helpButton.setImageResource(R.drawable.ic_unknown);

			ImageButton saveCode = (ImageButton) findViewById(R.id.snip_button);
			saveCode.setImageResource(R.drawable.ic_collection_new_1);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			// Change the button image back
			ImageButton helpButton = (ImageButton) findViewById(R.id.help_button);
			helpButton.setImageResource(R.drawable.ic_action_overflow_dark);

			ImageButton saveCode = (ImageButton) findViewById(R.id.snip_button);
			saveCode.setImageResource(R.drawable.ic_storage);

		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			// TODO Auto-generated method stub
			return false;
		}
	}
*/
    public void shareData() {
        EditText et = (EditText) findViewById(R.id.editor);
        int startSelection = et.getSelectionStart();
        int endSelection = et.getSelectionEnd();
        String selectedText = et.getText().toString().substring(startSelection, endSelection);

        if (selectedText.length() != 0) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, selectedText);
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        } else {
            String dataToShare = et.getText().toString();
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, dataToShare);
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        }
    }

    public void setSearch() {
        search();

        int startSelection = binding.editor.getSelectionStart();
        int endSelection = binding.editor.getSelectionEnd();
        String selectedText = binding.editor.getText().toString().substring(startSelection, endSelection);
        if (selectedText.length() != 0) {
            searchTopBinding.textSearch.setText(selectedText);
        }
    }

//    @Override
//    public int createLayout() {
//        if (NAction.getCode(getApplicationContext()).contains("qedit")) {
//
//            return R.layout.gd_content_drawer;
//        } else {
//            return R.layout.gd_content_normal;
//        }
//    }

    private static String getFileName(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private void initDrawerMenu(Context context) {
        //findViewById(R.id.handle_save).setVisibility(View.GONE);
        //findViewById(R.id.handle_history).setVisibility(View.GONE);

//        LinearLayout drawerPanent = (LinearLayout) findViewById(R.id.gd_drawer_layout_left);
//        drawerPanent.removeAllViews();

        View view = LayoutInflater.from(context).inflate(R.layout.qedit_drawer_menu, null);
        ListView menuList = (ListView) view.findViewById(R.id.drawer_menu_list);
//        ItemAdapter adapter = new ItemAdapter(this);
        menuList.setDivider(new ColorDrawable(getResources().getColor(R.color.cgrey6)));
        menuList.setDividerHeight(1);
        menuList.setCacheColorHint(0);
//        menuList.setAdapter(adapter);
        menuList.setBackgroundColor(Color.WHITE);

        menuList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> l, View view, int position, long id) {
//                final TextItem textItem = (TextItem) l.getAdapter().getItem(position);
//                final String act = (String) textItem.getTag(0);
//                if (act.equals("open")) {
//                    openFile();
//                } else if (act.equals("new")) {
//                    newProject();
//                } else if (act.equals("recent")) {
//                    openRecentFile();
//
//                }
                /*mAfterSave = new Runnable() {
                    @Override
					public void run() {
						doOpenFile(new File(fileName), false);
					}
				};
				closeDrawer();
				promptSaveDirty();*/
//                closeDrawer();
            }
        });

//        adapter.clear();
//        DrawableItem sItem1 = new DrawableItem(getString(R.string.menu_open), R.drawable.ic_create_black);
//        sItem1.setTag(0, "open");
//        DrawableItem sItem2 = new DrawableItem(getString(R.string.menu_new), R.drawable.ic_add_black);
//        sItem2.setTag(0, "new");
//	    DrawableItem sItem3 = new DrawableItem(getString(R.string.menu_open_recent), R.drawable.ic_sort_black);
//	    sItem3.setTag(0, "recent");
//        adapter.add(sItem1);
//        adapter.add(sItem2);
//		adapter.add(sItem3);

//		final ArrayList<String> mList = RecentFiles.getRecentFiles();
//		if (mList!=null) {
//			Log.d(TAG, "mList sizes:"+mList.size());
//
//			for (int i=0;i<mList.size();i++) {
//				String _x = mList.get(i);
//				
//				LongTextItem sItem = new LongTextItem(_x);
//				sItem.setTag(0, _x);
//				adapter.add(sItem);
//			}
//
//		}
//        adapter.notifyDataSetChanged();
//        drawerPanent.addView(view);
    }


    /**
     * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //Log.d("TED", "onRestoreInstanceState");
        //Log.v("TED", binding.editor.getText().toString());
    }


    /**
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bundle extras;
        if (CONF.DEBUG)
            Log.d(TAG, "onActivityResult");
        mReadIntent = false;

        if (resultCode == RESULT_CANCELED) {
            if (CONF.DEBUG)
                Log.d(TAG, "Result canceled");
            return;
        }

        if ((resultCode != RESULT_OK) || (data == null)) {
            if (CONF.DEBUG)
                Log.e(TAG, "Result error or null data! / " + resultCode);
            return;
        }

        extras = data.getExtras();
        if (extras == null) {
            if (CONF.DEBUG)
                Log.e(TAG, "No extra data ! ");
            return;
        }

        switch (requestCode) {
            case REQUEST_SAVE_AS:
                if (CONF.DEBUG)
                    Log.d(TAG, "Save as : " + extras.getString("path"));
                doSaveFile(extras.getString("path"), true);
                break;
            case REQUEST_OPEN:
            case REQUEST_RECENT:
                if (CONF.DEBUG)
                    Log.d(TAG, "Open : " + extras.getString("path"));
                doOpenFile(new File(extras.getString("path")), false);
                break;
        }
    }

    /**
     * @see android.app.Activity#onConfigurationChanged(android.content.res.Configuration)
     */
    /*
     * public void onConfigurationChanged(Configuration newConfig) { super.onConfigurationChanged(newConfig); if
	 * (CONF.DEBUG) Log.d(TAG, "onConfigurationChanged"); }
	 */

    /**
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

	/*
     * protected void prepareQuickActionBarM(int flag) { mBarM = new QuickActionBar(this); mBarM.addQuickAction(new
	 * MyQuickAction(this, R.drawable.ic_new_a, R.string.info_new)); mBarM.addQuickAction(new MyQuickAction(this,
	 * R.drawable.ic_go, R.string.info_go)); mBarM.addQuickAction(new MyQuickAction(this, R.drawable.ic_search2,
	 * R.string.info_help)); mBarM.setOnQuickActionClickListener(mActionListener); } protected
	 * OnQuickActionClickListener mActionListener = new OnQuickActionClickListener() { public void
	 * onQuickActionClicked(QuickActionWidget widget, int position) { switch (position) { case 0: newContent(); break;
	 * case 1: onPlay(null); //openFile(); break; case 2: String link = NAction.getExtP(getApplicationContext(),
	 * "conf_manul_link"); if (link.equals("")) { link = CONF.MANUAL_LINK; }
	 * startActivity(NAction.openRemoteLink(getApplicationContext(), link)); break; default: } } };
	 */

    /**
     * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
     */
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.clear();
        addMenuItem(menu, MENU_ID_OPEN, R.string.menu_open, R.drawable.ic_editer_arrow_left);
        addMenuItem(menu, MENU_ID_NEW, R.string.menu_new, R.drawable.ic_menu_file_new);
        if (!mReadOnly) {
            addMenuItem(menu, MENU_ID_SAVE, R.string.menu_save, R.drawable.ic_menu_save);
        }

        addMenuItem(menu, MENU_ID_SEARCH, R.string.menu_search, R.drawable.ic_menu_search);
        if ((!mReadOnly) && Settings.UNDO) {
            addMenuItem(menu, MENU_ID_UNDO, R.string.menu_undo, R.drawable.ic_menu_undo);
        }
        addMenuItem(menu, MENU_ID_SAVE_AS, R.string.menu_save_as, R.drawable.ic_menu_save);
        if (RecentFiles.getRecentFiles().size() > 0)
            addMenuItem(menu, MENU_ID_OPEN_RECENT, R.string.menu_open_recent, R.drawable.ic_editer_arrow_right);
        addMenuItem(menu, MENU_ID_SETTINGS, R.string.menu_settings, 0);
        addMenuItem(menu, MENU_ID_ABOUT, R.string.menu_about, 0);
        if (Settings.BACK_BTN_AS_UNDO && Settings.UNDO) {
            addMenuItem(menu, MENU_ID_QUIT, R.string.menu_quit, 0);
        }
        if ((!mReadOnly) && Settings.UNDO) {
            showMenuItemAsAction(menu.findItem(MENU_ID_NEW), R.drawable.ic_add);
        }
        showMenuItemAsAction(menu.findItem(MENU_ID_OPEN), R.drawable.ic_editor_open);


        return true;
    }


    /**
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mWarnedShouldQuit = false;
        switch (item.getItemId()) {
            case MENU_ID_NEW:
                newContent();
                return true;
            case MENU_ID_SAVE:
                saveContent();
                break;
            case MENU_ID_SAVE_AS:
                saveContentAs();
                break;
            case MENU_ID_OPEN:
                openFile();
                break;
            case MENU_ID_OPEN_RECENT:
                openRecentFile();
                break;
            case MENU_ID_SEARCH:
                search();
                break;
            case MENU_ID_SETTINGS:
                settingsActivity();
                return true;
            case MENU_ID_ABOUT:
                aboutActivity();
                return true;
            case MENU_ID_QUIT:
                quit();
                return true;
            case MENU_ID_UNDO:
                if (!undo()) {
                    Toast.makeText(getApplicationContext(), R.string.toast_warn_no_undo, Toast.LENGTH_SHORT).show();
                    // Crouton.showText(this, R.string.toast_warn_no_undo, Style.INFO);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * If no text is selected the button activates the settings screen If text is selected it activates the help menu
     *
     * @param v
     */

    public void onMore(View v) {
        int startSelection = binding.editor.getSelectionStart();
        int endSelection = binding.editor.getSelectionEnd();
        String selectedText = binding.editor.getText().toString().substring(startSelection, endSelection);
        if (selectedText.length() != 0) {
            // Toast.makeText(this, selectedText, Toast.LENGTH_SHORT).show();
            // String Search = "http://docs.qpython.org/2/search.html?q=" + selectedText;
            String Search = "http://docs.qpython.org/doc/?q=" + selectedText;
            if (NAction.getCode(this).startsWith("lua")) {
                Search = "http://qlua.quseit.com/doc/?q=" + selectedText;
            }
            Intent intent = new Intent(getApplicationContext(), MiniWebViewActivity.class);
            Uri data = Uri.parse(Search);
            mReadIntent = false;
            IS_DOC_BACK = true;
            intent.setData(data);
            startActivityForResult(intent, DOC_FLAG);
        } else {
            Intent intent = new Intent(this, TedSettingsActivity.class);
            startActivity(intent);
        }
    }

    /**
     * @see android.text.TextWatcher#beforeTextChanged(java.lang.CharSequence, int, int, int)
     */
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (Settings.UNDO && (!mInUndo) && (mWatcher != null))
            mWatcher.beforeChange(s, start, count, after);
    }

    /**
     * @see android.text.TextWatcher#onTextChanged(java.lang.CharSequence, int, int, int)
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (mInUndo)
            return;

        if (Settings.UNDO && (!mInUndo) && (mWatcher != null))
            mWatcher.afterChange(s, start, before, count);

    }

    /**
     * @see android.text.TextWatcher#afterTextChanged(android.text.Editable)
     */
    @Override
    public void afterTextChanged(Editable s) {
        if (!mDirty) {
            mDirty = true;
            updateTitle();
        }
    }

    /**
     * @see android.app.Activity#onKeyUp(int, android.view.KeyEvent)
     */

    // public boolean onKeyUp(int keyCode, KeyEvent event) {
    // TODO
    // // switch (keyCode) {
    // // case KeyEvent.KEYCODE_F5:
    // // Toast.makeText(this,"f5 f5 f5", Toast.LENGTH_SHORT).show();
    // // runScript();
    // // return true;
    // //
    // // }
    // // mWarnedShouldQuit = false;
    // return super.onKeyUp(keyCode, event);
    // }

    /**
     * @see OnClickListener#onClick(View)
     */
    @Override
    public void onClick(View v) {
        mWarnedShouldQuit = false;
//        if (v.getId() == R.id.buttonSearchClose) {
//            search();
//        } else if (v.getId() == R.id.buttonSearchNext) {
//            searchNext();
//        } else if (v.getId() == R.id.buttonSearchPrev) {
//            searchPrevious();
//        }
    }

    /**
     * Read the intent used to start this activity (open the text file) as well as the non configuration instance if
     * activity is started after a screen rotate
     */
    protected void readIntent() {
        readIntent(getIntent());
    }
    protected void readIntent(Intent intent) {
        String action;
        File file;

        if (intent == null) {
            if (CONF.DEBUG)
                Log.d(TAG, "No intent found, use default instead");
            doDefaultAction();
            return;
        }

        action = intent.getAction();
        if (action == null) {
            if (CONF.DEBUG)
                Log.d(TAG, "Intent w/o action, default action");
            doDefaultAction();
        } else if ((action.equals(Intent.ACTION_VIEW)) || (action.equals(Intent.ACTION_EDIT))) {
            try {
                file = new File(new URI(intent.getDataString()));
                doOpenFile(file, false);
            } catch (URISyntaxException e) {
                Crouton.showText(this, R.string.toast_intent_invalid_uri, Style.ALERT);
            } catch (IllegalArgumentException e) {
                Crouton.showText(this, R.string.toast_intent_illegal, Style.ALERT);
            }
        } else if (action.equals(ACTION_WIDGET_OPEN)) {
            try {
                file = new File(new URI(intent.getData().toString()));
                doOpenFile(file, intent.getBooleanExtra(EXTRA_FORCE_READ_ONLY, false));
            } catch (URISyntaxException e) {
                Crouton.showText(this, R.string.toast_intent_invalid_uri, Style.ALERT);
            } catch (IllegalArgumentException e) {
                Crouton.showText(this, R.string.toast_intent_illegal, Style.ALERT);
            }
        } else if (action.equals(ACTION_WIDGET_TEXT)) {
            doOpenText(intent.getStringExtra("TEXT"));
        } else {
            doDefaultAction();
        }
    }

    /**
     * Run the default startup action
     */
    protected void doDefaultAction() {
        File file;
        boolean loaded;

        loaded = doOpenBackup();

        if ((!loaded) && Settings.USE_HOME_PAGE) {
            file = new File(Settings.HOME_PAGE_PATH);
            if (!file.exists()) {
                Toast.makeText(getApplicationContext(), R.string.toast_open_home_page_error, Toast.LENGTH_SHORT).show();

                //Crouton.showText(this, R.string.toast_open_home_page_error, Style.ALERT);
            } else if (!file.canRead()) {
                Toast.makeText(getApplicationContext(), R.string.toast_home_page_cant_read, Toast.LENGTH_SHORT).show();

                //Crouton.showText(this, R.string.toast_home_page_cant_read, Style.ALERT);
            } else {
                loaded = doOpenFile(file, false);
            }
        }

        if (!loaded) doClearContents();
    }

    /**
     * Clears the content of the editor. Assumes that user was prompted and previous data was saved
     */
    protected void doClearContents() {
        mWatcher = null;
        mInUndo = true;
        binding.editor.setText("");
        mCurrentFilePath = null;
        mCurrentFileName = null;
        Settings.END_OF_LINE = Settings.DEFAULT_END_OF_LINE;
        mDirty = false;
        mReadOnly = false;
        mWarnedShouldQuit = false;
        mWatcher = new TextChangeWatcher();
        mInUndo = false;
        mDoNotBackup = false;

        TextFileUtils.clearInternal(getApplicationContext());

        // ImageButton pBtn = (ImageButton)findViewById(R.id.play_btn);
        // pBtn.setImageResource(R.drawable.transparent);
        // pBtn.setVisibility(View.GONE);
        updateTitle();
    }

    /**
     * Opens the given file and replace the editors content with the file. Assumes that user was prompted and previous
     * data was saved
     *
     * @param file          the file to load
     * @param forceReadOnly force the file to be used as read only
     * @return if the file was loaded successfully
     */
    protected boolean doOpenFile(File file, boolean forceReadOnly) {

        String text;

        if (file == null)
            return false;

        if (CONF.DEBUG)
            Log.i(TAG, "Openning file " + file.getName());

        try {
            text = TextFileUtils.readTextFile(file);
            mCurrentFilePath = getCanonizePath(file);
            mCurrentFileName = file.getName();
            if (file.canWrite() && (!forceReadOnly)) {
                mReadOnly = false;
                binding.editor.setEnabled(true);
            } else {
                mReadOnly = true;
                binding.editor.setEnabled(false);
            }
            setEditorText(text);
        } catch (OutOfMemoryError e) {
            Crouton.showText(this, R.string.toast_memory_open, Style.ALERT);
        }

        return false;
    }

    protected boolean doOpenText(String text) {
        return setEditorText(text);
    }

    private boolean setEditorText(String text) {
        if (text != null) {
            mInUndo = true;
            binding.editor.setText(text);
            mWatcher = new TextChangeWatcher();
            RecentFiles.updateRecentList(mCurrentFilePath);
            RecentFiles.saveRecentList(getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE));
            mDirty = false;
            mInUndo = false;
            mDoNotBackup = false;


            if (mCurrentFilePath != null && (mCurrentFilePath.endsWith(".py") || mCurrentFilePath.endsWith(".lua"))) {
                if (mCurrentFilePath.endsWith(".py")) {
                    binding.editor.updateFromSettings("py");
                } else {
                    binding.editor.updateFromSettings("lua");
                }
            } else {
                binding.editor.updateFromSettings("");

            }

            updateTitle();
            // initDrawerMenu(this);

            NStorage.setSP(getApplicationContext(), "qedit.last_filename", mCurrentFilePath);
            return true;
        } else {
            Toast.makeText(this, R.string.toast_open_error, Toast.LENGTH_SHORT).show();
            // Crouton.showText(this, R.string.toast_open_error, Style.ALERT);
            return false;
        }
    }

    /**
     * Open the last backup file
     *
     * @return if a backup file was loaded
     */
    protected boolean doOpenBackup() {

        String text;

        try {
            text = TextFileUtils.readInternal(this);
            if (!TextUtils.isEmpty(text)) {
                mInUndo = true;
                binding.editor.setText(text);
                mWatcher = new TextChangeWatcher();
                mCurrentFilePath = null;
                mCurrentFileName = null;
                mDirty = false;
                mInUndo = false;
                mDoNotBackup = false;
                mReadOnly = false;
                binding.editor.setEnabled(true);

                updateTitle();

                return true;
            } else {
                return false;
            }
        } catch (OutOfMemoryError e) {
            Toast.makeText(getApplicationContext(), R.string.toast_memory_open, Toast.LENGTH_SHORT).show();

            //Crouton.showText(this, R.string.toast_memory_open, Style.ALERT);
        }

        return true;
    }

    /**
     * Saves the text editor's content into a file at the given path. If an after save {@link Runnable} exists, run it
     *
     * @param path the path to the file (must be a valid path and not null)
     */
    protected void doSaveFile(String path, boolean show) {
        String content;

        if (path == null) {
            Toast.makeText(getApplicationContext(), R.string.toast_save_null, Toast.LENGTH_SHORT).show();

            //Crouton.showText(this, R.string.toast_save_null, Style.ALERT);
            return;
        }

        content = binding.editor.getText().toString();

        if (!TextFileUtils.writeTextFile(path + ".tmp", content)) {
            Toast.makeText(getApplicationContext(), R.string.toast_save_temp, Toast.LENGTH_SHORT).show();
            // Crouton.showText(this, R.string.toast_save_temp, Style.ALERT);
            return;
        }

        if (!deleteItem(path)) {
            Toast.makeText(getApplicationContext(), R.string.toast_save_delete, Toast.LENGTH_SHORT).show();
            // Crouton.showText(this, R.string.toast_save_delete, Style.ALERT);
            return;
        }

        if (!renameItem(path + ".tmp", path)) {
            Toast.makeText(getApplicationContext(), R.string.toast_save_rename, Toast.LENGTH_SHORT).show();
            // Crouton.showText(this, R.string.toast_save_rename, Style.ALERT);
            return;
        }

        mCurrentFilePath = getCanonizePath(new File(path));
        mCurrentFileName = (new File(path)).getName();
        RecentFiles.updateRecentList(path);
        RecentFiles.saveRecentList(getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE));
        mReadOnly = false;
        mDirty = false;
        updateTitle();
        if (mCurrentFilePath != null && (mCurrentFilePath.endsWith(".py") || mCurrentFilePath.endsWith(".lua"))) {
            if (mCurrentFilePath.endsWith(".py")) {
                binding.editor.updateFromSettings("py");
            } else {
                binding.editor.updateFromSettings("lua");
            }

        } else {
            binding.editor.updateFromSettings("");

        }

        NStorage.setSP(getApplicationContext(), "qedit.last_filename", mCurrentFilePath);

        // Crouton.showText(this, R.string.toast_save_success, Style.CONFIRM);
        if (show) {
            Toast.makeText(getApplicationContext(), R.string.toast_save_success, Toast.LENGTH_SHORT).show();
            // initDrawerMenu(this);
        }

        runAfterSave();
    }

    @SuppressWarnings("deprecation")
    protected void doAutoSaveFile(boolean show) {
        if (mDoNotBackup) {
            doClearContents();
        }

        String text = binding.editor.getText().toString();
        if (text.length() == 0)
            return;

        if (TextFileUtils.writeInternal(this, text)) {
            if (show)
                showToast(this, R.string.toast_file_saved_auto, false);
        }
    }

    /**
     * Undo the last change
     *
     * @return if an undo was don
     */
    protected boolean undo() {
        boolean didUndo = false;
        mInUndo = true;
        int caret;
        caret = mWatcher.undo(binding.editor.getText());
        if (caret >= 0) {
            binding.editor.setSelection(caret, caret);
            didUndo = true;
        }
        mInUndo = false;

        return didUndo;
    }

    /**
     * Prompt the user to save the current file before doing something else
     */
    protected void promptSaveDirty() {
        Builder builder;

        if (!mDirty) {
            runAfterSave();
            return;
        }

        builder = new Builder(this);
        builder.setTitle(R.string.app_name);
        builder.setMessage(R.string.ui_save_text);

        builder.setPositiveButton(R.string.ui_save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveContent();
                mDoNotBackup = true;
            }
        });
        builder.setNegativeButton(R.string.ui_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.setNeutralButton(R.string.ui_no_save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                runAfterSave();
                mDoNotBackup = true;
            }
        });

        builder.create().show();

    }

    /**
     * @param type WEB_PROJECT/CONSOLE_PROJECT/KIVY_PROJECT
     */
    protected void newProject(final String type) {
        NStorage.setSP(getApplicationContext(), "qedit.last_filename", "");
        new EnterDialog(TedActivity.this)
                .setTitle(getString(R.string.new_project))
                .setHint(getString(R.string.project_name))
                .setConfirmListener(new EnterDialog.ClickListener() {
                    @Override
                    public boolean OnClickListener(String name) {
                        Stack<String> curArtistDir = new Stack<>();
                        final boolean isQpy3 = NAction.isQPy3(getApplicationContext());

                        curArtistDir.push(Environment.getExternalStorageDirectory() + "/" + CONF.BASE_PATH
                                + "/" + (isQpy3 ? CONF.DFROM_PRJ3 : CONF.DFROM_PRJ2) + "/" + name);

                        File fileN = new File(curArtistDir.peek());
                        if (fileN.exists()) {
                            Toast.makeText(getApplicationContext(), R.string.file_exists, Toast.LENGTH_SHORT)
                                    .show();
                            return false;
                        } else {
                            try {
                                newContent();

                                if (fileN.mkdirs()) {
                                    File mainPy = new File(fileN.getAbsolutePath(), "main.py");
                                    if (mainPy.createNewFile()) {
                                        mCurrentFilePath = mainPy.getAbsolutePath();
                                        doOpenFile(mainPy, false);
                                        insertSnippet(type);
                                        saveContent();
                                    }
                                }
                            } catch (IOException e) {
                                Toast.makeText(TedActivity.this, R.string.error_dont_kn, Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                                return false;
                            }
                            Toast.makeText(getApplicationContext(), R.string.success, Toast.LENGTH_SHORT)
                                    .show();
                            return true;
                        }
                    }
                })
                .show();

    }

    protected void newContent() {
        mAfterSave = new Runnable() {
            @Override
            public void run() {
                doClearContents();
            }
        };

        promptSaveDirty();
    }

    /**
     * Runs the after save to complete
     */
    protected void runAfterSave() {
        if (mAfterSave == null) {
            if (CONF.DEBUG)
                Log.d(TAG, "No After shave, ignoring...");
            return;
        }

        mAfterSave.run();

        mAfterSave = null;
    }

    /**
     * Starts an activity to choose a file to open
     */
    protected void openFile() {
        if (CONF.DEBUG)
            Log.d(TAG, "openFile");

        mAfterSave = new Runnable() {
            @Override
            public void run() {
                Intent open = new Intent();
                open.setClass(getApplicationContext(), TedLocalActivity.class);
                // open = new Intent(ACTION_OPEN);
                open.putExtra(EXTRA_REQUEST_CODE, REQUEST_OPEN);
                try {
                    startActivityForResult(open, REQUEST_OPEN);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getApplicationContext(), R.string.toast_activity_open, Toast.LENGTH_SHORT).show();

                    //Crouton.showText(TedActivity.this, R.string.toast_activity_open, Style.ALERT);
                }
            }
        };

        promptSaveDirty();
    }

    /**
     * @param v
     */
    public void onBrowser(View v) {
        Intent intent = new Intent(this, MiniWebViewActivity.class);
        startActivity(intent);
    }

    /**
     * Open the recent files activity to open
     */
    protected void openRecentFile() {
        if (CONF.DEBUG)
            Log.d(TAG, "openRecentFile");

        if (RecentFiles.getRecentFiles().size() == 0) {
            Toast.makeText(getApplicationContext(), R.string.toast_no_recent_files, Toast.LENGTH_SHORT).show();
            // Crouton.showText(this, R.string.toast_no_recent_files, Style.ALERT);
            return;
        }

        mAfterSave = new Runnable() {
            @Override
            public void run() {
                Intent open;

                open = new Intent();
                open.setClass(TedActivity.this, TedLocalActivity.class);
                open.putExtra(EXTRA_REQUEST_CODE, REQUEST_RECENT);

                try {
                    startActivityForResult(open, REQUEST_RECENT);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getApplicationContext(), R.string.toast_activity_open_recent, Toast.LENGTH_SHORT)
                            .show();
                    // Crouton.showText(TedActivity.this, R.string.toast_activity_open_recent, Style.ALERT);
                }
            }
        };

        promptSaveDirty();
    }

    protected void finishWithoutSave() {
        mAfterSave = new Runnable() {
            @Override
            public void run() {

                finish();
            }
        };

        promptSaveDirty();
    }

    protected void quitWithoutSave() {
        mAfterSave = new Runnable() {
            @Override
            public void run() {
                doClearContents();
            }
        };

        promptSaveDirty();
    }

    /**
     * Warns the user that the next back press will quit the application, or quit if the warning has already been shown
     */
    protected void warnOrQuit() {
        if (mWarnedShouldQuit) {
            quit();
        } else {
            Toast.makeText(getApplicationContext(), R.string.toast_warn_no_undo_will_quit, Toast.LENGTH_SHORT).show();

            //Crouton.showText(this, R.string.toast_warn_no_undo_will_quit, Style.INFO);
            mWarnedShouldQuit = true;
        }
    }

    /**
     * Quit the app (user pressed back)
     */
    protected void quit() {
        final String code = NAction.getCode(this);

        mAfterSave = new Runnable() {
            @Override
            public void run() {
                if (code.contains("qedit")) {

                } else {
                    if (mCurrentFilePath == null) {

                        finish();
                    } else {
                        newContent();
                    }
                }
            }
        };

        promptSaveDirty();
    }

//    @Override
//    public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {
//        switch (item.getItemId()) {
//            case 10:
//                saveContentAs();
//                break;
//            case 20:
//                openFile();
//                break;
//            case 25:
//                saveContent();
//                break;
//
//            case 30:
//                // mBarM.show(item.getItemView());
//                // SnippetsList();
//                newProject();
//
//                // newContent();
//                // NStorage.setSP(getApplicationContext(), "qedit.last_filename", "");
//                // Toast.makeText(getApplicationContext(), R.string.success, Toast.LENGTH_SHORT).show();
//
//                break;
//		/*
//		 * case 35: newProject(); break;
//		 */
//
//            case 50:
//                onGSetting(null);
//                break;
//            default:
//
//        }
//        return super.onHandleActionBarItemClick(item, position);
//    }

    public void onSnippets(View v) {
        int startSelection = binding.editor.getSelectionStart();
        int endSelection = binding.editor.getSelectionEnd();
        String selectedText = binding.editor.getText().toString().substring(startSelection, endSelection);
        /**
         * Detect if the text is selected
         */
        if (selectedText.length() != 0) {
            getInfo();
        } else {

            SnippetsList(false);
        }
    }

    // TODO

    @Override
    public boolean onKeyUp(int keyCoder, KeyEvent event) {
        boolean isCtr = event.isCtrlPressed();
        if (keyCoder == KeyEvent.KEYCODE_BACK) {
            if (searchTopBinding != null && searchTopBinding.llSearch.getVisibility() == View.VISIBLE) {
                search();
            } else if (isKeyboardShowing) {
                hideKeyboard();
            } else {
                finishEdit();
            }
//            else if (Settings.UNDO && Settings.BACK_BTN_AS_UNDO) {
//                if (!undo()) {
//                    warnOrQuit();
//                }
//            }
            return true;
        } else if (isCtr) {
            switch (keyCoder) {
                case KeyEvent.KEYCODE_F:
                    setSearch();
                    break;
                case KeyEvent.KEYCODE_S:
                    saveContent();
                    break;
                case KeyEvent.KEYCODE_O:
                    openFile();
                    break;
                case KeyEvent.KEYCODE_Z:
                    undo();
                    break;
                case KeyEvent.KEYCODE_L:
                    goToLine();
                    break;
                case KeyEvent.KEYCODE_LEFT_BRACKET:
                    leftIndent();
                    break;
                case KeyEvent.KEYCODE_RIGHT_BRACKET:
                    rightIndent();
                    break;
                case KeyEvent.KEYCODE_R:
                    runScript();
                    break;
                case KeyEvent.KEYCODE_SOFT_LEFT:

                default:
                    break;
            }
        } else {
            switch (keyCoder) {
                // F5 ,run the code
                case KeyEvent.KEYCODE_F5:
                    // Toast.makeText(this,"f5 f5 f5", Toast.LENGTH_SHORT).show();
                    runScript();
                    break;
                // tab ,input 4 space
                case KeyEvent.KEYCODE_TAB:
                    rightIndent();
                    // binding.editor.setSelection(binding.editor.getText().length());
                    // binding.editor.setText(binding.editor.getText());
                    binding.editor.setFocusable(true);
                    binding.editor.setFocusableInTouchMode(true);
                    binding.editor.requestFocus();
                    binding.editor.requestFocusFromTouch();
                    binding.editor.findFocus();
                    // Toast.makeText(this,"tab tab tab", Toast.LENGTH_SHORT).show();
                    break;
                case KeyEvent.KEYCODE_BACK:

//                    if (mSearchLayout.getVisibility() != View.GONE)
//                        search();
//                    else if (Settings.UNDO && Settings.BACK_BTN_AS_UNDO) {
//
//                        if (!undo())
//                            warnOrQuit();
//                    }
                    // TODO: 2017-05-10
                case KeyEvent.KEYCODE_SEARCH:
                    search();
                    mWarnedShouldQuit = false;
                    break;
            }
            mWarnedShouldQuit = false;
        }

        return super.onKeyDown(keyCoder, event);
    }

    private void finishEdit() {
        if (IS_DOC_BACK) {
            IS_DOC_BACK = false;
        } else {
            if (NAction.getCode(this).contains("qedit")) {
                if (mCurrentFilePath == null) {
                    if (mDirty) {
                        quitWithoutSave();
                    } else {
                        finish();
                    }
                } else {
                    Intent intent = getIntent();
                    String action = intent.getAction();
                    if (action == null) {
                        newContent();
                    } else {
                        if (mDirty) {
                            finishWithoutSave();
                        } else {

                            finish();

                        }
                    }
                }
            } else {

                if (mCurrentFilePath == null) {
                    if (mDirty) {
                        quitWithoutSave();
                    } else {
                        finish();
                    }
                } else {
                    Intent intent = getIntent();
                    String action = intent.getAction();
                    if (action == null) {
                        newContent();
                    } else {
                        if (mDirty) {
                            finishWithoutSave();
                        } else {

                            finish();
                        }
                    }
                }
            }
        }
    }

    /**
     *
     */
    @SuppressWarnings("deprecation")
    public void endScreen() {
        String c = NStorage.getSP(this, "end_count");
        if (c.equals("")) {
            c = "0";
        }
        int e = Integer.parseInt(c) + 1;

        int x = 1;
        if (e > 10000) {
            //
        } else if (e > 100) {
            NStorage.setSP(this, "end_count", String.valueOf(e));

            x = e % 100;
        } else {
            NStorage.setSP(this, "end_count", String.valueOf(e));

            x = e % 10;
        }
        if (x == 0 || e < 3) {
//            WBase.setTxtDialogParam2(0, R.string.confirm_exit, getString(R.string.feed_back),
//                    getString(R.string.follow_community), getString(R.string.rate_app),
//                    getString(R.string.feedback_btn), getString(R.string.follow_community_btn),
//                    getString(R.string.rate_btn), new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//
//                            finish();
//                        }
//                    }, null);
//            showDialog(DialogBase.DIALOG_BTN_ENTRY1 + dialogIndex);
//            dialogIndex++;

        } else {
//            WBase.setTxtDialogParam2(0, R.string.confirm_exit, getString(R.string.feed_back),
//                    getString(R.string.follow_community), "", getString(R.string.feedback_btn),
//                    getString(R.string.follow_community_btn), "", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//
//                            finish();
//                        }
//                    }, null);
//            showDialog(DialogBase.DIALOG_BTN_ENTRY1 + dialogIndex);
//            dialogIndex++;
        }
    }

    /**
     * @param v
     */
    public void onExitPrompt1(View v) {

        Intent intent = NAction.getLinkAsIntent(getApplicationContext(), CONF.COMMUNITY_LINK);
        startActivity(intent);
    }

    /**
     * @param v
     */
    public void onExitPrompt2(View v) {
        String url = NAction.getExtP(this, "conf_promp3_link");
        if (url.equals("")) {
            url = CONF.NEWS_LINK;
        }
        Intent intent = NAction.getLinkAsIntent(getApplicationContext(), url);
        startActivity(intent);
    }

    /**
     * General save command : check if a path exist for the current content, then save it , else invoke the
     * {@link TedActivity#saveContentAs()} method
     */
    protected void saveContent() {
        if ((mCurrentFilePath == null) || (mCurrentFilePath.length() == 0)) {
            saveContentAs();
        } else {
            doSaveFile(mCurrentFilePath, true);
        }
    }

    /**
     * General Save as command : prompt the user for a location and file name, then save the editor'd content
     */
    protected void saveContentAs() {
        if (CONF.DEBUG)
            Log.d(TAG, "saveContentAs");
        Intent saveAs;
        saveAs = new Intent();
        saveAs.setClass(this, TedLocalActivity.class);
        saveAs.putExtra(EXTRA_REQUEST_CODE, REQUEST_SAVE_AS);

        try {
            startActivityForResult(saveAs, REQUEST_SAVE_AS);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(), R.string.toast_activity_save_as, Toast.LENGTH_SHORT).show();

            //Crouton.showText(this, R.string.toast_activity_save_as, Style.ALERT);
        }
    }

    /**
     * Opens / close the search interface
     */
    protected void search() {
        if (CONF.DEBUG)
            Log.d(TAG, "search");
        switch (searchTopBinding.llSearch.getVisibility()) {
            case View.GONE:
                binding.returnBarBox.setVisibility(View.GONE);
                searchTopBinding.llSearch.setVisibility(View.VISIBLE);
                binding.searchBottom.rlSearchBottom.setVisibility(View.VISIBLE);
                searchTopBinding.textSearch.requestFocus();
                break;
            case View.VISIBLE:
            default:
                searchTopBinding.llSearch.setVisibility(View.GONE);
                binding.searchBottom.rlSearchBottom.setVisibility(View.GONE);
                binding.returnBarBox.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * Uses the user input to search a file
     */
    @SuppressLint("DefaultLocale")
    protected void searchNext() {
        String search, text;
        int selection, next;

        search = searchTopBinding.textSearch.getText().toString();
        text = binding.editor.getText().toString();
        selection = binding.editor.getSelectionEnd();

        if (!Settings.SEARCHMATCHCASE) {
            search = search.toLowerCase();
            text = text.toLowerCase();
        }

        next = text.indexOf(search, selection);
        if (next > -1) {
            binding.editor.setSelection(next, next + search.length());
        } else {
            if (Settings.SEARCHWRAP) {
                next = text.indexOf(search);
                if (next > -1) {
                    binding.editor.setSelection(next, next + search.length());
                    if (!binding.editor.isFocused())
                        binding.editor.requestFocus();
                } else {
                    Crouton.showText(this, R.string.toast_search_not_found, Style.INFO);
                }
            } else {
                Crouton.showText(this, R.string.toast_search_eof, Style.INFO);
            }
        }
    }

    /**
     * Uses the user input to search a file
     */
    @SuppressLint("DefaultLocale")
    protected void searchPrevious() {
        String search, text;
        int selection, next;

        search = searchTopBinding.textSearch.getText().toString();
        text = binding.editor.getText().toString();
        selection = binding.editor.getSelectionStart() - 1;

        if (search.length() == 0) {
            Crouton.showText(this, R.string.toast_search_no_input, Style.INFO);
            return;
        }

        if (!Settings.SEARCHMATCHCASE) {
            search = search.toLowerCase();
            text = text.toLowerCase();
        }

        next = text.lastIndexOf(search, selection);

        if (next > -1) {
            binding.editor.setSelection(next, next + search.length());
            if (!binding.editor.isFocused())
                binding.editor.requestFocus();
        } else {
            if (Settings.SEARCHWRAP) {
                next = text.lastIndexOf(search);
                if (next > -1) {
                    binding.editor.setSelection(next, next + search.length());
                    if (!binding.editor.isFocused())
                        binding.editor.requestFocus();
                } else {
                    Crouton.showText(this, R.string.toast_search_not_found, Style.INFO);
                }
            } else {
                Crouton.showText(this, R.string.toast_search_eof, Style.INFO);
            }
        }
    }

    /**
     * @param v
     */
    public void onLeft(View v) {
        leftIndent();
    }

    /**
     * Indent the text left
     */
    public void leftIndent() {
        int index = binding.editor.getSelectionStart();
        Editable editable = binding.editor.getText();
        if (index >= 4) {
            if (editable.subSequence(index - 4, index).toString().equals("    ")) {
                editable.delete(index - 4, index);
            }
        }
    }

    /**
     * Indent the text right
     */
    public void rightIndent() {
        int startSelection = binding.editor.getSelectionStart();
        int endSelection = binding.editor.getSelectionEnd();
        String selectedText = binding.editor.getText().toString().substring(startSelection, endSelection);
        Editable editable = binding.editor.getText();
        if (selectedText.length() != 0) {
            String startData = binding.editor.getText().toString();
            String textData = startData.substring(0, startSelection);
            if (textData.contains("\n")) {
                int newLineIndex = textData.lastIndexOf("\n");
                editable.replace(newLineIndex, newLineIndex, "\n    ");
            } else {
                editable.insert(0, "    ");
            }
            String indentedText = selectedText.replace("\n", "\n    ");
            editable.replace(startSelection, endSelection, indentedText);
        } else {
            int index = binding.editor.getSelectionStart();
            editable.insert(index, "    ");
        }
    }

    public void onBack(View v) {
        if (!undo()) {
            Toast.makeText(getApplicationContext(), R.string.toast_warn_no_undo, Toast.LENGTH_SHORT).show();
            // Crouton.showText(this, R.string.toast_warn_no_undo, Style.INFO);
        }
    }

    /**
     * Calculate the index of the Nth new line
     *
     * @param indexNewLine New line to find
     * @return Gives the position of the Nth new line
     */
    public int NewLineIndex(int indexNewLine) {
        String data = binding.editor.getText().toString();
        final StringBuffer sb = new StringBuffer(data);
        List<Integer> myList = new ArrayList<Integer>();
        myList.add(0);
        for (int i = 0; i < sb.length(); i++) {
            if (sb.charAt(i) == '\n')
                myList.add(i);
        }
        return myList.get(indexNewLine);
    }

    public void goToLine() {
        new EnterDialog(this).setTitle(getString(R.string.jump_to))
                .setEnterType(InputType.TYPE_CLASS_NUMBER)
                .setHint("1~" + binding.editor.getLineCount())
                .setConfirmListener(new EnterDialog.ClickListener() {
                    @Override
                    public boolean OnClickListener(String content) {
                        int line = Integer.parseInt(content);
                        if (line > binding.editor.getLineCount()) {
                            Toast.makeText(TedActivity.this, R.string.fail_to_goto, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        int position = NewLineIndex(line);
                        binding.editor.setSelection(position);
                        return true;
                    }
                })
                .show();
        // String tip = "Line number less than "+binding.editor.getLineCount();
        // final EditText input = new EditText(this);

//        WBase.setTxtDialogParam(0, R.string.line_picker_title, "", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                AlertDialog ad = (AlertDialog) dialog;
//                EditText t = (EditText) ad.findViewById(R.id.editText_prompt);
//                String content = t.getText().toString();
//
//                int lineCount = binding.editor.getLineCount();
//                try {
//                    int lineNumberToGoTo = Integer.parseInt(content);
//                    if (lineNumberToGoTo < lineCount) {
//                        int position = NewLineIndex(lineNumberToGoTo);
//                        binding.editor.setSelection(position);
//                    } else {
//                        Toast.makeText(getApplicationContext(), R.string.fail_to_goto, Toast.LENGTH_SHORT).show();
//                    }
//                } catch (Exception e) {
//                    Toast.makeText(getApplicationContext(), R.string.fail_to_goto, Toast.LENGTH_SHORT).show();
//                }
//
//            }
//        }, null);
//        showDialog(DialogBase.DIALOG_TEXT_ENTRY + 1);
        /*
         * AlertDialog.Builder builder = new AlertDialog.Builder(this); builder.setTitle(R.string.line_picker_title);
		 * final EditText input = new EditText(this); input.setInputType(InputType.TYPE_CLASS_NUMBER); input.setHint();
		 * builder.setView(input); builder.setPositiveButton("Go To", new DialogInterface.OnClickListener() {
		 * @Override public void onClick(DialogInterface dialog, int which) { int lineCount = binding.editor.getLineCount();
		 * try { int lineNumberToGoTo = Integer.parseInt(input.getText().toString()); if(lineNumberToGoTo < lineCount){
		 * int position = NewLineIndex(lineNumberToGoTo); binding.editor.setSelection(position); }else{
		 * Toast.makeText(getApplicationContext(), R.string.fail_to_goto, Toast.LENGTH_SHORT).show(); } } catch
		 * (Exception e) { Toast.makeText(getApplicationContext(), R.string.fail_to_goto, Toast.LENGTH_SHORT).show(); }
		 * } }); builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		 * @Override public void onClick(DialogInterface dialog, int which) { dialog.cancel(); } }); builder.show();
		 */
    }

    /**
     * Run the current script
     */
    public void runScript() {
        if (binding.editor.getText() != null) {
            String content = binding.editor.getText().toString().trim();
            if (content.length() == 0) {
                Toast.makeText(getApplicationContext(), R.string.cannot_empty, Toast.LENGTH_SHORT).show();

            } else {
                if (mCurrentFilePath != null) {
                    if (content.startsWith("<") || mCurrentFilePath.endsWith(".md") || mCurrentFilePath.endsWith(".html")) {
                        // doAutoSaveFile(false);
                        doSaveFile(mCurrentFilePath, false);

                        Intent intent = new Intent(getApplicationContext(), MiniWebViewActivity.class);
                        Uri data = Uri.fromFile(new File(mCurrentFilePath));
                        intent.setData(data);
                        startActivity(intent);
                    } else if (mCurrentFilePath.endsWith(".sh")) {
                        // todo

                        String[] args = {"sh " + mCurrentFilePath};
//                        execInConsole(args);

                        // qpy not support
                    } else if (mCurrentFilePath.endsWith(".lua")) {

//                        callLuaApi("qedit", mCurrentFilePath, content);

                    } else {
                        callPyApi("qedit", mCurrentFilePath, content);
                    }
                }
            }
        }
    }

    /**
     * Opens the about activity
     */
    protected void aboutActivity() {
        Intent about = new Intent();
        about.setClass(this, TedAboutActivity.class);
        try {
            startActivity(about);
        } catch (ActivityNotFoundException e) {
            Crouton.showText(this, R.string.toast_activity_about, Style.ALERT);
        }
    }

    /**
     * Opens the settings activity
     */
    protected void settingsActivity() {

        mAfterSave = new Runnable() {
            @Override
            public void run() {
                Intent settings = new Intent();
                settings.setClass(TedActivity.this, TedSettingsActivity.class);
                try {
                    startActivity(settings);
                } catch (ActivityNotFoundException e) {
                    Crouton.showText(TedActivity.this, R.string.toast_activity_settings, Style.ALERT);
                }
            }
        };

        promptSaveDirty();
    }

    /**
     * Update the window title
     */
    protected void updateTitle() {
        String title;
        String name;

        name = "?";
        if ((mCurrentFileName != null) && (mCurrentFileName.length() > 0))
            name = mCurrentFileName;

        // Log.d(TAG, "updateTitle:"+mCurrentFileName);

        if (mReadOnly)
            title = getString(R.string.title_editor_readonly, name);
        else if (mDirty)
            title = getString(R.string.title_editor_dirty, name);
        else
            title = getString(R.string.title_editor, name);

        setTitle(title);

        invalidateOptionsMenu();
    }

    private void setTitle(String title) {
        binding.tvTitle.setText(title);
    }

    /**
     * Load File from Assets into String
     *
     * @param inFile File to load
     * @return File contents as String
     */
    public String LoadDataFromAssets(String inFile) {
        String tContents = "";

        try {
            InputStream stream = getAssets().open(inFile);
            int size = stream.available();
            byte[] buffer = new byte[size];
            stream.read(buffer);
            stream.close();
            tContents = new String(buffer);
        } catch (IOException e) {
        }
        return tContents;
    }

    /**
     * Read file to String
     *
     * @param
     * @return contents of file
     * @throws IOException
     */
    private String readFile(String pathname) throws IOException {

        File file = new File(pathname);
        StringBuilder fileContents = new StringBuilder((int) file.length());
        Scanner scanner = new Scanner(file);
        String lineSeparator = System.getProperty("line.separator");
        try {
            while (scanner.hasNextLine()) {
                fileContents.append(scanner.nextLine() + lineSeparator);
            }
            return fileContents.toString();
        } finally {
            scanner.close();
        }
    }

    /**
     * Write String to File
     *
     * @param filePath path of file to write to
     * @param data     String to write to file
     */
    public void writeToFile(String filePath, String data) {
        try {
            FileOutputStream fOut = new FileOutputStream(filePath);
            fOut.write(data.getBytes());
            fOut.flush();
            fOut.close();
        } catch (IOException iox) {
            iox.printStackTrace();
        }

    }

    public void onExitPrompt3(View v) {
        String url = NAction.getExtP(getApplicationContext(), "conf_rate_url");
        if (url.equals("")) {
            url = NAction.getInstallLink(getApplicationContext());
        }
        if (url.equals("")) {
            url = CONF.QEDIT_RATE_LINK;
        }
        try {
            Intent intent = NAction.getLinkAsIntent(getApplicationContext(), url);
            startActivity(intent);
        } catch (Exception e) {
            Intent intent = NAction.getLinkAsIntent(getApplicationContext(), CONF.QEDIT_RATE_LINK);
            startActivity(intent);

        }
    }

    private void hideKeyboard() {
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(TedActivity.this.getCurrentFocus().getWindowToken(), 0);
    }

    /**
     * Defines the keyboard layout
     *
     * @author kyle kersey http://developer.android.com/reference/android/view/KeyEvent.html
     */
    // @SuppressLint("NewApi")

    // public boolean onKeyShortcut(int keyCode, KeyEvent event){
    //
    //
    // Log.d(TAG, "TAG INFORMATION keycode:"+keyCode);
    // //TODO
    //
    // /*switch (keyCode) {
    // case KeyEvent.KEYCODE_TAB:
    // rightIndent();
    // break;
    // default:
    // break;
    // } */
    // return false;
    // }


    protected static final int SCRIPT_EXEC_PY = 2235;

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
            intent.setClassName(localPlugin, MPyApi.class.getName());
            intent.setAction("org.qpython.qpylib.action.MPyApi");

            Bundle mBundle = new Bundle();
            mBundle.putString("root", MyApp.getInstance().getRoot());

            mBundle.putString("app", NAction.getCode(getApplicationContext()));
            mBundle.putString("act", "onPyApi");
            mBundle.putString("flag", flag);
            mBundle.putString("param", param);
            mBundle.putString("pycode", proxyCode + pyCode);

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
                    mBundle.putString("pycode", proxyCode + pyCode);

                    intent.putExtras(mBundle);

                    startActivityForResult(intent, SCRIPT_EXEC_PY);

                } else {

//                    WBase.setTxtDialogParam(0, R.string.pls_install_qpy, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            String plgUrl = NAction.getExtP(getApplicationContext(), "ext_plugin_pkg3");
//                            if (plgUrl.equals("")) {
//                                plgUrl = CONF.EXT_PLG_URL3;
//                            }
//                            try {
//                                Intent intent = NAction.getLinkAsIntent(getApplicationContext(), plgUrl);
//                                startActivity(intent);
//                            } catch (Exception e) {
//                                plgUrl = CONF.EXT_PLG_URL3;
//                                Intent intent = NAction.getLinkAsIntent(getApplicationContext(), plgUrl);
//                                startActivity(intent);
//                            }
//                        }
//                    }, null);
//                    showDialog(DialogBase.DIALOG_EXIT+dialogIndex);
//                    dialogIndex++;

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
                    mBundle.putString("pycode", proxyCode + pyCode);

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
                    mBundle.putString("pycode", proxyCode + pyCode);

                    intent.putExtras(mBundle);

                    startActivityForResult(intent, SCRIPT_EXEC_PY);

                } else {

//                    WBase.setTxtDialogParam(0, R.string.pls_install_qpy, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//
//                            String plgUrl = NAction.getExtP(getApplicationContext(), "ext_plugin_pkg");
//                            if (plgUrl.equals("")) {
//                                plgUrl = com.quseit.config.CONF.EXT_PLG_URL;
//                            }
//                            Intent intent = NAction.getLinkAsIntent(getApplicationContext(), plgUrl);
//                            startActivity(intent);
//                        }
//                    }, null);
//                    showDialog(DialogBase.DIALOG_EXIT+dialogIndex);
//                    dialogIndex++;
                }
            }

        }
    }

}
