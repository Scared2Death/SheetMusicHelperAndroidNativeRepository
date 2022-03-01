package com.almadi.sheetmusichelper;

import android.Manifest;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.almadi.sheetmusichelper.utilities.Helpers;

public class LauncherActivity extends AppCompatActivity
{
    private static final String[] CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final int CAMERA_REQUEST_CODE = 10;

    private Activity currentActivityRef;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        currentActivityRef = this;
        Helpers.removeTitleBar(currentActivityRef);

        setContentView(R.layout.activity_launcher);

        if (checkCameraPermission())
        {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    navigateToMusicSheetHelperActivity();
                }
            }, 3000);
        }
        else
        {
            requestPermission();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        Helpers.hideNavBarAndStatusBar(currentActivityRef);
        Helpers.setStatusBarColor(currentActivityRef, Color.BLACK);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if (checkCameraPermission())
        {
            navigateToMusicSheetHelperActivity();
        }
        else
        {
            Helpers.showAlertDialog(this, "No permissions", new String[] {"No permissions have been granted for the app to run ... :("});
        }
    }

    private boolean checkCameraPermission()
    {
        boolean hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        return  hasCameraPermission;
    }

    private void navigateToMusicSheetHelperActivity()
    {
        Intent intent = new Intent(this, MusicSheetHelperActivity.class);

        startActivity(intent);
    }

    private void requestPermission()
    {
        ActivityCompat.requestPermissions(this, CAMERA_PERMISSION, CAMERA_REQUEST_CODE);
    }

}

// DISABLE ROTATION
// LANDSCAPE ONLY