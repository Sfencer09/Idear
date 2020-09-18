package com.example.idear;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button capture;
    private Button settings;
    private Button library;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        capture = (Button) findViewById(R.id.CaptureBtn);
        settings = (Button) findViewById(R.id.SettingBtn);
        library = (Button) findViewById(R.id.LibraryBtn);


        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(MainActivity.this, "Click, Sending Picture", Toast.LENGTH_SHORT).show();
            }
        });

        library.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent load= new Intent(MainActivity.this, Library.class);
                startActivity(load);
            }
        });

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent load= new Intent(MainActivity.this, SettingsAct.class);
                startActivity(load);


            }
        });
    }
}