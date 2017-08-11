package com.quseit.texteditor.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.quseit.texteditor.R;
import com.quseit.texteditor.databinding.ItemFolderBinding;

import java.io.File;
import java.util.List;

public class SlideFolederAdapter extends RecyclerView.Adapter<MyViewHolder<ItemFolderBinding>> {
    private List<File> dataList;

    public SlideFolederAdapter(List<File> dataList) {
        this.dataList = dataList;
    }

    @Override
    public MyViewHolder<ItemFolderBinding> onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyViewHolder<>(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder, parent));
    }

    @Override
    public void onBindViewHolder(MyViewHolder<ItemFolderBinding> holder, int position) {
        ItemFolderBinding binding = holder.getBinding();
    }

    @Override
    public int getItemCount() {
        return dataList == null ? 0 : dataList.size();
    }
}
