package com.quseit.texteditor.ui.view;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;


import com.quseit.texteditor.R;
import com.quseit.texteditor.databinding.PopupAddEditorBinding;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

/**
 * {@link com.quseit.texteditor.TedActivity}
 * Show when click add editor btn
 * Created by Hmei on 2017-05-18.
 */

public class NewEditorPopUp {
    private PopupAddEditorBinding binding;
    private PopupWindow           popupWindow;

    public NewEditorPopUp(Context context) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.popup_add_editor, null, false);
        popupWindow = new PopupWindow(binding.getRoot(), ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
        popupWindow.setOutsideTouchable(true);
    }

    public void initListener(final AddClick clickListener) {
        binding.tvBlankFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.blankFile();
                popupWindow.dismiss();
            }
        });
        binding.tvScript.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.newScript();
                popupWindow.dismiss();
            }
        });
        binding.tvWebApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.webApp();
                popupWindow.dismiss();
            }
        });
        binding.tvConsoleApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.consoleApp();
                popupWindow.dismiss();
            }
        });
        binding.tvKivyApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.kivyApp();
                popupWindow.dismiss();
            }
        });
    }

    public void show(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            popupWindow.showAsDropDown(view, 0, 0, Gravity.RIGHT);
        } else {
            popupWindow.showAtLocation(view, Gravity.RIGHT, 0, 0);
        }
    }

    public interface AddClick {
        void blankFile();

        void newScript();

        void webApp();

        void consoleApp();

        void kivyApp();
    }

}
