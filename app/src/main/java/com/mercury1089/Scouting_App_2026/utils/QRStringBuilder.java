package com.mercury1089.Scouting_App_2026.utils;

import android.content.Context;

import com.mercury1089.Scouting_App_2026.HashMapManager;

import java.util.LinkedHashMap;

public class QRStringBuilder {

    private static StringBuilder QRString = new StringBuilder();
    public static final int SCOUTER_NAME_INDEX = 0;
    public static final int TEAM_NUM_INDEX     = 1;
    public static final int MATCH_NUM_INDEX    = 2;
    public static final String DELIMITER       = ",";
    public static final String ROW_DELIMITER   = "\n";

    // ─────────────────────────────────────────
    // LEVEL CONVERSION: "EMPTY"→0, "25%"→0.25, "50%"→0.5, "75%"→0.75, "FULL"→1
    // ─────────────────────────────────────────
    private static String levelToDecimal(String level) {
        if (level == null) return "0";
        switch (level.toUpperCase().trim()) {
            case "EMPTY": return "0";
            case "25%":   return "0.25";
            case "50%":   return "0.5";
            case "75%":   return "0.75";
            case "FULL":  return "1";
            default:      return "0";
        }
    }

    // ─────────────────────────────────────────
    // CLIMB CONVERSION: text → 0 or 1
    // "DID NOT ATTEMPT" | "None" → 0
    // "1" → 1
    // ─────────────────────────────────────────
    private static String climbToNumeric(String climb) {
        if (climb == null || climb.isEmpty()) return "0";
        return "1".equals(climb.trim()) ? "1" : "0";
    }

    // ─────────────────────────────────────────
    // PRELOAD CONVERSION: string → int (1-8)
    // Falls back to "1" if invalid
    // ─────────────────────────────────────────
    private static String preloadToInt(String preload) {
        if (preload == null || preload.isEmpty()) return "1";
        try {
            int val = Integer.parseInt(preload.trim());
            return (val >= 1 && val <= 8) ? String.valueOf(val) : "1";
        } catch (NumberFormatException e) {
            return "1";
        }
    }

    // ─────────────────────────────────────────
    // BUILD
    // CSV columns per row:
    // Scouter, Team, Match, Alliance, NoShow, Preload (1-8), FellOver,
    // A-Collecting, A-Ferrying, A-Missed, A-StartLevel (decimal), A-StopLevel (decimal),
    // A-AttemptedClimb (0/1), A-SuccessfulClimbed (0/1), A-ClimbLocation,
    // T-Collecting, T-Ferrying, T-Missed, T-StartLevel (decimal), T-StopLevel (decimal),
    // T-AttemptedClimb (0/1), T-SuccessfulClimbed (0/1), T-ClimbLocation,
    // E-Collecting, E-Ferrying, E-Missed, E-StartLevel (decimal), E-StopLevel (decimal),
    // E-AttemptedClimb (0/1), E-SuccessfulClimbed (0/1), E-ClimbLocation
    // ─────────────────────────────────────────
    public static void buildQRString() {
        LinkedHashMap<String, String> setup  = HashMapManager.getSetupHashMap();
        LinkedHashMap<String, String> auton  = HashMapManager.getAutonHashMap();
        LinkedHashMap<String, String> teleop = HashMapManager.getAutonHashMap();
        LinkedHashMap<String, String> endgame = HashMapManager.getAutonHashMap();

        // How many snapshots were saved?
        int autonCount  = parseIndex(auton.get("AutonSaveIndex"));
        int teleopCount = parseIndex(teleop.get("TeleopSaveIndex"));
        int endgameCount = parseIndex(endgame.get("EndgameSaveIndex"));
        int maxRows     = Math.max(1, Math.max(autonCount, Math.max(teleopCount, endgameCount)));

        // Fixed setup fields (same for every row)
        String scouter  = nvl(setup.get("ScouterName"));
        String team     = nvl(setup.get("TeamNumber"));
        String match    = nvl(setup.get("MatchNumber"));
        String alliance = nvl(setup.get("AllianceColor"));
        String noShow   = nvl(setup.get("NoShow"));
        String preload  = preloadToInt(setup.get("PreloadNote"));
        String fellOver = nvl(setup.get("FellOver"));


        for (int i = 1; i <= maxRows; i++) {
            // Row 1: include setup fields; subsequent rows: leave them blank
            if (i == 1) {
                QRString.append(scouter).append(",");
                QRString.append(team).append(",");
                QRString.append(match).append(",");
                QRString.append(alliance).append(",");
                QRString.append(noShow).append(",");
                QRString.append(preload).append(",");
                QRString.append(fellOver).append(",");

            } else {
                QRString.append(",,,,,,,"); // 7 empty setup fields
            }

            // ── Auton snapshot i ──
            if (i <= autonCount) {
                String sep = "__" + i;
                QRString.append(nvl(auton.get("Collecting"      + sep))).append(",");
                QRString.append(nvl(auton.get("Ferrying"        + sep))).append(",");
                QRString.append(nvl(auton.get("Missed"          + sep))).append(",");
                QRString.append(levelToDecimal(auton.get("StartLevel"    + sep))).append(",");
                QRString.append(levelToDecimal(auton.get("StopLevel"     + sep))).append(",");
                QRString.append(climbToNumeric(auton.get("AttemptedClimb"  + sep))).append(",");
                QRString.append(climbToNumeric(auton.get("SuccessfulClimbed"+ sep))).append(",");
                QRString.append(nvl(auton.get("ClimbLocation"   + sep))).append(",");
            } else if (i == 1) {
                // Fall back to base (non-snapshot) auton data for row 1 if no snapshots
                QRString.append(nvl(auton.get("Collecting"))).append(",");
                QRString.append(nvl(auton.get("Ferrying"))).append(",");
                QRString.append(nvl(auton.get("Missed"))).append(",");
                QRString.append(levelToDecimal(auton.get("StartLevel"))).append(",");
                QRString.append(levelToDecimal(auton.get("StopLevel"))).append(",");
                QRString.append(climbToNumeric(auton.get("AttemptedClimb"))).append(",");
                QRString.append(climbToNumeric(auton.get("SuccessfulClimbed"))).append(",");
                QRString.append(nvl(auton.get("ClimbLocation"))).append(",");
            } else {
                QRString.append(",,,,,,,,,"); // 8 empty auton fields
            }

            // ── Teleop snapshot i ──
            if (i <= teleopCount) {
                String sep = "__" + i;
                QRString.append(nvl(teleop.get("Collecting"      + sep))).append(",");
                QRString.append(nvl(teleop.get("Ferrying"         + sep))).append(",");
                QRString.append(nvl(teleop.get("Missed"           + sep))).append(",");
                QRString.append(levelToDecimal(teleop.get("StartLevel"     + sep))).append(",");
                QRString.append(levelToDecimal(teleop.get("StopLevel"      + sep))).append(",");
                QRString.append(climbToNumeric(teleop.get("AttemptedClimb"   + sep))).append(",");
                QRString.append(climbToNumeric(teleop.get("SuccessfulClimbed"+ sep))).append(",");
                QRString.append(nvl(teleop.get("ClimbLocation"    + sep))).append(",");
            } else if (i == 1) {
                // Fall back to base teleop data for row 1 if no snapshots
                QRString.append(nvl(teleop.get("Collecting"))).append(",");
                QRString.append(nvl(teleop.get("Ferrying"))).append(",");
                QRString.append(nvl(teleop.get("Missed"))).append(",");
                QRString.append(levelToDecimal(teleop.get("StartLevel"))).append(",");
                QRString.append(levelToDecimal(teleop.get("StopLevel"))).append(",");
                QRString.append(climbToNumeric(teleop.get("AttemptedClimb"))).append(",");
                QRString.append(climbToNumeric(teleop.get("SuccessfulClimbed"))).append(",");
                QRString.append(nvl(teleop.get("ClimbLocation"))).append(",");
            } else {
                QRString.append(",,,,,,,,,"); // 8 empty teleop fields
            }

            // ── Endgame snapshot i ──
            if (i <= endgameCount) {
                String sep = "__" + i;
                QRString.append(nvl(endgame.get("Collecting"      + sep))).append(",");
                QRString.append(nvl(endgame.get("Ferrying"         + sep))).append(",");
                QRString.append(nvl(endgame.get("Missed"           + sep))).append(",");
                QRString.append(levelToDecimal(endgame.get("StartLevel"     + sep))).append(",");
                QRString.append(levelToDecimal(endgame.get("StopLevel"      + sep))).append(",");
                QRString.append(climbToNumeric(endgame.get("AttemptedClimb"   + sep))).append(",");
                QRString.append(climbToNumeric(endgame.get("SuccessfulClimbed"+ sep))).append(",");
                QRString.append(nvl(endgame.get("ClimbLocation"    + sep)));
            } else if (i == 1) {
                // Fall back to base endgame data for row 1 if no snapshots
                QRString.append(nvl(endgame.get("Collecting"))).append(",");
                QRString.append(nvl(endgame.get("Ferrying"))).append(",");
                QRString.append(nvl(endgame.get("Missed"))).append(",");
                QRString.append(levelToDecimal(endgame.get("StartLevel"))).append(",");
                QRString.append(levelToDecimal(endgame.get("StopLevel"))).append(",");
                QRString.append(climbToNumeric(endgame.get("AttemptedClimb"))).append(",");
                QRString.append(climbToNumeric(endgame.get("SuccessfulClimbed"))).append(",");
                QRString.append(nvl(endgame.get("ClimbLocation")));
            } else {
                QRString.append(",,,,,,,"); // 8 empty endgame fields (no trailing comma on last)
            }

            if (i < maxRows) QRString.append(ROW_DELIMITER);
        }
    }

    // ─────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    private static int parseIndex(String s) {
        if (s == null) return 0;
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return 0; }
    }

    // ─────────────────────────────────────────
    // ACCESSORS  (split on first row only)
    // ─────────────────────────────────────────

    public static String getQRString() { return QRString.toString(); }

    public static String getScouterName() {
        return getField(SCOUTER_NAME_INDEX);
    }

    public static String getTeamNumber() {
        return getField(TEAM_NUM_INDEX);
    }

    public static String getMatchNumber() {
        return getField(MATCH_NUM_INDEX);
    }

    private static String getField(int index) {
        if (QRString.toString().isEmpty()) return null;
        String firstRow = QRString.toString().split(ROW_DELIMITER)[0];
        String[] parts  = firstRow.split(DELIMITER);
        return index < parts.length ? parts[index] : null;
    }

    public static void storeQRString(Context ctx) {
        HashMapManager.appendQRList(QRString.toString(), ctx);
    }

    public static void clearQRString() {
        QRString = new StringBuilder();
    }
}