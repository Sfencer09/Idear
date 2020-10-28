package com.example.idear;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;
import android.util.Log;
import java.util.Locale;
import android.speech.tts.Voice;

public class Settings {
    private static Settings instance = new Settings();

    private double speed;
    private int voiceOption;
    private String text;
    //private TextToSpeech textToSpeech;

    private Settings() {
        speed = 1;
        voiceOption = 0;
        //init_tts();
    }

    public static Settings getInstance() {
        return instance;
    }
    public void setText(String s){
        text = s;
    }
    public String getText(){
        return text;
    }
    public double getSpeed() {
        return speed;
    }
    public int voiceOption() {
        return voiceOption;
    }
    public void setSpeed(double sp) {
        Log.i("Settings", "Speed: " + speed);
        speed = sp;
        // textToSpeech.setSpeechRate((float)speed);


    }

}

// https://stackoverflow.com/questions/9815245/android-text-to-speech-male-voice
// https://developer.android.com/reference/android/speech/tts/Voice.html#getFeatures()


// a. we need to find a way to instantiate TextToSpeech without using getApplicationContext()
//https://stackoverflow.com/questions/12805491/android-tts-from-multiple-activities
//OR
//b. instantiate the Settings TextToSpeech object as null in the Settings constructor, then create a new TextToSpeech object on every Activity that we wish to use it on.