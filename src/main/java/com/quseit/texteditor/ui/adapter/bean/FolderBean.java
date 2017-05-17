package com.quseit.texteditor.ui.adapter.bean;

import com.quseit.texteditor.common.CommonEnums.FileType;

import java.io.File;

/**
 * To store path list's item data
 * Created by Hmei on 2017-05-11.
 */

public class FolderBean {
    private File     file;
    private FileType type;
    private String   name;
    private String   path;

    public FolderBean(File file) {
        this.file = file;
        type = file.isDirectory() ? FileType.FOLDER : FileType.FILE;
        name = file.getName();
        path = file.getAbsolutePath();
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

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
}
