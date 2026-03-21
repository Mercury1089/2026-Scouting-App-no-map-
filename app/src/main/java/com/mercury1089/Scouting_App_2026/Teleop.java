package com.mercury1089.Scouting_App_2026;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
            "teamNumber,scouterName,collecting,ferrying,scored,missed,attemptedClimb,successfulClimbed,climbLocation,noShow";

    // Counter toggles
    private RadioGroup collectingCounterToggle;
    private RadioGroup ferryingCounterToggle;
    private RadioGroup scoringCounterToggle;   // FIX 2
    private RadioGroup missedCounterToggle;

    // Climbing section
    private RadioGroup attemptedClimbToggle;
    private RadioGroup successfulClimbedToggle;
    private RadioGroup successfullyClimbedLocationToggle;

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
    private int collectingCount = 0;
    private int ferryingCount   = 0;
    private int scoredCount     = 0;   // FIX 2
    private int missedCount     = 0;

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
        collectingCounterToggle           = getView().findViewById(R.id.CollectingCounterToggle);
        ferryingCounterToggle             = getView().findViewById(R.id.FerryingCounterToggle);
        scoringCounterToggle              = getView().findViewById(R.id.ScoredCounterToggle);   // FIX 2
        missedCounterToggle               = getView().findViewById(R.id.MissedCounterToggle);
        attemptedClimbToggle              = getView().findViewById(R.id.AttemptedClimbToggle);
        successfulClimbedToggle           = getView().findViewById(R.id.SuccessfulClimbed);
        successfullyClimbedLocationToggle = getView().findViewById(R.id.SuccessfullyClimbedLocation);
        noShowSwitch                      = getView().findViewById(R.id.NoShowSwitch);
        saveButton                        = getView().findViewById(R.id.SaveButton);
        resetButton                       = getView().findViewById(R.id.ResetButton);
        nextButtonEndGame                  = getView().findViewById(R.id.NextButtonEndGame);   // FIX 10
        timerID                           = getView().findViewById(R.id.IDTeleopSeconds1);
        secondsRemaining                  = getView().findViewById(R.id.TeleopSeconds);
        endgameWarning                    = getView().findViewById(R.id.endgameWarning);
        topEdgeBar                        = getView().findViewById(R.id.topEdgeBar);
        bottomEdgeBar                     = getView().findViewById(R.id.bottomEdgeBar);
        leftEdgeBar                       = getView().findViewById(R.id.leftEdgeBar);
        rightEdgeBar                      = getView().findViewById(R.id.rightEdgeBar);

        initializeSnapshots();
        loadTeleopData();
        setupCounterListeners();
        setupCascadingListeners();
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

        // FIX 4: pull teamNumber and scouterName from setupHashMap; removed startLevel/stopLevel
        String teamNumber  = setupHashMap.get("TeamNumber");
        if (teamNumber == null) teamNumber = "";
        String scouterName = setupHashMap.get("ScouterName");
        if (scouterName == null) scouterName = "";

        String snapshotLine = String.format("%s,%s,%d,%d,%d,%d,%s,%s,%s,%s\n",
                teamNumber,
                scouterName,
                collectingCount,
                ferryingCount,
                scoredCount,
                missedCount,
                getSelectedText(attemptedClimbToggle,              "DID NOT ATTEMPT"),
                getSelectedText(successfulClimbedToggle,           "None"),
                getSelectedText(successfullyClimbedLocationToggle, "LEFT"),
                (noShowSwitch != null && noShowSwitch.isChecked()) ? "1" : "0");

        snapshotBuilder.append(snapshotLine);
        teleopSnapshotCount++;

        teleopHashMap.put("snapshots", snapshotBuilder.toString());
        teleopHashMap.put("TeleopSaveIndex", String.valueOf(teleopSnapshotCount));
        HashMapManager.putTeleopHashMap(teleopHashMap);
    }

    private int countSnapshots() {
        if (snapshotBuilder == null) return 0;
        String content = snapshotBuilder.toString();
        int count = 0;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') count++;
        }
        return count - 1; // subtract header line
    }

    public String getSnapshotsAsString() {
        return snapshotBuilder != null ? snapshotBuilder.toString() : "";
    }

    public String exportSnapshotsCSV() {
        return getSnapshotsAsString();
    }

    // ─────────────────────────────────────────
    // UI RESET
    // ─────────────────────────────────────────

    private void resetTeleopUI() {
        collectingCount = 0;
        ferryingCount   = 0;
        scoredCount     = 0;   // FIX 5
        missedCount     = 0;

        refreshDisplay(collectingCounterToggle, R.id.CollectingCounter, collectingCount);
        refreshDisplay(ferryingCounterToggle,   R.id.FerryingCounter,   ferryingCount);
        refreshDisplay(scoringCounterToggle,    R.id.ScoredCounter,     scoredCount);   // FIX 5
        refreshDisplay(missedCounterToggle,     R.id.MissedCounter,     missedCount);

        // FIX 5: removed startLevelToggle/stopLevelToggle resets — not in XML
        if (attemptedClimbToggle != null && attemptedClimbToggle.getChildCount() > 0) {
            attemptedClimbToggle.check(((RadioButton) attemptedClimbToggle.getChildAt(0)).getId());
        }
        if (successfulClimbedToggle != null && successfulClimbedToggle.getChildCount() > 0) {
            successfulClimbedToggle.check(((RadioButton) successfulClimbedToggle.getChildAt(0)).getId());
        }
        if (successfullyClimbedLocationToggle != null && successfullyClimbedLocationToggle.getChildCount() > 0) {
            successfullyClimbedLocationToggle.check(((RadioButton) successfullyClimbedLocationToggle.getChildAt(0)).getId());
        }
        if (noShowSwitch != null) {
            noShowSwitch.setChecked(false);
        }

        updateClimbStates();
    }

    // ─────────────────────────────────────────
    // COUNTER LISTENERS
    // ─────────────────────────────────────────

    private void setupCounterListeners() {
        setupCollectingListener();
        setupFerryingListener();
        setupScoredListener();   // FIX 2
        setupMissedListener();
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
        RadioButton display = group.findViewById(displayId);
        if (display != null) {
            display.setText(String.valueOf(count));
        }
        group.setOnCheckedChangeListener(null);
        group.check(displayId);
        if      (group == collectingCounterToggle) setupCollectingListener();
        else if (group == ferryingCounterToggle)   setupFerryingListener();
        else if (group == scoringCounterToggle)    setupScoredListener();   // FIX 2
        else if (group == missedCounterToggle)     setupMissedListener();
    }

    private void setupCollectingListener() {
        collectingCounterToggle.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.CollectingCounter) return;
            collectingCount = clamp(collectingCount + deltaFor(id,
                    R.id.CollectingMinus10, R.id.CollectingMinus5, R.id.CollectingMinus,
                    R.id.CollectingPlus,    R.id.CollectingPlus5,  R.id.CollectingPlus10));
            refreshDisplay(collectingCounterToggle, R.id.CollectingCounter, collectingCount);
        });
    }

    private void setupFerryingListener() {
        ferryingCounterToggle.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.FerryingCounter) return;
            ferryingCount = clamp(ferryingCount + deltaFor(id,
                    R.id.FerryingMinus10, R.id.FerryingMinus5, R.id.FerryingMinus,
                    R.id.FerryingPlus,    R.id.FerryingPlus5,  R.id.FerryingPlus10));
            refreshDisplay(ferryingCounterToggle, R.id.FerryingCounter, ferryingCount);
        });
    }

    private void setupScoredListener() {
        scoringCounterToggle.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.ScoredCounter) return;
            scoredCount = clamp(scoredCount + deltaFor(id,
                    R.id.ScoredMinus10, R.id.ScoredMinus5, R.id.ScoredMinus,
                    R.id.ScoredPlus,    R.id.ScoredPlus5,  R.id.ScoredPlus10));
            refreshDisplay(scoringCounterToggle, R.id.ScoredCounter, scoredCount);
        });
    }

    private void setupMissedListener() {
        missedCounterToggle.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.MissedCounter) return;
            missedCount = clamp(missedCount + deltaFor(id,
                    R.id.MissedMinus10, R.id.MissedMinus5, R.id.MissedMinus,
                    R.id.MissedPlus,    R.id.MissedPlus5,  R.id.MissedPlus10));
            refreshDisplay(missedCounterToggle, R.id.MissedCounter, missedCount);
        });
    }

    // ─────────────────────────────────────────
    // CASCADING LOGIC
    // ─────────────────────────────────────────

    private void setupCascadingListeners() {
        // FIX 6+7: no startLevel/stopLevel toggles in XML — removed updateFuelStates entirely
        attemptedClimbToggle.setOnCheckedChangeListener((g, id)    -> updateClimbStates());
        successfulClimbedToggle.setOnCheckedChangeListener((g, id) -> updateClimbStates());
        updateClimbStates();
    }

    /**
     * FIX 11: Teleop has buttons DNA | 1 | 2 | 3 for attempted and None | 1 | 2 | 3 for successful.
     * Location enabled only when attempted != DNA and successful != None (i.e. first button not selected).
     */
    private void updateClimbStates() {
        int attemptedId  = attemptedClimbToggle.getCheckedRadioButtonId();
        int successfulId = successfulClimbedToggle.getCheckedRadioButtonId();
        boolean attempted  = attemptedId  != -1 && attemptedId  != R.id.AttemptedNo;
        boolean successful = successfulId != -1 && successfulId != R.id.DidNotAttempt;
        setGroupEnabled(successfullyClimbedLocationToggle, attempted && successful);
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

        timer = new CountDownTimer(130000, 1000) {
            @Override
            public void onTick(long ms) {
                if (secondsRemaining == null) return;
                long secs = ms / 1000;
                long mins = secs / 60;
                long rem  = secs % 60;

                secondsRemaining.setText(mins + ":" + String.format("%02d", rem));

                if (!running) return;

                if (secs <= 30 && secs > 0) {
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
                if (!running) return;
                try {
                    if (secondsRemaining != null) secondsRemaining.setText("0");
                    setAllEdgeBars(R.drawable.teleop_warning);
                    if (timerID != null) {
                        timerID.setTextColor(context.getResources().getColor(R.color.border_warning));
                        timerID.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.timer_red, 0, 0, 0);
                    }
                    if (endgameWarning != null) {
                        endgameWarning.setVisibility(View.VISIBLE);
                        endgameWarning.setTextColor(getResources().getColor(R.color.white));
                        endgameWarning.setText(getString(R.string.EndGameWarning));
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
                ObjectAnimator anim = ObjectAnimator.ofFloat(bar, View.ALPHA, 0f, 1f);
                anim.setDuration(500);
                anim.setRepeatMode(ObjectAnimator.REVERSE);
                anim.setRepeatCount(1);
                anim.start();
            }
        }
    }

    private void setAllEdgeBars(int drawableRes) {
        for (ImageView bar : new ImageView[]{topEdgeBar, bottomEdgeBar, leftEdgeBar, rightEdgeBar}) {
            if (bar != null) {
                bar.setBackground(getResources().getDrawable(drawableRes));
            }
        }
    }

    // ─────────────────────────────────────────
    // GET / SET HELPERS
    // ─────────────────────────────────────────

    private String getSelectedText(RadioGroup group, String defaultVal) {
        int id = group.getCheckedRadioButtonId();
        if (id == -1) return defaultVal;
        RadioButton btn = group.findViewById(id);
        return btn != null ? btn.getText().toString().trim() : defaultVal;
    }

    private void selectByText(RadioGroup group, String value) {
        for (int i = 0; i < group.getChildCount(); i++) {
            RadioButton btn = (RadioButton) group.getChildAt(i);
            if (btn.getText().toString().trim().equalsIgnoreCase(value)) {
                group.check(btn.getId());
                return;
            }
        }
        if (group.getChildCount() > 0)
            group.check(((RadioButton) group.getChildAt(0)).getId());
    }

    private void setGroupEnabled(RadioGroup group, boolean enabled) {
        for (int i = 0; i < group.getChildCount(); i++)
            group.getChildAt(i).setEnabled(enabled);
    }

    // ─────────────────────────────────────────
    // DATA PERSISTENCE
    // ─────────────────────────────────────────

    private void loadTeleopData() {
        collectingCount = parseCount(hm("Collecting", "0"));
        ferryingCount   = parseCount(hm("Ferrying",   "0"));
        scoredCount     = parseCount(hm("Scored",     "0"));   // FIX 8
        missedCount     = parseCount(hm("Missed",     "0"));

        refreshDisplay(collectingCounterToggle, R.id.CollectingCounter, collectingCount);
        refreshDisplay(ferryingCounterToggle,   R.id.FerryingCounter,   ferryingCount);
        refreshDisplay(scoringCounterToggle,    R.id.ScoredCounter,     scoredCount);   // FIX 8
        refreshDisplay(missedCounterToggle,     R.id.MissedCounter,     missedCount);

        // FIX 8: removed startLevelToggle/stopLevelToggle selectByText calls
        selectByText(attemptedClimbToggle,              hm("AttemptedClimb",    "DID NOT ATTEMPT"));
        selectByText(successfulClimbedToggle,           hm("SuccessfulClimbed", "None"));
        selectByText(successfullyClimbedLocationToggle, hm("ClimbLocation",     "LEFT"));

        noShowSwitch.setChecked("Y".equals(hm("RobotFellOver", "N")));

        updateClimbStates();
    }

    private void saveTeleopData() {
        // FIX 9: removed StartLevel/StopLevel; added Scored
        teleopHashMap.put("Collecting",        String.valueOf(collectingCount));
        teleopHashMap.put("Ferrying",          String.valueOf(ferryingCount));
        teleopHashMap.put("Scored",            String.valueOf(scoredCount));
        teleopHashMap.put("Missed",            String.valueOf(missedCount));
        teleopHashMap.put("AttemptedClimb",    getSelectedText(attemptedClimbToggle,              "DID NOT ATTEMPT"));
        teleopHashMap.put("SuccessfulClimbed", getSelectedText(successfulClimbedToggle,           "None"));
        teleopHashMap.put("ClimbLocation",     getSelectedText(successfullyClimbedLocationToggle, "LEFT"));
        teleopHashMap.put("RobotFellOver",     noShowSwitch.isChecked() ? "Y" : "N");
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
        if (this.isVisible()) {
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