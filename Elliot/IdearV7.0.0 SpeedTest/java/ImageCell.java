package com.example.idear;

public class ImageCell{
    private String ImageFilePath;
    private String Date;
    private int WordLength;

    public ImageCell(String path, String d, int wL){
        ImageFilePath= path;
        Date = d;
        WordLength= wL;
    }

    public String getImageFilePath() {
        return ImageFilePath;
    }

    public String getDate() {
        return Date;
    }

    public int getWordLength() {
        return WordLength;
    }
}
