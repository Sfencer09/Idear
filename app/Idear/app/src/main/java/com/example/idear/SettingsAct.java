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

import java.text.DecimalFormat;
import java.util.Locale;
import android.speech.tts.Voice;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsAct extends AppCompatActivity {

    private double vSpeed = 1;
    private double speed;
    private boolean colorBlindMode = false;
    private boolean DownloadAudio = false;
    private TextToSpeech textToSpeech;

    public void increaseSpeed() {//This increases vSpeed by .1 to speed up the text2speech function
        vSpeed = Double.valueOf(new DecimalFormat("#.##").format(vSpeed + 0.1));
        textToSpeech.setSpeechRate((float)vSpeed);
    }


    public void decreaseSpeed() {//Decreases vSpeed by .1 while vSpeed is >= 0.1 Text to speech can't read below 0.
        if (vSpeed > 0.1) {
            vSpeed = Double.valueOf(new DecimalFormat("#.##").format(vSpeed - 0.1));
            textToSpeech.setSpeechRate((float)vSpeed);
        }
    }

    //Initallizes the text2speech option
    public void init_tts(){
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {//Checks it see if the local language is supported
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
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {// Executes when screen is loaded in
        super.onCreate(savedInstanceState);
        Log.i("SettingAct", "Entering Settings");
        setContentView(R.layout.activity_settings);

        //Created buttons to to transition between activities.
        Button capture = findViewById(R.id.CaptureBtn);
        Button Library = findViewById(R.id.LibraryBtn);
        Button testRead = findViewById(R.id.testReadBtn);

        //Create buttons to adjust the speed of the text2speech reader
        Button buttonUp = findViewById(R.id.buttonUp);
        Button buttonDown = findViewById(R.id.buttonDown);



        //Create radio button group
        RadioGroup RB = findViewById(R.id.VoiceOptionRB);

        //Setting up for level 2 requirements, these would have been worked on if we had more time.
        Switch colorBindMode = findViewById(R.id.colorSwitch);
        Switch downloadAudio = findViewById(R.id.switchDownloadAudio);

        //Text2Speech
        init_tts();


        //Transfers user to the Camera Activity/Page
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent load = new Intent(SettingsAct.this, ImagePreview.class);

                startActivity(load);
            }
        });

        //Transfers user to the Library Activity/Page
        Library.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent load = new Intent(SettingsAct.this, Library.class);
                startActivity(load);
            }
        });



        //This calls increaseSpeed and then sets the SpeedTextView accordingly
        buttonUp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i("SettingAct", "Before Up: " + vSpeed);
                //Toast.makeText(SettingsAct.this, "Voice =" + checkedId, Toast.LENGTH_SHORT).show();

                increaseSpeed();
                TextView tvSpeed = findViewById(R.id.textSpeedTextView);
                tvSpeed.setText("" + vSpeed + "");
                Log.i("SettingAct", "OnCheckChange: " + vSpeed);
                Settings.getInstance().setSpeed(vSpeed);
            }
        });

        //This calls decreaseSpeed and then sets the SpeedTextView accordingly
        buttonDown.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i("SettingAct", "Before Down: " + vSpeed);
                //Toast.makeText(SettingsAct.this, "Voice =" + checkedId, Toast.LENGTH_SHORT).show();

               decreaseSpeed();
                TextView tvDown = findViewById(R.id.textSpeedTextView);
                tvDown.setText("" + vSpeed + "");
                Log.i("SettingAct", "Check Speed: " + vSpeed);
                Settings.getInstance().setSpeed(vSpeed);
            }
        });


        //This will read a preset string when clicked
        testRead.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Settings s =Settings.getInstance();

                double settingVoiceSpeed= s.getSpeed();
                textToSpeech.setSpeechRate((float) settingVoiceSpeed);
                int speechStatus = textToSpeech.speak("This is a test of Idear's speech system. 1, 2, 3, 4", TextToSpeech.QUEUE_FLUSH, null);
                if (speechStatus == TextToSpeech.ERROR) {
                    Toast.makeText(SettingsAct.this, "Text2Speech ERROR", Toast.LENGTH_SHORT).show();
                }
            }
        });

        RB.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {//When a RB is selected it will set the text2speech voice option globely, and un-select the previos option
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.i("SettingAct", "OnCheckChange: " + checkedId);
                RadioButton checkedRadioButton = (RadioButton) group.findViewById(checkedId);
                boolean isChecked = checkedRadioButton.isChecked();
                Settings s = Settings.getInstance();
                if (isChecked) {

                    //If radio button 1 is selected, set text to speech voice
                    if (checkedId == 2131230737) {//English, United States, Female
                        Voice v = new Voice("en-us-x-sfg-local", new Locale("en", "US"), 400, 200, true, null);
                        textToSpeech.setVoice(v);
                        s.setVoiceOption(0);

                    // If radio button 2 is selected, set text to speech voice
                    } else if (checkedId == 2131230738) { //English, India, Female
                        Voice v = new Voice("en-in-x-cxx-local", new Locale("en", "US"), 400, 200, true, null);
                        textToSpeech.setVoice(v);
                        s.setVoiceOption(1);

                    // If radio button 3 is selected, set text to speech voice
                    } else if (checkedId == 2131230739) { //English, Great Britain, Male
                        Voice v = new Voice("en-gb-x-gbd-local", new Locale("en", "gb"), 400, 200, true, null);
                         textToSpeech.setVoice(v);
                        s.setVoiceOption(2);
                    }
                    //Keep this line to test what the radio button ID  for when it is being pressed
                    //Toast.makeText(SettingsAct.this, "Voice =" + checkedId, Toast.LENGTH_SHORT).show();
                }
            }
        });

        //This sets the radio button based on what is previously setup.
        Settings s =Settings.getInstance();
        if(s.getVoiceOption()==0){
            int rbID = 2131230737;
            RB.check(rbID);
        }
        else if(s.getVoiceOption()==1){
            int rbID = 2131230738;
            RB.check(rbID);
        }
        else if(s.getVoiceOption()==2){
            int rbID = 2131230739;
            RB.check(rbID);
        }


        //This sets the Speed & TextView for Speed based off of the Settings.java global variables
        double settingVoiceSpeed= s.getSpeed();
        TextView tvDown = findViewById(R.id.textSpeedTextView);
        tvDown.setText("" + settingVoiceSpeed + "");
        vSpeed = settingVoiceSpeed;

    }

}