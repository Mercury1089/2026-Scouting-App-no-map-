package com.mercury1089.Scouting_App_2026;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.mercury1089.Scouting_App_2026.listeners.UpdateListener;
import com.mercury1089.Scouting_App_2026.utils.GenUtils;

import java.util.LinkedHashMap;

public class Auton extends Fragment implements UpdateListener {

    private static final String TAG = "Auton Fragment";

    private int autonSnapshotCount = 0;
    private LinkedHashMap<String, String> setupHashMap;
    private LinkedHashMap<String, String> autonHashMap;

    // Snapshot System (CSV format)
    private StringBuilder snapshotBuilder;
    private static final String SNAPSHOT_HEADER = "collecting,ferrying,missed,startLevel,stopLevel,attemptedClimb,successfulClimbed,climbLocation,robotFellOver";

    // Fuel section
    private RadioGroup collectingCounterToggle;
    private RadioGroup ferryingCounterToggle;
    private RadioGroup startLevelToggle;
    private RadioGroup stopLevelToggle;
    private RadioGroup missedCounterToggle;

    // Climbing section
    private RadioGroup attemptedClimbToggle;
    private RadioGroup successfulClimbedToggle;
    private RadioGroup successfullyClimbedLocationToggle;

    // Other controls
    private Switch noShowSwitch;
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
    private int collectingCount = 0;
    private int ferryingCount   = 0;
    private int missedCount     = 0;

    public static Auton newInstance() {
        Auton fragment = new Auton();
        fragment.setArguments(new Bundle());
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = (MatchActivity) getActivity();
        try {
            return inflater.inflate(R.layout.auton_screen, container, false);
        } catch (InflateException e) {
            Log.d("Auton", "Inflate error: " + e.getMessage());
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
        collectingCounterToggle          = getView().findViewById(R.id.CollectingCounterToggle);
        ferryingCounterToggle            = getView().findViewById(R.id.FerryingCounterToggle);
        startLevelToggle                 = getView().findViewById(R.id.StartLevelToggle);
        stopLevelToggle                  = getView().findViewById(R.id.StopLevelToggle);
        missedCounterToggle              = getView().findViewById(R.id.MissedCounterToggle);
        attemptedClimbToggle             = getView().findViewById(R.id.AttemptedClimbToggle);
        successfulClimbedToggle          = getView().findViewById(R.id.SuccessfulClimbed);
        successfullyClimbedLocationToggle = getView().findViewById(R.id.SuccessfullyClimbedLocation);
        noShowSwitch                     = getView().findViewById(R.id.NoShowSwitch);
        saveButton                       = getView().findViewById(R.id.SaveButton);
        resetButton                      = getView().findViewById(R.id.ResetButton);
        nextButtonAuton                  = getView().findViewById(R.id.NextTeleopButton);
        timerID                          = getView().findViewById(R.id.IDAutonSeconds1);
        secondsRemaining                 = getView().findViewById(R.id.AutonSeconds);
        teleopWarning                    = getView().findViewById(R.id.TeleopWarning);
        topEdgeBar                       = getView().findViewById(R.id.topEdgeBar);
        bottomEdgeBar                    = getView().findViewById(R.id.bottomEdgeBar);
        leftEdgeBar                      = getView().findViewById(R.id.leftEdgeBar);
        rightEdgeBar                     = getView().findViewById(R.id.rightEdgeBar);

        initializeSnapshots();
        loadAutonData();
        setupCounterListeners();
        setupCascadingListeners();
        setupButtonListeners();
        setupTimer();
    }

    // ─────────────────────────────────────────
    // SNAPSHOT SYSTEM
    // ─────────────────────────────────────────

    private void initializeSnapshots() {
        String snapshotsString = autonHashMap.get("snapshots");
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

    private void appendAutonSnapshot() {
        if (snapshotBuilder == null) {
            initializeSnapshots();
        }

        String snapshotLine = String.format("%d,%d,%d,%s,%s,%s,%s,%s,%s\n",
                collectingCount,
                ferryingCount,
                missedCount,
                getLevelValue(startLevelToggle),
                getLevelValue(stopLevelToggle),
                getSelectedText(attemptedClimbToggle, "DID NOT ATTEMPT"),
                getSelectedText(successfulClimbedToggle, "None"),
                getSelectedText(successfullyClimbedLocationToggle, "LEFT"),
                (noShowSwitch != null && noShowSwitch.isChecked()) ? "1" : "0");

        snapshotBuilder.append(snapshotLine);
        autonSnapshotCount++;

        autonHashMap.put("snapshots", snapshotBuilder.toString());
        autonHashMap.put("AutonSaveIndex", String.valueOf(autonSnapshotCount));
        HashMapManager.putAutonHashMap(autonHashMap);
    }

    private int countSnapshots() {
        if (snapshotBuilder == null) return 0;
        String content = snapshotBuilder.toString();
        int count = 0;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') count++;
        }
        return count - 1; // Subtract header line
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

    private void resetAutonUI() {
        // Reset counters
        collectingCount = 0;
        ferryingCount = 0;
        missedCount = 0;

        // Reset level toggles to EMPTY (first button)
        if (startLevelToggle != null && startLevelToggle.getChildCount() > 0) {
            startLevelToggle.check(((RadioButton) startLevelToggle.getChildAt(0)).getId());
        }
        if (stopLevelToggle != null && stopLevelToggle.getChildCount() > 0) {
            stopLevelToggle.check(((RadioButton) stopLevelToggle.getChildAt(0)).getId());
        }

        // Reset climb counters/toggles
        if (attemptedClimbToggle != null && attemptedClimbToggle.getChildCount() > 0) {
            attemptedClimbToggle.check(((RadioButton) attemptedClimbToggle.getChildAt(0)).getId());
        }
        if (successfulClimbedToggle != null && successfulClimbedToggle.getChildCount() > 0) {
            successfulClimbedToggle.check(((RadioButton) successfulClimbedToggle.getChildAt(0)).getId());
        }
        if (successfullyClimbedLocationToggle != null && successfullyClimbedLocationToggle.getChildCount() > 0) {
            successfullyClimbedLocationToggle.check(((RadioButton) successfullyClimbedLocationToggle.getChildAt(0)).getId());
        }

        // Reset switch
        if (noShowSwitch != null) {
            noShowSwitch.setChecked(false);
        }

        // Update cascading logic
        updateFuelStates();
        updateClimbStates();
    }

    // ─────────────────────────────────────────
    // COUNTER LISTENERS
    // Tapping -10/-5/-/+/+5/+10 adjusts the running count.
    // The centre button is just a display; tapping it does nothing.
    // After each tap the selection snaps back to the centre display button.
    // ─────────────────────────────────────────

    private void setupCounterListeners() {
        collectingCounterToggle.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.CollectingCounter) return;
            collectingCount = clamp(collectingCount + deltaFor(id,
                    R.id.CollectingMinus10, R.id.CollectingMinus5, R.id.CollectingMinus,
                    R.id.CollectingPlus,    R.id.CollectingPlus5,  R.id.CollectingPlus10));
            refreshDisplay(collectingCounterToggle, R.id.CollectingCounter, collectingCount);
        });

        ferryingCounterToggle.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.FerryingCounter) return;
            ferryingCount = clamp(ferryingCount + deltaFor(id,
                    R.id.FerryingMinus10, R.id.FerryingMinus5, R.id.FerryingMinus,
                    R.id.FerryingPlus,    R.id.FerryingPlus5,  R.id.FerryingPlus10));
            refreshDisplay(ferryingCounterToggle, R.id.FerryingCounter, ferryingCount);
        });

        missedCounterToggle.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.MissedCounter) return;
            missedCount = clamp(missedCount + deltaFor(id,
                    R.id.MissedMinus10, R.id.MissedMinus5, R.id.MissedMinus,
                    R.id.MissedPlus,    R.id.MissedPlus5,  R.id.MissedPlus10));
            refreshDisplay(missedCounterToggle, R.id.MissedCounter, missedCount);
        });
    }

    /** Returns the delta for the tapped button id (-10/-5/-1/+1/+5/+10). */
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

    /** Clamps a count to a minimum of 0. */
    private int clamp(int value) {
        return Math.max(0, value);
    }

    /**
     * Updates the display button text and snaps the RadioGroup selection back to it.
     * Temporarily removes the listener to avoid recursive calls.
     */
    private void refreshDisplay(RadioGroup group, int displayId, int count) {
        RadioButton display = group.findViewById(displayId);
        if (display != null) {
            display.setText(String.valueOf(count)); // plain number, no leading zeros
        }
        group.setOnCheckedChangeListener(null);
        group.check(displayId);
        // Re-attach the right listener
        if      (group == collectingCounterToggle) setupCollectingListener();
        else if (group == ferryingCounterToggle)   setupFerryingListener();
        else if (group == missedCounterToggle)      setupMissedListener();
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
        startLevelToggle.setOnCheckedChangeListener((g, id) -> updateFuelStates());
        stopLevelToggle.setOnCheckedChangeListener((g, id)  -> updateFuelStates());
        updateFuelStates();

        attemptedClimbToggle.setOnCheckedChangeListener((g, id)    -> updateClimbStates());
        successfulClimbedToggle.setOnCheckedChangeListener((g, id) -> updateClimbStates());
        updateClimbStates();
    }

    /** Missed only enabled when BOTH start and stop are not EMPTY. */
    private void updateFuelStates() {
        boolean bothSet = !getLevelValue(startLevelToggle).equals("EMPTY")
                && !getLevelValue(stopLevelToggle).equals("EMPTY");
        setGroupEnabled(missedCounterToggle, bothSet);
    }

    /** Climb location only enabled when attempted = "1" AND successful = "1". */
    private void updateClimbStates() {
        String attempted  = getSelectedText(attemptedClimbToggle, "");
        String successful = getSelectedText(successfulClimbedToggle, "");
        boolean climbed = "1".equals(attempted) && "1".equals(successful);
        setGroupEnabled(successfullyClimbedLocationToggle, climbed);
    }

    // ─────────────────────────────────────────
    // BUTTON LISTENERS
    // ─────────────────────────────────────────

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
                loadAutonData();
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

    // ─────────────────────────────────────────
    // TIMER
    // ─────────────────────────────────────────

    private void setupTimer() {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        timer = new CountDownTimer(20000, 1000) {
            @Override
            public void onTick(long ms) {
                if (secondsRemaining == null) return;

                long secs = ms / 1000;
                try {
                    secondsRemaining.setText(String.valueOf(secs));
                } catch (Exception e) {
                    Log.e(TAG, "Error setting seconds remaining: " + e.getMessage());
                }

                if (!running) return;

                if (secs <= 3 && secs > 0) {
                    try {
                        if (teleopWarning != null) {
                            teleopWarning.setVisibility(View.VISIBLE);
                        }
                        if (timerID != null) {
                            timerID.setTextColor(context.getResources().getColor(R.color.banana));
                            timerID.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.timer_yellow, 0, 0, 0);
                        }
                        if (vibrator != null) vibrator.vibrate(500);
                        pulseEdgeBars();
                    } catch (Exception e) {
                        Log.e(TAG, "Error in timer warning: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFinish() {
                if (!running) return;

                try {
                    if (secondsRemaining != null) {
                        secondsRemaining.setText("0");
                    }
                    setAllEdgeBars(R.drawable.teleop_warning);
                    if (timerID != null) {
                        timerID.setTextColor(context.getResources().getColor(R.color.border_warning));
                        timerID.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.timer_red, 0, 0, 0);
                    }
                    if (teleopWarning != null) {
                        teleopWarning.setVisibility(View.VISIBLE);
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
        try {
            for (ImageView bar : new ImageView[]{topEdgeBar, bottomEdgeBar, leftEdgeBar, rightEdgeBar}) {
                if (bar != null) {
                    ObjectAnimator anim = ObjectAnimator.ofFloat(bar, View.ALPHA, 0f, 1f);
                    anim.setDuration(500);
                    anim.setRepeatMode(ObjectAnimator.REVERSE);
                    anim.setRepeatCount(1);
                    anim.start();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error pulsing edge bars: " + e.getMessage());
        }
    }

    private void setAllEdgeBars(int drawableRes) {
        try {
            for (ImageView bar : new ImageView[]{topEdgeBar, bottomEdgeBar, leftEdgeBar, rightEdgeBar}) {
                if (bar != null) {
                    bar.setBackground(getResources().getDrawable(drawableRes));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting edge bars: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────
    // GET / SET HELPERS
    // ─────────────────────────────────────────

    /**
     * Returns the text of the currently selected button in a RadioGroup,
     * or defaultVal if nothing is selected.
     */
    private String getSelectedText(RadioGroup group, String defaultVal) {
        int id = group.getCheckedRadioButtonId();
        if (id == -1) return defaultVal;
        RadioButton btn = group.findViewById(id);
        return btn != null ? btn.getText().toString().trim() : defaultVal;
    }

    /**
     * Returns the normalised level value for Start/Stop toggles.
     * Button texts from strings.xml: "EMPTY", "25%", "50%", "75%", "FULL"
     */
    private String getLevelValue(RadioGroup group) {
        return getSelectedText(group, "EMPTY");
    }

    /**
     * Selects the radio button whose text matches value (case-insensitive).
     * Falls back to the first button if no match found.
     */
    private void selectByText(RadioGroup group, String value) {
        for (int i = 0; i < group.getChildCount(); i++) {
            RadioButton btn = (RadioButton) group.getChildAt(i);
            if (btn.getText().toString().trim().equalsIgnoreCase(value)) {
                group.check(btn.getId());
                return;
            }
        }
        // Default: first button
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

    private void loadAutonData() {
        collectingCount = parseCount(hm("Collecting", "0"));
        ferryingCount   = parseCount(hm("Ferrying",   "0"));
        missedCount     = parseCount(hm("Missed",     "0"));

        // Snap display buttons to loaded counts (re-attaches listeners inside)
        refreshDisplay(collectingCounterToggle, R.id.CollectingCounter, collectingCount);
        refreshDisplay(ferryingCounterToggle,   R.id.FerryingCounter,     ferryingCount);
        refreshDisplay(missedCounterToggle,     R.id.MissedCounter,       missedCount);

        // Level toggles — stored as exact button text e.g. "EMPTY", "25%", "FULL"
        selectByText(startLevelToggle, hm("StartLevel", "EMPTY"));
        selectByText(stopLevelToggle,  hm("StopLevel",  "EMPTY"));

        // Climb toggles — stored as exact button text
        // AttemptedClimbToggle buttons: "DID NOT ATTEMPT" | "1"
        selectByText(attemptedClimbToggle,             hm("AttemptedClimb",    "DID NOT ATTEMPT"));
        // SuccessfulClimbed buttons: "None" | "1"
        selectByText(successfulClimbedToggle,          hm("SuccessfulClimbed", "None"));
        // Location buttons: "LEFT" | "CENTER" | "RIGHT"
        selectByText(successfullyClimbedLocationToggle, hm("ClimbLocation",    "LEFT"));

        noShowSwitch.setChecked("Y".equals(hm("RobotFellOver", "N")));

        updateFuelStates();
        updateClimbStates();
    }

    private void saveAutonData() {
        autonHashMap.put("Collecting",        String.valueOf(collectingCount));
        autonHashMap.put("Ferrying",          String.valueOf(ferryingCount));
        autonHashMap.put("Missed",            String.valueOf(missedCount));
        autonHashMap.put("StartLevel",        getLevelValue(startLevelToggle));
        autonHashMap.put("StopLevel",         getLevelValue(stopLevelToggle));
        autonHashMap.put("AttemptedClimb",    getSelectedText(attemptedClimbToggle,             "DID NOT ATTEMPT"));
        autonHashMap.put("SuccessfulClimbed", getSelectedText(successfulClimbedToggle,          "None"));
        autonHashMap.put("ClimbLocation",     getSelectedText(successfullyClimbedLocationToggle, "LEFT"));
        autonHashMap.put("RobotFellOver",     noShowSwitch.isChecked() ? "Y" : "N");
        HashMapManager.putAutonHashMap(autonHashMap);
    }

    /** API 21-safe HashMap get with default. */
    private String hm(String key, String def) {
        String v = autonHashMap.get(key);
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
                autonHashMap = HashMapManager.getAutonHashMap();
                initializeSnapshots();
                loadAutonData();
            } else {
                saveAutonData();
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
    public void onUpdate() { loadAutonData(); }
}