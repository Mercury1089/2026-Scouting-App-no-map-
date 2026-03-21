package com.mercury1089.Scouting_App_2026.qr;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.mercury1089.Scouting_App_2026.HashMapManager;
import com.mercury1089.Scouting_App_2026.PregameActivity;
import com.mercury1089.Scouting_App_2026.R;
import com.mercury1089.Scouting_App_2026.utils.GenUtils;
import com.mercury1089.Scouting_App_2026.utils.QRStringBuilder;

import java.util.LinkedHashMap;

public class QRRunnable implements Runnable {
    private final Activity context;
    private final Dialog loading_alert;
    LinkedHashMap<String, String> setupHashMap = HashMapManager.getSetupHashMap();
    private String scouter, teamNum, matchNum, qrString;
    private boolean needsToBeStored;

    public QRRunnable(Activity ctx, Dialog loading_alert) {
        this.context = ctx;
        this.loading_alert = loading_alert;
        QRStringBuilder.buildQRString();
        this.qrString = QRStringBuilder.getQRString();
        this.scouter  = QRStringBuilder.getScouterName();
        this.teamNum  = QRStringBuilder.getTeamNumber();
        this.matchNum = QRStringBuilder.getMatchNumber();
        needsToBeStored = true;
    }

    public QRRunnable(String qrString, Activity ctx, Dialog loading_alert) {
        this.context = ctx;
        this.loading_alert = loading_alert;
        this.qrString = qrString;
        // Multi-row QR string — parse scouter/team/match from the first row only
        String firstRow = qrString.split(QRStringBuilder.ROW_DELIMITER)[0];
        String[] data   = firstRow.split(QRStringBuilder.DELIMITER);
        this.scouter  = data[QRStringBuilder.SCOUTER_NAME_INDEX];
        this.teamNum  = data[QRStringBuilder.TEAM_NUM_INDEX];
        this.matchNum = data[QRStringBuilder.MATCH_NUM_INDEX];
        needsToBeStored = false;
    }

    @Override
    public void run() {
        // Once QR is generated, hashmap values go back to defaults
        HashMapManager.setDefaultValues(HashMapManager.HASH.AUTON);
        HashMapManager.setDefaultValues(HashMapManager.HASH.TELEOP);
        HashMapManager.setDefaultValues(HashMapManager.HASH.ENDGAME);

        try {
            Bitmap bitmap = QRUtils.textToImageEncode(qrString);
            context.runOnUiThread(() -> {
                if (needsToBeStored) {
                    HashMapManager.putSetupHashMap(setupHashMap);

                    Dialog dialog = new Dialog(context);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.setContentView(R.layout.popup_qr);
                    if (needsToBeStored) QRStringBuilder.storeQRString(context);

                    ImageView imageView  = dialog.findViewById(R.id.imageView);
                    TextView scouterName = dialog.findViewById(R.id.ScouterNameQR);
                    TextView teamNumber = dialog.findViewById(R.id.TeamNumberQR);
                    TextView matchNumber = dialog.findViewById(R.id.MatchNumberQR);
                    Button goBackToMain  = dialog.findViewById(R.id.GoBackButton);
                    ImageButton closeButton = dialog.findViewById(R.id.CloseButton);
                    imageView.setImageBitmap(bitmap);

                    dialog.setCancelable(false);

                    scouterName.setText(this.scouter);
                    teamNumber.setText(this.teamNum);
                    matchNumber.setText(this.matchNum);

                    closeButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    if (loading_alert != null && loading_alert.isShowing()) {
                        loading_alert.dismiss();
                    }
                    dialog.show();

                    goBackToMain.setOnClickListener(v -> {
                        Dialog confirmDialog = new Dialog(context);
                        confirmDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        confirmDialog.setContentView(R.layout.popup_setup_next_match_confirm);
                        Button setupNextMatchButton = confirmDialog.findViewById(R.id.SetupNextMatchButton);
                        Button cancelConfirm        = confirmDialog.findViewById(R.id.CancelConfirm);

                        confirmDialog.show();

                        setupNextMatchButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                QRStringBuilder.clearQRString();
                                HashMapManager.setupNextMatch();
                                Intent intent = new Intent(context, PregameActivity.class);
                                dialog.dismiss();
                                context.startActivity(intent);
                                context.finish();
                                confirmDialog.dismiss();
                            }
                        });

                        cancelConfirm.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                confirmDialog.dismiss();
                            }
                        });
                    });
                } else {
                    Dialog dialog = new Dialog(context);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.setContentView(R.layout.popup_qr_cached);

                    ImageView imageView = dialog.findViewById(R.id.imageView);
                    TextView scouterName = dialog.findViewById(R.id.ScouterNameQR);
                    TextView teamNumber = dialog.findViewById(R.id.TeamNumberQR);
                    TextView matchNumber = dialog.findViewById(R.id.MatchNumberQR);
                    imageView.setImageBitmap(bitmap);

                    dialog.setCancelable(false);

                    scouterName.setText(scouter);
                    teamNumber.setText(GenUtils.padLeftZeros(teamNum, 2));
                    matchNumber.setText(GenUtils.padLeftZeros(matchNum, 2));

                    loading_alert.dismiss();

                    dialog.show();
                }
            });
        } catch (Exception e) {
            Log.d("QRGen", "Something went wrong while generating a QR Code.");
        }
    }
}