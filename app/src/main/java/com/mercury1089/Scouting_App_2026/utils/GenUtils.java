package com.mercury1089.Scouting_App_2026.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import com.mercury1089.Scouting_App_2026.R;
import androidx.core.content.ContextCompat;

public class GenUtils {

    private final static int QRCodeSize = 500;

    public static int getAColor(Context context, int id) {
        return ContextCompat.getColor(context, id);
    }

    public static void defaultButtonState (Context context, Button button) {
        button.setBackgroundColor(GenUtils.getAColor(context, R.color.light));
        button.setTextColor(GenUtils.getAColor(context, R.color.grey));
    }
    public static void selectedButtonState(Context context, Button button) {
        button.setBackgroundColor(GenUtils.getAColor(context, R.color.orange));
        button.setTextColor(GenUtils.getAColor(context, R.color.light));
    }
    public static void disabledButtonState(Context context, Button button) {
        button.setBackgroundColor(GenUtils.getAColor(context, R.color.grey));
        button.setTextColor(GenUtils.getAColor(context, R.color.light));
        button.setEnabled(false);
    }

    public static String padLeftZeros(String input, int length){
        if (input == null) return new String(new char[length]).replace("\0", "0");
        if(input.length() < length){
            String result = "";
            for(int i = 0; i < length - input.length(); i++){
                result += "0";
            }
            return result + input;
        }
        return input;
    }

    /**
     * Enables immersive full-screen mode by hiding system bars.
     */
    public static void setFullscreen(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = activity.getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // Support for older versions
            activity.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }
}
