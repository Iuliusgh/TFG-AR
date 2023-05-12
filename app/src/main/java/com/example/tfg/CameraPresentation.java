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
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class CameraPresentation extends Presentation implements LifecycleOwner, ImageAnalysis.Analyzer {
    String TAG="CameraPresentation: ";
    private static PreviewView mPreviewView;
    private static SurfaceView mSurfaceView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private LifecycleRegistry mLifecycleRegistry;

    private static boolean grayScaleMode= false;
    private static boolean edgesMode = false;
    private static boolean normalMode;
    private static float contrastValue = 1;
    private static int brightnessValue = 0;
    private static boolean CBMode=false;

    private final float DEFAULT_CONTRAST = 0f;
    private float prevContrast=DEFAULT_CONTRAST;
    private Mat lut ;

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

    public static CameraInfo getCameraInfo(){
        return camera.getCameraInfo();
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
        image.close();
        if(checkNormalMode()){
            if (isSurfaceViewVisible())setSurfaceViewVisible(false);
        }
        else {
            if (!isSurfaceViewVisible()) setSurfaceViewVisible(true);
            Bitmap bitmap = mPreviewView.getBitmap();
            if (mSurfaceView.getHolder().getSurface().isValid()) {
                Canvas canvas = mSurfaceView.getHolder().lockCanvas();
                if (edgesMode) {
                    canvas.drawBitmap(edgeDetection(bitmap), 0f, 0f, null);
                }
                if (grayScaleMode) {
                    canvas.drawBitmap(toGrayscale(bitmap), 0f, 0f, null);
                }
                if(CBMode){
                    if(prevContrast!=contrastValue){
                        lut = contrastLookupTable(contrastValue);
                    }
                    canvas.drawBitmap(contrastAndBrightness(bitmap),0f,0f,null);

                }
                mSurfaceView.getHolder().unlockCanvasAndPost(canvas);
            }
        }

    }

    private Bitmap toGrayscale(Bitmap bitmap) {
        Mat mat = new Mat();
        Mat gray = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        Imgproc.cvtColor(mat,gray,Imgproc.COLOR_BGR2GRAY);
        Utils.matToBitmap(gray, bitmap);
        return bitmap;
    }
    private Bitmap edgeDetection(Bitmap bitmap) {
        //Declaration of all the Mats needed
        Mat mat = new Mat();
        Mat blur = new Mat();
        Mat edges = new Mat();
        //Transformation of bitmap input into Mat
        Utils.bitmapToMat(bitmap, mat);
        //Conversion to grayscale
        Imgproc.cvtColor(mat,mat,Imgproc.COLOR_BGR2GRAY);
        //Image blur
        org.opencv.core.Size kernel = new org.opencv.core.Size(3.0,3.0);
        Imgproc.GaussianBlur(mat,blur,kernel,0);
        //Use of adaptiveThreshold to enhance the edge detection
        //Imgproc.adaptiveThreshold(blur,edges,255,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,Imgproc.THRESH_BINARY_INV,3,2);
        //Edge detection using Canny's algorithm
        double maxThreshold=100.0;
        Imgproc.Canny(blur, edges, maxThreshold/2, maxThreshold);
        //Transformation back to bitmap
        Utils.matToBitmap(edges, bitmap);
        return bitmap;
    }
    private Bitmap contrastAndBrightness(Bitmap bitmap){
        Mat src = new Mat();
        Mat dst = new Mat();
        Utils.bitmapToMat(bitmap,src);

        Core.LUT(src,lut,dst);
        dst.convertTo(dst,-1,1.0,brightnessValue);
        Utils.matToBitmap(dst,bitmap);
        return bitmap;
    }
    Mat contrastLookupTable(float contrast) {
        Mat lut = new Mat(1, 256, CvType.CV_8U);
        byte[] lutData =  new byte[(int) (lut.total()*lut.channels())];

        for (int i=0 ; i < lut.cols(); i++) {
            lutData[i] = saturate(contrast*(i-255)+255);
        }
        lut.put(0,0,lutData);
        return lut;
    }
    private byte saturate(float val) {
        int iVal = (int) Math.round(val);
        if (iVal>255) iVal=255;
        else if (iVal<0) iVal=0;
        return (byte) iVal;
    }
    private boolean isSurfaceViewVisible(){
        return mSurfaceView.getVisibility() == View.VISIBLE;
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
    public static void setGrayScaleMode(boolean grayScaleMode) {
        CameraPresentation.grayScaleMode = grayScaleMode;
    }
    public static void setEdgesMode(boolean edgesMode) {
        CameraPresentation.edgesMode = edgesMode;
    }
    private boolean checkNormalMode(){
        normalMode=!(grayScaleMode || edgesMode || CBMode);
        return normalMode;
    }

    public static void setContrast(float contrast) {
        contrastValue=contrast;
    }

    public static void setBrightness(int brightness) {
        brightnessValue=brightness;
    }

    public static void setCBMode(boolean CBMode) {
        CameraPresentation.CBMode=CBMode;
    }
}

