package com.example.tfg;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private CameraPresentation mPresentation;
    private static final int REQUEST_CODE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();
    }
    private void startCamera(){
        DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
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
