package com.example.idear;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

//Camera code is courtesy of Mobile Application Tutorial's Camera2 API tutorial set.
//    https://www.youtube.com/playlist?list=PL9jCwTXYWjDIHNEGtsRdCTk79I9-95TbJ

public class ImagePreview extends AppCompatActivity {

    //Establish the buttons that are used in the app itself
    private Button capture;
    private Button settings;
    private Button library;

    //Textureview that the preview is projected onto
    private TextureView mHolder;

    //Camera codes used for state and permission checks
    private static final int REQUEST_IMAGE_CAPTURE = 0;
    private static final int REQUEST_CAMERA_PERMISSION_CODE = 1;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    //Initialize one off the bat
    private int mCaptureState = STATE_PREVIEW;

    //Helps the computer determine
    int totalRotation;

    //File directory and temporary file, respectively
    private File mPhotoFolder;
    private File imageFile;

    //Just used to keep track of time between button pressed and message received, for testing purposes
    private long time;

    //Keeps track of the size of the image
    private Size mImageSize;
    //Create an imageReader and assign a listener: this is responsible for handling the image once one is created
    private ImageReader mImageReader;
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new
            ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    mBackgroundHandler.post(new imageSaver(reader.acquireNextImage()));
                }
            };
    //Create a Capture Session and an associated callback: this is the springboard of the whole thing
    private CameraCaptureSession mPreviewCaptureSession;
    private CameraCaptureSession.CaptureCallback mPreviewCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        //Once we have a capture result, if we are in the 'wait lock' state, we're ready to proceed with actually capturing an image
        public void process(CaptureResult capture){
            switch(mCaptureState){
                case STATE_PREVIEW:
                    break;
                case STATE_WAIT_LOCK:
                    mCaptureState = STATE_PREVIEW;
                    Integer autoFocusState = capture.get(CaptureResult.CONTROL_AF_STATE);
                    //if(autoFocusState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || autoFocusState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED){
                    Toast.makeText(getApplicationContext(), "Auto Focus is on!", Toast.LENGTH_SHORT).show();
                    startStillCaptureRequest();
                    //}
                    break;
            }
        }
        //Once we've completed a capture request, get the result and process it above
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result){
            super.onCaptureCompleted(session, request, result);
            process(result);
        }
    };
    //This should be run last: this takes the image supplied by the capturesession and saves it to the system
    private class imageSaver implements Runnable{
        //Establish an image
        private final Image mImage;
        //Simple constructor
        public imageSaver(Image image){
            mImage = image;
        }
        //Once we're ready to save...
        @Override
        public void run(){
            //Give the image an infallable name so that we don't have repeats
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + ".png";
            //Get the external file directory, and assign it to currentPhotoPath: this is the directory we created earlier
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), imageFileName);
            currentPhotoPath = file.getAbsolutePath();
            //deconstruct the captured image into bytes
            ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            FileOutputStream fOutPut = null;
            try {
                //Write the bytes out through a fileOutputStream to the filepath from earlier
                fOutPut = new FileOutputStream(currentPhotoPath);
                fOutPut.write(bytes);
            }catch (IOException e){

            } finally {
                //Once done, close the image and the outputStream
                mImage.close();
                if(fOutPut != null){
                    try {
                        fOutPut.close();
                    }catch(IOException e){

                    }
                    //Make sure the image appears in Android's file browser
                    ExportToMedia(getApplicationContext(), imageFile, ".png");

                    try {
                        //send the image over TCP using IdearNetworking, then call the intent to move to the Library
                        IdearNetworking.sendImageTCP(currentPhotoPath, false);
                        Intent intent = new Intent(ImagePreview.this, Library.class);
                        //Include information on this page switch with the path of the file and the starting time
                        intent.putExtra("address", currentPhotoPath);
                        intent.putExtra("time", time);
                        //Finally, move over
                        startActivity(intent);
                    }catch (IOException e){

                    }
                }
            }
        }
    }


    private TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        //If we have an available surface, set the camera up and connect it
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height){
            cameraSetup(width, height);
            connectCamera();
        }
        //You're required to have the rest of these for a complete SurfaceTextureListener, just leave them empty
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height){

        }

        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface){
            return false;
        }

        public void onSurfaceTextureUpdated(SurfaceTexture surface){

        }
    };

    //This is required to have camera functionality at all
    private CameraDevice mCameraD;
    private CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
        //Once the camera is opened, call one of the major functions of this program, startPreview
        @Override
        public void onOpened(CameraDevice camera){
            mCameraD = camera;
            Toast.makeText(getApplicationContext(), "We have a successful connection!", Toast.LENGTH_SHORT).show();
            startPreview();
        }
        //If there's an issue, try to safely close the camera
        @Override
        public void onDisconnected(CameraDevice camera){
            camera.close();
            mCameraD = null;
        }
        //same as above
        @Override
        public void onError(CameraDevice camera, int error){
            camera.close();
            mCameraD = null;
        }
    };

    //Multiple things related to threading
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    //The size for the camera preview
    private Size mPreviewSize;
    private CaptureRequest.Builder mCaptureRequestBuilder;

    private String mCamID;
    //To help determine what angle the camera should be
    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    //This should be self-explanatory. It compares two Size objects
    private static class compareSize implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs){
            return Long.signum(((long)lhs.getWidth() * lhs.getHeight()) / ((long)rhs.getWidth() * rhs.getHeight()));
        }
    }

    //This is the method called at the very start of the progam. Think of it as a constructor.
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        //Establish which layout file this pertains to, and load it
        setContentView(R.layout.activity_camera);

        //Establish our image surface and buttons
        mHolder = (TextureView) findViewById(R.id.textureView);

        capture = (Button) findViewById(R.id.CaptureBtn);
        settings = (Button) findViewById(R.id.SettingBtn);
        library = (Button) findViewById(R.id.LibraryBtn);

        //Create the image directory if it doesn't already exist
        createImageFolder();
        //Boot up the IdearNetworking class to establish a connection
        try {
            IdearNetworking.initialize();
        }catch (IOException e){

        }
        //For now, our login information is hard-coded, since we don't have a functioning database yet
        byte[] hash = new byte[]{0x12, 0x34, 0x56, 0x78, 0x90-256, 0xAB-256, 0xCD-256, 0xEF-256, 0x12, 0x34, 0x56, 0x78, 0x90-256, 0xAB-256, 0xCD-256, 0xEF-256, 0x12, 0x34, 0x56, 0x78, 0x90-256, 0xAB-256, 0xCD-256, 0xEF-256, 0x12, 0x34, 0x56, 0x78, 0x90-256, 0xAB-256, 0xCD-256, 0xEF-256};
        String pass = hash.toString();
        IdearNetworking.login("test@gmail.com", pass);

        //This is the camera button's functionality. If detect a click, call lockFocus to start the entire process of taking a picture
        // also log what time we started taking a picture on
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                time = System.currentTimeMillis();
                lockFocus();
            }
        });

        //This is the library button. If clicked, move over to the library.
        library.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("MyApp1","1");
                Intent load= new Intent(ImagePreview.this, Library.class);
                Log.i("MyApp2","2");
                startActivity(load);
                Log.i("MyApp3","3");

            }
        });

        //this is the settings button. If clicked, move over to the settings menu.
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("MyApp1","1");
                Intent load= new Intent(ImagePreview.this, SettingsAct.class);
                Log.i("MyApp2","2");
                startActivity(load);
                Log.i("MyApp3","3");

            }
        });
    }

    //@Override
    //protected void onActivityResult(int requestCode, int resultCode, Intent data){
    //if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK ) {
    //super.onActivityResult(requestCode, resultCode, data);
    //Bundle extras = data.getExtras();
    //image = (Bitmap) extras.get("data");
    //}
    //}

    //Will create an image directory for images to go to if it doesn't exist already
    private void createImageFolder(){
        //if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
        File toImageFile = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        mPhotoFolder = new File(toImageFile, "Idear");
        if (!mPhotoFolder.exists()) {
            mPhotoFolder.mkdirs();
        }
        //}
    }

    //This string is INCREDIBLY important, as it internally keeps track of the file directory
    String currentPhotoPath;

    //This code creates a temporary file that's ready to receive information from the reader. Most of this code
    // was repurposed up in imageSaver to act independently, so if we had time to mess about with the code,
    // it could be safely assumed that this method is obsolete.
    private File createImageFile() throws IOException {
        File mimageFile = null;
        //if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        mimageFile = File.createTempFile(imageFileName, ".png", mPhotoFolder);
        ExportToMedia(this, mimageFile, ".png");
        currentPhotoPath = mimageFile.getAbsolutePath();
        //}
        return mimageFile;
    }

    //Allows Android's native file explorer to see whatever file we create
    private void ExportToMedia(Context context, File f, String mimeType){
        MediaScannerConnection.scanFile(context, new String[] {f.getAbsolutePath()}, new String[] {mimeType}, null);
    }

    //Another bread-and-butter method. This method configures the camera and all associated services.
    private void cameraSetup(int width, int height){
        //Create a local camera manager (which is self-explanatory)
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            //for all cameras on this device...
            for (String camID : cameraManager.getCameraIdList()) {
                //Get the camera characteristics: which way does it face, etc.
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(camID);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                //This block of code up until the if statement deals with device rotation. Unfortunately, this code is currently nonfunctional.
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                Log.d("CREATE", "Orientation = " + deviceOrientation);
                totalRotation = senseDeviceRotation(cameraCharacteristics, deviceOrientation);
                Log.d("CREATE", "Rotation = " + totalRotation);
                //Invert the orientation of the preview if we're on our side
                boolean swapRotation = (totalRotation == 90 || totalRotation == 270);
                int rotateWidth = width;
                int rotateHeight = height;
                if(swapRotation == true){
                    rotateWidth = height;
                    rotateHeight = width;
                }
                //Establish the size of our preview and the images it will take
                mPreviewSize = chooseSize(map.getOutputSizes(SurfaceTexture.class), rotateWidth, rotateHeight);
                mImageSize = chooseSize(map.getOutputSizes(ImageFormat.JPEG), rotateWidth, rotateHeight);
                //Finally instantiate the ImageReader we declared so long ago with information related to our new camera,
                // and set it up to start listening for images on the background thread
                mImageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG,1);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
                //Instantiate the camera ID with the found camera ID
                mCamID = camID;
                return;
            }
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    //Yet another bread-and-butter method. This method connects the camera to the background thread
    private void connectCamera(){
        //Create a local cameraManager
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            //If the version of Android requires permission..
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                //Make sure the user has allowed permission for the camera
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                    //FINALLY open the camera and attach it to the background thread
                    cameraManager.openCamera(mCamID, mCameraStateCallback, mBackgroundHandler);
                } else {
                    //If not, tell the user we need it, and request permission
                    if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                        Toast.makeText(this, "Please note: Idear requires access to the camera in order to function.", Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_CODE);
                }
            } else {
                //If we don't need permission, just go right on ahead and do it
                cameraManager.openCamera(mCamID, mCameraStateCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //This method starts the live feedback from the camera.
    private void startPreview(){
        //Get the surface we're projecting onto from mHolder.
        SurfaceTexture surfaceTexture = mHolder.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        //Create a new local Surface using the SurfaceTexture we were supplied
        Surface preview = new Surface(surfaceTexture);
        try {
            //Instantiate our captureRequestBuilder with our createCaptureRequest method
            mCaptureRequestBuilder = mCameraD.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //Add the surface we're projecting onto
            mCaptureRequestBuilder.addTarget(preview);
            //Creating a capture session. This involves threading, so it requires callbacks. Our CameraDevice (mCameraD) will be
            // used throughout the program
            mCameraD.createCaptureSession(Arrays.asList(preview, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mPreviewCaptureSession = session;
                    try {
                        mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(getApplicationContext(), "ERROR: Failure establishing capture preview!", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //Takes a picture
    private void startStillCaptureRequest(){
        try {
            //Create a capture request and find the surface we're capturing from
            mCaptureRequestBuilder = mCameraD.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, totalRotation + 90);
            CameraCaptureSession.CaptureCallback stillCaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                    try {
                        //This can likely be removed, but the code is fragile, so for now, it stays
                        imageFile = createImageFile();
                    }catch (IOException e){

                    }
                }
            };

            mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), stillCaptureCallback, null);
        }catch (CameraAccessException e) {

        }
    }

    //If the page is loaded into from another page, start the background thread and resume the preview
    @Override
    protected void onResume(){
        super.onResume();
        startBackgroundThread();

        if(mHolder.isAvailable()){
            cameraSetup(mHolder.getWidth(), mHolder.getHeight());
            connectCamera();
        } else {
            mHolder.setSurfaceTextureListener(mTextureListener);
        }
    }

    //This is just a glorified error handler for if we don't have permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CAMERA_PERMISSION_CODE){
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(getApplicationContext(), "Idear cannot run without camera!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //This method is supposed to automatically focus the camera on a button press, but it is currently nonfunctional.
    private void lockFocus(){
        mCaptureState = STATE_WAIT_LOCK;
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        try {
            mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), mPreviewCaptureCallback, mBackgroundHandler);
        }catch (CameraAccessException e){

        }
    }

    //If we leave the page, call this
    @Override
    protected void onPause(){
        closeCamera();

        stopBackgroundThread();
        super.onPause();
    }

    //Closes the camera device, self-explanatory
    private void closeCamera() {
        if(mCameraD != null) {
            mCameraD.close();
            mCameraD = null;
        }
    }

    //Called for when we return to the page
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("IdearCamera");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    //Called for when we leave the page
    private void stopBackgroundThread(){
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    //This method is supposed to sense the orientation of the device, but it seems functionality is broken.
    private static int senseDeviceRotation(CameraCharacteristics cameraCharacteristics, int orientation){
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        orientation = ORIENTATIONS.get(orientation);
        return (sensorOrientation + orientation + 360)%360;
    }

    //This method will take from the list of possible sizes supplied and compare them to the supplied width and height to
    // return the closest accurate size
    private static Size chooseSize(Size[] choices, int width, int height) {
        List<Size> correctSize = new ArrayList<Size>();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * height / width && option.getWidth() >= width && option.getHeight() >= height) {
                correctSize.add(option);
            }
        }
        if (correctSize.size() > 0) {
            return Collections.min(correctSize, new ImagePreview.compareSize());
        } else {
            return choices[0];
        }
    }
}
