package com.example.noteshot;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class ImageModel {
    String imageName;
    String imageDatetime;
    String imageUri;

    ImageModel(String imageUri, Timestamp timestamp) {
        this.imageUri = imageUri;
        SimpleDateFormat fileNameFormat = new SimpleDateFormat("ddMMyyyy_HHmmss");
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy, HH:mm");
        this.imageName = "IMG_" + fileNameFormat.format(timestamp) + ".jpg";
        imageDatetime = dateTimeFormat.format(timestamp);
    }

    public String getImageName() {
        return imageName;
    }

    public String getImageDatetime() {
        return imageDatetime;
    }

    public String getImageUri() {
        return imageUri;
    }
}
