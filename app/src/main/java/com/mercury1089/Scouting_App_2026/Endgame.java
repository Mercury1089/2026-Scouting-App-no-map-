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
import com.mercury1089.Scouting_App_2026.listeners.UpdateListener;
import com.mercury1089.Scouting_App_2026.qr.QRRunnable;

import java.util.LinkedHashMap;

public class Endgame extends Fragment implements UpdateListener {

    private static final String TAG = "EndGame Fragment";

    private int endGameSnapshotCount = 0;
    private LinkedHashMap<String, String> setupHashMap;
    private LinkedHashMap<String, String> endGameHashMap;
    private LinkedHashMap<String, String> teleopHasMap;

    // Snapshot System (CSV format)
    private StringBuilder snapshotBuilder;
    private static final String SNAPSHOT_HEADER =
            "scouterName,teamNumber,matchNumber,collecting,ferrying,scored,missed,attemptedClimb,successfulClimbed,climbLocation,noShow";

    // Climbing section
    private RadioGroup attemptedClimbToggle;
    private RadioGroup successfulClimbedToggle;
    private RadioGroup successfullyClimbedLocationToggle;

    // Other controls
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
        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.TELEOP);
        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.ENDGAME);
        setupHashMap  = HashMapManager.getSetupHashMap();
        teleopHasMap = HashMapManager.getTeleopHashMap();
        endGameHashMap = HashMapManager.getEndgameHashMap();

        // Link views
        assert getView() != null;
        attemptedClimbToggle              = getView().findViewById(R.id.AttemptedClimbToggle);
        successfulClimbedToggle           = getView().findViewById(R.id.SuccessfulClimbed);
        successfullyClimbedLocationToggle = getView().findViewById(R.id.SuccessfullyClimbedLocation);
        saveButton                        = getView().findViewById(R.id.SaveButton);
        resetButton                       = getView().findViewById(R.id.ResetButton);
        generateQRButton                  = getView().findViewById(R.id.NextQRButton);   // FIX 12

        initializeSnapshots();
        loadEndGameData();
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
        String matchNumber = setupHashMap.get("MatchNumber");
        if (matchNumber == null) matchNumber = "";
        String noShow = teleopHasMap.get("RobotFellOver");
        if (noShow == null) noShow = "N";

        String snapshotLine = String.format("%s,%s,%s,%d,%d,%d,%d,%s,%s,%s,%s\n",
                scouterName,
                teamNumber,
                matchNumber,
                collectingCount,
                ferryingCount,
                scoredCount,
                missedCount,
                getSelectedText(attemptedClimbToggle,              "0"),
                getSelectedText(successfulClimbedToggle,           "0"),
                getSelectedText(successfullyClimbedLocationToggle, "N"),
                noShow);

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
        missedCount     = 0;;

        if (attemptedClimbToggle != null) attemptedClimbToggle.clearCheck();
        if (successfulClimbedToggle != null) successfulClimbedToggle.clearCheck();
        if (successfullyClimbedLocationToggle != null) successfullyClimbedLocationToggle.clearCheck();

        updateClimbStates();
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

    // ─────────────────────────────────────────
    // DATA PERSISTENCE
    // ─────────────────────────────────────────

    private void loadEndGameData() {
        collectingCount = parseCount(hm("Collecting", "0"));
        ferryingCount   = parseCount(hm("Ferrying",   "0"));
        scoredCount     = parseCount(hm("Scored",     "0"));   // FIX 2
        missedCount     = parseCount(hm("Missed",     "0"));

        // FIX: removed startLevelToggle/stopLevelToggle selectByText calls
        selectByText(attemptedClimbToggle,              hm("AttemptedClimb",    "0"));
        selectByText(successfulClimbedToggle,           hm("SuccessfulClimbed", "0"));
        selectByText(successfullyClimbedLocationToggle, hm("ClimbLocation",     ""));

        updateClimbStates();
    }

    private void saveEndGameData() {
        // FIX: removed StartLevel/StopLevel; added Scored
        endGameHashMap.put("Collecting",        String.valueOf(collectingCount));
        endGameHashMap.put("Ferrying",          String.valueOf(ferryingCount));
        endGameHashMap.put("Scored",            String.valueOf(scoredCount));
        endGameHashMap.put("Missed",            String.valueOf(missedCount));
        endGameHashMap.put("AttemptedClimb",    getSelectedText(attemptedClimbToggle,              "0"));
        endGameHashMap.put("SuccessfulClimbed", getSelectedText(successfulClimbedToggle,           "0"));
        endGameHashMap.put("ClimbLocation",     getSelectedText(successfullyClimbedLocationToggle, ""));
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