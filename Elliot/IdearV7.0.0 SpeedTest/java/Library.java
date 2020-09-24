package com.example.idear;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;
import android.widget.Toast;
import android.util.Log;
import android.widget.TextView;
import android.widget.ListView;
import android.app.Activity;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class Library extends AppCompatActivity {
    private static final String TAG = "ListViewActivity";

    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        Button capture = findViewById(R.id.HomeBTN);
        Button settings = findViewById(R.id.SettingsBTN);

        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                Intent load = new Intent( Library.this, MainActivity.class);
                startActivity(load);
            }
        });

        settings.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent load = new Intent( Library.this, SettingsAct.class);
                startActivity(load);
            }
        });






    }


}