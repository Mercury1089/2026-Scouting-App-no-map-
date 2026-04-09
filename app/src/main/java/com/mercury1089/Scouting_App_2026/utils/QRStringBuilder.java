package com.mercury1089.Scouting_App_2026.utils;

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
    // BUILD
    // ─────────────────────────────────────────
    public static void buildQRString() {
        QRString = new StringBuilder();

        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.SETUP);
        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.AUTON);
        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.TELEOP);
        HashMapManager.checkNullOrEmpty(HashMapManager.HASH.ENDGAME);

        LinkedHashMap<String, String> setup   = HashMapManager.getSetupHashMap();
        LinkedHashMap<String, String> auton   = HashMapManager.getAutonHashMap();
        LinkedHashMap<String, String> teleop  = HashMapManager.getTeleopHashMap();
        LinkedHashMap<String, String> endgame = HashMapManager.getEndgameHashMap();

        // Line 1: Setup
        // Scouter,Team,Match,Partner1,Partner2,Alliance,Preload,NoShow
        String scouter  = nvl(setup.get("ScouterName"));
        String team     = nvl(setup.get("TeamNumber"));
        String match    = nvl(setup.get("MatchNumber"));
        String partner1 = nvl(setup.get("AlliancePartner1"));
        String partner2 = nvl(setup.get("AlliancePartner2"));
        String alliance = nvl(setup.get("AllianceColor"));
        String noShow   = nvl(setup.get("NoShow"));
        String preload  = nvl(setup.get("PreloadedFuel"));
        if (preload.isEmpty()) preload = nvl(setup.get("PreloadFuel")); // Fallback
        if (preload.isEmpty()) preload = "0";

        QRString.append(scouter).append(",")
                .append(team).append(",")
                .append(match).append(",")
                .append(partner1).append(",")
                .append(partner2).append(",")
                .append(alliance).append(",")
                .append(preload).append(",")
                .append(noShow);

        // If "No Show" is checked, we only return the setup line.
        if (noShow.equalsIgnoreCase("Y")) {
            return;
        }

        QRString.append(ROW_DELIMITER);

        // Lines 2+: Auton Snapshots
        String autonSnaps = getSnapshotsOnly(auton.get("snapshots"));
        if (autonSnaps.isEmpty()) {
            QRString.append(formatDefaultLine(scouter, team, match, auton, "AUTON")).append(ROW_DELIMITER);
        } else {
            QRString.append(autonSnaps);
        }

        // Lines 3+: Teleop Snapshots
        String teleopSnaps = getSnapshotsOnly(teleop.get("snapshots"));
        if (teleopSnaps.isEmpty()) {
            QRString.append(formatDefaultLine(scouter, team, match, teleop, "TELEOP")).append(ROW_DELIMITER);
        } else {
            QRString.append(teleopSnaps);
        }

        // Lines 4+: Endgame Snapshots
        String endgameSnaps = getSnapshotsOnly(endgame.get("snapshots"));
        if (endgameSnaps.isEmpty()) {
            QRString.append(formatDefaultLine(scouter, team, match, endgame, "ENDGAME")).append(ROW_DELIMITER);
        } else {
            QRString.append(endgameSnaps);
        }

        // Trim trailing row delimiter
        if (QRString.length() > 0 && QRString.lastIndexOf(ROW_DELIMITER) == QRString.length() - ROW_DELIMITER.length()) {
            QRString.setLength(QRString.length() - ROW_DELIMITER.length());
        }
    }

    private static String getSnapshotsOnly(String snapshots) {
        if (snapshots == null || snapshots.isEmpty()) return "";
        String[] lines = snapshots.split("\n");
        StringBuilder sb = new StringBuilder();
        // Skip header (index 0)
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty()) {
                sb.append(line).append(ROW_DELIMITER);
            }
        }
        return sb.toString();
    }

    private static String formatDefaultLine(String scouter, String team, String match, LinkedHashMap<String, String> map, String type) {
        StringBuilder sb = new StringBuilder();
        sb.append(scouter).append(",")
          .append(team).append(",")
          .append(match).append(",");

        // 8 Auton cols
        if (type.equals("AUTON")) {
            sb.append(nvl(map.get("Collecting"), "")).append(",")
              .append(nvl(map.get("Scored"), "")).append(",")
              .append(nvl(map.get("Missed"), "")).append(",")
              .append(nvl(map.get("Ferrying"), "")).append(",")
              .append(nvl(map.get("RobotFellOver"), "N")).append(",")
              .append(climbToNumeric(map.get("AttemptedClimb"))).append(",")
              .append(climbToNumeric(map.get("SuccessfulClimbed"))).append(",")
              .append(nvl(map.get("ClimbLocation"), "")).append(",");
        } else {
            sb.append(",,,,,,,,");
        }

        // 5 Teleop cols
        if (type.equals("TELEOP")) {
            sb.append(nvl(map.get("Collecting"), "")).append(",")
              .append(nvl(map.get("Scored"), "")).append(",")
              .append(nvl(map.get("Missed"), "")).append(",")
              .append(nvl(map.get("Ferrying"), "")).append(",")
              .append(nvl(map.get("RobotFellOver"), "N")).append(",");
        } else {
            sb.append(",,,,,");
        }

        // 3 Endgame cols
        if (type.equals("ENDGAME")) {
            sb.append(climbToNumeric(map.get("AttemptedClimb"))).append(",")
              .append(climbToNumeric(map.get("SuccessfulClimbed"))).append(",")
              .append(nvl(map.get("ClimbLocation"), ""));
        } else {
            sb.append(",,");
        }

        if (!type.equals("ENDGAME")) {
            sb.append(",").append(nvl(map.get("Timestamp"), "0"));
        }

        return sb.toString();
    }

    private static String climbToNumeric(String climb) {
        if (climb == null || climb.isEmpty()) return null;
        String t = climb.trim();
        if (t.equalsIgnoreCase("NO ATTEMPT") || t.equalsIgnoreCase("NONE") || t.equalsIgnoreCase("DID NOT ATTEMPT") || t.equals("0"))
            return null;
        try {
            Integer.parseInt(t);
            return t;
        } catch (NumberFormatException e) {
            return "9"; // Fallback for any other non-zero level string
        }
    }

    private static String nvl(String s) {
        return (s == null || s.trim().isEmpty()) ? "" : s.trim();
    }

    private static String nvl(String s, String def) {
        return (s == null || s.trim().isEmpty()) ? def : s.trim();
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
