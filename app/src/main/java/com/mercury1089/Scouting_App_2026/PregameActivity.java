package com.mercury1089.Scouting_App_2026;

import com.mercury1089.Scouting_App_2026.qr.QRRunnable;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.button.MaterialButton;


import java.util.LinkedHashMap;
import java.util.Objects;

public class PregameActivity extends AppCompatActivity {
    // Strategy was here
    //Set the default password in HashMapManager.setDefaultValues();
    String password;

    //variables that store elements of the screen for the output variables
    //Buttons
    private ImageButton settingsButton;
    private Button blueButton;
    private Button redButton;
    private Button clearButton;
    private MaterialButton startButton;

    //Text Fields
    private EditText scouterNameInput;
    private EditText matchNumberInput;
    private EditText teamNumberInput;
    private EditText firstAlliancePartnerInput;
    private EditText secondAlliancePartnerInput;
    private TextView startDirectionsToast;

    //Switches & Counters
    private MaterialSwitch noShowSwitch;
    private RadioGroup preloadedCargoToggle;
    private RadioButton preloadCargoDisplay;


    //HashMaps
    private LinkedHashMap<String, String> settingsHashMap;
    private LinkedHashMap<String, String> setupHashMap;
    private Dialog loading_alert;
    boolean isQRButton = false;

    //Max and Min of of Preloading
    private static final int PRELOAD_CARGO_MAX = 8;
    private static final int PRELOAD_CARGO_MIN = 0;
    //others
    private MediaPlayer rooster;

    // Unified listener for the preloaded cargo counter
    private final RadioGroup.OnCheckedChangeListener preLoadListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (checkedId == R.id.PreloadedCargoDisplay) {
                return;
            }
            int currentCargo = getPreloadCargoCount();
            if (checkedId == R.id.PreloadedCargoMinus) {
                if (currentCargo > PRELOAD_CARGO_MIN) {
                    currentCargo--;
                }
            } else if (checkedId == R.id.PreloadedCargoPlus) {
                if (currentCargo < PRELOAD_CARGO_MAX) {
                    currentCargo++;
                }
            }
            updatePreloadCargoCount(currentCargo);
            refreshPreloadDisplay();
            updateXMLObjects(false);
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_pregame);

        // Initialize views here
        scouterNameInput = findViewById(R.id.ScouterNameInput);
        matchNumberInput = findViewById(R.id.MatchNumberInput);
        teamNumberInput = findViewById(R.id.TeamNumberInput);
        firstAlliancePartnerInput = findViewById(R.id.FirstAlliancePartnerInput);
        secondAlliancePartnerInput = findViewById(R.id.SecondAlliancePartnerInput);

        //Alliance Color
        blueButton = findViewById(R.id.BlueButton);
        redButton = findViewById(R.id.RedButton);

        //Buttons
        clearButton = findViewById(R.id.ClearButton);
        startButton = findViewById(R.id.StartButton);
        settingsButton = findViewById(R.id.SettingsButton);

        //Preload Cargo Toggle (RadioGroup)
        preloadedCargoToggle = findViewById(R.id.CollectingCounterToggle);
        preloadCargoDisplay = findViewById(R.id.PreloadedCargoDisplay);

        //misc
        startDirectionsToast = findViewById(R.id.IDStartDirections);
        noShowSwitch = findViewById(R.id.NoShowSwitch);

        rooster = MediaPlayer.create(PregameActivity.this, R.raw.sound);

        // Make sure hash maps are not empty/null, then get HashMaps and set password for settings screen
        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.SETTINGS);
        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.SETUP);
        settingsHashMap = HashMapManager.getSettingsHashMap();
        setupHashMap = HashMapManager.getSetupHashMap();
        password = settingsHashMap.get("DefaultPassword");

        //setting group buttons to default state
        updateXMLObjects(true);

        scouterNameInput.addTextChangedListener(getTextWatcher("ScouterName"));
        matchNumberInput.addTextChangedListener(getTextWatcher("MatchNumber"));
        teamNumberInput.addTextChangedListener(getTextWatcher("TeamNumber"));
        firstAlliancePartnerInput.addTextChangedListener(getTextWatcher("AlliancePartner1"));
        secondAlliancePartnerInput.addTextChangedListener(getTextWatcher("AlliancePartner2"));

        noShowSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setupHashMap.put("NoShow", isChecked ? "Y" : "N");
                updateXMLObjects(false);
            }
        });

        //click methods
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] passwordData = HashMapManager.pullSettingsPassword(PregameActivity.this);
                final String password, requiredPassword;
                String tempPassword, tempRequired;
                try {
                    if (passwordData != null) {
                        tempPassword = passwordData[0];
                        tempRequired = passwordData[1];
                    } else {
                        tempPassword = PregameActivity.this.password;
                        tempRequired = "N";
                    }
                } catch (Exception e) {
                    tempPassword = PregameActivity.this.password;
                    tempRequired = "N";
                }

                password = tempPassword;
                requiredPassword = tempRequired;

                if (requiredPassword.equals("N")) {
                    HashMapManager.putSetupHashMap(setupHashMap);
                    Intent intent = new Intent(PregameActivity.this, SettingsActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }

                Dialog dialog = new Dialog(PregameActivity.this);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.popup_settings_password);

                TextView passwordField = dialog.findViewById(R.id.PasswordField);
                Button confirm = dialog.findViewById(R.id.ConfirmButton);
                Button cancel = dialog.findViewById(R.id.CancelButton);
                ImageView topEdgeBar = dialog.findViewById(R.id.topEdgeBar);
                ImageView bottomEdgeBar = dialog.findViewById(R.id.bottomEdgeBar);
                ImageView leftEdgeBar = dialog.findViewById(R.id.leftEdgeBar);
                ImageView rightEdgeBar = dialog.findViewById(R.id.rightEdgeBar);

                dialog.show();

                passwordField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                            //do what you want on the press of 'done'
                            confirm.performClick();
                        }
                        return false;
                    }
                });

                confirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String savedPassword = !password.equals("") ? password : PregameActivity.this.password;
                        if (passwordField.getText().toString().equals(savedPassword)) {
                            HashMapManager.putSetupHashMap(setupHashMap);
                            Intent intent = new Intent(PregameActivity.this, SettingsActivity.class);
                            startActivity(intent);
                            dialog.dismiss();
                            finish();
                        } else {
                            Toast.makeText(PregameActivity.this, "Incorrect Password", Toast.LENGTH_SHORT).show();

                            ObjectAnimator topEdgeLighter = ObjectAnimator.ofFloat(topEdgeBar, View.ALPHA, 0.0f, 1.0f);
                            ObjectAnimator bottomEdgeLighter = ObjectAnimator.ofFloat(bottomEdgeBar, View.ALPHA, 0.0f, 1.0f);
                            ObjectAnimator rightEdgeLighter = ObjectAnimator.ofFloat(rightEdgeBar, View.ALPHA, 0.0f, 1.0f);
                            ObjectAnimator leftEdgeLighter = ObjectAnimator.ofFloat(leftEdgeBar, View.ALPHA, 0.0f, 1.0f);

                            topEdgeLighter.setDuration(500);
                            bottomEdgeLighter.setDuration(500);
                            leftEdgeLighter.setDuration(500);
                            rightEdgeLighter.setDuration(500);

                            topEdgeLighter.setRepeatMode(ObjectAnimator.REVERSE);
                            topEdgeLighter.setRepeatCount(1);
                            bottomEdgeLighter.setRepeatMode(ObjectAnimator.REVERSE);
                            bottomEdgeLighter.setRepeatCount(1);
                            leftEdgeLighter.setRepeatMode(ObjectAnimator.REVERSE);
                            leftEdgeLighter.setRepeatCount(1);
                            rightEdgeLighter.setRepeatMode(ObjectAnimator.REVERSE);
                            rightEdgeLighter.setRepeatCount(1);

                            AnimatorSet animatorSet = new AnimatorSet();
                            animatorSet.playTogether(topEdgeLighter, bottomEdgeLighter, leftEdgeLighter, rightEdgeLighter);
                            animatorSet.start();
                        }
                    }
                });

                cancel.setOnClickListener((new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                }));
            }
        });

        blueButton.setOnClickListener(v -> {
            setupHashMap.put("AllianceColor", Objects.equals(setupHashMap.get("AllianceColor"), "Blue") ? "" : "Blue");
            updateXMLObjects(false);
        });

        redButton.setOnClickListener(v -> {
            setupHashMap.put("AllianceColor", Objects.equals(setupHashMap.get("AllianceColor"), "Red") ? "" : "Red");
            updateXMLObjects(false);
        });

        preloadedCargoToggle.setOnCheckedChangeListener(preLoadListener);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isQRButton) {
                    Dialog dialog = new Dialog(PregameActivity.this);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.setContentView(R.layout.popup_generate_qrcode_confirm);

                    Button confirmBtn = dialog.findViewById(R.id.GenerateQRButton);
                    Button cancelBtn = dialog.findViewById(R.id.CancelConfirm);

                    dialog.show();

                    confirmBtn.setOnClickListener(view -> {
                        HashMapManager.putSetupHashMap(setupHashMap);
                        dialog.dismiss();
                        
                        loading_alert = new Dialog(PregameActivity.this);
                        loading_alert.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        loading_alert.setContentView(R.layout.screen_qr_loading);
                        loading_alert.setCancelable(false);
                        loading_alert.show();

                        QRRunnable runnable = new QRRunnable(PregameActivity.this, loading_alert);
                        new Thread(runnable).start();
                    });

                    cancelBtn.setOnClickListener(view -> dialog.dismiss());
                } else {
                    HashMapManager.putSetupHashMap(setupHashMap);
                    if (scouterNameInput.getText().toString().equals("Mercury") &&
                        matchNumberInput.getText().toString().equals("1") &&
                        teamNumberInput.getText().toString().equals("0") &&
                        firstAlliancePartnerInput.getText().toString().equals("8") &&
                        secondAlliancePartnerInput.getText().toString().equals("9")) {
                            settingsHashMap.put("NothingToSeeHere", "1");
                            HashMapManager.setDefaultValues(HashMapManager.HASH.SETUP);
                            setupHashMap = HashMapManager.getSetupHashMap();

                            updateXMLObjects(true);
                            return;
                    } else if (scouterNameInput.getText().toString().equals("admin") &&
                            matchNumberInput.getText().toString().equals("1") &&
                            teamNumberInput.getText().toString().equals("0") &&
                            firstAlliancePartnerInput.getText().toString().equals("8") &&
                            secondAlliancePartnerInput.getText().toString().equals("9")) {
                                HashMapManager.saveSettingsPassword(new String[]{"", "N"}, PregameActivity.this);
                                HashMapManager.setDefaultValues(HashMapManager.HASH.SETUP);
                                setupHashMap = HashMapManager.getSetupHashMap();

                                updateXMLObjects(true);
                                return;
                    } else if (Objects.equals(settingsHashMap.get("NothingToSeeHere"), "1")) {
                        rooster.start();
                    } else if (teamNumberInput.getText().toString().equals(firstAlliancePartnerInput.getText().toString()) || teamNumberInput.getText().toString().equals(secondAlliancePartnerInput.getText().toString())) {
                        Toast.makeText(PregameActivity.this, "A team cannot be its own partner.", Toast.LENGTH_SHORT).show();
                        setupHashMap.put("TeamNumber", "");
                        setupHashMap.put("AlliancePartner1", "");
                        setupHashMap.put("AlliancePartner2", "");
                        teamNumberInput.requestFocus();
                        updateXMLObjects(true);
                        return;

                    }
                    Intent intent = new Intent(PregameActivity.this, MatchActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });

        clearButton.setOnClickListener(v -> {
            Dialog dialog = new Dialog(PregameActivity.this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.popup_clear_confirm);

            Button clearConfirm = dialog.findViewById(R.id.ClearConfirm);
            Button cancelConfirm = dialog.findViewById(R.id.CancelConfirm);

            dialog.show();

            cancelConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });

            clearConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                    HashMapManager.setDefaultValues(HashMapManager.HASH.SETUP);
                    setupHashMap = HashMapManager.getSetupHashMap();
                    updateXMLObjects(true);
                }
            });
        });
    }

    private TextWatcher getTextWatcher(String key) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                setupHashMap.put(key, s.toString());
                updateXMLObjects(false);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
    }

    private int getPreloadCargoCount() {
        try {
            String cargoStr = setupHashMap.get("PreloadedCargo");
            return Integer.parseInt(cargoStr != null ? cargoStr : "0");
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void updatePreloadCargoCount(int count) {
        setupHashMap.put("PreloadedCargo", String.valueOf(count));
        updatePreloadCargoDisplay(count);
    }

    private void updatePreloadCargoDisplay(int count) {
        preloadCargoDisplay.setText(String.format("%03d", count));
    }

    private void refreshPreloadDisplay() {
        preloadedCargoToggle.setOnCheckedChangeListener(null);
        preloadedCargoToggle.check(R.id.PreloadedCargoDisplay);
        preloadedCargoToggle.setOnCheckedChangeListener(preLoadListener);
    }

    private boolean readyToStart() {
        String allianceColor = setupHashMap.get("AllianceColor");
        return scouterNameInput.getText().length() > 0 &&
                matchNumberInput.getText().length() > 0 &&
                teamNumberInput.getText().length() > 0 &&
                firstAlliancePartnerInput.getText().length() > 0 &&
                secondAlliancePartnerInput.getText().length() > 0 &&
                allianceColor != null && !allianceColor.isEmpty() &&
                (Objects.equals(setupHashMap.get("NoShow"), "Y") || Objects.equals(setupHashMap.get("NoShow"), "N"));
    }

    /*
    - Check to see if there's any values that need to be cleared
    - (if nothing is filled out, clear button should be disabled)
     */
    private boolean canClearInputs() {
        String allianceColor = setupHashMap.get("AllianceColor");
        return scouterNameInput.getText().length() > 0 ||
                matchNumberInput.getText().length() > 0 ||
                teamNumberInput.getText().length() > 0 ||
                noShowSwitch.isChecked() ||
                firstAlliancePartnerInput.getText().length() > 0 ||
                secondAlliancePartnerInput.getText().length() > 0 ||
                (allianceColor != null && !allianceColor.isEmpty()) || 
                getPreloadCargoCount() > 0;
    }

    /*
    - Big complicated looking function, so let's break it down
        - This is called on most events (so in all the View EventListeners)
        - It updates hashmaps and the visual appearance of Views
     */
    private void updateXMLObjects(boolean updateText) {
        /*
        - updateText should only be true if you want to reset the basic info fields to the stored hashmap values
            - e.g. if you're returning from SettingsActivity or if you used the "Clear" button
         */
        if (updateText) {
            scouterNameInput.setText(setupHashMap.get("ScouterName"));
            matchNumberInput.setText(setupHashMap.get("MatchNumber"));
            teamNumberInput.setText(setupHashMap.get("TeamNumber"));
            firstAlliancePartnerInput.setText(setupHashMap.get("AlliancePartner1"));
            secondAlliancePartnerInput.setText(setupHashMap.get("AlliancePartner2"));
            updatePreloadCargoDisplay(getPreloadCargoCount());
            refreshPreloadDisplay();
        }

        String allianceColor = setupHashMap.get("AllianceColor");
        blueButton.setSelected(Objects.equals(allianceColor, "Blue"));
        redButton.setSelected(Objects.equals(allianceColor, "Red"));

        if (Objects.equals(setupHashMap.get("NoShow"), "Y")) {
            noShowSwitch.setChecked(true);
            startButton.setIconResource(R.drawable.qr_states);
            startButton.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
            startButton.setIconSize(20);
            startButton.setText(R.string.GenerateQRCode);
            startButton.setSelected(true);
            isQRButton = true;
        } else {
            noShowSwitch.setChecked(false);
            startButton.setIconResource(R.drawable.start_button_symbol_states);
            startButton.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_END);
            startButton.setIconSize(26);
            startButton.setText(R.string.Start);
            startButton.setSelected(true);
            isQRButton = false;
        }

        boolean readyToStart = readyToStart();
        startButton.setEnabled(readyToStart);
        startDirectionsToast.setEnabled(readyToStart && !isQRButton);
        clearButton.setEnabled(canClearInputs());
    }

    @Override protected void onResume() {
        super.onResume();
        updateXMLObjects(true); }

    @Override public boolean dispatchTouchEvent(MotionEvent ev) {
        if (getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        return super.dispatchTouchEvent(ev);
    }
}
