package com.example.idear;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.HandlerThread;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.Toast;
import android.os.Handler;
import android.util.Size;
import android.content.Context;
import android.hardware.camera2.CaptureRequest;
import android.view.Surface;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.view.TextureView;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ImagePreview extends AppCompatActivity {
    private TextureView mHolder;
    private static final int REQUEST_CAMERA_PERMISSION_CODE = 0;

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
        setContentView(R.layout.activity_main);
        Log.d("CREATE", "Image Preview is running.");

        mHolder = (TextureView) findViewById(R.id.textureView);
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
                int totalRotation = senseDeviceRotation(cameraCharacteristics, deviceOrientation);
                boolean swapRotation = totalRotation == 90 || totalRotation == 270;
                int rotateWidth = width;
                int rotateHeight = height;
                if(swapRotation == true){
                    rotateWidth = height;
                    rotateHeight = width;
                }
                mPreviewSize = chooseSize(map.getOutputSizes(SurfaceTexture.class), rotateWidth, rotateHeight);
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
            mCameraD.createCaptureSession(Arrays.asList(preview), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mBackgroundHandler);
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

    private static Size chooseSize(Size[] choices, int width, int height){
        List<Size> correctSize = new ArrayList<Size>();
        for(Size option : choices) {
            if(option.getHeight() == option.getWidth() * height / width && option.getWidth() >= width && option.getHeight() >= height){
                correctSize.add(option);
            }
        }
        if(correctSize.size() > 0){
            return Collections.min(correctSize, new compareSize());
        } else {
            return choices[0];
        }
    }
}
