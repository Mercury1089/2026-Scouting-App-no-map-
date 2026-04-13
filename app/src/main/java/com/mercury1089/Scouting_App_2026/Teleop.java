package com.mercury1089.Scouting_App_2026;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
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

import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.mercury1089.Scouting_App_2026.listeners.UpdateListener;

import java.util.LinkedHashMap;

public class Teleop extends Fragment implements UpdateListener {

    private static final String TAG = "Teleop Fragment";

    private int teleopSnapshotCount = 0;
    private LinkedHashMap<String, String> setupHashMap;
    private LinkedHashMap<String, String> teleopHashMap;

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

    // Other controls
    private MaterialSwitch noShowSwitch;
    private Button saveButton;
    private Button resetButton;
    private Button nextButtonEndGame;

    // Timer & animation
    private TextView timerID;
    private TextView secondsRemaining;
    private TextView endgameWarning;
    private ImageView topEdgeBar, bottomEdgeBar, leftEdgeBar, rightEdgeBar;

    private static CountDownTimer timer;
    private boolean firstTime = true;
    private boolean running = true;
    private MatchActivity context;

    // Running counts
    private int ferryingCount   = 0;
    private int scoredCount     = 0;
    private int missedCount     = 0;
    private long secondsLeft    = 140;

    public static Teleop newInstance() {
        Teleop fragment = new Teleop();
        fragment.setArguments(new Bundle());
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = (MatchActivity) getActivity();
        try {
            return inflater.inflate(R.layout.screen_teleop, container, false);
        } catch (InflateException e) {
            Log.d(TAG, "Inflate error: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.SETUP);
        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.TELEOP);
        setupHashMap = HashMapManager.getSetupHashMap();
        teleopHashMap = HashMapManager.getTeleopHashMap();

        // Link views
        ferryingCounterToggle             = getView().findViewById(R.id.FerryingCounterToggle);
        ferryingEditText                  = getView().findViewById(R.id.FerryingCounter);
        scoringCounterToggle              = getView().findViewById(R.id.ScoredCounterToggle);
        scoredEditText                    = getView().findViewById(R.id.ScoredCounter);
        missedCounterToggle               = getView().findViewById(R.id.MissedCounterToggle);
        missedEditText                    = getView().findViewById(R.id.MissedCounter);
        noShowSwitch                      = getView().findViewById(R.id.NoShowSwitch);

        timerID                           = getView().findViewById(R.id.IDTeleopSeconds);
        secondsRemaining                  = getView().findViewById(R.id.TeleopSeconds);
        endgameWarning                    = getView().findViewById(R.id.EndgameWarning);
        topEdgeBar                        = getView().findViewById(R.id.topEdgeBar);
        bottomEdgeBar                     = getView().findViewById(R.id.bottomEdgeBar);
        leftEdgeBar                       = getView().findViewById(R.id.leftEdgeBar);
        rightEdgeBar                      = getView().findViewById(R.id.rightEdgeBar);

        saveButton                        = getView().findViewById(R.id.SaveButton);
        resetButton                       = getView().findViewById(R.id.ResetButton);
        nextButtonEndGame                  = getView().findViewById(R.id.NextButtonEndGame);

        initializeSnapshots();
        loadTeleopData();
        setupCounterListeners();
        setupTextWatchers();
        setupButtonListeners();
        setupTimer();
    }

    // ─────────────────────────────────────────
    // SNAPSHOT SYSTEM
    // ─────────────────────────────────────────

    private void initializeSnapshots() {
        String snapshotsString = teleopHashMap.get("snapshots");
        if (snapshotsString == null || snapshotsString.isEmpty()) {
            snapshotBuilder = new StringBuilder();
            snapshotBuilder.append(SNAPSHOT_HEADER).append("\n");
        } else {
            snapshotBuilder = new StringBuilder(snapshotsString);
            if (!snapshotsString.endsWith("\n")) {
                snapshotBuilder.append("\n");
            }
        }
    }

    private void appendTeleopSnapshot() {
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
            /* auton: A_scor,A_miss,A_ferr,A_died,A_att,A_succ,A_loc, */ ",,,,,,," +
            /* teleop  T_scor,T_miss,T_ferr,T_died, */ "%d,%d,%d,%s," +
            /* endgame E_scor,E_miss,E_ferr,E_att,E_succ,E_loc, */ ",,,,,," +
            /* timestamp */ "%s\n",
            scouterName,
            teamNumber,
            matchNumber,
            //nuull auton values
            scoredCount,
            missedCount,
            ferryingCount,
            (noShowSwitch != null && noShowSwitch.isChecked()) ? "Y" : "N",
            //null endgame values
            timestamp);

        snapshotBuilder.append(snapshotLine);
        teleopSnapshotCount++;

        teleopHashMap.put("snapshots", snapshotBuilder.toString());
        teleopHashMap.put("TeleopSaveIndex", String.valueOf(teleopSnapshotCount));
        HashMapManager.putTeleopHashMap(teleopHashMap);
    }

    // ─────────────────────────────────────────
    // UI RESET
    // ─────────────────────────────────────────

    private void resetTeleopUI() {
        ferryingCount   = 0;
        scoredCount     = 0;
        missedCount     = 0;

        refreshDisplay(ferryingCounterToggle,   R.id.FerryingCounter,   ferryingCount);
        refreshDisplay(scoringCounterToggle,    R.id.ScoredCounter,     scoredCount);
        refreshDisplay(missedCounterToggle,     R.id.MissedCounter,     missedCount);

        // RobotFellOver (noShowSwitch) removed from reset to maintain persistence after save
    }

    // ─────────────────────────────────────────
    // COUNTER LISTENERS
    // ─────────────────────────────────────────

    private void setupCounterListeners() {
        setupFerryingListener();
        setupScoredListener();
        setupMissedListener();
        if (noShowSwitch != null) {
            noShowSwitch.setOnCheckedChangeListener((v, isChecked) -> {
                String state = isChecked ? "Y" : "N";
                teleopHashMap.put("RobotFellOver", state);
                HashMapManager.putTeleopHashMap(teleopHashMap);
                updateEnabledStates();
            });
        }
    }

    private void updateEnabledStates() {
        boolean robotDied = noShowSwitch != null && noShowSwitch.isChecked();
        boolean enabled = !robotDied;

        setGroupEnabled(ferryingCounterToggle, enabled);
        setGroupEnabled(scoringCounterToggle, enabled);
        setGroupEnabled(missedCounterToggle, enabled);

        if (ferryingEditText != null) ferryingEditText.setEnabled(enabled);
        if (scoredEditText != null) scoredEditText.setEnabled(enabled);
        if (missedEditText != null) missedEditText.setEnabled(enabled);
    }

    private void setGroupEnabled(RadioGroup group, boolean enabled) {
        if (group == null) return;
        for (int i = 0; i < group.getChildCount(); i++) {
            group.getChildAt(i).setEnabled(enabled);
        }
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
        group.clearCheck();
        if (group == ferryingCounterToggle)   setupFerryingListener();
        else if (group == scoringCounterToggle)    setupScoredListener();
        else if (group == missedCounterToggle)     setupMissedListener();
    }

    private void setupFerryingListener() {
        ferryingCounterToggle.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.FerryingCounter) return;
            ferryingCount = parseCount(ferryingEditText.getText().toString());
            // Ferrying in Teleop only has +/- 5 and 10 buttons
            ferryingCount = clamp(ferryingCount + deltaFor(id,
                    R.id.FerryingMinus10, R.id.FerryingMinus5, View.NO_ID,
                    View.NO_ID,    R.id.FerryingPlus5,  R.id.FerryingPlus10));
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

    // ─────────────────────────────────────────
    // BUTTON LISTENERS
    // ─────────────────────────────────────────

    private void setupButtonListeners() {
        if (saveButton != null) {
            saveButton.setOnClickListener(v -> {
                saveTeleopData();
                appendTeleopSnapshot();
                resetTeleopUI();
                Toast.makeText(context, "Teleop snapshot saved", Toast.LENGTH_SHORT).show();
            });
        }

        if (resetButton != null) {
            resetButton.setOnClickListener(v -> {
                resetTeleopUI();
                Toast.makeText(context, "Changes cancelled", Toast.LENGTH_SHORT).show();
            });
        }

        if (nextButtonEndGame != null) {
            nextButtonEndGame.setOnClickListener(v -> {
                saveTeleopData();
                appendTeleopSnapshot();
                resetTeleopUI();
                context.tabs.getTabAt(2).select();
            });
        }
    }

    // ─────────────────────────────────────────
    // TIMER
    // ─────────────────────────────────────────

    private void setupTimer() {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        timer = new CountDownTimer(110000, 1000) {
            @Override
            public void onTick(long ms) {
                if (secondsRemaining == null) return;
                long secs = ms / 1000;
                secondsLeft = Math.max(0, Math.min(secs, 110) - 1);
                
                long displaySecs = Math.min(secs, 110);
                long mins = displaySecs / 60;
                long rem  = displaySecs % 60;

                secondsRemaining.setText(mins + ":" + String.format("%02d", rem));

                if (!running) return;
                if (secs <= 8 && secs > 0) {
                    if (endgameWarning != null) {
                        endgameWarning.setVisibility(View.VISIBLE);
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
                nextButtonEndGame.setBackgroundColor(context.getResources().getColor(R.color.fire));
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
                    if (endgameWarning != null) {
                        endgameWarning.setVisibility(View.VISIBLE);
                        endgameWarning.setBackground(getResources().getDrawable(R.drawable.teleop_error));
                        endgameWarning.setTextColor(getResources().getColor(R.color.white));
                        endgameWarning.setText(getString(R.string.EndGameError));

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

    // ─────────────────────────────────────────
    // DATA PERSISTENCE
    // ─────────────────────────────────────────

    private void loadTeleopData() {
        ferryingCount   = parseCount(hm("Ferrying",   ""));
        scoredCount     = parseCount(hm("Scored",     ""));
        missedCount     = parseCount(hm("Missed",     ""));

        refreshDisplay(ferryingCounterToggle,   R.id.FerryingCounter,   ferryingCount);
        refreshDisplay(scoringCounterToggle,    R.id.ScoredCounter,     scoredCount);
        refreshDisplay(missedCounterToggle,     R.id.MissedCounter,     missedCount);

        // Persistence: Check Teleop map first, then Auton map
        String fellOver = hm("RobotFellOver", "");
        if (fellOver.isEmpty()) { 
            String autonFell = HashMapManager.getAutonHashMap().get("RobotFellOver");
            if (autonFell != null && !autonFell.isEmpty()) {
                fellOver = autonFell;
                // Propagate to current map
                teleopHashMap.put("RobotFellOver", fellOver);
                HashMapManager.putTeleopHashMap(teleopHashMap);
            } else {
                fellOver = "N";
            }
        }
        noShowSwitch.setChecked("Y".equals(fellOver));
        updateEnabledStates();
    }

    private void saveTeleopData() {
        teleopHashMap.put("Ferrying",          String.valueOf(ferryingCount));
        teleopHashMap.put("Scored",            String.valueOf(scoredCount));
        teleopHashMap.put("Missed",            String.valueOf(missedCount));
        teleopHashMap.put("RobotFellOver",     (noShowSwitch != null && noShowSwitch.isChecked()) ? "Y" : "N");
        teleopHashMap.put("Timestamp",         String.valueOf(secondsLeft));
        HashMapManager.putTeleopHashMap(teleopHashMap);
    }

    private String hm(String key, String def) {
        String v = teleopHashMap.get(key);
        return v != null ? v : def;
    }

    private int parseCount(String s) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return 0; }
    }

    // ─────────────────────────────────────────
    // LIFECYCLE
    // ─────────────────────────────────────────

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (this.isVisible() && getView() != null) {
            if (isVisibleToUser) {
                setupHashMap = HashMapManager.getSetupHashMap();
                teleopHashMap = HashMapManager.getTeleopHashMap();
                initializeSnapshots();
                loadTeleopData();
            } else {
                saveTeleopData();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        running = false;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void onUpdate() { loadTeleopData(); }
}
