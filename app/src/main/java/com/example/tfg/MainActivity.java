package com.example.tfg;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.camera.core.ExposureState;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.epson.moverio.util.PermissionGrantResultCallback;

import java.io.IOException;


public class MainActivity extends AppCompatActivity{
    private CameraPresentation mPresentation;
    private static final int REQUEST_CODE = 1;

    private static SeekBar exposureSeekBar;
    private static SeekBar zoomSeekbar;
    private static SwitchCompat grayscaleSwitch;
    private static SwitchCompat edgesSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
        setContentView(R.layout.activity_main);
        exposureSeekBar=findViewById(R.id.exposureSeekBar);
        exposureSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                TextView textView = findViewById(R.id.exposureText);
                textView.setText("Exposición: " + exposureSeekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MainActivity.this, "New camera exposure: " + CameraPresentation.setExposure(exposureSeekBar.getProgress()), Toast.LENGTH_SHORT).show();
            }
        });
        zoomSeekbar=findViewById(R.id.zoomSeekBar);
        zoomSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                TextView textView = findViewById(R.id.zoomText);
                textView.setText("Zoom: " + zoomSeekbar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                CameraPresentation.setZoom((float) zoomSeekbar.getProgress()/100);
                Toast.makeText(MainActivity.this, "New zoom: " + zoomSeekbar.getProgress(), Toast.LENGTH_SHORT).show();

            }
        });
        grayscaleSwitch = findViewById(R.id.grayscaleSwitch);
        grayscaleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                CameraPresentation.setGrayScaleMode(isChecked);
            }
        });
        edgesSwitch = findViewById(R.id.edgesSwitch);
        edgesSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                CameraPresentation.setEdgesMode(isChecked);
            }
        });
    }
    public static void initUI(){
        initSeekBar(exposureSeekBar);

    }
    private static void initSeekBar(SeekBar seekBar){
        if (seekBar==exposureSeekBar) {
            ExposureState exposureState = CameraPresentation.getCameraInfo().getExposureState();
            seekBar.setEnabled(exposureState.isExposureCompensationSupported());
            seekBar.setMax(exposureState.getExposureCompensationRange().getUpper());
            seekBar.setMin(exposureState.getExposureCompensationRange().getLower());
            seekBar.setProgress(exposureState.getExposureCompensationIndex());
        }
    }

    private void startCamera(){
        DisplayManager displayManager =(DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        Display[] displays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        if (displays.length > 0) {
            Display display = displays[0];
            mPresentation = new CameraPresentation(this, display);
            mPresentation.show();
            mPresentation.startCameraPreview();
        }
    }
    private void checkPermissions(){
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            // You can directly ask for the permission.
            ActivityCompat.requestPermissions(MainActivity.this,new String[] { Manifest.permission.CAMERA},REQUEST_CODE);

        } else {
            // You can use the API that requires the permission.
            startCamera();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==REQUEST_CODE){
            if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                startCamera();
            }
            else{
                Toast.makeText(this, "Esta aplicación necesita utilizar la cámara para fucionar.", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
