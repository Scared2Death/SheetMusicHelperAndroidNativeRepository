package com.almadi.sheetmusichelper.utilities;

import android.view.View;
import android.app.Activity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import android.content.Context;
import android.app.AlertDialog;
import android.content.DialogInterface;

import androidx.core.content.ContextCompat;

public class Helpers
{
    public static void showToastNotification(Context context, String message, int duration)
    {
        Toast.makeText(context, message, duration);
    }

    public static void hideNavBarAndStatusBar(Activity activity)
    {
        View decorView = activity.getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;

        decorView.setSystemUiVisibility(uiOptions);
    }

    public static void showAlertDialog(Context context, String title, String message[])
    {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setItems(message, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        return;
                    }
                })
                .setNeutralButton("Ok", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        return;
                    }
                })
                .show();
    }

    public static void removeTitleBar(Activity activity)
    {
        activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    public static void setStatusBarColor(Activity activity, int color)
    {
        Window window = activity.getWindow();

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(color);
    }

}