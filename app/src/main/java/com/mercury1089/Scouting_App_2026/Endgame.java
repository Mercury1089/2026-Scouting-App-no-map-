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
        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.AUTON);
        setupHashMap   = HashMapManager.getSetupHashMap();
        endgameHashMap = HashMapManager.getAutonHashMap();

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
        topEdgeBar                        = getView().findViewById(R.id.topEdgeBar);
        bottomEdgeBar                     = getView().findViewById(R.id.bottomEdgeBar);
        leftEdgeBar                       = getView().findViewById(R.id.leftEdgeBar);
        rightEdgeBar                      = getView().findViewById(R.id.rightEdgeBar);

        loadEndgameData();
        setupCascadingListeners();
        setupButtonListeners();
        setupTimer();
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
            Toast.makeText(context, "Snapshot saved", Toast.LENGTH_SHORT).show();
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

        timer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long ms) {
                long secs = ms / 1000;
                secondsRemaining.setText(String.valueOf(secs));
                if (!running) return;

                if (secs <= 5 && secs > 0) {
                    postMatchWarning.setVisibility(View.VISIBLE);
                    timerID.setTextColor(context.getResources().getColor(R.color.banana));
                    timerID.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.timer_yellow, 0, 0, 0);
                    if (vibrator != null) vibrator.vibrate(500);
                    pulseEdgeBars();
                }
            }

            @Override
            public void onFinish() {
                if (!running) return;
                secondsRemaining.setText("0");
                setAllEdgeBars(R.drawable.teleop_warning);
                timerID.setTextColor(context.getResources().getColor(R.color.border_warning));
                timerID.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.timer_red, 0, 0, 0);
                postMatchWarning.setVisibility(View.VISIBLE);
                postMatchWarning.setTextColor(getResources().getColor(R.color.white));
                postMatchWarning.setText(getString(R.string.EndGameError));
            }
        };

        if (firstTime) {
            firstTime = false;
            timer.start();
        }
    }

    private void pulseEdgeBars() {
        for (ImageView bar : new ImageView[]{topEdgeBar, bottomEdgeBar, leftEdgeBar, rightEdgeBar}) {
            ObjectAnimator anim = ObjectAnimator.ofFloat(bar, View.ALPHA, 0f, 1f);
            anim.setDuration(500);
            anim.setRepeatMode(ObjectAnimator.REVERSE);
            anim.setRepeatCount(1);
            anim.start();
        }
    }

    private void setAllEdgeBars(int drawableRes) {
        for (ImageView bar : new ImageView[]{topEdgeBar, bottomEdgeBar, leftEdgeBar, rightEdgeBar})
            bar.setBackground(getResources().getDrawable(drawableRes));
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

    private static final String KEY_ENDGAME_SAVE_INDEX = "EndgameSaveIndex";
    private static final String SNAP_SEP = "__";

    private int nextEndgameSaveIndex() {
        String cur = endgameHashMap.get(KEY_ENDGAME_SAVE_INDEX);
        int idx = 0;
        try { idx = (cur == null) ? 0 : Integer.parseInt(cur); }
        catch (NumberFormatException ignored) { idx = 0; }
        idx += 1;
        endgameHashMap.put(KEY_ENDGAME_SAVE_INDEX, String.valueOf(idx));
        return idx;
    }

    private String snapKey(String baseKey, int idx) {
        return baseKey + SNAP_SEP + idx;
    }

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
        HashMapManager.putAutonHashMap(endgameHashMap);
    }

    private void appendEndgameSnapshot() {
        saveEndgameData();

        int idx = nextEndgameSaveIndex();

        endgameHashMap.put(snapKey("ts", idx),                String.valueOf(System.currentTimeMillis()));
        endgameHashMap.put(snapKey("Collecting", idx),        getSelectedText(collectingCounterToggle, ">75"));
        endgameHashMap.put(snapKey("Ferrying", idx),          getSelectedText(ferryingCounterToggle,   ">75"));
        endgameHashMap.put(snapKey("Missed", idx),            getSelectedText(missedCounterToggle,     ">75"));
        endgameHashMap.put(snapKey("StartLevel", idx),        getLevelValue(startLevelToggle));
        endgameHashMap.put(snapKey("StopLevel", idx),         getLevelValue(stopLevelToggle));
        endgameHashMap.put(snapKey("AttemptedClimb", idx),    getSelectedText(attemptedClimbToggle,               "DID NOT ATTEMPT"));
        endgameHashMap.put(snapKey("SuccessfulClimbed", idx), getSelectedText(successfulClimbedToggle,            "None"));
        endgameHashMap.put(snapKey("ClimbLocation", idx),     getSelectedText(successfullyClimbedLocationToggle,  "LEFT"));
        endgameHashMap.put(snapKey("RobotFellOver", idx),     noShowSwitch.isChecked() ? "Y" : "N");

        HashMapManager.putAutonHashMap(endgameHashMap);
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
                endgameHashMap = HashMapManager.getAutonHashMap();
                loadEndgameData();
            } else {
                saveEndgameData();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        running = false;
        if (timer != null) timer.cancel();
    }

    @Override
    public void onUpdate() { loadEndgameData(); }
}