package com.quseit.texteditor.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.quseit.texteditor.R;
import com.quseit.texteditor.ui.adapter.bean.FolderBean;

import java.util.List;

/**
 * A File List Adapter
 *
 * @author Hmei
 */
public class PathListAdapter extends BaseAdapter {
    private List<FolderBean> dataList;

    public PathListAdapter(List<FolderBean> dataList) {
        this.dataList = dataList;
    }

    @Override
    public int getCount() {
        return dataList.size();
    }

    @Override
    public FolderBean getItem(int position) {
        return dataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder, null);
            holder.setName((TextView) convertView.findViewById(R.id.tv_file_name));
            holder.setIcon((ImageView) convertView.findViewById(R.id.iv_file_icon));
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.getName().setText(getItem(position).getName());
        switch (getItem(position).getType()) {
            case FILE:
                holder.getIcon().setImageResource(R.drawable.ic_editor_pyfile);
                break;
            case FOLDER:
                holder.getIcon().setImageResource(R.drawable.ic_editor_folder);
                break;
        }
        return convertView;
    }

    private static class ViewHolder {
        private ImageView icon;
        private TextView  name;

        public ImageView getIcon() {
            return icon;
        }

        public void setIcon(ImageView icon) {
            this.icon = icon;
        }

        public TextView getName() {
            return name;
        }

        public void setName(TextView name) {
            this.name = name;
        }
    }
}
