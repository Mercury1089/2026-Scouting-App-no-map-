package com.mercury1089.Scouting_App_2026;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
    private Button generateQRButton;

    private MatchActivity context;

    // Running counts
    private int collectingCount = 0;
    private int ferryingCount   = 0;
    private int scoredCount     = 0;   // FIX 2
    private int missedCount     = 0;

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
        assert getView() != null;
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
        generateQRButton                  = getView().findViewById(R.id.NextQRButton);   // FIX 12

        initializeSnapshots();
        loadEndGameData();
        setupCounterListeners();
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

    private void appendEndGameSnapshot() {
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
        endGameSnapshotCount++;

        endGameHashMap.put("snapshots", snapshotBuilder.toString());
        endGameHashMap.put("EndGameSaveIndex", String.valueOf(endGameSnapshotCount));
        HashMapManager.putEndgameHashMap(endGameHashMap);
    }

    // ─────────────────────────────────────────
    // UI RESET
    // ─────────────────────────────────────────

    private void resetEndGameUI() {
        collectingCount = 0;
        ferryingCount   = 0;
        scoredCount     = 0;   // FIX 5
        missedCount     = 0;

        // FIX 5: removed duplicate refreshDisplay calls and level toggle resets
        refreshDisplay(collectingCounterToggle, R.id.CollectingCounter, collectingCount);
        refreshDisplay(ferryingCounterToggle,   R.id.FerryingCounter,   ferryingCount);
        refreshDisplay(scoringCounterToggle,    R.id.ScoredCounter,     scoredCount);
        refreshDisplay(missedCounterToggle,     R.id.MissedCounter,     missedCount);

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

    // FIX 6: replaced inline lambdas with setup methods to match Auton/Teleop pattern
    private void setupCounterListeners() {
        setupCollectingListener();
        setupFerryingListener();
        setupScoredListener();
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
        // FIX 7: added scored branch
        if      (group == collectingCounterToggle) setupCollectingListener();
        else if (group == ferryingCounterToggle)   setupFerryingListener();
        else if (group == scoringCounterToggle)    setupScoredListener();
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
        // FIX 8+9: removed startLevelToggle/stopLevelToggle listeners and updateFuelStates entirely
        attemptedClimbToggle.setOnCheckedChangeListener((g, id)    -> updateClimbStates());
        successfulClimbedToggle.setOnCheckedChangeListener((g, id) -> updateClimbStates());
        updateClimbStates();
    }

    /**
     * FIX 10: check button IDs directly instead of fragile string comparisons.
     * Endgame XML has DNA | 1 | 2 | 3 for attempted and None | 1 | 2 | 3 for successful.
     * Location enabled only when neither first button is selected.
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
                saveEndGameData();
                appendEndGameSnapshot();
                resetEndGameUI();
                Toast.makeText(context, "EndGame snapshot saved", Toast.LENGTH_SHORT).show();
            });
        }

        if (resetButton != null) {
            resetButton.setOnClickListener(v -> {
                resetEndGameUI();
                Toast.makeText(context, "Changes cancelled", Toast.LENGTH_SHORT).show();
            });
        }

        // FIX 11+12: NextQRButton serves as both "next" and QR generation in endgame
        if (generateQRButton != null) {
            generateQRButton.setOnClickListener(v -> {
                saveEndGameData();
                appendEndGameSnapshot();
                Dialog loading_alert = new Dialog(context);
                loading_alert.requestWindowFeature(Window.FEATURE_NO_TITLE);
                loading_alert.setContentView(R.layout.screen_qr_loading);
                loading_alert.setCancelable(false);
                loading_alert.show();
                new Thread(new QRRunnable(context, loading_alert)).start();
            });
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

    private void loadEndGameData() {
        collectingCount = parseCount(hm("Collecting", "0"));
        ferryingCount   = parseCount(hm("Ferrying",   "0"));
        scoredCount     = parseCount(hm("Scored",     "0"));   // FIX 2
        missedCount     = parseCount(hm("Missed",     "0"));

        refreshDisplay(collectingCounterToggle, R.id.CollectingCounter, collectingCount);
        refreshDisplay(ferryingCounterToggle,   R.id.FerryingCounter,   ferryingCount);
        refreshDisplay(scoringCounterToggle,    R.id.ScoredCounter,     scoredCount);   // FIX 2
        refreshDisplay(missedCounterToggle,     R.id.MissedCounter,     missedCount);

        // FIX: removed startLevelToggle/stopLevelToggle selectByText calls
        selectByText(attemptedClimbToggle,              hm("AttemptedClimb",    "DID NOT ATTEMPT"));
        selectByText(successfulClimbedToggle,           hm("SuccessfulClimbed", "None"));
        selectByText(successfullyClimbedLocationToggle, hm("ClimbLocation",     "LEFT"));

        noShowSwitch.setChecked("Y".equals(hm("RobotFellOver", "N")));

        updateClimbStates();
    }

    private void saveEndGameData() {
        // FIX: removed StartLevel/StopLevel; added Scored
        endGameHashMap.put("Collecting",        String.valueOf(collectingCount));
        endGameHashMap.put("Ferrying",          String.valueOf(ferryingCount));
        endGameHashMap.put("Scored",            String.valueOf(scoredCount));
        endGameHashMap.put("Missed",            String.valueOf(missedCount));
        endGameHashMap.put("AttemptedClimb",    getSelectedText(attemptedClimbToggle,              "DID NOT ATTEMPT"));
        endGameHashMap.put("SuccessfulClimbed", getSelectedText(successfulClimbedToggle,           "None"));
        endGameHashMap.put("ClimbLocation",     getSelectedText(successfullyClimbedLocationToggle, "LEFT"));
        endGameHashMap.put("RobotFellOver",     noShowSwitch.isChecked() ? "Y" : "N");
        HashMapManager.putEndgameHashMap(endGameHashMap);
    }

    private String hm(String key, String def) {
        String v = endGameHashMap.get(key);
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
                setupHashMap   = HashMapManager.getSetupHashMap();
                endGameHashMap = HashMapManager.getEndgameHashMap();
                initializeSnapshots();
                loadEndGameData();
            } else {
                saveEndGameData();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onUpdate() { loadEndGameData(); }
}