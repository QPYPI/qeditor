package com.quseit.texteditor.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.quseit.texteditor.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EditorActivity extends AppCompatActivity {
    private static final String FOR_TEST     = Environment.getExternalStorageDirectory().getPath() + "/qpython/projects/KivySample";
    private static final String PROJECT_PATH = "path";
    private List<File> dataList;

    public static void start(String projectPath, Context context) {
        Intent starter = new Intent(context, EditorActivity.class);
        starter.putExtra(PROJECT_PATH, projectPath);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initFolderList();
    }

    private void initView() {
        setContentView(R.layout.activity_editor);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void initFolderList() {
        dataList = new ArrayList<>();
        dataList.addAll(getChildFiles(FOR_TEST));
    }

    private List<File> getChildFiles(String path) {
        return Arrays.asList(new File(path).listFiles());
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.editor_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
//                if (editorPopUp == null) {
//                    List<PopupItemBean> itemBeanList = new ArrayList<>();
//                    itemBeanList.add(new PopupItemBean(getResources().getString(R.string.empty_file), v15 -> newContent()));
//                    itemBeanList.add(new PopupItemBean(getString(R.string.script), v14 -> newScript()));
//                    itemBeanList.add(new PopupItemBean(getString(R.string.webapp_project), v13 -> newProject(WEB_PROJECT)));
//                    itemBeanList.add(new PopupItemBean(getString(R.string.console_app_project), v12 -> newProject(CONSOLE_PROJECT)));
//                    itemBeanList.add(new PopupItemBean(getString(R.string.kivy_app_project), v1 -> newProject(KIVY_PROJECT)));
//                    editorPopUp = new EditorPopUp(TedActivity.this, itemBeanList);
//                }
//                editorPopUp.show(binding.rlTop);
                break;
            case R.id.menu_open:
//                TedLocalActivity.start(TedActivity.this);
                break;
            case R.id.menu_more:
//                TedSettingsActivity.start(TedActivity.this);
                break;
        }
        return true;
    }
}
