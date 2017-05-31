package com.quseit.texteditor.ui.adapter.bean;

import android.view.View;

/**
 * A bean class that describe popup item
 * Created by Hmei on 2017-05-31.
 */

public class PopupItemBean {
    private String               title;
    private View.OnClickListener clickListener;

    public PopupItemBean(String title, View.OnClickListener clickListener) {
        this.title = title;
        this.clickListener = clickListener;
    }

    public View.OnClickListener getClickListener() {
        return clickListener;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return title;
    }
}
