package com.example.idear;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;
import android.graphics.Bitmap;
import android.content.ContentProvider;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    //Instantiate the buttons and textfields
    private Button login;

    private EditText emailText;
    private EditText passwordText;
    private EditText passwordText1;

    //All the strings that are processed by the textfield
    private String email;
    private String password;
    private String password1;

    //When the app is started...
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set the current layout to the login screen
        setContentView(R.layout.activity_main);

        //Assign all the text fields
        emailText = (EditText) findViewById(R.id.editTextEmail);
        passwordText = (EditText) findViewById(R.id.editTextPassword);
        passwordText1 = (EditText) findViewById(R.id.editTextPassword2);

        //Assign the button
        login = (Button) findViewById(R.id.submitButton);

        //Set up a listener for the button
        login.setOnClickListener(new View.OnClickListener() {
            //On button press...
            @Override
            public void onClick(View v) {
                //Get the text from all the textfields
                email = emailText.getText().toString();
                password = passwordText.getText().toString();
                password1 = passwordText1.getText().toString();

                //A bunch of rudimentary tests of valid password and email checks.
                if(email.isEmpty() && password.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Please enter your account information!", Toast.LENGTH_SHORT).show();
                }
                else if(email.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Please enter your email address.", Toast.LENGTH_SHORT).show();
                }
                else if(password.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Please enter your password.", Toast.LENGTH_SHORT).show();
                }
                else if(!(password.equals(password1))){
                    Toast.makeText(getApplicationContext(), "Please make sure your passwords match!", Toast.LENGTH_SHORT).show();
                }
                else if(!(email.contains("@")&&(email.contains(".com")||email.contains(".net")||email.contains(".org")||email.contains(".gov")||email.contains(".edu")))){
                    Toast.makeText(getApplicationContext(), "Invalid email entered.", Toast.LENGTH_SHORT).show();
                }
                else if(!(email.isEmpty()&&password.isEmpty()))
                {
                    //If all is okay, move forward
                    Log.i("MyApp1", "1");
                    Intent load = new Intent(MainActivity.this, ImagePreview.class);
                    Log.i("MyApp2", "2");
                    startActivity(load);
                    Log.i("MyApp3", "3");
                }
            }
        });
        /*
        Looper.prepare();
        Looper l = Looper.getMainLooper();

        Handler h = Handler.createAsync(l, new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                Bundle b = msg.getData();
                String type = b.getString("type");
                if(type.equals("loginStatus")){

                    return true;
                }
            }
        })
        */
    }
}