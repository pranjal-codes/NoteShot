package com.example.noteshot;

public class FolderModel {
    private final String folderName;
    private final String timestamp;

    public FolderModel(String description, long l) {
        this.folderName = description;
        this.timestamp = String.valueOf(l);
    }

    public String getFolderName() {
        return folderName;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
