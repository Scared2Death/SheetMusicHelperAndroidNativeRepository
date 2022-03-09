package com.almadi.sheetmusichelper;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.almadi.sheetmusichelper.utilities.Helpers;

import java.util.Arrays;

public class NoPermissionsProvidedActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_permissions_provided);
    }

    @Override
    public void onBackPressed()
    {
        Helpers.navigateToActivity(this, LauncherActivity.class, Arrays.asList());
    }

}