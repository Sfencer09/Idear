package com.example.idear;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;
import android.util.Log;
import android.widget.TextView;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsAct extends AppCompatActivity {

    private double vSpeed = 5.0;
    private boolean colorBlindMode = false;
    private boolean DownloadAudio = false;
    private TextToSpeech textToSpeech;

    public void increaseSpeed(){
        vSpeed = Math.floor(vSpeed + 1);
    }

    public void decreaseSpeed(){
        vSpeed = Math.floor(vSpeed - 1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button capture = findViewById(R.id.CaptureBtn);
        Button Library = findViewById(R.id.LibraryBtn);
        Button testRead = findViewById(R.id.testReadBtn);
        RadioGroup Voice = findViewById(R.id.VoiceOptionRB);
        Log.i("Cap ", "" +capture);
        Log.i("Lib ", ""+ Library);

        Button buttonUp  = findViewById(R.id.buttonUp);
        Button buttonDown = findViewById(R.id.buttonDown);

        Switch colorBindMode = findViewById(R.id.colorSwitch);
        Switch downloadAudio = findViewById(R.id.switchDownloadAudio);


        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent load = new Intent(SettingsAct.this, MainActivity.class);

                startActivity(load);
            }
        });

        Voice.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton checkedRadioButton = (RadioButton)group.findViewById(checkedId);
                boolean isChecked = checkedRadioButton.isChecked();
                if (isChecked){
                    Toast.makeText(SettingsAct.this, "Voice =" + checkedId, Toast.LENGTH_SHORT).show();
                }
            }
        });

        Library.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent load = new Intent( SettingsAct.this, Library.class);
                startActivity(load);
            }
        });

        buttonUp.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                increaseSpeed();
                TextView tvSpeed = findViewById(R.id.textSpeedTextView);
                tvSpeed.setText("" + vSpeed +"");

            }

        });

        buttonDown.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                decreaseSpeed();
                TextView tvDown = findViewById(R.id.textSpeedTextView);
                tvDown.setText("" + vSpeed +"");
            }

        });





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
                    Toast.makeText(getApplicationContext(), "TTS Initialization failed!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        testRead.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                int speechStatus =textToSpeech.speak("This is a test. 1, 2, 3, 4", TextToSpeech.QUEUE_FLUSH,null);
                if(speechStatus== TextToSpeech.ERROR){
                    Toast.makeText(SettingsAct.this, "Text2Speech ERROR", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

}