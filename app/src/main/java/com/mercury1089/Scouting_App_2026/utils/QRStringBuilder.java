package com.mercury1089.Scouting_App_2026.utils;

import com.mercury1089.Scouting_App_2026.HashMapManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class QRStringBuilder {

    private static StringBuilder QRString = new StringBuilder();
    public static final int SCOUTER_NAME_INDEX = 0;
    public static final int TEAM_NUM_INDEX     = 1;
    public static final int MATCH_NUM_INDEX    = 2;
    public static final String DELIMITER       = ",";
    public static final String ROW_DELIMITER   = "\n";

    // ─────────────────────────────────────────
    // LEVEL CONVERSION
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
    // CLIMB CONVERSION
    // "DID NOT ATTEMPT" | "None" → 0, anything else → 1
    // ─────────────────────────────────────────
    private static String climbToNumeric(String climb) {
        if (climb == null || climb.isEmpty()) return "0";
        String t = climb.trim();
        if (t.equalsIgnoreCase("DID NOT ATTEMPT") || t.equalsIgnoreCase("None") || t.equals("0"))
            return "0";
        return "1";
    }

    // ─────────────────────────────────────────
    // PRELOAD CONVERSION
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
    // PARSE SNAPSHOT CSV
    // Returns list of data rows (skips header line)
    // Each row is a String[] of columns
    // ─────────────────────────────────────────
    private static List<String[]> parseSnapshots(String snapshotCsv) {
        List<String[]> rows = new ArrayList<>();
        if (snapshotCsv == null || snapshotCsv.isEmpty()) return rows;

        String[] lines = snapshotCsv.split("\n");
        // lines[0] is the header — skip it
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty()) {
                rows.add(line.split(",", -1));
            }
        }
        return rows;
    }

    // Column indices matching SNAPSHOT_HEADER:
    // collecting,ferrying,missed,startLevel,stopLevel,attemptedClimb,successfulClimbed,climbLocation,robotFellOver
    private static final int COL_COLLECTING       = 0;
    private static final int COL_FERRYING         = 1;
    private static final int COL_MISSED           = 2;
    private static final int COL_START_LEVEL      = 3;
    private static final int COL_STOP_LEVEL       = 4;
    private static final int COL_ATTEMPTED_CLIMB  = 5;
    private static final int COL_SUCCESSFUL_CLIMB = 6;
    private static final int COL_CLIMB_LOCATION   = 7;
    private static final int COL_FELL_OVER        = 8;

    private static String col(String[] row, int index) {
        if (row == null || index >= row.length) return "";
        return row[index] != null ? row[index].trim() : "";
    }

    // ─────────────────────────────────────────
    // BUILD
    // ─────────────────────────────────────────
    public static void buildQRString() {
        // Always reset before building
        QRString = new StringBuilder();

        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.SETUP);
        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.AUTON);
        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.TELEOP);
        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.ENDGAME);

        LinkedHashMap<String, String> setup   = HashMapManager.getSetupHashMap();
        LinkedHashMap<String, String> auton   = HashMapManager.getAutonHashMap();
        LinkedHashMap<String, String> teleop  = HashMapManager.getTeleopHashMap();
        LinkedHashMap<String, String> endgame = HashMapManager.getEndgameHashMap();

        // Parse snapshot CSVs
        List<String[]> autonRows   = parseSnapshots(auton.get("snapshots"));
        List<String[]> teleopRows  = parseSnapshots(teleop.get("snapshots"));
        List<String[]> endgameRows = parseSnapshots(endgame.get("snapshots"));

        int maxRows = Math.max(1, Math.max(autonRows.size(),
                Math.max(teleopRows.size(), endgameRows.size())));

        // Fixed setup fields
        String scouter  = nvl(setup.get("ScouterName"));
        String team     = nvl(setup.get("TeamNumber"));
        String match    = nvl(setup.get("MatchNumber"));
        String alliance = nvl(setup.get("AllianceColor"));
        String noShow   = nvl(setup.get("NoShow"));
        String preload  = preloadToInt(setup.get("PreloadNote"));
        String fellOver = nvl(setup.get("FellOver"));

        for (int i = 0; i < maxRows; i++) {
            // Row 0: include setup fields; subsequent rows: leave blank
            if (i == 0) {
                QRString.append(scouter).append(",");
                QRString.append(team).append(",");
                QRString.append(match).append(",");
                QRString.append(alliance).append(",");
                QRString.append(noShow).append(",");
                QRString.append(preload).append(",");
                QRString.append(fellOver).append(",");
            } else {
                QRString.append(",,,,,,,");
            }

            // ── Auton snapshot i ──
            if (i < autonRows.size()) {
                String[] row = autonRows.get(i);
                QRString.append(col(row, COL_COLLECTING)).append(",");
                QRString.append(col(row, COL_FERRYING)).append(",");
                QRString.append(col(row, COL_MISSED)).append(",");
                QRString.append(levelToDecimal(col(row, COL_START_LEVEL))).append(",");
                QRString.append(levelToDecimal(col(row, COL_STOP_LEVEL))).append(",");
                QRString.append(climbToNumeric(col(row, COL_ATTEMPTED_CLIMB))).append(",");
                QRString.append(climbToNumeric(col(row, COL_SUCCESSFUL_CLIMB))).append(",");
                QRString.append(col(row, COL_CLIMB_LOCATION)).append(",");
            } else if (i == 0) {
                // No snapshots — fall back to base hashmap values
                QRString.append(nvl(auton.get("Collecting"))).append(",");
                QRString.append(nvl(auton.get("Ferrying"))).append(",");
                QRString.append(nvl(auton.get("Missed"))).append(",");
                QRString.append(levelToDecimal(auton.get("StartLevel"))).append(",");
                QRString.append(levelToDecimal(auton.get("StopLevel"))).append(",");
                QRString.append(climbToNumeric(auton.get("AttemptedClimb"))).append(",");
                QRString.append(climbToNumeric(auton.get("SuccessfulClimbed"))).append(",");
                QRString.append(nvl(auton.get("ClimbLocation"))).append(",");
            } else {
                QRString.append(",,,,,,,,");
            }

            // ── Teleop snapshot i ──
            if (i < teleopRows.size()) {
                String[] row = teleopRows.get(i);
                QRString.append(col(row, COL_COLLECTING)).append(",");
                QRString.append(col(row, COL_FERRYING)).append(",");
                QRString.append(col(row, COL_MISSED)).append(",");
                QRString.append(levelToDecimal(col(row, COL_START_LEVEL))).append(",");
                QRString.append(levelToDecimal(col(row, COL_STOP_LEVEL))).append(",");
                QRString.append(climbToNumeric(col(row, COL_ATTEMPTED_CLIMB))).append(",");
                QRString.append(climbToNumeric(col(row, COL_SUCCESSFUL_CLIMB))).append(",");
                QRString.append(col(row, COL_CLIMB_LOCATION)).append(",");
            } else if (i == 0) {
                QRString.append(nvl(teleop.get("Collecting"))).append(",");
                QRString.append(nvl(teleop.get("Ferrying"))).append(",");
                QRString.append(nvl(teleop.get("Missed"))).append(",");
                QRString.append(levelToDecimal(teleop.get("StartLevel"))).append(",");
                QRString.append(levelToDecimal(teleop.get("StopLevel"))).append(",");
                QRString.append(climbToNumeric(teleop.get("AttemptedClimb"))).append(",");
                QRString.append(climbToNumeric(teleop.get("SuccessfulClimbed"))).append(",");
                QRString.append(nvl(teleop.get("ClimbLocation"))).append(",");
            } else {
                QRString.append(",,,,,,,,");
            }

            // ── Endgame snapshot i ──
            if (i < endgameRows.size()) {
                String[] row = endgameRows.get(i);
                QRString.append(col(row, COL_COLLECTING)).append(",");
                QRString.append(col(row, COL_FERRYING)).append(",");
                QRString.append(col(row, COL_MISSED)).append(",");
                QRString.append(levelToDecimal(col(row, COL_START_LEVEL))).append(",");
                QRString.append(levelToDecimal(col(row, COL_STOP_LEVEL))).append(",");
                QRString.append(climbToNumeric(col(row, COL_ATTEMPTED_CLIMB))).append(",");
                QRString.append(climbToNumeric(col(row, COL_SUCCESSFUL_CLIMB))).append(",");
                QRString.append(col(row, COL_CLIMB_LOCATION));
            } else if (i == 0) {
                QRString.append(nvl(endgame.get("Collecting"))).append(",");
                QRString.append(nvl(endgame.get("Ferrying"))).append(",");
                QRString.append(nvl(endgame.get("Missed"))).append(",");
                QRString.append(levelToDecimal(endgame.get("StartLevel"))).append(",");
                QRString.append(levelToDecimal(endgame.get("StopLevel"))).append(",");
                QRString.append(climbToNumeric(endgame.get("AttemptedClimb"))).append(",");
                QRString.append(climbToNumeric(endgame.get("SuccessfulClimbed"))).append(",");
                QRString.append(nvl(endgame.get("ClimbLocation")));
            } else {
                QRString.append(",,,,,,,");
            }

            if (i < maxRows - 1) QRString.append(ROW_DELIMITER);
        }
    }

    // ─────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    // ─────────────────────────────────────────
    // ACCESSORS
    // ─────────────────────────────────────────

    public static String getQRString() { return QRString.toString(); }

    public static String getScouterName() { return getField(SCOUTER_NAME_INDEX); }
    public static String getTeamNumber()  { return getField(TEAM_NUM_INDEX); }
    public static String getMatchNumber() { return getField(MATCH_NUM_INDEX); }

    private static String getField(int index) {
        if (QRString.toString().isEmpty()) return null;
        String firstRow = QRString.toString().split(ROW_DELIMITER)[0];
        String[] parts  = firstRow.split(DELIMITER, -1);
        return index < parts.length ? parts[index] : null;
    }

    public static void storeQRString(android.content.Context ctx) {
        HashMapManager.appendQRList(QRString.toString(), ctx);
    }

    public static void clearQRString() {
        QRString = new StringBuilder();
    }
}