package com.example.tfg;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExposureState;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import android.annotation.SuppressLint;
import android.app.Presentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class CameraPresentation extends Presentation implements LifecycleOwner, ImageAnalysis.Analyzer {
    String TAG="CameraPresentation: ";
    private static PreviewView mPreviewView;
    private static SurfaceView mSurfaceView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private LifecycleRegistry mLifecycleRegistry;

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
        mSurfaceView = findViewById(R.id.surfaceView);
        mPreviewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        mLifecycleRegistry.setCurrentState(Lifecycle.State.STARTED);
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV library not loaded!");
        } else {
            Log.d(TAG, "OpenCV library loaded successfully.");
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        mLifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);
    }

    @NonNull
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
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(getTargetResolution())
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(getContext()),this );

        camera=cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        MainActivity.initUI();
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
    public static int setExposure(int exposureValue){
        camera.getCameraControl().setExposureCompensationIndex(exposureValue);
        return camera.getCameraInfo().getExposureState().getExposureCompensationIndex();
    }
    public static void setZoom( float zoomValue){
        camera.getCameraControl().setLinearZoom(zoomValue);
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        Bitmap bitmap = mPreviewView.getBitmap();
        if (mSurfaceView.getHolder().getSurface().isValid()){
            Canvas canvas = mSurfaceView.getHolder().lockCanvas();
            canvas.drawBitmap(edgeDetection(bitmap), 0f, 0f, null);
            mSurfaceView.getHolder().unlockCanvasAndPost(canvas);
        }
        image.close();
    }

    private Bitmap toGrayscale(Bitmap bitmap) {

        Paint grayscalePaint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        grayscalePaint.setColorFilter(new ColorMatrixColorFilter(cm));

        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(bitmap, 0f, 0f, grayscalePaint);

        return bitmap;
    }
    private Bitmap edgeDetection(Bitmap bitmap) {

        // Apply the edge detection filter
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        Mat edges = new Mat();
        Imgproc.Canny(mat, edges, 100/3, 100);
        Utils.matToBitmap(edges, bitmap);

        return bitmap;
    }
    public static void setSurfaceViewVisible(boolean b) {
        if (b) {
            mPreviewView.setVisibility(View.GONE);
            mSurfaceView.setVisibility(View.VISIBLE);
        } else {
            mSurfaceView.setVisibility(View.GONE);
            mPreviewView.setVisibility(View.VISIBLE);
        }
    }
}

