package com.quseit.texteditor;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.quseit.texteditor.common.CommonEnums;
import com.quseit.texteditor.common.Constants;
import com.quseit.texteditor.common.RecentFiles;
import com.quseit.texteditor.databinding.ActivityLocalBinding;
import com.quseit.texteditor.databinding.ViewStubSavePromptBinding;
import com.quseit.texteditor.ui.adapter.PathListAdapter;
import com.quseit.texteditor.ui.adapter.bean.FolderBean;
import com.quseit.texteditor.ui.view.EnterDialog;
import com.quseit.texteditor.ui.view.MyItemDecoration;
import com.quseit.texteditor.ui.view.MyTouchHelperCallback;
import com.quseit.texteditor.widget.crouton.Crouton;
import com.quseit.texteditor.widget.crouton.Style;
import com.quseit.util.FileHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import static com.quseit.util.FolderUtil.sortTypeByName;

public class TedLocalActivity extends Activity implements Constants {
    private static final String TAG = "local";
    private ActivityLocalBinding      binding;
    private ViewStubSavePromptBinding saveBinding;

    private Stack<String>    curArtistDir;
    private Stack<String>    prevDir;
    private List<FolderBean> folderList;
    private PathListAdapter  adapter;

    private int request;
    private int _GLOBAL_DEPTH = 0;
    private boolean deleteable;

    @SuppressWarnings({"deprecation", "unchecked", "rawtypes"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_local);
        initView();
        initField();
        initListener();
        switch (request) {
            case REQUEST_RECENT:
                binding.toolbarTitle.setText(R.string.recent);
                binding.rlPath.setVisibility(View.GONE);
                break;
            case REQUEST_OPEN:
                binding.toolbarTitle.setText(R.string.open);
                break;
            case REQUEST_SAVE_AS:
                binding.toolbarTitle.setText(R.string.save_as);
                binding.ivNewFolder.setVisibility(View.VISIBLE);
                saveBinding = DataBindingUtil.bind(binding.vsSave.getViewStub().inflate());
                initSaveListener();
                break;
            case REQUEST_HOME_PAGE:
                binding.toolbarTitle.setText(R.string.explorer);
                binding.ivNewFolder.setVisibility(View.VISIBLE);
                deleteable = true;
                break;
        }
        myloadContent("", -1);

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void initView() {
        folderList = new ArrayList<>();
        adapter = new PathListAdapter(folderList, this, new PathListAdapter.Callback() {
            @Override
            public void click(int position) {
                FolderBean item = folderList.get(position);
                if (item.getType().equals(CommonEnums.FileType.FILE)) {
                    TedActivity.start(TedLocalActivity.this, Intent.ACTION_EDIT, Uri.fromFile(item.getFile()));
                } else {
                    myloadContent(item.getName(), -1);
                }
            }
        });
        binding.lvFolders.setLayoutManager(new LinearLayoutManager(this));
        binding.lvFolders.setAdapter(adapter);
        if (deleteable) {
            binding.lvFolders.setHasFixedSize(true);
            new ItemTouchHelper(new MyTouchHelperCallback(0, ItemTouchHelper.LEFT, adapter, this)).attachToRecyclerView(binding.lvFolders);
            binding.lvFolders.addItemDecoration(new MyItemDecoration());
        }
    }

    private void initField() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            request = extras.getInt(EXTRA_REQUEST_CODE);
        } else {
            request = -1;
        }
        prevDir = new Stack();
        curArtistDir = new Stack<>();

        // init folder stack
        String root = CONF.ABSOLUTE_PATH; // for qpython apps
        String[] path = root.split("/");
        _GLOBAL_DEPTH = path.length;
        curArtistDir.push("/");
        if (path.length > 1) {
            for (String folder : path) {
                if (!folder.equals("")) {
                    String yy;
                    if (curArtistDir.peek().endsWith("/")) {
                        yy = curArtistDir.peek() + folder;
                    } else {
                        yy = curArtistDir.peek() + "/" + folder;
                    }
                    curArtistDir.push(yy);
                }
            }
        }

    }

    public static void start(Context context) {
        Intent starter = new Intent(context, TedLocalActivity.class);
        context.startActivity(starter);
    }

    public static void start(Context context, int requestCode) {
        Intent starter = new Intent(context, TedLocalActivity.class);
        starter.putExtra(EXTRA_REQUEST_CODE, requestCode);
        context.startActivity(starter);
    }

    public boolean onTop() {
        if (curArtistDir.size() == 1) {
            finish();

            return false;

        } else {

            String xx = curArtistDir.pop();
            /*if (xx.lastIndexOf("/")+1 < xx.length()) {
                xx = xx.substring(xx.lastIndexOf("/")+1);
    			prevDir.push(xx);
    		}*/
            prevDir.push(xx);

            //Log.d(TAG, "prevDir:"+prevDir);
            myloadContent("", 0);
            return true;
        }
    }


//    private OnQuickActionClickListener mActionListener = new OnQuickActionClickListener() {
//        @Override
//		public void onQuickActionClicked(QuickActionWidget widget, int position) {
//        	switch (position) {
//	        	case 0:
//
//    				deleteCurItem();
//    				//Toast.makeText(getApplicationContext(), R.string.not_implement, Toast.LENGTH_SHORT).show();
//                    break;
//        		case 1:
//        			renameItem(curTextItem);
//        	    	//infoOpen(curTextItem, 0);
//        			break;
//        		case 2:
//        			shareFile();
//        			break;
//        		case 3:
//                    break;
//                default:
//        	}
//        }
//    };
//

    public String getCurrentDir() {
        return curArtistDir.peek();
    }

    @SuppressLint("DefaultLocale")
    public void myloadContent(String dirname, int position) {
        //String code = NAction.getCode(getApplicationContext());
        if (request == REQUEST_RECENT) {
            folderList.clear();
            for (String path : RecentFiles.getRecentFiles()
                    ) {
                folderList.add(new FolderBean(new File(path)));
            }
            adapter.notifyDataSetChanged();
        } else {
            if (dirname != null && !dirname.equals("")) {
                curArtistDir.push(curArtistDir.peek() + "/" + dirname);
            }

            String curDir = getCurrentDir();
            binding.tvPath.setText(curDir);
            File d = new File(curDir);
            if (d.exists()) {
                try {
                    File[] files = FileHelper.getABSPath(curDir).listFiles();
                    if (files != null) {
                        Arrays.sort(files, sortTypeByName);
                        folderList.clear();
                        for (File file : files) {
                            folderList.add(new FolderBean(file));
                        }
                        adapter.notifyDataSetChanged();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(getApplicationContext(), R.string.file_not_exits, Toast.LENGTH_SHORT).show();
            }

            if (position != -1) {
                binding.lvFolders.smoothScrollToPosition(position);
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void renameItem(/*final TextItem textItem*/) {
//    	Object o1 = textItem.getTag(1);
//    	if (o1!=null) {
//			final String fullname = o1.toString();
//			final File oldf = new File(fullname);
//
//			WBase.setTxtDialogParam(R.drawable.ic_setting, R.string.info_rename, oldf.getName(),
//					new DialogInterface.OnClickListener() {
//						@Override
//						public void onClick(DialogInterface dialog, int which) {
//					        AlertDialog ad = (AlertDialog) dialog;
//					        EditText t = (EditText) ad.findViewById(R.id.editText_prompt);
//					        String filename = t.getText().toString().trim();
//					        File newf = new File(oldf.getParent()+"/"+filename);
//					        if (newf.exists()) {
//					        	Toast.makeText(getApplicationContext(), R.string.file_exists, Toast.LENGTH_SHORT).show();
//					        	renameItem(textItem);
//					        } else {
//					        	oldf.renameTo(newf);
//						        myloadContent("", curPosition);
//					        }
//
//						}
//					},null);
//			showDialog(DialogBase.DIALOG_TEXT_ENTRY+dialogIndex);
//			dialogIndex++;
//    	}
    }

    @SuppressWarnings("deprecation")
    public void deleteCurItem() {
//    	Object o1 = curTextItem.getTag(1);
//    	final String filename = o1.toString();
//		WBase.setTxtDialogParam(R.drawable.alert_dialog_icon, R.string.confirm_delete, new DialogInterface.OnClickListener() {
//			@Override
//			public void onClick(DialogInterface dialog, int which) {
//		    	File file = new File(filename);
//		    	if (file.isFile()) {
//		    		file.delete();
//		    	} else {
//		    		FileHelper.clearDir(filename, 0, true);
//		    	}
//		    	adapter.remove(curTextItem);
//		    	adapter.notifyDataSetChanged();
//			}
//			});
//			showDialog(DialogBase.DIALOG_YES_NO_MESSAGE+dialogIndex);
//			dialogIndex++;
    }

    /**
     * Share the selected file
     */
    public void shareFile() {
//    	Object o1 = curTextItem.getTag(1);
//    	String filename = o1.toString();
//    	File file = new File(filename);
//    	if (file.isFile()) {
//    		//Bug: Not filtering for share file intent
//    		Intent sendIntent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));
//    		sendIntent.setType("text/plain");
//    		sendIntent.putExtra(Intent.EXTRA_SUBJECT, file.getName());
//        	sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
//        	startActivity(Intent.createChooser(sendIntent, "Share file  "+file.getName()));
//    	} else {
//    		Toast.makeText(getApplicationContext(), "Not a file", Toast.LENGTH_SHORT).show();
//    	}
    }

    protected boolean setOpenResult(File file) {
        Log.d(TAG, "setOpenResult");
        Intent result;

        if (!file.canRead()) {
            Toast.makeText(this, R.string.toast_file_cant_read, Toast.LENGTH_SHORT).show();

            //Crouton.showText(this, R.string.toast_file_cant_read, Style.ALERT);
            return false;
        }

        result = new Intent();
        result.putExtra("path", file.getAbsolutePath());

        setResult(RESULT_OK, result);
        return true;
    }

    public void onInputClicked(View v) {

    }

    public void onForward(View v) {
        if (prevDir.size() == 0) {
            Toast.makeText(this, R.string.cannot_foward, Toast.LENGTH_SHORT).show();

        } else {
            String xx = prevDir.pop();
            if (xx.equals("..")) {
                curArtistDir.pop();
                xx = "";
            } else {
                if (xx.lastIndexOf("/") + 1 < xx.length()) {
                    xx = xx.substring(xx.lastIndexOf("/") + 1);
                } else {
                    xx = "";
                }
            }
            myloadContent(xx, 0);

        }
    }

    @SuppressWarnings("deprecation")
    public void doNewDir(View v) {
        // TODO: 2017-05-11
        new EnterDialog(this)
                .setTitle("新建文件夹")
                .setHint("文件夹名称")
                .setConfirmListener(new EnterDialog.ClickListener() {
                    @Override
                    public boolean OnClickListener(String name) {
                        if (name.equals("")) {
                            Crouton.makeText(TedLocalActivity.this, "文件夹名称不能为空", Style.ALERT).show();
                            return false;
                        }
                        File dirN = new File(curArtistDir.peek(), name);
                        if (dirN.exists()) {
                            Crouton.makeText(TedLocalActivity.this, "文件夹已存在", Style.ALERT).show();
                            return false;
                        } else {
                            dirN.mkdirs();
                            myloadContent(name, 0);
                            return true;
                        }
                    }
                })
                .show();
    }

    @SuppressWarnings("deprecation")
    public void doSave(String fn) {
        if (fn.length() == 0) {
            Toast.makeText(getApplicationContext(), R.string.toast_filename_empty, Toast.LENGTH_SHORT).show();
        } else {
            String filename = curArtistDir.peek() + "/" + fn;
            final File f = new File(filename);
            if (f.exists()) {
                Toast.makeText(this, R.string.file_exist_hint, Toast.LENGTH_SHORT).show();
            } else {
                setSaveResult(f.getAbsolutePath());
            }
        }
    }

    protected boolean setSaveResult(String filepath) {
        Intent result;

        File f = new File(filepath);
        if (f.getParentFile().canWrite()) {
            result = new Intent();
            result.putExtra("path", filepath);

            setResult(RESULT_OK, result);
            finish();
        } else {
            Toast.makeText(getApplicationContext(), R.string.toast_folder_cant_write, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    public void onUp(View v) {
        onTop();
    }

    @Override
    public boolean onKeyUp(int keyCoder, KeyEvent event) {
        if (keyCoder == KeyEvent.KEYCODE_BACK) {
            if (curArtistDir.size() > _GLOBAL_DEPTH) {
                onTop();
                return true;
            } else {
                finish();
            }
        }
        return super.onKeyDown(keyCoder, event);

    }

    /**
     * public void cloneRepository() throws IOException, InvalidRemoteException, TransportException, GitAPIException{
     * final Context context = this;
     * LayoutInflater li = LayoutInflater.from(context);
     * View promptsView = li.inflate(R.layout.repo_pick, null);
     * <p>
     * AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
     * context);
     * <p>
     * // set prompts.xml to alertdialog builder
     * alertDialogBuilder.setView(promptsView);
     * <p>
     * final EditText mRepoUrl = (EditText) promptsView.findViewById(R.id.repo_url);
     * final EditText mSaveRepoAs = (EditText) promptsView.findViewById(R.id.save_repo_as);
     * <p>
     * // set dialog message
     * alertDialogBuilder
     * .setCancelable(false)
     * .setPositiveButton("OK",
     * new DialogInterface.OnClickListener() {
     * public void onClick(DialogInterface dialog,int id) {
     * try {
     * String repo = mRepoUrl.getText().toString();
     * String repoName = mSaveRepoAs.getText().toString();
     * <p>
     * if(repo != null && repoName != null){
     * File localPath = new File(getCurrentDir()+"/"+repoName);
     * localPath.mkdir();
     * <p>
     * System.out.println("Cloning from " + repo + " to " + localPath);
     * Git.cloneRepository()
     * .setURI(repo)
     * .setDirectory(localPath)
     * .call();
     * <p>
     * // now open the created repository
     * FileRepositoryBuilder builder = new FileRepositoryBuilder();
     * Repository repository = builder.setGitDir(localPath)
     * .readEnvironment() // scan environment GIT_* variables
     * .findGitDir() // scan up the file system tree
     * .build();
     * <p>
     * System.out.println("Having repository: " + repository.getDirectory());
     * <p>
     * repository.close();
     * }
     * <p>
     * <p>
     * } catch (IOException e) {
     * // TODO Auto-generated catch block
     * e.printStackTrace();
     * } catch (InvalidRemoteException e) {
     * // TODO Auto-generated catch block
     * e.printStackTrace();
     * } catch (TransportException e) {
     * // TODO Auto-generated catch block
     * e.printStackTrace();
     * } catch (GitAPIException e) {
     * // TODO Auto-generated catch block
     * e.printStackTrace();
     * }
     * }
     * })
     * .setNegativeButton("Cancel",
     * new DialogInterface.OnClickListener() {
     * public void onClick(DialogInterface dialog,int id) {
     * dialog.cancel();
     * }
     * });
     * <p>
     * // create alert dialog
     * AlertDialog alertDialog = alertDialogBuilder.create();
     * <p>
     * // show it
     * alertDialog.show();
     * <p>
     * }
     * <p>
     * public void cloneRepo(View v) throws InvalidRemoteException, TransportException, IOException, GitAPIException{
     * //Toast.makeText(this, getCurrentDir(), Toast.LENGTH_SHORT).show();
     * cloneRepository();
     * }
     */

    private void initListener() {

        binding.ivNewFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doNewDir(v);
            }
        });

    }

    private void initSaveListener() {
        saveBinding.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSave(saveBinding.etName.getText().toString());
            }
        });
        saveBinding.etName.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_RIGHT = 2;

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (v.getRight() - ((EditText) v).getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        ((EditText) v).setText("");
                        return true;
                    }
                }
                return false;
            }
        });
    }
}
