package com.mercury1089.Scouting_App_2026;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.mercury1089.Scouting_App_2026.listeners.UpdateListener;
import com.mercury1089.Scouting_App_2026.qr.QRRunnable;

import java.util.LinkedHashMap;

public class Endgame extends Fragment implements UpdateListener {

    private LinkedHashMap<String, String> setupHashMap;
    private LinkedHashMap<String, String> endgameHashMap;

    private int endgameSnapshotCount = 0;

    // Range toggles (select one range bucket)
    private RadioGroup collectingCounterToggle;
    private RadioGroup ferryingCounterToggle;
    private RadioGroup missedCounterToggle;

    // Level toggles
    private RadioGroup startLevelToggle;
    private RadioGroup stopLevelToggle;

    // Climbing section
    private RadioGroup attemptedClimbToggle;
    private RadioGroup successfulClimbedToggle;
    private RadioGroup successfullyClimbedLocationToggle;

    // Other controls
    private Switch noShowSwitch;
    private Button saveButton;
    private Button generateQRButton;

    // Timer & animation
    private TextView timerID;
    private TextView secondsRemaining;
    private TextView postMatchWarning;
    private ImageView topEdgeBar, bottomEdgeBar, leftEdgeBar, rightEdgeBar;

    private static CountDownTimer timer;
    private boolean firstTime = true;
    private boolean running = true;
    private MatchActivity context;

    // SNAPSHOT SYSTEM - CSV FORMAT
    private StringBuilder snapshotBuilder;
    private static final String SNAPSHOT_HEADER =
            "collecting,ferrying,missed,startLevel,stopLevel," +
                    "attemptedClimb,successfulClimbed,climbLocation,robotFellOver";

    public static Endgame newInstance() {
        Endgame fragment = new Endgame();
        fragment.setArguments(new Bundle());
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = (MatchActivity) getActivity();
        try {
            return inflater.inflate(R.layout.endgame_screen, container, false);
        } catch (InflateException e) {
            Log.d("Endgame", "Inflate error: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.SETUP);
        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.ENDGAME);
        setupHashMap   = HashMapManager.getSetupHashMap();
        endgameHashMap = HashMapManager.getEndgameHashMap();

        // Link views
        collectingCounterToggle           = getView().findViewById(R.id.CollectingCounterToggle);
        ferryingCounterToggle             = getView().findViewById(R.id.FerryingCounterToggle);
        missedCounterToggle               = getView().findViewById(R.id.MissedCounterToggle);
        startLevelToggle                  = getView().findViewById(R.id.StartLevelToggle);
        stopLevelToggle                   = getView().findViewById(R.id.StopLevelToggle);
        attemptedClimbToggle              = getView().findViewById(R.id.AttemptedClimbToggle);
        successfulClimbedToggle           = getView().findViewById(R.id.SuccessfulClimbed);
        successfullyClimbedLocationToggle = getView().findViewById(R.id.SuccessfullyClimbedLocation);
        noShowSwitch                      = getView().findViewById(R.id.NoShowSwitch);
        saveButton                        = getView().findViewById(R.id.SaveButton);
        generateQRButton                  = getView().findViewById(R.id.GenerateQRButton);
        timerID                           = getView().findViewById(R.id.IDEndGameSeconds1);
        secondsRemaining                  = getView().findViewById(R.id.EndGameSeconds);
        postMatchWarning                  = getView().findViewById(R.id.PostMatchWarning);
        topEdgeBar                        = getView().findViewById(R.id.topEdgeBar);
        bottomEdgeBar                     = getView().findViewById(R.id.bottomEdgeBar);
        leftEdgeBar                       = getView().findViewById(R.id.leftEdgeBar);
        rightEdgeBar                      = getView().findViewById(R.id.rightEdgeBar);

        // Initialize snapshot system
        initializeSnapshots();

        loadEndgameData();
        setupCascadingListeners();
        setupButtonListeners();
        setupTimer();
    }

    // SNAPSHOT INITIALIZATION
    private void initializeSnapshots() {
        String existingSnapshots = endgameHashMap.get("snapshots");
        if (existingSnapshots != null && !existingSnapshots.isEmpty()) {
            snapshotBuilder = new StringBuilder(existingSnapshots);
            Log.d("Endgame", "Restored " + countSnapshots() + " existing snapshots");
        } else {
            snapshotBuilder = new StringBuilder();
            snapshotBuilder.append(SNAPSHOT_HEADER).append("\n");
            Log.d("Endgame", "Initialized new snapshot buffer");
        }
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

    private void updateFuelStates() {
        boolean bothSet = !getLevelValue(startLevelToggle).equals("EMPTY")
                && !getLevelValue(stopLevelToggle).equals("EMPTY");
        setGroupEnabled(missedCounterToggle, bothSet);
    }

    private void updateClimbStates() {
        String attempted  = getSelectedText(attemptedClimbToggle, "");
        String successful = getSelectedText(successfulClimbedToggle, "");
        setGroupEnabled(successfullyClimbedLocationToggle, "1".equals(attempted) && "1".equals(successful));
    }

    // ─────────────────────────────────────────
    // BUTTON LISTENERS
    // ─────────────────────────────────────────

    private void setupButtonListeners() {
        saveButton.setOnClickListener(v -> {
            appendEndgameSnapshot();
        });

        generateQRButton.setOnClickListener(v -> {
            saveEndgameData();

            Dialog loading_alert = new Dialog(context);
            loading_alert.requestWindowFeature(Window.FEATURE_NO_TITLE);
            loading_alert.setContentView(R.layout.loading_screen);
            loading_alert.setCancelable(false);
            loading_alert.show();

            QRRunnable runnable = new QRRunnable(context, loading_alert);
            new Thread(runnable).start();
        });
    }

    // ─────────────────────────────────────────
    // TIMER — 30 seconds, warning at 5s
    // ─────────────────────────────────────────

    private void setupTimer() {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        timer = new CountDownTimer(160000, 1000) {
            @Override
            public void onTick(long ms) {
                long secs = ms / 1000;

                if (secondsRemaining != null) {
                    secondsRemaining.setText(String.valueOf(secs));
                }

                if (!running) return;

                if (secs <= 5 && secs > 0) {
                    if (postMatchWarning != null) {
                        postMatchWarning.setVisibility(View.VISIBLE);
                    }

                    if (timerID != null) {
                        try {
                            timerID.setTextColor(getResources().getColor(R.color.banana));
                            timerID.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.timer_yellow, 0, 0, 0);
                        } catch (Exception e) {
                            Log.e("Endgame", "Timer warning color error: " + e.getMessage());
                        }
                    }

                    if (vibrator != null) vibrator.vibrate(500);

                    try {
                        pulseEdgeBars();
                    } catch (Exception e) {
                        Log.e("Endgame", "Pulse edge bars error: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFinish() {
                if (!running) return;

                if (secondsRemaining != null) {
                    secondsRemaining.setText("0");
                }

                setAllEdgeBars(R.drawable.teleop_warning);

                if (timerID != null) {
                    try {
                        timerID.setTextColor(getResources().getColor(R.color.border_warning));
                        timerID.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.timer_red, 0, 0, 0);
                    } catch (Exception e) {
                        Log.e("Endgame", "Timer finish color error: " + e.getMessage());
                    }
                }

                if (postMatchWarning != null) {
                    postMatchWarning.setVisibility(View.VISIBLE);
                    try {
                        postMatchWarning.setTextColor(getResources().getColor(R.color.white));
                    } catch (Exception e) {
                        Log.e("Endgame", "Warning text color error: " + e.getMessage());
                    }
                    postMatchWarning.setText(getString(R.string.EndGameError));
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
            if (bar == null) continue;
            ObjectAnimator anim = ObjectAnimator.ofFloat(bar, View.ALPHA, 0f, 1f);
            anim.setDuration(500);
            anim.setRepeatMode(ObjectAnimator.REVERSE);
            anim.setRepeatCount(1);
            anim.start();
        }
    }

    private void setAllEdgeBars(int drawableRes) {
        for (ImageView bar : new ImageView[]{topEdgeBar, bottomEdgeBar, leftEdgeBar, rightEdgeBar}) {
            if (bar == null) continue;
            try {
                bar.setBackground(getResources().getDrawable(drawableRes));
            } catch (Exception e) {
                Log.e("Endgame", "Edge bar drawable error: " + e.getMessage());
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

    private String getLevelValue(RadioGroup group) {
        return getSelectedText(group, "EMPTY");
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

    private void loadEndgameData() {
        selectByText(collectingCounterToggle, hm("Collecting", ">75"));
        selectByText(ferryingCounterToggle,   hm("Ferrying",   ">75"));
        selectByText(missedCounterToggle,     hm("Missed",     ">75"));

        selectByText(startLevelToggle, hm("StartLevel", "EMPTY"));
        selectByText(stopLevelToggle,  hm("StopLevel",  "EMPTY"));

        selectByText(attemptedClimbToggle,              hm("AttemptedClimb",    "DID NOT ATTEMPT"));
        selectByText(successfulClimbedToggle,           hm("SuccessfulClimbed", "None"));
        selectByText(successfullyClimbedLocationToggle, hm("ClimbLocation",     "LEFT"));

        noShowSwitch.setChecked("Y".equals(hm("RobotFellOver", "N")));

        updateFuelStates();
        updateClimbStates();
    }

    private void saveEndgameData() {
        endgameHashMap.put("Collecting",        getSelectedText(collectingCounterToggle, ">75"));
        endgameHashMap.put("Ferrying",          getSelectedText(ferryingCounterToggle,   ">75"));
        endgameHashMap.put("Missed",            getSelectedText(missedCounterToggle,     ">75"));
        endgameHashMap.put("StartLevel",        getLevelValue(startLevelToggle));
        endgameHashMap.put("StopLevel",         getLevelValue(stopLevelToggle));
        endgameHashMap.put("AttemptedClimb",    getSelectedText(attemptedClimbToggle,              "DID NOT ATTEMPT"));
        endgameHashMap.put("SuccessfulClimbed", getSelectedText(successfulClimbedToggle,           "None"));
        endgameHashMap.put("ClimbLocation",     getSelectedText(successfullyClimbedLocationToggle, "LEFT"));
        endgameHashMap.put("RobotFellOver",     noShowSwitch.isChecked() ? "Y" : "N");
        HashMapManager.putEndgameHashMap(endgameHashMap);
    }

    // SNAPSHOT SYSTEM - CSV SERIALIZATION
    private void appendEndgameSnapshot() {
        saveEndgameData();

        String collecting = getSelectedText(collectingCounterToggle, ">75");
        String ferrying = getSelectedText(ferryingCounterToggle, ">75");
        String missed = getSelectedText(missedCounterToggle, ">75");
        String startLevel = getLevelValue(startLevelToggle);
        String stopLevel = getLevelValue(stopLevelToggle);
        String attemptedClimb = getSelectedText(attemptedClimbToggle, "DID NOT ATTEMPT");
        String successfulClimbed = getSelectedText(successfulClimbedToggle, "None");
        String climbLocation = getSelectedText(successfullyClimbedLocationToggle, "LEFT");
        String robotFellOver = noShowSwitch.isChecked() ? "Y" : "N";

        // Create CSV line
        String snapshotLine = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                collecting, ferrying, missed, startLevel, stopLevel,
                attemptedClimb, successfulClimbed, climbLocation, robotFellOver);

        // Append to builder
        snapshotBuilder.append(snapshotLine);
        endgameSnapshotCount++;

        // Save to HashMap for persistence
        endgameHashMap.put("snapshots", snapshotBuilder.toString());
        endgameHashMap.put("EndgameSaveIndex", String.valueOf(endgameSnapshotCount));
        HashMapManager.putEndgameHashMap(endgameHashMap);

        int snapshotCount = countSnapshots();
        Toast.makeText(context, "Snapshot #" + snapshotCount + " saved", Toast.LENGTH_SHORT).show();
        Log.d("Endgame", "Snapshot appended: " + snapshotLine.trim());

        // RESET UI
        resetEndgameUI();
    }

    private void resetEndgameUI() {
        // Reset counter toggles
        selectByText(collectingCounterToggle, ">75");
        selectByText(ferryingCounterToggle, ">75");
        selectByText(missedCounterToggle, ">75");

        // Reset level toggles
        selectByText(startLevelToggle, "EMPTY");
        selectByText(stopLevelToggle, "EMPTY");

        // Reset climb toggles
        selectByText(attemptedClimbToggle, "DID NOT ATTEMPT");
        selectByText(successfulClimbedToggle, "None");
        selectByText(successfullyClimbedLocationToggle, "LEFT");

        // Reset switch
        noShowSwitch.setChecked(false);

        // Update cascading logic
        updateFuelStates();
        updateClimbStates();

        // Save reset state to HashMap
        endgameHashMap.put("Collecting", ">75");
        endgameHashMap.put("Ferrying", ">75");
        endgameHashMap.put("Missed", ">75");
        endgameHashMap.put("StartLevel", "EMPTY");
        endgameHashMap.put("StopLevel", "EMPTY");
        endgameHashMap.put("AttemptedClimb", "DID NOT ATTEMPT");
        endgameHashMap.put("SuccessfulClimbed", "None");
        endgameHashMap.put("ClimbLocation", "LEFT");
        endgameHashMap.put("RobotFellOver", "N");
        HashMapManager.putEndgameHashMap(endgameHashMap);

        Log.d("Endgame", "UI reset after snapshot save");
    }

    private int countSnapshots() {
        String content = snapshotBuilder.toString();
        if (content.isEmpty()) return 0;
        String[] lines = content.split("\n");
        return Math.max(0, lines.length - 2);
    }

    public String getSnapshotsAsString() {
        return snapshotBuilder.toString();
    }

    public String exportSnapshotsCSV() {
        return snapshotBuilder.toString();
    }

    private String hm(String key, String def) {
        String v = endgameHashMap.get(key);
        return v != null ? v : def;
    }

    // ─────────────────────────────────────────
    // LIFECYCLE
    // ─────────────────────────────────────────

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (this.isVisible()) {
            if (isVisibleToUser) {
                setupHashMap   = HashMapManager.getSetupHashMap();
                endgameHashMap = HashMapManager.getEndgameHashMap();
                loadEndgameData();
                initializeSnapshots();
            } else {
                saveEndgameData();
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
    public void onUpdate() { loadEndgameData(); }
}