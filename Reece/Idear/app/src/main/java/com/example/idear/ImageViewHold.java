package com.example.idear;

import android.media.Image;

public class ImageViewHold {
    private Image image;
    private String name;
    private String wordCount;

    //default constructor
    public ImageViewHold(){
        this(null, "." ,"0");


    }

    //parameterized constructor
    public ImageViewHold(Image img, String n, String wc){
        image = img;
        name = n;
        wordCount = wc;
    }

    public Image getImage() {
        return image; }
    public void setImage(Image img) {
        image = img; }

    public String getName() {
        return name; }
    public void setName(String s) {
        name = s; }

    public String getWordCount() {
        return wordCount; }
    public void setWordCount(String s) {
        wordCount = s; }

    public String toString() {
        return image + "\n" + name + "\n" + wordCount + "\n";
    }

}
