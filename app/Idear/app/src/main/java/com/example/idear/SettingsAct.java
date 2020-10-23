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

    public void increaseSpeed() {

        vSpeed = Double.valueOf(new DecimalFormat("#.##").format(vSpeed + 0.1));

        textToSpeech.setSpeechRate((float)vSpeed);
    }

    public void decreaseSpeed() {
        if (vSpeed > 0.1) {
            vSpeed = Double.valueOf(new DecimalFormat("#.##").format(vSpeed - 0.1));
            textToSpeech.setSpeechRate((float)vSpeed);
        }
    }

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

    public void setVoiceOption(int opt) {
        int voiceOption = 0;
        Log.i("Settings", "Opt: " + opt);
        Log.i("Settings", "VoiceOption: " + voiceOption);
        if (opt >= 0 && opt < 3) {
            voiceOption = opt;
        }
        Voice v = null;
        if (voiceOption == 0) {
            v = new Voice("en-us-x-sfg#male_1-local", new Locale("en", "US"), 400, 200, true, null);
        } else if (voiceOption == 1) {
            v = new Voice("en-us-x-sfg#female_1-local", new Locale("en", "US"), 400, 200, true, null);
        } else if (voiceOption == 2) { //English- Great Britain
            v = new Voice("en-gb-x-gbd-local", new Locale("en", "gb"), 400, 200, true, null);
        }
        textToSpeech.setVoice(v);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("SettingAct", "Entering Settings");
        setContentView(R.layout.activity_settings);

        Button capture = findViewById(R.id.CaptureBtn);
        Button Library = findViewById(R.id.LibraryBtn);
        Button testRead = findViewById(R.id.testReadBtn);
        RadioGroup RB = findViewById(R.id.VoiceOptionRB);
        Log.i("Cap ", "" + capture);
        Log.i("Lib ", "" + Library);

        Button buttonUp = findViewById(R.id.buttonUp);
        Button buttonDown = findViewById(R.id.buttonDown);

        Switch colorBindMode = findViewById(R.id.colorSwitch);
        Switch downloadAudio = findViewById(R.id.switchDownloadAudio);

        init_tts();







        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent load = new Intent(SettingsAct.this, ImagePreview.class);

                startActivity(load);
            }
        });

        RB.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.i("SettingAct", "OnCheckChange: " + checkedId);
                RadioButton checkedRadioButton = (RadioButton) group.findViewById(checkedId);
                boolean isChecked = checkedRadioButton.isChecked();
                if (isChecked) {
                    if (checkedId == 2131165201) {
                        //Voice v = new Voice("en-us-x-sfg#male_1-local", new Locale("en", "US"), 400, 200, true, null);
                        //textToSpeech.setVoice(v);
                        setVoiceOption(0);
                    } else if (checkedId == 2131165202) {
                        //Voice v = new Voice("en-us-x-sfg#female_1-local", new Locale("en", "US"), 400, 200, true, null);
                        //textToSpeech.setVoice(v);
                        setVoiceOption(1);
                    } else if (checkedId == 2131165203) { //English- Great Britain
                        //Voice v = new Voice("en-gb-x-gbd-local", new Locale("en", "gb"), 400, 200, true, null);
                        // textToSpeech.setVoice(v);
                        setVoiceOption(2);

                    }
                    //Toast.makeText(SettingsAct.this, "Voice =" + checkedId, Toast.LENGTH_SHORT).show();
                    //String voices = "";
                    //for (Voice tmpVoice : textToSpeech.getVoices()) {
                    //voices += tmpVoice.getName() + " ";
                    //}
                    //Toast.makeText(SettingsAct.this, " " + voices + "\n", Toast.LENGTH_LONG).show();
                }
            }
        });


        Library.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent load = new Intent(SettingsAct.this, Library.class);
                startActivity(load);
            }
        });

        buttonUp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i("SettingAct", "Before Up: " + vSpeed);
                increaseSpeed();
                TextView tvSpeed = findViewById(R.id.textSpeedTextView);
                tvSpeed.setText("" + vSpeed + "");
                Log.i("SettingAct", "OnCheckChange: " + vSpeed);
                Settings.getInstance().setSpeed(vSpeed);


            }

        });

        buttonDown.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i("SettingAct", "Before Down: " + vSpeed);
                decreaseSpeed();
                TextView tvDown = findViewById(R.id.textSpeedTextView);
                tvDown.setText("" + vSpeed + "");
                Log.i("SettingAct", "Check Speed: " + vSpeed);
                Settings.getInstance().setSpeed(vSpeed);
            }

        });

        testRead.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Settings s =Settings.getInstance();
                //TextToSpeech textToSpeech =s.getTextToSpeech();
                //double settingVoiceSpeed= s.getSpeed();
                // textToSpeech.setSpeechRate((float) settingVoiceSpeed);
                int speechStatus = textToSpeech.speak("This is a speed test of Idear's speech system. 1, 2, 3, 4", TextToSpeech.QUEUE_FLUSH, null);
                if (speechStatus == TextToSpeech.ERROR) {
                    Toast.makeText(SettingsAct.this, "Text2Speech ERROR", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //This sets the radio button based on what is currently setup.
        Settings s =Settings.getInstance();
        if(s.voiceOption()==0){
            int rbID = 2131165201;
            RB.check(rbID);

        }
        else if(s.voiceOption()==1){
            int rbID = 2131165202;
            RB.check(rbID);

        }
        else if(s.voiceOption()==2){
            int rbID = 2131165203;
            RB.check(rbID);
        }
        //This sets the TextView based off of the settings.
        double settingVoiceSpeed= s.getSpeed();
        TextView tvDown = findViewById(R.id.textSpeedTextView);
        tvDown.setText("" + settingVoiceSpeed + "");
        vSpeed = settingVoiceSpeed;

    }

}