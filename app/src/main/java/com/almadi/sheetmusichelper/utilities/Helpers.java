package com.almadi.sheetmusichelper.utilities;

import android.util.Log;
import android.view.View;
import android.view.Window;
import android.app.Activity;
import android.widget.Toast;
import android.content.Intent;
import android.content.Context;
import android.app.AlertDialog;
import android.view.WindowManager;
import android.content.DialogInterface;

import androidx.core.content.ContextCompat;

import com.almadi.sheetmusichelper.enums.LogType;
import com.almadi.sheetmusichelper.models.IntentData;
import com.almadi.sheetmusichelper.MusicSheetHelperActivity;

import java.util.List;

public class Helpers
{
    public static void showToastNotification(Context context, String message, int duration)
    {
        Toast.makeText(context, message, duration).show();
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

    public static void log(LogType logType, String message)
    {
        switch (logType)
        {
            case INFORMATION:
                Log.i("[INFORMATION]", message);
                break;

            case ERROR:
                Log.e("[ERROR]", message);
                break;
        }
    }

    public static void navigateToActivity(Context context, Class targetClass, List<Integer> flags, IntentData... intentData)
    {
        Intent intent = new Intent(context, targetClass);

        if (!flags.isEmpty())
        {
            for (int flag: flags)
            {
                intent.setFlags(flag);
            }
        }

        if (intentData.length > 0)
        {
            for (IntentData data: intentData)
            {
                intent.putExtra(data.Key, data.Value);
            }
        }

        context.startActivity(intent);
    }

}