package com.almadi.sheetmusichelper;

import android.net.Uri;
import android.Manifest;
import android.os.Looper;
import android.view.View;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.graphics.Color;
import android.content.Intent;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.almadi.sheetmusichelper.enums.LogType;
import com.almadi.sheetmusichelper.models.IntentData;
import com.almadi.sheetmusichelper.utilities.Constants;
import com.almadi.sheetmusichelper.utilities.Helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.lang.reflect.Array;
import java.net.URLDecoder;

public class LauncherActivity extends AppCompatActivity
{
    private final String[] CAMERA_PERMISSION = new String[]{ Manifest.permission.CAMERA };
    private final int CAMERA_REQUEST_CODE = 10;

    private final int SELECT_PDF_FILE_REQUEST_CODE = 11;

    // 2 seconds
    private final int timeIntervalBeforeNavigation = 2000;

    private Activity currentActivityRef;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        currentActivityRef = this;
        Helpers.removeTitleBar(currentActivityRef);

        setContentView(R.layout.activity_launcher);

        findViewById(R.id.selectFileButton).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                selectPDFFile();
            }
        });
        findViewById(R.id.startWithSampleFileButton).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                navigateToMusicSheetActivity();
            }
        });

        if (!checkCameraPermission())
        {
            requestPermission();
        }
        else
        {
            setActivityContentVisible();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        Helpers.setStatusBarColor(currentActivityRef, Color.BLACK);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if (checkCameraPermission()) setActivityContentVisible();
        else Helpers.navigateToActivity(this, NoPermissionsProvidedActivity.class, Arrays.asList());
    }

    private void navigateToMusicSheetActivity()
    {
        Helpers.navigateToActivity(this, MusicSheetHelperActivity.class, Arrays.asList());
    }

    private boolean checkCameraPermission()
    {
        boolean hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        return hasCameraPermission;
    }

    private void requestPermission()
    {
        ActivityCompat.requestPermissions(this, CAMERA_PERMISSION, CAMERA_REQUEST_CODE);
    }

    private void setActivityContentVisible()
    {
        findViewById(R.id.selectFileTextView).setVisibility(View.VISIBLE);
        findViewById(R.id.selectFileButton).setVisibility(View.VISIBLE);
        findViewById(R.id.startWithSampleFileButton).setVisibility(View.VISIBLE);
    }

    private void selectPDFFile()
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        startActivityForResult(intent, SELECT_PDF_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case SELECT_PDF_FILE_REQUEST_CODE:
                if (resultCode == RESULT_OK)
                {
                    try
                    {
                        Uri uri = data.getData();
                        String uriString = uri.toString();

                        String keyOne = Constants.SELECTED_FILEURI_INTENT_DATA_KEY;
                        String valueOne = uriString;

                        String keyTwo = Constants.SELECTED_FILENAME_INTENT_DATA_KEY;
                        String valueTwo = new File(URLDecoder.decode(uriString)).getName();

                        Helpers.navigateToActivity(this, MusicSheetHelperActivity.class, Arrays.asList(), new IntentData(keyOne, valueOne), new IntentData(keyTwo, valueTwo));
                    }
                    catch (Exception ex)
                    {
                        Helpers.showToastNotification(this, "Error while trying to handle the selected pdf file ...", Toast.LENGTH_LONG);
                        Helpers.log(LogType.ERROR, String.format("Error while trying to handle the selected pdf file with the following details: %s", ex.getStackTrace()));
                    }
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

}