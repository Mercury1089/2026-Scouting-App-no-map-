package com.mercury1089.Scouting_App_2026;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.mercury1089.Scouting_App_2026.listeners.UpdateListener;
import com.mercury1089.Scouting_App_2026.qr.QRRunnable;

import java.util.LinkedHashMap;

public class Endgame extends Fragment implements UpdateListener {

    private static final String TAG = "EndGame Fragment";

    private int endGameSnapshotCount = 0;
    private LinkedHashMap<String, String> setupHashMap;
    private LinkedHashMap<String, String> endGameHashMap;

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
    private Button generateQRButton;
    private MatchActivity context;

    // Running counts
    private int ferryingCount   = 0;
    private int scoredCount     = 0;
    private int missedCount     = 0;
    private boolean isUpdating = false;

    public static Endgame newInstance() {
        Endgame fragment = new Endgame();
        fragment.setArguments(new Bundle());
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = (MatchActivity) getActivity();
        try {
            return inflater.inflate(R.layout.screen_endgame, container, false);
        } catch (InflateException e) {
            Log.d(TAG, "Inflate error: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.SETUP);
        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.ENDGAME);
        setupHashMap  = HashMapManager.getSetupHashMap();
        endGameHashMap = HashMapManager.getEndgameHashMap();

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
        locationText                     = getView().findViewById(R.id.TowerClimbPlacement);
        successfullyClimbedLocationToggle = getView().findViewById(R.id.SuccessfullyClimbedLocation);
        noShowSwitch                      = getView().findViewById(R.id.NoShowSwitch);
        saveButton                        = getView().findViewById(R.id.SaveButton);
        resetButton                       = getView().findViewById(R.id.ResetButton);
        generateQRButton                  = getView().findViewById(R.id.NextQRButton);

        initializeSnapshots();
        loadEndgameData();
        setupCounterListeners();
        setupTextWatchers();
        setupCascadingListeners();
        setupButtonListeners();
    }

    // ─────────────────────────────────────────
    // SNAPSHOT SYSTEM
    // ─────────────────────────────────────────

    private void initializeSnapshots() {
        String snapshotsString = endGameHashMap.get("snapshots");
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

    private void loadEndgameData() {
        ferryingCount = parseCount(hm("Ferrying", ""));
        scoredCount = parseCount(hm("Scored", ""));
        missedCount = parseCount(hm("Missed", ""));

        refreshDisplay(ferryingCounterToggle, R.id.FerryingCounter, ferryingCount);
        refreshDisplay(scoringCounterToggle, R.id.ScoredCounter, scoredCount);
        refreshDisplay(missedCounterToggle, R.id.MissedCounter, missedCount);

        selectByText(attemptedClimbToggle, hm("AttemptedClimb", ""));
        selectByText(successfulClimbedToggle, hm("SuccessfulClimbed", ""));
        selectByText(successfullyClimbedLocationToggle, hm("ClimbLocation", ""));

        // Persistence: Check Endgame map first, then Teleop map, then Auton map
        String fellOver = hm("RobotFellOver", "");
        if (fellOver.isEmpty()) {
            String teleopFell = HashMapManager.getTeleopHashMap().get("RobotFellOver");
            if (teleopFell != null && !teleopFell.isEmpty()) {
                fellOver = teleopFell;
            } else {
                String autonFell = HashMapManager.getAutonHashMap().get("RobotFellOver");
                if (autonFell != null && !autonFell.isEmpty()) {
                    fellOver = autonFell;
                }
            }
            if (fellOver != null && !fellOver.isEmpty()) {
                endGameHashMap.put("RobotFellOver", fellOver);
                HashMapManager.putEndgameHashMap(endGameHashMap);
            } else {
                fellOver = "N";
            }
        }
        if (noShowSwitch != null) {
            noShowSwitch.setChecked("Y".equals(fellOver));
        }

        updateClimbStates();
    }

    private void saveEndgameData() {
        endGameHashMap.put("Ferrying",          String.valueOf(ferryingCount));
        endGameHashMap.put("Scored",            String.valueOf(scoredCount));
        endGameHashMap.put("Missed",            String.valueOf(missedCount));
        endGameHashMap.put("AttemptedClimb",    getSelectedText(attemptedClimbToggle,              ""));
        endGameHashMap.put("SuccessfulClimbed", getSelectedText(successfulClimbedToggle,           ""));
        endGameHashMap.put("ClimbLocation",     getSelectedText(successfullyClimbedLocationToggle, ""));
        endGameHashMap.put("RobotFellOver",     (noShowSwitch != null && noShowSwitch.isChecked()) ? "Y" : "N");
        HashMapManager.putEndgameHashMap(endGameHashMap);
    }

    private void appendEndGameSnapshot() {
        if (snapshotBuilder == null) {
            initializeSnapshots();
        }

        String teamNumber  = setupHashMap.get("TeamNumber");
        if (teamNumber == null) teamNumber = "";
        String scouterName = setupHashMap.get("ScouterName");
        if (scouterName == null) scouterName = "";
        String matchNumber = setupHashMap.get("MatchNumber");
        if (matchNumber == null) matchNumber = "";

        String snapshotLine = String.format(
        /* setup: scouter, team, match */ "%s,%s,%s," +
        /* auton: A_scor,A_miss,A_ferr,A_died,A_att,A_succ,A_loc, */ ",,,,,,," +
        /* teleop  T_scor,T_miss,T_ferr,T_died, */ ",,,," +
        /* endgame E_scor,E_miss,E_ferr,E_att,E_succ,E_loc, */ "%d,%d,%d,%s,%s,%s," +
        /* timestamp */ "0\n",
            scouterName,
            teamNumber,
            matchNumber,
            scoredCount,
            missedCount,
            ferryingCount,
            getSelectedText(attemptedClimbToggle, ""),
            getSelectedText(successfulClimbedToggle, ""),
            getSelectedText(successfullyClimbedLocationToggle, "")
        );


        snapshotBuilder.append(snapshotLine);
        endGameSnapshotCount++;

        endGameHashMap.put("snapshots", snapshotBuilder.toString());
        endGameHashMap.put("EndGameSaveIndex", String.valueOf(endGameSnapshotCount));
        HashMapManager.putEndgameHashMap(endGameHashMap);
    }

    private void resetEndgameUI() {
        ferryingCount = 0;
        scoredCount = 0;
        missedCount = 0;
        refreshDisplay(ferryingCounterToggle, R.id.FerryingCounter, ferryingCount);
        refreshDisplay(scoringCounterToggle, R.id.ScoredCounter, scoredCount);
        refreshDisplay(missedCounterToggle, R.id.MissedCounter, missedCount);
        if (attemptedClimbToggle != null) attemptedClimbToggle.clearCheck();
        if (successfulClimbedToggle != null) successfulClimbedToggle.clearCheck();
        if (successfullyClimbedLocationToggle != null) successfullyClimbedLocationToggle.clearCheck();
        // RobotFellOver (noShowSwitch) removed from reset to maintain persistence after save
        updateClimbStates();
    }

    private String hm(String key, String def) {
        String v = endGameHashMap.get(key);
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
        if (noShowSwitch != null) {
            noShowSwitch.setOnCheckedChangeListener((v, isChecked) -> {
                String state = isChecked ? "Y" : "N";
                endGameHashMap.put("RobotFellOver", state);
                HashMapManager.putEndgameHashMap(endGameHashMap);
                updateClimbStates();
            });
        }
        updateClimbStates();
    }

    private void updateClimbStates() {
        if (isUpdating) return;
        isUpdating = true;
        try {
            boolean robotDied = noShowSwitch != null && noShowSwitch.isChecked();
            boolean enabled = !robotDied;

            // Counters and Text Fields
            setGroupEnabled(ferryingCounterToggle, enabled);
            setGroupEnabled(scoringCounterToggle, enabled);
            setGroupEnabled(missedCounterToggle, enabled);
            if (ferryingEditText != null) ferryingEditText.setEnabled(enabled);
            if (scoredEditText != null) scoredEditText.setEnabled(enabled);
            if (missedEditText != null) missedEditText.setEnabled(enabled);

            // Climb logic
            setGroupEnabled(attemptedClimbToggle, enabled);
            int attemptedId = attemptedClimbToggle != null ? attemptedClimbToggle.getCheckedRadioButtonId() : -1;
            boolean attempted = enabled && (attemptedId != -1 && attemptedId != R.id.AttemptedNo);

            if (successfulText != null) successfulText.setEnabled(attempted);
            setGroupEnabled(successfulClimbedToggle, attempted);

            if (!attempted && successfulClimbedToggle != null && successfulClimbedToggle.getCheckedRadioButtonId() != -1) {
                successfulClimbedToggle.clearCheck();
            }

            int successfulId = successfulClimbedToggle != null ? successfulClimbedToggle.getCheckedRadioButtonId() : -1;
            boolean successful = attempted && (successfulId != -1 && successfulId != R.id.DidNotAttempt);

            if (locationText != null) locationText.setEnabled(successful);
            setGroupEnabled(successfullyClimbedLocationToggle, successful);

            if (!successful && successfullyClimbedLocationToggle != null && successfullyClimbedLocationToggle.getCheckedRadioButtonId() != -1) {
                successfullyClimbedLocationToggle.clearCheck();
            }
        } finally {
            isUpdating = false;
        }
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
                saveEndgameData();
                appendEndGameSnapshot();
                resetEndgameUI();
                Toast.makeText(context, "Endgame snapshot saved", Toast.LENGTH_SHORT).show();
            });
        }

        if (resetButton != null) {
            resetButton.setOnClickListener(v -> {
                resetEndgameUI();
                Toast.makeText(context, "Changes cancelled", Toast.LENGTH_SHORT).show();
            });
        }

        if (generateQRButton != null) {
            generateQRButton.setOnClickListener(v -> {
                saveEndgameData();
                appendEndGameSnapshot();
                Dialog loading_alert = new Dialog(context);
                loading_alert.requestWindowFeature(Window.FEATURE_NO_TITLE);
                loading_alert.setContentView(R.layout.screen_qr_loading);
                loading_alert.setCancelable(false);
                loading_alert.show();
                new Thread(new QRRunnable(context, loading_alert)).start();
            });
        }

        setupToggleableRadioGroup(attemptedClimbToggle);
        setupToggleableRadioGroup(successfulClimbedToggle);
        setupToggleableRadioGroup(successfullyClimbedLocationToggle);
    }

    private void setupToggleableRadioGroup(RadioGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View v = group.getChildAt(i);
            if (v instanceof RadioButton) {
                v.setOnClickListener(view -> {
                    if (((RadioButton) view).isChecked() && view.getTag() != null && (boolean) view.getTag()) {
                        group.clearCheck();
                        updateTags(group, -1);
                    } else {
                        updateTags(group, view.getId());
                    }
                    updateClimbStates();
                });
            }
        }
    }

    private void updateTags(RadioGroup group, int checkedId) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View v = group.getChildAt(i);
            if (v instanceof RadioButton) {
                v.setTag(v.getId() == checkedId);
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
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (this.isVisible() && getView() != null) {
            if (isVisibleToUser) {
                setupHashMap   = HashMapManager.getSetupHashMap();
                endGameHashMap = HashMapManager.getEndgameHashMap();
                initializeSnapshots();
                loadEndgameData();
            } else {
                saveEndgameData();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onUpdate() { loadEndgameData(); }
}