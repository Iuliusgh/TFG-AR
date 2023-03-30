package com.example.tfg;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ExposureState;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {
    private CameraPresentation mPresentation;
    private static final int REQUEST_CODE = 1;
    private Button brilloBoton;
    private static SeekBar exposureSeekBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
        setContentView(R.layout.activity_main);
        brilloBoton = findViewById(R.id.brillo);
        brilloBoton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                brillo();
            }
        });
        exposureSeekBar=findViewById(R.id.exposureSeekBar1);
        exposureSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
    public static void initUI(){
        initSeekBar(exposureSeekBar);
    }
    private static void initSeekBar(SeekBar seekBar){
       ExposureState exposureState = CameraPresentation.getExposureCompensationRange();
       seekBar.setEnabled(exposureState.isExposureCompensationSupported());
       seekBar.setMax(exposureState.getExposureCompensationRange().getUpper());
       seekBar.setMin(exposureState.getExposureCompensationRange().getLower());
       seekBar.setProgress(exposureState.getExposureCompensationIndex());
    }
    private void brillo(){
        com.epson.moverio.hardware.display.DisplayManager mDisplayManager = new com.epson.moverio.hardware.display.DisplayManager(this);
        try {
            mDisplayManager.open();
            Toast.makeText(this, "Brillo cambiado", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mDisplayManager.setBrightnessMode(com.epson.moverio.hardware.display.DisplayManager.BRIGHTNESS_MODE_MANUAL);
        mDisplayManager.setBrightness(1);
        mDisplayManager.close();
        mDisplayManager.release();

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
