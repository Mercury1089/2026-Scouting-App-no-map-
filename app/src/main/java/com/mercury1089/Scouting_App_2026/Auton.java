package com.mercury1089.Scouting_App_2026;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.mercury1089.Scouting_App_2026.listeners.UpdateListener;
import com.mercury1089.Scouting_App_2026.utils.GenUtils;

import java.util.LinkedHashMap;
import java.util.Objects;

public class Auton extends Fragment implements UpdateListener {

    private static final String TAG = "Auton Fragment";

    private int autonSnapshotCount = 0;
    private LinkedHashMap<String, String> setupHashMap;
    private LinkedHashMap<String, String> autonHashMap;

    // Snapshot System (CSV format)
    private StringBuilder snapshotBuilder;
    private static final String SNAPSHOT_HEADER =
            "scouterName,teamNumber,matchNumber,A_scor,A_miss,A_ferr,A_died,A_att,A_succ,A_loc,T_scor,T_miss,T_ferr,T_died,E_scor,E_miss,E_ferr,E_att,E_succ,E_loc,timestamp";

    // Counter toggles
    private RadioGroup ferryingCounterToggle;
    private RadioGroup scoringCounterToggle;
    private RadioGroup missedCounterToggle;

    // EditText display fields
    private EditText ferryingEditText;
    private EditText scoredEditText;
    private EditText missedEditText;

    // Climbing toggles
    private RadioGroup attemptedClimbToggle;
    private RadioGroup successfulClimbedToggle;
    private RadioGroup successfullyClimbedLocationToggle;
    private TextView locationText;
    private TextView successfulText;


    // Other controls
    private MaterialSwitch noShowSwitch;
    private Button saveButton;
    private Button resetButton;
    private Button nextButtonAuton;

    // Timer & animation
    private TextView timerID;
    private TextView secondsRemaining;
    private TextView teleopWarning;
    private ImageView topEdgeBar, bottomEdgeBar, leftEdgeBar, rightEdgeBar;

    private static CountDownTimer timer;
    private boolean firstTime = true;
    private boolean running = true;
    private MatchActivity context;

    // Running counts
    private int ferryingCount   = 0;
    private int scoredCount     = 0;
    private int missedCount     = 0;
    private long secondsLeft    = 20;

    public static Auton newInstance() {
        Auton fragment = new Auton();
        fragment.setArguments(new Bundle());
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = (MatchActivity) getActivity();
        try {
            return inflater.inflate(R.layout.screen_auton, container, false);
        } catch (InflateException e) {
            Log.d(TAG, "Inflate error: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.SETUP);
        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.AUTON);
        setupHashMap = HashMapManager.getSetupHashMap();
        autonHashMap = HashMapManager.getAutonHashMap();

        // Link views
        ferryingCounterToggle             = getView().findViewById(R.id.FerryingCounterToggle);
        ferryingEditText                  = getView().findViewById(R.id.FerryingCounter);
        scoringCounterToggle              = getView().findViewById(R.id.ScoredCounterToggle);
        scoredEditText                    = getView().findViewById(R.id.ScoredCounter);
        missedCounterToggle               = getView().findViewById(R.id.MissedCounterToggle);
        missedEditText                    = getView().findViewById(R.id.MissedCounter);
        attemptedClimbToggle              = getView().findViewById(R.id.AttemptedClimbToggle);
        successfulText                    = getView().findViewById(R.id.SuccessTitle);
        successfulClimbedToggle           = getView().findViewById(R.id.SuccessfulClimbed);
        locationText                     = getView().findViewById(R.id.locationTitle);
        successfullyClimbedLocationToggle = getView().findViewById(R.id.SuccessfullyClimbedLocation);
        noShowSwitch                      = getView().findViewById(R.id.NoShowSwitch);
        saveButton                        = getView().findViewById(R.id.SaveButton);
        resetButton                       = getView().findViewById(R.id.ResetButton);
        nextButtonAuton                   = getView().findViewById(R.id.NextTeleopButton);
        timerID                           = getView().findViewById(R.id.IDAutonSeconds);
        secondsRemaining                  = getView().findViewById(R.id.AutonSeconds);
        teleopWarning                     = getView().findViewById(R.id.TeleopWarning);
        topEdgeBar                        = getView().findViewById(R.id.topEdgeBar);
        bottomEdgeBar                     = getView().findViewById(R.id.bottomEdgeBar);
        leftEdgeBar                       = getView().findViewById(R.id.leftEdgeBar);
        rightEdgeBar                      = getView().findViewById(R.id.rightEdgeBar);

        initializeSnapshots();
        loadAutonData();
        setupCounterListeners();
        setupTextWatchers();
        setupCascadingListeners();
        setupButtonListeners();
        setupTimer();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            GenUtils.setFullscreen(getActivity());
        }
    }

    // ─────────────────────────────────────────
    // SNAPSHOT SYSTEM
    // ─────────────────────────────────────────

    private void initializeSnapshots() {
        snapshotBuilder = new StringBuilder();
        snapshotBuilder.append(SNAPSHOT_HEADER).append("\n");
        autonSnapshotCount = 0;
    }

    private void loadAutonData() {
        ferryingCount   = parseCount(hm("Ferrying",   ""));
        scoredCount     = parseCount(hm("Scored",     ""));
        missedCount     = parseCount(hm("Missed",     ""));

        refreshDisplay(ferryingCounterToggle,   R.id.FerryingCounter,   ferryingCount);
        refreshDisplay(scoringCounterToggle,    R.id.ScoredCounter,     scoredCount);
        refreshDisplay(missedCounterToggle,     R.id.MissedCounter,     missedCount);

        selectByText(attemptedClimbToggle,              hm("AttemptedClimb",    ""));
        selectByText(successfulClimbedToggle,           hm("SuccessfulClimbed", ""));
        selectByText(successfullyClimbedLocationToggle, hm("ClimbLocation",     ""));

        noShowSwitch.setChecked("Y".equals(hm("RobotFellOver", "N")));
        updateClimbStates();
    }

    private void saveAutonData() {
        autonHashMap.put("Ferrying",          String.valueOf(ferryingCount));
        autonHashMap.put("Scored",            String.valueOf(scoredCount));
        autonHashMap.put("Missed",            String.valueOf(missedCount));
        autonHashMap.put("AttemptedClimb",    getSelectedText(attemptedClimbToggle,              ""));
        autonHashMap.put("SuccessfulClimbed", getSelectedText(successfulClimbedToggle,           ""));
        autonHashMap.put("ClimbLocation",     getSelectedText(successfullyClimbedLocationToggle, ""));
        autonHashMap.put("RobotFellOver",     noShowSwitch.isChecked() ? "Y" : "N");
        autonHashMap.put("Timestamp",         String.valueOf(secondsLeft + 140));
        HashMapManager.putAutonHashMap(autonHashMap);
    }

    private void appendAutonSnapshot() {
        if (snapshotBuilder == null) {
            initializeSnapshots();
        }

        String teamNumber  = setupHashMap.get("TeamNumber");
        if (teamNumber == null) teamNumber = "";
        String scouterName = setupHashMap.get("ScouterName");
        if (scouterName == null) scouterName = "";
        String matchNumber = setupHashMap.get("MatchNumber");
        if (matchNumber == null) matchNumber = "";

        String timestamp = String.valueOf(secondsLeft);

        String snapshotLine = String.format(
            /* setup: scouter, team, match */ "%s,%s,%s," +
            /* auton: A_scor,A_miss,A_ferr,A_died,A_att,A_succ,A_loc, */ "%d,%d,%d,%s,%s,%s,%s," +
            /* teleop  T_scor,T_miss,T_ferr,T_died, */ ",,,," +
            /* endgame E_scor,E_miss,E_ferr,E_att,E_succ,E_loc, */ ",,,,,," +
            /* timestamp */ "%s\n",
            scouterName,
            teamNumber,
            matchNumber,
            scoredCount,
            missedCount,
            ferryingCount,
            (noShowSwitch != null && noShowSwitch.isChecked()) ? "Y" : "N",
            attemptedClimbToggle,
            successfulClimbedToggle,
            successfullyClimbedLocationToggle,
            //null teleop values
            //null endgame values
            timestamp);

        snapshotBuilder.append(snapshotLine);
        autonSnapshotCount++;

        autonHashMap.put("snapshots", snapshotBuilder.toString());
        autonHashMap.put("AutonSaveIndex", String.valueOf(autonSnapshotCount));
        HashMapManager.putAutonHashMap(autonHashMap);
    }

    private void resetAutonUI() {
        ferryingCount = 0;
        scoredCount = 0;
        missedCount = 0;
        refreshDisplay(ferryingCounterToggle,   R.id.FerryingCounter,   ferryingCount);
        refreshDisplay(scoringCounterToggle,    R.id.ScoredCounter,     scoredCount);
        refreshDisplay(missedCounterToggle,     R.id.MissedCounter,     missedCount);
        attemptedClimbToggle.clearCheck();
        successfulClimbedToggle.clearCheck();
        successfullyClimbedLocationToggle.clearCheck();
        noShowSwitch.setChecked(false);
        updateClimbStates();
    }

    private String hm(String key, String def) {
        String v = autonHashMap.get(key);
        return v != null ? v : def;
    }

    private int parseCount(String s) {
        try { return Integer.parseInt(s); }
        catch (Exception e) { return 0; }
    }

    private void setupCounterListeners() {
        setupFerryingListener();
        setupScoredListener();
        setupMissedListener();
    }

    private void setupTextWatchers() {
        ferryingEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                ferryingCount = parseCount(s.toString());
            }
        });
        scoredEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                scoredCount = parseCount(s.toString());
            }
        });
        missedEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                missedCount = parseCount(s.toString());
            }
        });
    }
    private void setupCascadingListeners() {
        attemptedClimbToggle.setOnCheckedChangeListener((g, id)    -> updateClimbStates());
        successfulClimbedToggle.setOnCheckedChangeListener((g, id) -> updateClimbStates());
        updateClimbStates();
    }

    private void updateClimbStates() {
        int attemptedId  = attemptedClimbToggle.getCheckedRadioButtonId();
        boolean attempted  = attemptedId  != -1 && attemptedId  != R.id.AttemptedNo;
        successfulText.setEnabled(attempted);
        setGroupEnabled(successfulClimbedToggle, attempted);

        if (!attempted) {successfulClimbedToggle.clearCheck();}

        int successfulId = successfulClimbedToggle.getCheckedRadioButtonId();
        boolean successful = successfulId != -1 && successfulId != R.id.DidNotAttempt;
        locationText.setEnabled(attempted && successful);
        setGroupEnabled(successfullyClimbedLocationToggle, attempted && successful);

        if (!attempted || !successful) { successfullyClimbedLocationToggle.clearCheck();}

    }

    private int deltaFor(int id,
                         int m10, int m5, int m1,
                         int p1,  int p5, int p10) {
        if (id == m10) return -10;
        if (id == m5)  return -5;
        if (id == m1)  return -1;
        if (id == p1)  return +1;
        if (id == p5)  return +5;
        if (id == p10) return +10;
        return 0;
    }

    private int clamp(int value) {
        return Math.max(0, value);
    }

    private void refreshDisplay(RadioGroup group, int displayId, int count) {
        EditText display = group.findViewById(displayId);
        if (display != null) {
            display.setText(String.valueOf(count));
        }
        group.setOnCheckedChangeListener(null);
        group.check(displayId);
        if (group == ferryingCounterToggle)   setupFerryingListener();
        else if (group == scoringCounterToggle)    setupScoredListener();
        else if (group == missedCounterToggle)     setupMissedListener();
    }

    private void setupFerryingListener() {
        ferryingCounterToggle.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.FerryingCounter) return;
            ferryingCount = parseCount(ferryingEditText.getText().toString());
            ferryingCount = clamp(ferryingCount + deltaFor(id,
                    R.id.FerryingMinus10, R.id.FerryingMinus5, R.id.FerryingMinus,
                    R.id.FerryingPlus,    R.id.FerryingPlus5,  R.id.FerryingPlus10));
            refreshDisplay(ferryingCounterToggle, R.id.FerryingCounter, ferryingCount);
        });
    }

    private void setupScoredListener() {
        scoringCounterToggle.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.ScoredCounter) return;
            scoredCount = parseCount(scoredEditText.getText().toString());
            scoredCount = clamp(scoredCount + deltaFor(id,
                    R.id.ScoredMinus10, R.id.ScoredMinus5, R.id.ScoredMinus,
                    R.id.ScoredPlus,    R.id.ScoredPlus5,  R.id.ScoredPlus10));
            refreshDisplay(scoringCounterToggle, R.id.ScoredCounter, scoredCount);
        });
    }

    private void setupMissedListener() {
        missedCounterToggle.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.MissedCounter) return;
            missedCount = parseCount(missedEditText.getText().toString());
            missedCount = clamp(missedCount + deltaFor(id,
                    R.id.MissedMinus10, R.id.MissedMinus5, R.id.MissedMinus,
                    R.id.MissedPlus,    R.id.MissedPlus5,  R.id.MissedPlus10));
            refreshDisplay(missedCounterToggle, R.id.MissedCounter, missedCount);
        });
    }

    private void setupButtonListeners() {
        if (saveButton != null) {
            saveButton.setOnClickListener(v -> {
                saveAutonData();
                appendAutonSnapshot();
                resetAutonUI();
                Toast.makeText(context, "Auton snapshot saved", Toast.LENGTH_SHORT).show();
            });
        }

        if (resetButton != null) {
            resetButton.setOnClickListener(v -> {
                resetAutonUI();
                Toast.makeText(context, "Changes cancelled", Toast.LENGTH_SHORT).show();
            });
        }

        if (nextButtonAuton != null) {
            nextButtonAuton.setOnClickListener(v -> {
                saveAutonData();
                appendAutonSnapshot();
                resetAutonUI();
                context.tabs.getTabAt(1).select();
            });
        }
    }

    private void setupTimer() {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        timer = new CountDownTimer(20000, 1000) {
            @Override
            public void onTick(long ms) {
                if (secondsRemaining == null) return;
                long secs = ms / 1000;
                secondsLeft = Math.min(secs, 20);
                long mins = secondsLeft / 60;
                long rem  = secondsLeft % 60;

                secondsRemaining.setText(mins + ":" + String.format("%02d", rem));

                if (!running) return;

                if (secs <= 8 && secs > 0) {
                    if (teleopWarning != null) {
                        teleopWarning.setVisibility(View.VISIBLE);
                    }
                    if (timerID != null) {
                        try {
                            timerID.setTextColor(getResources().getColor(R.color.banana));
                            timerID.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.timer_yellow, 0, 0, 0);
                        } catch (Exception e) {
                            Log.e(TAG, "Timer warning color error: " + e.getMessage());
                        }
                    }
                    if (vibrator != null) vibrator.vibrate(500);
                    try {
                        pulseEdgeBars();
                    } catch (Exception e) {
                        Log.e(TAG, "Pulse edge bars error: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFinish() {
                secondsLeft = 0;
                nextButtonAuton.setBackgroundColor(context.getResources().getColor(R.color.fire));
                topEdgeBar.setBackgroundColor(context.getResources().getColor(R.color.fire));
                bottomEdgeBar.setBackgroundColor(context.getResources().getColor(R.color.fire));
                leftEdgeBar.setBackgroundColor(context.getResources().getColor(R.color.fire));
                rightEdgeBar.setBackgroundColor(context.getResources().getColor(R.color.fire));

                if (!running) return;
                try {
                    if (secondsRemaining != null) {
                        secondsRemaining.setText("00");
                    }

                    if (timerID != null) {
                        timerID.setTextColor(context.getResources().getColor(R.color.fire));
                        timerID.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.timer_red, 0, 0, 0);

                    }
                    if (teleopWarning != null) {
                        teleopWarning.setVisibility(View.VISIBLE);
                        teleopWarning.setBackground(getResources().getDrawable(R.drawable.teleop_error));
                        teleopWarning.setTextColor(getResources().getColor(R.color.white));
                        teleopWarning.setText(getString(R.string.TeleopError));

                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in timer finish: " + e.getMessage());
                }
            }
        };

        if (firstTime) {
            firstTime = false;
            timer.start();
        }
    }

    private void pulseEdgeBars() {
        for (ImageView bar : new ImageView[]{topEdgeBar, bottomEdgeBar, leftEdgeBar, rightEdgeBar}) {
            if (bar != null) {
                ObjectAnimator pulse = ObjectAnimator.ofFloat(bar, "alpha", 1f, 0.2f, 1f);
                pulse.setDuration(500);
                pulse.start();
            }
        }
    }

    private String getSelectedText(RadioGroup group, String defaultVal) {
        int id = group.getCheckedRadioButtonId();
        if (id == -1) return defaultVal;
        RadioButton btn = group.findViewById(id);
        return btn != null ? btn.getText().toString().trim() : defaultVal;
    }

    private void selectByText(RadioGroup group, String value) {
        if (group == null) return;
        group.clearCheck();
        if (value == null || value.isEmpty()) return;
        for (int i = 0; i < group.getChildCount(); i++) {
            View v = group.getChildAt(i);
            if (v instanceof RadioButton) {
                RadioButton btn = (RadioButton) v;
                if (btn.getText().toString().trim().equalsIgnoreCase(value)) {
                    group.check(btn.getId());
                    return;
                }
            }
        }
    }

    private void setGroupEnabled(RadioGroup group, boolean enabled) {
        if (group == null) return;
        for (int i = 0; i < group.getChildCount(); i++)
            group.getChildAt(i).setEnabled(enabled);
    }

    @Override
    public void onUpdate() {
        // Implementation of UpdateListener
    }
}
