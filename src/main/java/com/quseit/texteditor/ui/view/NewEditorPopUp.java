package com.quseit.texteditor.ui.view;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.quseit.texteditor.R;
import com.quseit.texteditor.ui.adapter.bean.PopupItemBean;

import java.util.List;

/**
 * Show
 * Created by Hmei on 2017-05-18.
 */

public class NewEditorPopUp {
    private PopupWindow popupWindow;

    public NewEditorPopUp(Context context, final List<PopupItemBean> itemBeanList) {
        FrameLayout root = (FrameLayout) LayoutInflater.from(context).inflate(R.layout.popup_add_editor, null);
        ListView listView = (ListView) root.findViewById(R.id.list_view);

        ArrayAdapter<PopupItemBean> adapter = new ArrayAdapter<>(context, R.layout.item_pop_up, itemBeanList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                itemBeanList.get(position).getClickListener().onClick(view);
            }
        });

        popupWindow = new PopupWindow(root, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
        popupWindow.setOutsideTouchable(true);
    }

    public void show(View view, int gravity, int x, int y) {
        popupWindow.showAtLocation(view, gravity, x, y);
    }

    public void show(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            popupWindow.showAsDropDown(view, 0, 0, Gravity.RIGHT);
        } else {
            popupWindow.showAtLocation(view, Gravity.RIGHT, 0, 0);
        }
    }

    public void dismiss() {
        popupWindow.dismiss();
    }
}
