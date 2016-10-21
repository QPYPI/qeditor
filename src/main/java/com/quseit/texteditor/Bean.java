package com.quseit.texteditor;

import android.content.Context;

public class Bean{
	protected String title;
    protected Context context;
    //private final String TAG = "BEAN";
 
    public Bean(Context context) {
    	this.context = context;
    }
    public void setTitle(String title){
        this.title = title;
    }


    public String getTitle(){
        return this.title;
    }
}
