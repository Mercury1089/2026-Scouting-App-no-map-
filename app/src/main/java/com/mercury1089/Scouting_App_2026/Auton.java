package com.mercury1089.Scouting_App_2026;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Vibrator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import java.util.LinkedHashMap;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.mercury1089.Scouting_App_2026.listeners.UpdateListener;
import com.mercury1089.Scouting_App_2026.utils.GenUtils;

public class Auton extends Fragment implements UpdateListener {
    // HashMaps for data persistence
    private LinkedHashMap<String, String> setupHashMap;
    private LinkedHashMap<String, String> autonHashMap;

    // ═════════════════════════════════════════
    // FUEL SECTION (Left Side)
    // ═════════════════════════════════════════
    private RadioGroup collectingCounterToggle;
    private RadioGroup ferryingCounterToggle;
    private RadioGroup startLevelToggle;
    private RadioGroup stopLevelToggle;
    private RadioGroup missedCounterToggle;

    // ═════════════════════════════════════════
    // CLIMBING SECTION (Right Side)
    // ═════════════════════════════════════════
    private RadioGroup attemptedClimbToggle;
    private RadioGroup successfulClimbedToggle;
    private RadioGroup successfullyClimbedLocationToggle;

    // ═════════════════════════════════════════
    // OTHER CONTROLS
    // ═════════════════════════════════════════
    private Switch noShowSwitch;
    private Button saveButton;
    private Button cancelButton;
    private Button nextButtonAuton;

    // ═════════════════════════════════════════
    // TIMER & ANIMATION
    // ═════════════════════════════════════════
    private TextView timerID;
    private TextView secondsRemaining;
    private TextView teleopWarning;

    // Edge bars for animation
    private ImageView topEdgeBar;
    private ImageView bottomEdgeBar;
    private ImageView leftEdgeBar;
    private ImageView rightEdgeBar;

    private static CountDownTimer timer;
    private boolean firstTime = true;
    private boolean running = true;
    private ValueAnimator teleopButtonAnimation;
    private AnimatorSet animatorSet;
    private MatchActivity context;

    public static Auton newInstance() {
        Auton fragment = new Auton();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = (MatchActivity) getActivity();
        View inflated = null;
        try {
            // //FIX: Use the correct layout file name for the tower/form-based auton screen
            inflated = inflater.inflate(R.layout.auton_screen, container, false);
        } catch (InflateException e) {
            Log.d("Auton", "Layout inflation error: " + e.getMessage());
            throw e;
        }
        return inflated;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onStart() {
        setupHashMap = HashMapManager.getSetupHashMap();
        autonHashMap = HashMapManager.getAutonHashMap();
        super.onStart();

        // ═════════════════════════════════════════
        // LINK FUEL SECTION VIEWS
        // ═════════════════════════════════════════
        collectingCounterToggle = getView().findViewById(R.id.CollectingCounterToggle);
        ferryingCounterToggle = getView().findViewById(R.id.FerryingCounterToggle);
        startLevelToggle = getView().findViewById(R.id.StartLevelToggle);
        stopLevelToggle = getView().findViewById(R.id.StopLevelToggle);
        missedCounterToggle = getView().findViewById(R.id.MissedCounterToggle);

        // ═════════════════════════════════════════
        // LINK CLIMBING SECTION VIEWS
        // ═════════════════════════════════════════
        attemptedClimbToggle = getView().findViewById(R.id.AttemptedClimbToggle);
        successfulClimbedToggle = getView().findViewById(R.id.SuccessfulClimbed);
        successfullyClimbedLocationToggle = getView().findViewById(R.id.SuccessfullyClimbedLocation);

        // ═════════════════════════════════════════
        // LINK OTHER CONTROLS
        // ═════════════════════════════════════════
        noShowSwitch = getView().findViewById(R.id.NoShowSwitch);
        saveButton = getView().findViewById(R.id.SaveButton);
        cancelButton = getView().findViewById(R.id.CancelButton);
        nextButtonAuton = getView().findViewById(R.id.NextButtonAuton);

        // ═════════════════════════════════════════
        // LINK TIMER VIEWS
        // ═════════════════════════════════════════
        timerID = getView().findViewById(R.id.IDAutonSeconds1);
        secondsRemaining = getView().findViewById(R.id.AutonSeconds);
        teleopWarning = getView().findViewById(R.id.TeleopWarning);

        // Link edge bars for animations
        topEdgeBar = getView().findViewById(R.id.topEdgeBar);
        bottomEdgeBar = getView().findViewById(R.id.bottomEdgeBar);
        leftEdgeBar = getView().findViewById(R.id.leftEdgeBar);
        rightEdgeBar = getView().findViewById(R.id.rightEdgeBar);

        // Get HashMap data (fill with defaults if empty or null)
        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.SETUP);
        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.AUTON);
        setupHashMap = HashMapManager.getSetupHashMap();
        autonHashMap = HashMapManager.getAutonHashMap();

        // Load saved data into UI
        loadAutonData();

        // ═════════════════════════════════════════
        // SETUP CASCADING LOGIC LISTENERS
        // ═════════════════════════════════════════

        // Fuel section cascading: MISSED only enabled when START & STOP both selected
        RadioGroup.OnCheckedChangeListener fuelStateListener = (group, checkedId) ->
                updateFuelButtonStates();

        startLevelToggle.setOnCheckedChangeListener(fuelStateListener);
        stopLevelToggle.setOnCheckedChangeListener(fuelStateListener);

        // Initial state update for fuel section
        updateFuelButtonStates();

        // Climbing section cascading: Location only enabled when Attempted = 1 AND Successful = 1
        RadioGroup.OnCheckedChangeListener climbingStateListener = (group, checkedId) ->
                updateClimbingButtonStates();

        attemptedClimbToggle.setOnCheckedChangeListener(climbingStateListener);
        successfulClimbedToggle.setOnCheckedChangeListener(climbingStateListener);

        // Initial state update for climbing section
        updateClimbingButtonStates();

        // ═════════════════════════════════════════
        // SETUP BUTTON LISTENERS
        // ═════════════════════════════════════════

        saveButton.setOnClickListener(v -> {
            saveAutonData();
            Toast.makeText(context, "Data saved", Toast.LENGTH_SHORT).show();
        });

        cancelButton.setOnClickListener(v -> {
            // Cancel without saving - just reload from HashMap
            loadAutonData();
            Toast.makeText(context, "Changes cancelled", Toast.LENGTH_SHORT).show();
        });

        nextButtonAuton.setOnClickListener(v -> {
            // Save before moving to next phase
            saveAutonData();
            context.tabs.getTabAt(1).select();
        });

        // ═════════════════════════════════════════
        // SETUP TIMER
        // ═════════════════════════════════════════

        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        timer = new CountDownTimer(20000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                secondsRemaining.setText(GenUtils.padLeftZeros("" + millisUntilFinished / 1000, 2));

                if (!running) return;

                // Warning at 3 seconds remaining
                if (millisUntilFinished / 1000 <= 3 && millisUntilFinished / 1000 > 0) {
                    teleopWarning.setVisibility(View.VISIBLE);
                    timerID.setTextColor(context.getResources().getColor(R.color.banana));
                    timerID.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.timer_yellow, 0, 0, 0);

                    if (vibrator != null) {
                        vibrator.vibrate(500);
                    }

                    // Animate edge bars
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

                    AnimatorSet edgeAnimatorSet = new AnimatorSet();
                    edgeAnimatorSet.playTogether(topEdgeLighter, bottomEdgeLighter, leftEdgeLighter, rightEdgeLighter);
                    edgeAnimatorSet.start();
                }
            }

            @Override
            public void onFinish() {
                if (running) {
                    secondsRemaining.setText("00");
                    topEdgeBar.setBackground(getResources().getDrawable(R.drawable.teleop_warning));
                    bottomEdgeBar.setBackground(getResources().getDrawable(R.drawable.teleop_warning));
                    leftEdgeBar.setBackground(getResources().getDrawable(R.drawable.teleop_warning));
                    rightEdgeBar.setBackground(getResources().getDrawable(R.drawable.teleop_warning));
                    timerID.setTextColor(context.getResources().getColor(R.color.border_warning));
                    timerID.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.timer_red, 0, 0, 0);
                    teleopWarning.setVisibility(View.VISIBLE);
                    teleopWarning.setTextColor(getResources().getColor(R.color.white));
                    teleopWarning.setText(getResources().getString(R.string.TeleopError));
                }
            }
        };

        if (firstTime) {
            firstTime = false;
            timer.start();
        }
    }

    // ═════════════════════════════════════════
    // CASCADING LOGIC - FUEL SECTION
    // ═════════════════════════════════════════

    /**
     * Update fuel button states based on cascading logic:
     * - Collecting: Always enabled
     * - Ferrying: Always enabled
     * - Start Level: Always enabled
     * - Stop Level: Always enabled
     * - MISSED: ONLY enabled when BOTH Start ≠ Empty AND Stop ≠ Empty
     */
    private void updateFuelButtonStates() {
        String startLevel = getStartLevelValue();
        String stopLevel = getStopLevelValue();

        // Always enable these
        setRadioGroupEnabled(collectingCounterToggle, true);
        setRadioGroupEnabled(ferryingCounterToggle, true);
        setRadioGroupEnabled(startLevelToggle, true);
        setRadioGroupEnabled(stopLevelToggle, true);

        // MISSED only enabled if BOTH levels are selected
        boolean bothLevelsSelected = !startLevel.equals("Empty") && !stopLevel.equals("Empty");
        setRadioGroupEnabled(missedCounterToggle, bothLevelsSelected);
    }

    // ═════════════════════════════════════════
    // CASCADING LOGIC - CLIMBING SECTION
    // ═════════════════════════════════════════

    /**
     * Update climbing button states based on cascading logic:
     * - Attempted Climb: Always enabled
     * - Successfully Climbed: Always enabled
     * - Tower Climb Location: ONLY enabled when Attempted = "1" AND Successfully Climbed = "1"
     */
    private void updateClimbingButtonStates() {
        String attemptedValue = getAttemptedClimbValue();
        String successfulValue = getSuccessfulClimbValue();

        // Always enable these
        setRadioGroupEnabled(attemptedClimbToggle, true);
        setRadioGroupEnabled(successfulClimbedToggle, true);

        // Location only enabled if robot successfully climbed
        boolean robotClimbed = "1".equals(attemptedValue) && "1".equals(successfulValue);
        setRadioGroupEnabled(successfullyClimbedLocationToggle, robotClimbed);
    }

    // ═════════════════════════════════════════
    // HELPER METHODS - GET VALUES
    // ═════════════════════════════════════════

    private String getCounterValue(RadioGroup group) {
        int selectedId = group.getCheckedRadioButtonId();
        if (selectedId == -1) return "000";
        RadioButton button = group.findViewById(selectedId);
        return button != null ? button.getText().toString().trim() : "000";
    }

    private String getStartLevelValue() {
        int selectedId = startLevelToggle.getCheckedRadioButtonId();
        if (selectedId == -1) return "Empty";
        RadioButton button = startLevelToggle.findViewById(selectedId);
        if (button != null) {
            String text = button.getText().toString().trim();
            if (text.toLowerCase().contains("empty")) return "Empty";
            if (text.contains("25")) return "25";
            if (text.contains("50")) return "50";
            if (text.contains("75")) return "75";
            if (text.toLowerCase().contains("full")) return "Full";
            return text;
        }
        return "Empty";
    }

    private String getStopLevelValue() {
        int selectedId = stopLevelToggle.getCheckedRadioButtonId();
        if (selectedId == -1) return "Empty";
        RadioButton button = stopLevelToggle.findViewById(selectedId);
        if (button != null) {
            String text = button.getText().toString().trim();
            if (text.toLowerCase().contains("empty")) return "Empty";
            if (text.contains("25")) return "25";
            if (text.contains("50")) return "50";
            if (text.contains("75")) return "75";
            if (text.toLowerCase().contains("full")) return "Full";
            return text;
        }
        return "Empty";
    }

    private String getAttemptedClimbValue() {
        int selectedId = attemptedClimbToggle.getCheckedRadioButtonId();
        if (selectedId == -1) return "DNA";
        RadioButton button = attemptedClimbToggle.findViewById(selectedId);
        return button != null ? button.getText().toString().trim() : "DNA";
    }

    private String getSuccessfulClimbValue() {
        int selectedId = successfulClimbedToggle.getCheckedRadioButtonId();
        if (selectedId == -1) return "None";
        RadioButton button = successfulClimbedToggle.findViewById(selectedId);
        return button != null ? button.getText().toString().trim() : "None";
    }

    private String getClimbLocationValue() {
        int selectedId = successfullyClimbedLocationToggle.getCheckedRadioButtonId();
        if (selectedId == -1) return "None";
        RadioButton button = successfullyClimbedLocationToggle.findViewById(selectedId);
        return button != null ? button.getText().toString().trim() : "None";
    }

    // ═════════════════════════════════════════
    // HELPER METHODS - SET VALUES
    // ═════════════════════════════════════════

    private void setCounterValue(RadioGroup group, String value) {
        for (int i = 0; i < group.getChildCount(); i++) {
            RadioButton button = (RadioButton) group.getChildAt(i);
            if (button.getText().toString().trim().equals(value)) {
                group.check(button.getId());
                return;
            }
        }
        // Default to first button if not found
        if (group.getChildCount() > 0) {
            group.check(((RadioButton) group.getChildAt(0)).getId());
        }
    }

    private void setLevelValue(RadioGroup group, String value) {
        for (int i = 0; i < group.getChildCount(); i++) {
            RadioButton button = (RadioButton) group.getChildAt(i);
            String buttonText = button.getText().toString().trim();

            if (buttonText.equalsIgnoreCase(value) ||
                    (value.equals("25") && buttonText.contains("25")) ||
                    (value.equals("50") && buttonText.contains("50")) ||
                    (value.equals("75") && buttonText.contains("75"))) {
                group.check(button.getId());
                return;
            }
        }
        // Default to first button
        if (group.getChildCount() > 0) {
            group.check(((RadioButton) group.getChildAt(0)).getId());
        }
    }

    private void setClimbValue(RadioGroup group, String value) {
        for (int i = 0; i < group.getChildCount(); i++) {
            RadioButton button = (RadioButton) group.getChildAt(i);
            if (button.getText().toString().trim().equals(value)) {
                group.check(button.getId());
                return;
            }
        }
        // Default to first button
        if (group.getChildCount() > 0) {
            group.check(((RadioButton) group.getChildAt(0)).getId());
        }
    }

    // ═════════════════════════════════════════
    // HELPER METHODS - ENABLE/DISABLE
    // ═════════════════════════════════════════

    /**
     * Enable or disable all radio buttons in a RadioGroup
     */
    private void setRadioGroupEnabled(RadioGroup group, boolean enabled) {
        for (int i = 0; i < group.getChildCount(); i++) {
            group.getChildAt(i).setEnabled(enabled);
        }
    }

    // ═════════════════════════════════════════
    // DATA PERSISTENCE
    // ═════════════════════════════════════════

    /**
     * Load all auton data from HashMap into UI controls
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void loadAutonData() {
        // Load fuel section
        setCounterValue(collectingCounterToggle, autonHashMap.getOrDefault("Collecting", "000"));
        setCounterValue(ferryingCounterToggle, autonHashMap.getOrDefault("Ferrying", "000"));
        setLevelValue(startLevelToggle, autonHashMap.getOrDefault("StartLevel", "Empty"));
        setLevelValue(stopLevelToggle, autonHashMap.getOrDefault("StopLevel", "Empty"));
        setCounterValue(missedCounterToggle, autonHashMap.getOrDefault("Missed", "000"));

        // Load climbing section
        setClimbValue(attemptedClimbToggle, autonHashMap.getOrDefault("AttemptedClimb", "DNA"));
        setClimbValue(successfulClimbedToggle, autonHashMap.getOrDefault("SuccessfulClimbed", "None"));
        setClimbValue(successfullyClimbedLocationToggle, autonHashMap.getOrDefault("ClimbLocation", "None"));

        // Load robot fell over switch
        noShowSwitch.setChecked("Y".equals(autonHashMap.getOrDefault("RobotFellOver", "N")));

        // Update cascading logic states
        updateFuelButtonStates();
        updateClimbingButtonStates();
    }

    /**
     * Save all auton data from UI controls to HashMap
     */
    private void saveAutonData() {
        // Save fuel section
        autonHashMap.put("Collecting", getCounterValue(collectingCounterToggle));
        autonHashMap.put("Ferrying", getCounterValue(ferryingCounterToggle));
        autonHashMap.put("StartLevel", getStartLevelValue());
        autonHashMap.put("StopLevel", getStopLevelValue());
        autonHashMap.put("Missed", getCounterValue(missedCounterToggle));

        // Save climbing section
        autonHashMap.put("AttemptedClimb", getAttemptedClimbValue());
        autonHashMap.put("SuccessfulClimbed", getSuccessfulClimbValue());
        autonHashMap.put("ClimbLocation", getClimbLocationValue());

        // Save robot fell over switch
        autonHashMap.put("RobotFellOver", noShowSwitch.isChecked() ? "Y" : "N");

        // Update HashMapManager
        HashMapManager.putAutonHashMap(autonHashMap);
    }

    // ═════════════════════════════════════════
    // LIFECYCLE METHODS
    // ═════════════════════════════════════════

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (this.isVisible()) {
            if (isVisibleToUser) {
                // Fragment becoming visible - load fresh data
                setupHashMap = HashMapManager.getSetupHashMap();
                autonHashMap = HashMapManager.getAutonHashMap();
                loadAutonData();
            } else {
                // Fragment hiding - save current data
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
        }
    }

    @Override
    public void onUpdate() {
        loadAutonData();
    }
}