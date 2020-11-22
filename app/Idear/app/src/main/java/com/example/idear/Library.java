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
    private long time;

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
        Button test = findViewById(R.id.LibPlayBTN);

        init_tts();

        ImageView libraryView = findViewById(R.id.libraryView);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if(extras != null){
            time = intent.getLongExtra("time", 0);
            String address = intent.getStringExtra("address");
            Bitmap image = BitmapFactory.decodeFile(address);
            libraryView.setImageBitmap(image);
            //text = intent.getStringExtra("text");
        }
        //Transfers user to the Camera Activity/Page
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent load = new Intent(Library.this, ImagePreview.class);
                startActivity(load);
            }
        });
        //Transfers user to the Settings Activity/Page
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent load = new Intent(Library.this, SettingsAct.class);
                startActivity(load);
            }
        });

        //Reads what is coming from the server along along with with the global settings such as voice and speed implemented
        test.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //voiceOption brings in the chosen voice from settings
                int voiceOption = (Settings.getInstance().getVoiceOption());
                //voiceSpeed brings in the chosen speed from settings
                double voiceSpeed = (Settings.getInstance().getSpeed());
                //text is the string that is brought in from the network transferred into setting and then brought in here to read
                String text = (Settings.getInstance().getText());
                //passedTime is how to calculate latency between phone to server to phon
                double passedTime = ((Settings.getInstance().getTime()) - time) / 1000.0;
                Log.d("CREATE", "Processing took " + passedTime + " seconds.");
                //Toast.makeText(getApplicationContext(), "Processing time: " + passedTime + " seconds", Toast.LENGTH_LONG).show();
                if (voiceOption == 0) {//English, United States, Female
                    Voice v1 = new Voice("en-us-x-sfg-local", new Locale("en", "US"), 400, 200, true, null);
                    textToSpeech.setVoice(v1);
                    //Toast.makeText(getApplicationContext(), "Radio ID: " + , Toast.LENGTH_SHORT).show();
                } else if (voiceOption == 1) {//English, India, Female
                    Voice v1 = new Voice("en-in-x-cxx-local", new Locale("en", "US"), 400, 200, true, null);
                    textToSpeech.setVoice(v1);
                } else if (voiceOption == 2) { //English, Great Britain, Male
                    Voice v1 = new Voice("en-gb-x-gbd-local", new Locale("en", "gb"), 400, 200, true, null);
                    textToSpeech.setVoice(v1);
                }
                textToSpeech.setSpeechRate((float)voiceSpeed);
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                int speechStatus = textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                if (speechStatus == TextToSpeech.ERROR) {
                }
            }
        });


    }
}