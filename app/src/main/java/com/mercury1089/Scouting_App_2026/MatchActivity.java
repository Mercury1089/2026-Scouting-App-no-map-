package com.mercury1089.Scouting_App_2026;

import com.google.android.material.tabs.TabLayout;
import com.mercury1089.Scouting_App_2026.utils.GenUtils;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class MatchActivity extends AppCompatActivity {

    //variables that store elements of the screen for the output variables
    public TabLayout tabs;

    //for QR code generator
    public final static int QRCodeSize = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_match_tabs);

        //initializers
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setOffscreenPageLimit(4);
        viewPager.setAdapter(sectionsPagerAdapter);
        
        tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        // Update tab margins AFTER setupWithViewPager
        // setupWithViewPager clears existing tabs and recreates them, so we must loop after it.
        // We use tabs.getChildAt(0) to get the internal SlidingTabStrip.
        ViewGroup tabStrip = (ViewGroup) tabs.getChildAt(0);
        for (int i = 0; i < tabs.getTabCount(); i++) {
            View tab = tabStrip.getChildAt(i);
            if (tab != null) {
                ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) tab.getLayoutParams();
                p.setMargins(0, 0, 1, 0);
                tab.requestLayout();
            }
        }

        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.SETUP);
        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.AUTON);
        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.TELEOP);
        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.ENDGAME);

        // Handle back press using the modern OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitConfirmationDialog();
            }
        });
    }

    private void showExitConfirmationDialog() {
        Dialog dialog = new Dialog(MatchActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.popup_setup_next_match_confirm);

        Button exitConfirm = dialog.findViewById(R.id.CancelConfirm);
        Button cancelConfirm = dialog.findViewById(R.id.CancelConfirm);

        dialog.show();

        cancelConfirm.setOnClickListener(view -> dialog.dismiss());

        exitConfirm.setOnClickListener(view -> {
            dialog.dismiss();
            HashMapManager.setDefaultValues(HashMapManager.HASH.SETUP);
            HashMapManager.setDefaultValues(HashMapManager.HASH.AUTON);
            HashMapManager.setDefaultValues(HashMapManager.HASH.TELEOP);
            HashMapManager.setDefaultValues(HashMapManager.HASH.ENDGAME);
            Intent intent = new Intent(MatchActivity.this, PregameActivity.class);
            startActivity(intent);
            finish();
        });
    }

    //QR Generation
    Bitmap TextToImageEncode(String Value) throws WriterException {
        BitMatrix bitMatrix;
        try {
            bitMatrix = new MultiFormatWriter().encode(
                    Value,
                    BarcodeFormat.QR_CODE,
                    QRCodeSize, QRCodeSize, null
            );
        } catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }

        int bitMatrixWidth = bitMatrix.getWidth();
        int bitMatrixHeight = bitMatrix.getHeight();
        int[] pixels = new int[bitMatrixWidth * bitMatrixHeight];
        for (int y = 0; y < bitMatrixHeight; y++) {
            int offset = y * bitMatrixWidth;
            for (int x = 0; x < bitMatrixWidth; x++) {
                pixels[offset + x] = bitMatrix.get(x, y) ?
                        GenUtils.getAColor(MatchActivity.this, R.color.black) : GenUtils.getAColor(MatchActivity.this, R.color.white);
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, bitMatrixWidth, 0, 0, bitMatrixWidth, bitMatrixHeight);
        return bitmap;
    }
}
