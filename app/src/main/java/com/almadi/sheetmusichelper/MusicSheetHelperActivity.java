package com.almadi.sheetmusichelper;

import java.util.List;

import android.app.Activity;
import android.graphics.Color;
import android.util.Size;
import android.os.Bundle;
import android.media.Image;
import android.widget.Toast;
import android.widget.TextView;
import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Preview;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.CameraSelector;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.lifecycle.ProcessCameraProvider;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.common.util.concurrent.ListenableFuture;

import com.almadi.sheetmusichelper.utilities.Helpers;

public class MusicSheetHelperActivity extends AppCompatActivity
{
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private TextView textView;

    private Activity currentActivityRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        currentActivityRef = this;
        Helpers.removeTitleBar(currentActivityRef);

        setContentView(R.layout.activity_music_sheet_helper);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        Helpers.hideNavBarAndStatusBar(currentActivityRef);
        Helpers.setStatusBarColor(currentActivityRef, Color.BLACK);

        startCameraFeedAndAnalysisActivities();
    }

    private void startCameraFeedAndAnalysisActivities()
    {
        previewView = findViewById(R.id.previewView);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);
                }
                catch (Exception ex)
                {
                    String errorMessage = "Error occurred with the following details: " + ex.getMessage();
                    Helpers.showToastNotification(getApplicationContext(), errorMessage, Toast.LENGTH_LONG);
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider)
    {
        cameraProvider.unbindAll();

        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(previewView.getWidth(), previewView.getHeight()))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer()
        {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy)
            {
                int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();

                @SuppressLint("UnsafeOptInUsageError")
                Image imageBuffer = imageProxy.getImage();

                if (imageBuffer != null)
                {
                    InputImage image = InputImage.fromMediaImage(imageBuffer, rotationDegrees);

                    FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                            .build();

                    FaceDetector faceDetector = FaceDetection.getClient(options);

                    faceDetector.process(image)
                            .addOnSuccessListener(
                                    new OnSuccessListener<List<Face>>()
                                    {
                                        @Override
                                        public void onSuccess(List<Face> faces)
                                        {
                                            if (!faces.isEmpty())
                                            {
                                                processDetectedFaces(faces);
                                            }
                                            else
                                            {
                                                Helpers.showToastNotification(getApplicationContext(),"No faces detected .", Toast.LENGTH_SHORT);
                                            }
                                        }
                                    })
                            .addOnFailureListener(e -> Helpers.showToastNotification(getApplicationContext(), e.getStackTrace().toString(), Toast.LENGTH_LONG)
                            );
                }
            }
        });

        cameraProvider.bindToLifecycle((LifecycleOwner) this,cameraSelector, preview, imageAnalysis);
    }

    private void processDetectedFaces(List<Face> faces)
    {
        int faceCount = faces.size();

        Helpers.showToastNotification(getApplicationContext(), String.format("Number of faces detected: %s", faceCount), Toast.LENGTH_SHORT);
    }

}