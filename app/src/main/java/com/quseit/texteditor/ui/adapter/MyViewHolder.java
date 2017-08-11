package com.quseit.texteditor.ui.adapter;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class MyViewHolder<T extends ViewDataBinding> extends RecyclerView.ViewHolder {
    private T binding;
    public MyViewHolder(View itemView) {
        super(itemView);
        binding = DataBindingUtil.bind(itemView);
    }

    public T getBinding() {
        return binding;
    }
}
