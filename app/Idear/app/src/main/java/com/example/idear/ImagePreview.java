package com.example.idear;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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

public class ImagePreview extends AppCompatActivity {

    private Button capture;
    private Button settings;
    private Button library;

    private Uri mFileUri;

    private Bitmap image;

    private TextureView mHolder;
    private static final int REQUEST_IMAGE_CAPTURE = 0;
    private static final int REQUEST_CAMERA_PERMISSION_CODE = 1;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    private int mCaptureState = STATE_PREVIEW;

    int totalRotation;

    private File mPhotoFolder;
    private String photoID;
    private String photoDate;

    private Size mImageSize;
    private ImageReader mImageReader;
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new
            ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    mBackgroundHandler.post(new imageSaver(reader.acquireLatestImage()));
                }
            };
    private CameraCaptureSession mPreviewCaptureSession;
    private CameraCaptureSession.CaptureCallback mPreviewCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        //public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            //super.onCaptureStarted(session, request, timestamp, frameNumber);
        //}

        public void process(CaptureResult capture){
            Log.d("CREATE" ,"In Process.");
            switch(mCaptureState){
                case STATE_PREVIEW:
                    Log.d("CREATE" ,"Switch returned state preview.");
                    break;
                case STATE_WAIT_LOCK:
                    Log.d("CREATE" ,"Switch returned wait lock.");
                    mCaptureState = STATE_PREVIEW;
                    Integer autoFocusState = capture.get(CaptureResult.CONTROL_AF_STATE);
                    Log.d("CREATE" ,"Current AF state is " + autoFocusState + ", whereas Focus Locked is " + CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED + " and the opposite is " + CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED);
                    Log.d("CREATE" ,"Checking autoFocusState...");
                    //if(autoFocusState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || autoFocusState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED){
                        Toast.makeText(getApplicationContext(), "Auto Focus is on!", Toast.LENGTH_SHORT).show();
                        Log.d("CREATE" ,"Starting still capture request...");
                        startStillCaptureRequest();
                    //}
                    break;
            }
        }
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result){
            super.onCaptureCompleted(session, request, result);
            Log.d("CREATE" ,"Capture has been completed.");
            process(result);
        }
    };
    private class imageSaver implements Runnable{
        private final Image mImage;
        public imageSaver(Image image){
            mImage = image;
        }
        @Override
        public void run(){
            Log.d("CREATE" ,"Running imageSaver.");
            ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            Log.d("CREATE" ,"Number of bytes: " + bytes.length);
            FileOutputStream fOutPut = null;
            try {
                Log.d("CREATE" ,"Made it into the try harness..");
                fOutPut = new FileOutputStream(currentPhotoPath);
                fOutPut.write(bytes);
            }catch (IOException e){

            } finally {
                Log.d("CREATE" ,"Cleaning up...");
                mImage.close();
                if(fOutPut != null){
                    try {
                        fOutPut.close();
                    }catch(IOException e){

                    }
                }
            }
        }
    }


    private TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height){
            Log.d("CREATE" ,"We have liftoff!");
            cameraSetup(width, height);
            connectCamera();
        }

        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height){

        }

        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface){
            return false;
        }

        public void onSurfaceTextureUpdated(SurfaceTexture surface){

        }
    };

    private CameraDevice mCameraD;
    private CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera){
            mCameraD = camera;
            Toast.makeText(getApplicationContext(), "We have a successful connection!", Toast.LENGTH_SHORT).show();
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera){
            camera.close();
            mCameraD = null;
        }

        @Override
        public void onError(CameraDevice camera, int error){
            camera.close();
            mCameraD = null;
        }
    };

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private Size mPreviewSize;
    private CaptureRequest.Builder mCaptureRequestBuilder;

    private String mCamID;
    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private static class compareSize implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs){
            return Long.signum(((long)lhs.getWidth() * lhs.getHeight()) / ((long)rhs.getWidth() * rhs.getHeight()));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);
        Log.d("CREATE", "Image Preview is running.");

        mHolder = (TextureView) findViewById(R.id.textureView);

        capture = (Button) findViewById(R.id.CaptureBtn);
        settings = (Button) findViewById(R.id.SettingBtn);
        library = (Button) findViewById(R.id.LibraryBtn);

        createImageFolder();

        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //dispatchTakePictureIntent();
                //Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                //stopBackgroundThread();
                //dispatchTakePictureIntent();
                lockFocus();
                //startBackgroundThread();
                Log.d("CREATE", "made it through ActivityResult. What happened??.");
            }
        });


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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK ) {
            super.onActivityResult(requestCode, resultCode, data);
            Bundle extras = data.getExtras();
            image = (Bitmap) extras.get("data");
        }
    }

    //private void dispatchTakePictureIntent() {
        //Log.d("CREATE", "dispatchTakePictureIntent ran.");
        //Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //if (intent.resolveActivity(getPackageManager()) != null) {
            //Log.d("CREATE", "activity is not null.");
            //File tempFile = null;
            //try {
                //Log.d("CREATE", "creating image file...");
                //tempFile = createImageFile();
            //} catch (IOException e) {
                //Log.d("CREATE", "Error attempting to create image file!");
            //}
            //if (tempFile != null) {
                //Log.d("CREATE", "tempFile is not null.");
                //Uri photoURI = FileProvider.getUriForFile(this, "com.example.idear.fileprovider", tempFile);
                //Log.d("CREATE", "URI successfully created.");
                //intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                //try {
                    //Log.d("CREATE", "starting activity...");
                    //startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
                //} catch (ActivityNotFoundException e) {

                //}
           // }
        //}
    //}

    private void createImageFolder(){
        File toImageFile = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        mPhotoFolder = new File(toImageFile, "Idear");
        if(!mPhotoFolder.exists()) {
            mPhotoFolder.mkdirs();
        }
    }

    String currentPhotoPath;

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        photoID = imageFileName;
        //File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(imageFileName, ".png", mPhotoFolder);
        currentPhotoPath = imageFile.getAbsolutePath();
        Log.d("CREATE", currentPhotoPath);
        Log.d("CREATE", "imagefile created..");
        return imageFile;
    }

    private void cameraSetup(int width, int height){
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String camID : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(camID);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                totalRotation = senseDeviceRotation(cameraCharacteristics, deviceOrientation);
                boolean swapRotation = totalRotation == 90 || totalRotation == 270;
                int rotateWidth = width;
                int rotateHeight = height;
                if(swapRotation == true){
                    rotateWidth = height;
                    rotateHeight = width;
                }
                mPreviewSize = chooseSize(map.getOutputSizes(SurfaceTexture.class), rotateWidth, rotateHeight);
                mImageSize = chooseSize(map.getOutputSizes(ImageFormat.JPEG), rotateWidth, rotateHeight);
                mImageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG,1);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
                mCamID = camID;
                return;
            }
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    private void connectCamera(){
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                    cameraManager.openCamera(mCamID, mCameraStateCallback, mBackgroundHandler);
                } else {
                    if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                        Toast.makeText(this, "Please note: Idear requires access to the camera in order to function.", Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_CODE);
                }
            } else {
                cameraManager.openCamera(mCamID, mCameraStateCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview(){
        SurfaceTexture surfaceTexture = mHolder.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface preview = new Surface(surfaceTexture);
        try {
            mCaptureRequestBuilder = mCameraD.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(preview);
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

    private void startStillCaptureRequest(){
        try {
            Log.d("CREATE" ,"Capture Request harness entered.");
            mCaptureRequestBuilder = mCameraD.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, totalRotation);
            CameraCaptureSession.CaptureCallback stillCaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                        try {
                            Log.d("CREATE" ,"Creating image file...");
                            createImageFile();
                        }catch (IOException e){

                        }
                    }
                };

            mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), stillCaptureCallback, null);
        }catch (CameraAccessException e) {

        }
    }

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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CAMERA_PERMISSION_CODE){
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(getApplicationContext(), "Idear cannot run without camera!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void lockFocus(){
        Log.d("CREATE" ,"LockFocus was called.");
        mCaptureState = STATE_WAIT_LOCK;
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        try {
            Log.d("CREATE" ,"Starting capture...");
            mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), mPreviewCaptureCallback, mBackgroundHandler);
        }catch (CameraAccessException e){

        }
    }

    @Override
    protected void onPause(){
        closeCamera();

        stopBackgroundThread();
        super.onPause();
    }

    private void closeCamera() {
        if(mCameraD != null) {
            mCameraD.close();
            mCameraD = null;
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("IdearCamera");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

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

    private static int senseDeviceRotation(CameraCharacteristics cameraCharacteristics, int orientation){
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        orientation = ORIENTATIONS.get(orientation);
        return (sensorOrientation + orientation + 360)%360;
    }

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
