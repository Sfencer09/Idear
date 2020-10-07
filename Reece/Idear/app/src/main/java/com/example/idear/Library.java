package com.example.idear;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.util.Log;

public class Library extends AppCompatActivity {

    public int numImages = 2;
    private LinearLayout lin;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        Button capture = findViewById(R.id.mainMenu);
        Button settings = findViewById(R.id.settingMenu);

        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent load = new Intent(Library.this, MainActivity.class);
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

        for (int i = 0; i < numImages; i++) {
            //String temp = getResources().getString(R.)
                    //< CardView toInsert
        }
    }
}