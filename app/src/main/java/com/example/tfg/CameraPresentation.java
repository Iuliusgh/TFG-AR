package com.example.tfg;

import androidx.camera.camera2.internal.ExposureControl;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExposureState;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import android.app.Presentation;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Size;
import android.view.Display;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class CameraPresentation extends Presentation implements LifecycleOwner {

    private PreviewView mPreviewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private LifecycleRegistry mLifecycleRegistry;

    private CameraControl cameraControl;
    private static Camera camera;
    public CameraPresentation(Context context, Display display) {
        super(context, display);
        mLifecycleRegistry = new LifecycleRegistry(this);
        mLifecycleRegistry.setCurrentState(Lifecycle.State.CREATED);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.secondary_display);

        mPreviewView = findViewById(R.id.displayPreviewView);
        mLifecycleRegistry.setCurrentState(Lifecycle.State.STARTED);
    }
    @Override
    protected void onStop() {
        super.onStop();
        mLifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);
    }

    public LifecycleRegistry getLifecycle() {
        return mLifecycleRegistry;
    }
    public void startCameraPreview() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }
    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .setTargetResolution(getTargetResolution()).build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());

        camera=cameraProvider.bindToLifecycle(this, cameraSelector, preview);
    }
    private Size getTargetResolution() {
        Display display = getDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        return new Size(width, height);
    }

    public static ExposureState getExposureCompensationRange(){
        CameraInfo cameraInfo = camera.getCameraInfo();
        ExposureState exposureState = cameraInfo.getExposureState();
        return exposureState;
    }
}

