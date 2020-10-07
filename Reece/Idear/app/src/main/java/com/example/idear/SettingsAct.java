package com.example.idear;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsAct extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button capture = findViewById(R.id.CaptureBtn);
        Button Library = findViewById(R.id.LibraryBtn);
        RadioGroup Voice = findViewById(R.id.VoiceOptionRB);
        Log.i("Cap ", "" +capture);
        Log.i("Lib ", ""+ Library);

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
                Toast.makeText(SettingsAct.this, "Sorry, not yet!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
