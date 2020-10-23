package com.example.idear;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.util.Log;
import android.widget.Toast;
import android.util.Log;
import android.widget.TextView;
import android.widget.ListView;
import android.app.Activity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

public class Library extends AppCompatActivity {
    private static final String TAG = "ListViewActivity";

    private ListView listView;
    private TextToSpeech textToSpeech;
    private String readME;

    public void init_tts(){
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int ttsLang = textToSpeech.setLanguage(Locale.US);

                    if (ttsLang == TextToSpeech.LANG_MISSING_DATA
                            || ttsLang == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "The Language is not supported!");
                    } else {
                        Log.i("TTS", "Language Supported.");
                    }
                    Log.i("TTS", "Initialization success.");
                } else {
                    // Toast.makeText(getApplicationContext(), "TTS Initialization failed!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i("Lib", "Entering Lib");
        setContentView(R.layout.activity_library);
        Log.i("Lib", "Before");
        Button capture = findViewById(R.id.HomeBTN);
        Button settings = findViewById(R.id.SettingsBTN);
        Button test = findViewById(R.id.LibTestBTN);

        init_tts();

        ImageView libraryView = findViewById(R.id.libraryView);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if(extras != null){
            String address = intent.getStringExtra("address");
            Bitmap image = BitmapFactory.decodeFile(address);
            libraryView.setImageBitmap(image);
            readME = intent.getStringExtra("text");
        }

        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent load = new Intent(Library.this, ImagePreview.class);
                startActivity(load);
            }
        });

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent load = new Intent(Library.this, SettingsAct.class);
                startActivity(load);
            }
        });


        test.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int voiceOption = (Settings.getInstance().voiceOption());
                double voiceSpeed = (Settings.getInstance().getSpeed());
                if (voiceOption == 0) {
                    Voice v1 = new Voice("en-us-x-sfg#male_1-local", new Locale("en", "US"), 400, 200, true, null);
                    textToSpeech.setVoice(v1);
                    //Toast.makeText(getApplicationContext(), "Radio ID: " + , Toast.LENGTH_SHORT).show();
                } else if (voiceOption == 1) {
                    Voice v1 = new Voice("en-us-x-sfg#female_1-local", new Locale("en", "US"), 400, 200, true, null);
                    textToSpeech.setVoice(v1);
                } else if (voiceOption == 2) { //English- Great Britain
                    Voice v1 = new Voice("en-gb-x-gbd-local", new Locale("en", "gb"), 400, 200, true, null);
                    textToSpeech.setVoice(v1);
                }
                //readME = "Test Test Test";
                textToSpeech.setSpeechRate((float)voiceSpeed);
                Toast.makeText(getApplicationContext(), readME, Toast.LENGTH_SHORT).show();
                int speechStatus = textToSpeech.speak(readME, TextToSpeech.QUEUE_FLUSH, null);
                if (speechStatus == TextToSpeech.ERROR) {
                }
            }
        });
    }
}