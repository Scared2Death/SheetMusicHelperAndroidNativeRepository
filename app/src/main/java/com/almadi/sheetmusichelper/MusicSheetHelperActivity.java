package com.almadi.sheetmusichelper;

import java.io.File;
import java.util.List;
import java.util.Arrays;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;
import android.os.Bundle;
import android.media.Image;
import android.app.Activity;
import android.view.View;
import android.widget.Toast;
import android.graphics.Color;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.os.CountDownTimer;
import android.os.ParcelFileDescriptor;
import android.annotation.SuppressLint;
import android.graphics.pdf.PdfRenderer;

import androidx.annotation.RawRes;
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

import com.almadi.sheetmusichelper.enums.LogType;
import com.almadi.sheetmusichelper.utilities.Helpers;
import com.almadi.sheetmusichelper.utilities.Constants;

public class MusicSheetHelperActivity extends AppCompatActivity
{
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private PdfRenderer pdfRenderer;

    private final String SAMPLE_FILE_NAME = "test_sheet_music.pdf";

    private Activity currentActivityRef;

    private int currentlyLoadedPDFPage = -1;
    private int pageCount;

    private final float smilingProbabilityBoundary = 0.7f;

    // 2 seconds
    private final int minimumTimeIntervalBetweenSmileDetections = 2000;
    private boolean isSmileDetectionEnabled = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        currentActivityRef = this;
        Helpers.removeTitleBar(currentActivityRef);

        try
        {
            Intent intent = getIntent();
            Bundle intentData = intent.getExtras();

            if (intentData == null || intentData.get(Constants.SELECTED_FILEURI_INTENT_DATA_KEY) == null || intentData.get(Constants.SELECTED_FILENAME_INTENT_DATA_KEY) == null)
            {
                File outputFilePath = new File(getCacheDir(), SAMPLE_FILE_NAME);
                copySamplePDFFileToLocalCache(outputFilePath, R.raw.test_sheet_music);

                ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(outputFilePath, ParcelFileDescriptor.MODE_READ_ONLY);

                pdfRenderer = new PdfRenderer(fileDescriptor);
                pageCount = pdfRenderer.getPageCount();
            }
            else
            {
                String selectedFileURIString = intentData.get(Constants.SELECTED_FILEURI_INTENT_DATA_KEY).toString();
                Uri selectedFileURI = Uri.parse(selectedFileURIString);

                String selectedFileName = intentData.get(Constants.SELECTED_FILENAME_INTENT_DATA_KEY).toString();
                File outputFilePath = new File(getCacheDir(), selectedFileName);

                copyStoragePDFFileToLocalCache(selectedFileURI, outputFilePath);

                ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(outputFilePath, ParcelFileDescriptor.MODE_READ_ONLY);

                pdfRenderer = new PdfRenderer(fileDescriptor);
                pageCount = pdfRenderer.getPageCount();
            }
        }
        catch (Exception ex)
        {
            Helpers.showToastNotification(this, "Error while trying to load pdf ...", Toast.LENGTH_LONG);
            Helpers.log(LogType.ERROR, String.format("Error while trying to load pdf with the following details: %s", ex.getStackTrace()));
        }

        setContentView(R.layout.activity_music_sheet_helper);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        Helpers.setStatusBarColor(currentActivityRef, Color.BLACK);

        startCameraFeedAndAnalysisActivities();

        try
        {
            displayPDFPage(0);
        }
        catch (Exception ex)
        {
            Helpers.showToastNotification(this, "Error while trying to display pdf page ...", Toast.LENGTH_LONG);
            Helpers.log(LogType.ERROR, String.format("Error while trying to display pdf page with the following details: %s", ex.getStackTrace()));
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if (pdfRenderer != null) pdfRenderer.close();
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
                            .addOnSuccessListener
                                    (
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
                                                        Helpers.log(LogType.INFORMATION, "No faces detected .");
                                                    }

                                                    imageProxy.close();
                                                }
                                            }
                                    )
                            .addOnFailureListener
                                    (
                                            e -> Helpers.log(LogType.ERROR, e.getStackTrace().toString())
                                    );

                }
            }
        });

        cameraProvider.bindToLifecycle((LifecycleOwner) this,cameraSelector, preview, imageAnalysis);
    }

    private void processDetectedFaces(List<Face> faces)
    {
        if (!isSmileDetectionEnabled)
        {
            return;
        }
        else
        {
            Face faceDetected = faces.get(0);

            float smilingProbability = faceDetected.getSmilingProbability();

            if (smilingProbability >= smilingProbabilityBoundary)
            {
                isSmileDetectionEnabled = false;

                new CountDownTimer(minimumTimeIntervalBetweenSmileDetections, 1000)
                {
                    public void onTick(long millisUntilFinished) { }

                    public void onFinish()
                    {
                        isSmileDetectionEnabled = true;
                    }
                }.start();

                // NUMBER OF PAGES IS E.G. 13, BUT THE PdfRenderer OBJECT INDEXES PAGES FROM 0 ...
                if (currentlyLoadedPDFPage < pageCount - 1)
                {
                    try
                    {
                        displayPDFPage(++currentlyLoadedPDFPage);
                    }
                    catch (Exception ex)
                    {
                        Helpers.showToastNotification(this, "Error while trying to jump to the next pdf page ...", Toast.LENGTH_LONG);
                        Helpers.log(LogType.ERROR, String.format("Error while trying to jump to the next pdf page with the following details: %s", ex.getStackTrace()));
                    }
                }
                else
                {
                    showEndOfSheetMusicContent();
                }
            }
        }
    }

    private void displayPDFPage(int pageNumber) throws IOException
    {
        PdfRenderer.Page pdfPage = pdfRenderer.openPage(pageNumber);

        Bitmap bitmap = Bitmap.createBitmap(pdfPage.getWidth(), pdfPage.getHeight(), Bitmap.Config.ARGB_8888);

        pdfPage.render(bitmap, null, null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        ImageView pdfImageView = findViewById(R.id.pdfImageView);
        pdfImageView.setImageBitmap(bitmap);

        currentlyLoadedPDFPage = pageNumber;

        pdfPage.close();
    }

    private void copySamplePDFFileToLocalCache(File outputFilePath, @RawRes int pdfResource) throws IOException
    {
        InputStream inputStream = getResources().openRawResource(pdfResource);

        processWriteProcedureToLocalCache(inputStream, outputFilePath);
    }

    private void copyStoragePDFFileToLocalCache(Uri inputFileURI, File outputFilePath) throws IOException
    {
        InputStream inputStream = getContentResolver().openInputStream(inputFileURI);

        processWriteProcedureToLocalCache(inputStream, outputFilePath);
    }

    private void processWriteProcedureToLocalCache(InputStream inputStream, File outputFilePath) throws IOException
    {
        if (!outputFilePath.exists())
        {
            FileOutputStream fileOutputStream = new FileOutputStream(outputFilePath);

            byte[] buffer = new byte[1024];

            int size;
            while ((size = inputStream.read(buffer)) != -1)
            {
                fileOutputStream.write(buffer, 0, size);
            }

            inputStream.close();
            fileOutputStream.close();
        }
    }

    private void showEndOfSheetMusicContent()
    {
        findViewById(R.id.previewViewFrameLayout).setVisibility(View.GONE);
        findViewById(R.id.pdfImageView).setVisibility(View.GONE);

        findViewById(R.id.endOfSheetMusicLinearLayout).setVisibility(View.VISIBLE);

        findViewById(R.id.returnToMainLauncherButton).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Helpers.navigateToActivity(getApplicationContext(), LauncherActivity.class, Arrays.asList(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        });
    }

}