package com.quseit.texteditor.ui.adapter.bean;

import com.quseit.texteditor.common.CommonEnums.FileType;

/**
 * Created by Hmei on 2017-05-11.
 */

public class FolderBean  {
    private FileType type;
    private String name;
    private String path;

    public FolderBean(FileType type, String name, String path) {
        this.type = type;
        this.name = name;
        this.path = path;
    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
