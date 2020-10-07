package com.example.idear;

public class Photo {
    private static final String TAG = "Photo";

    private int photoImg;
    private String photoDateStr;
    private String wordLengthStr;

    public Photo(int photoImg, String photoDateStr,String wordLengthStr) {
        super();
        this.setPhotoImg(photoImg);
        this.setPhotoDateStr(photoDateStr);
        this.setWordLengthStr(wordLengthStr);
    }

    public String getPhotoDateStr() {
        return photoDateStr;
    }

    public void setPhotoDateStr(String photoDateStr) {
        this.photoDateStr = photoDateStr;
    }

    public int getPhotoImg() {
        return photoImg;
    }

    public void setPhotoImg(int photoImg) {
        this.photoImg = photoImg;
    }

    public String getWordLengthStr() {
        return wordLengthStr;
    }

    public void setWordLengthStr(String wordLengthStr) {
        this.wordLengthStr = wordLengthStr;
    }
}