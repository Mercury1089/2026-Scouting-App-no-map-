package com.mercury1089.Scouting_App_2026;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

public class HashMapManager {

    public enum HASH {
        SETTINGS,
        SETUP,
        AUTON,
        TELEOP,
        ENDGAME
    }

    private static LinkedHashMap<String, String> settingsHashMap;
    private static LinkedHashMap<String, String> setupHashMap;
    private static LinkedHashMap<String, String> autonHashMap;
    private static LinkedHashMap<String, String> teleopHashMap;
    private static LinkedHashMap<String, String> endgameHashMap;

    /**
     *
     * Used to get the setttingsHashMap
     * Call when an activity starts and assign to global variable
     * @return  the settingsHashMap
     *
     */
    public static LinkedHashMap<String, String> getSettingsHashMap(){
        return settingsHashMap;
    }

    /**
     *
     * Used to get the setupHashMap
     * Call when an activity starts and assign to global variable
     * @return  the setupHashMap
     *
     */
    public static LinkedHashMap<String, String> getSetupHashMap(){
        return setupHashMap;
    }

    /**
     *
     * Used to get the autonHashMap
     * Call when an activity starts and assign to global variable
     * @return  the autonHashMap
     *
     */
    public static LinkedHashMap<String, String> getAutonHashMap(){
        return autonHashMap;
    }

    /**
     *
     * Used to get the teleopHashMap
     * Call when an activity starts and assign to global variable
     * @return the teleopHashMap
     *
     */
    public static LinkedHashMap<String, String> getTeleopHashMap(){
        return teleopHashMap;
    }

    /**
     *
     * Used to get the endgameHashMap
     * Call when an activity starts and assign to global variable
     * @return the endgameHashMap
     *
     */
    public static LinkedHashMap<String, String> getEndgameHashMap(){
        return endgameHashMap;
    }


    /**
     *
     * Used to set the app wide settingsHashMap
     * Call before leaving an activity to update the app wide settingsHashMap
     * @param settingsData  The data to append to settingsHashMap
     *
     */
    public static void putSettingsHashMap(LinkedHashMap<String, String> settingsData){
        if(settingsData == null)
            return;
        settingsHashMap = settingsData;
    }

    /**
     *
     * Used to set the app wide setupHashMap
     * Call before leaving an activity to update the app wide setupHashMap
     * @param setupData The data to be put in the setupHashMap
     *
     */
    public static void putSetupHashMap(LinkedHashMap<String, String> setupData){
        if(setupData == null) {
            return;
        }
        setupHashMap = setupData;
    }

    /**
     *
     * Used to set the app wide autonHashMap
     * Call before leaving an activity to update the app wide autonHashMap
     * @param autonData the data to be put in the autonHashMap
     *
     */
    public static void putAutonHashMap(LinkedHashMap<String, String> autonData){
        if(autonData == null)
            return;
        autonHashMap = autonData;
    }

    /**
     *
     * Used to set the app wide teleopHashMap
     * Call before leaving an activity to update the app wide teleopHashMap
     * @param teleopData the data to be put in the teleopHashMap
     *
     */
    public static void putTeleopHashMap(LinkedHashMap<String, String> teleopData){
        if(teleopData == null)
            return;
        teleopHashMap = teleopData;
    }

    /**
     *
     * Used to set the app wide endgameHashMap
     * Call before leaving an activity to update the app wide endgameHashMap
     * @param endgameData the data to be put in the endgameHashMap
     *
     */
    public static void putEndgameHashMap(LinkedHashMap<String, String> endgameData){
        if(endgameData == null)
            return;
        endgameHashMap = endgameData;
    }


    /**
     *
     * Adds a value to the list of values (also known as the "qr list").
     * qrList values are part of the final CSV output, and later parsed to represent actual match data
     * @param qrString the new value to be added to qrList
     * @param context the app context (to be passed into {@link #outputQRList(String[], Context)}
     *
     */
    public static void appendQRList(String qrString, Context context){
        String[] qrList = setupQRList(context);

        // Check for duplicates based on Scouter, Team, and Match (first line of the string)
        String firstLine = qrString.split("\n")[0];
        String[] parts = firstLine.split(",");
        if (parts.length >= 3) {
            String scouter = parts[0];
            String team = parts[1];
            String match = parts[2];

            for (int i = 0; i < qrList.length; i++) {
                String existingFirstLine = qrList[i].split("\n")[0];
                String[] existingParts = existingFirstLine.split(",");
                if (existingParts.length >= 3) {
                    if (existingParts[0].equals(scouter) && 
                        existingParts[1].equals(team) && 
                        existingParts[2].equals(match)) {
                        // Duplicate found, replace it
                        qrList[i] = qrString;
                        outputQRList(qrList, context);
                        return;
                    }
                }
            }
        }

        // No duplicate found, append normally
        String[] newList = new String[qrList.length + 1];
        Log.d("QRStuff", "" + qrList.length + " " + newList.length);
        for(int i = 0; i < qrList.length; i++)
            newList[i] = qrList[i];
        newList[newList.length - 1] = qrString;
        outputQRList(newList, context);
    }

    /**
     *
     * <p>Reads values from QRData file and updates the qrList array accordingly</p>
     * <p>Called after appending values to qrList</p>
     * @param context App context required to access the device file system
     * @return the qrList;
     *
     */
    public static String[] setupQRList(Context context){
        String filename = "QRData";
        String[] qrList = new String[0];
        try {
            FileInputStream fs = context.openFileInput(filename);
            InputStreamReader inputStreamReader = new InputStreamReader(fs, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            
            StringBuilder fullContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                fullContent.append(line).append("\n");
            }
            reader.close();

            String content = fullContent.toString();
            if (content.trim().isEmpty()) return qrList;

            // Split by the custom delimiter (double newline) or look for the start of a new record
            // Records start with the Scouter Name. However, since snapshots also contain newlines,
            // we should be careful. 
            // The QRRunnable stores the WHOLE qrString which contains \n.
            // When reading back, readLine() splits it.
            
            // Let's re-parse based on the structure: A record starts with scouterName and has specific columns.
            // A simpler way: when writing, we could use a different delimiter for records.
            // But since we are already using \n for snapshots, let's try to detect the header/first line pattern.
            
            String[] rawLines = content.split("\n");
            java.util.ArrayList<String> records = new java.util.ArrayList<>();
            StringBuilder currentRecord = new StringBuilder();
            
            for (String l : rawLines) {
                if (l.trim().isEmpty()) continue;
                // If the line looks like a "Setup" line (6 columns), it's a new record
                if (l.split(",").length == 6 && !l.contains("A_coll")) {
                    if (currentRecord.length() > 0) {
                        records.add(currentRecord.toString().trim());
                    }
                    currentRecord = new StringBuilder();
                }
                currentRecord.append(l).append("\n");
            }
            if (currentRecord.length() > 0) {
                records.add(currentRecord.toString().trim());
            }
            
            return records.toArray(new String[0]);

        } catch(Exception e){
            File file = new File(context.getFilesDir(), filename);
            try {
                file.createNewFile();
            }catch (Exception e1) {}
        }
        return qrList;
    }

    /**
     *
     * Writes the qrList array contents to a "QRData" file
     * @param qrList The array of data to be written
     * @param context The app context required to access the file system
     *
     */
    public static void outputQRList(String[] qrList, Context context){
        String filename = "QRData";
        try {
            File file = new File(context.getFilesDir(), filename);
            if(file.exists()) {
                file.delete();
            }
            file.createNewFile();
            FileOutputStream fs = new FileOutputStream(file);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fs));
            for(String qrString : qrList){
                Log.d("QRStuff2", "" + qrList.length);
                bw.write(qrString);
                bw.newLine();
                // Add an extra newline to help separate records since each record has internal newlines
                bw.newLine(); 
            }
            bw.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Gets the settings password from file
     * @param context   App context requred to access device file systme
     * @return  The settings password
     */
    public static String[] pullSettingsPassword(Context context){
        String filename = "SettingsPassword";
        String password, usePassword;
        try {
            FileInputStream fs = context.openFileInput(filename);
            InputStreamReader inputStreamReader = new InputStreamReader(fs, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            password = reader.readLine();
            usePassword = reader.readLine();
            if(password == null || usePassword == null){
                return new String[] {"", "N"};
            }
            return new String[] {password, usePassword};
        } catch(Exception e){
            File file = new File(context.getFilesDir(), filename);
            try {
                file.createNewFile();
            }catch (Exception e1) {}
            return null;
        }
    }

    /**
     * Saves a new settings password to file
     * @param passwordStuff A two element String array-- <br>"password" is the actual password string, <br>"usePassword" is "Y" or "N" depending on if password is REQUIRED
     * @param context   App context needed to access device file system
     */
    public static void saveSettingsPassword(String[] passwordStuff, Context context){
        String filename = "SettingsPassword";
        try {
            String password = passwordStuff[0];
            String usePassword = passwordStuff[1];
            File file = new File(context.getFilesDir(), filename);
            if(file.exists()) {
                file.delete();
            }
            file.createNewFile();
            FileOutputStream fs = new FileOutputStream(file);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fs));
            bw.write(password);
            bw.newLine();
            bw.write(usePassword);
            bw.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * Used to reset all the setupHashMap values to their default values
     * Fill in default values to prevent null pointer exceptions
     * @param map   The hashmap to be reset
     *
     */
    public static void setDefaultValues(HASH map){
        String snapshotHeader = "scouterName,teamNumber,matchNumber,A_coll,A_scor,A_miss,A_ferr,A_died,A_att,A_succ,A_loc,T_coll,T_scor,T_miss,T_ferr,T_died,E_att,E_succ,E_loc,timestamp\n";
        String endgameHeader = "scouterName,teamNumber,matchNumber,A_coll,A_scor,A_miss,A_ferr,A_died,A_att,A_succ,A_loc,T_coll,T_scor,T_miss,T_ferr,T_died,E_att,E_succ,E_loc\n";

        switch(map) {
            case SETTINGS:
                settingsHashMap.put("HashMapName", "Settings");
                settingsHashMap.put("NothingToSeeHere", "0");
                settingsHashMap.put("DefaultPassword", "abc");
                break;
            case SETUP:
                setupHashMap.put("HashMapName", "Setup");
                setupHashMap.put("ScouterName", "");
                setupHashMap.put("MatchNumber", "");
                setupHashMap.put("TeamNumber", "");
                setupHashMap.put("NoShow", "N");
                setupHashMap.put("PreloadedCargo", "0");
                setupHashMap.put("AlliancePartner1", "");
                setupHashMap.put("AlliancePartner2", "");
                setupHashMap.put("AllianceColor", "");
                break;
            case AUTON:
                // 2026 Fuel Game - Autonomous Phase Defaults
                autonHashMap.put("HashMapName", "Auton");

                // Game data fields
                autonHashMap.put("Collecting", "");
                autonHashMap.put("Ferrying", "");
                autonHashMap.put("Missed", "");
                autonHashMap.put("Scored", "");
                autonHashMap.put("RobotFellOver", "N");
                autonHashMap.put("AttemptedClimb", "");
                autonHashMap.put("SuccessfulClimbed", "");
                autonHashMap.put("ClimbLocation", "");

                // CSV Snapshot buffer - initialize with header
                autonHashMap.put("snapshots", snapshotHeader);

                break;
            case TELEOP:
                // 2026 Fuel Game - Teleoperated Phase Defaults
                teleopHashMap.put("HashMapName", "Teleop");

                // Game data fields
                teleopHashMap.put("Collecting", "");
                teleopHashMap.put("Ferrying", "");
                teleopHashMap.put("Missed", "");
                teleopHashMap.put("Scored", "");
                teleopHashMap.put("RobotFellOver", "N");

                // CSV Snapshot buffer - initialize with header
                teleopHashMap.put("snapshots", snapshotHeader);

                break;
            case ENDGAME:
                // 2026 Fuel Game - End Game Phase Defaults
                endgameHashMap.put("HashMapName", "Endgame");

                // Game data fields
                endgameHashMap.put("Collecting", "");
                endgameHashMap.put("Ferrying", "");
                endgameHashMap.put("Missed", "");
                endgameHashMap.put("Scored", "");
                endgameHashMap.put("AttemptedClimb", "");
                endgameHashMap.put("SuccessfulClimbed", "");
                endgameHashMap.put("ClimbLocation", "");

                // CSV Snapshot buffer - initialize with header
                endgameHashMap.put("snapshots", endgameHeader);

                break;
        }
    }

    /**
     * Initializes all HashMaps. <br>
     * Call when Pregame activity starts.
     */
    public static void initHashMaps(){
        settingsHashMap = new LinkedHashMap<>();
        setupHashMap = new LinkedHashMap<>();
        autonHashMap = new LinkedHashMap<>();
        teleopHashMap = new LinkedHashMap<>();
        endgameHashMap = new LinkedHashMap<>();

        setDefaultValues(HASH.SETTINGS);
        setDefaultValues(HASH.SETUP);
        setDefaultValues(HASH.AUTON);
        setDefaultValues(HASH.TELEOP);
        setDefaultValues(HASH.ENDGAME);
    }

    /**
     * Verifies that the requested HashMap is NOT null or empty. <br>
     * If it is, re-initializes it with default values.
     * @param map The requested HashMap
     */
    public static void checkNullOrEmpty(HASH map){
        switch (map){
            case SETTINGS:
                if(settingsHashMap == null || settingsHashMap.isEmpty()) {
                    settingsHashMap = new LinkedHashMap<>();
                    setDefaultValues(HASH.SETTINGS);
                }
                break;
            case SETUP:
                if(setupHashMap == null || setupHashMap.isEmpty()) {
                    setupHashMap = new LinkedHashMap<>();
                    setDefaultValues(HASH.SETUP);
                }
                break;
            case AUTON:
                if(autonHashMap == null || autonHashMap.isEmpty()) {
                    autonHashMap = new LinkedHashMap<>();
                    setDefaultValues(HASH.AUTON);
                }
                break;
            case TELEOP:
                if(teleopHashMap == null || teleopHashMap.isEmpty()) {
                    teleopHashMap = new LinkedHashMap<>();
                    setDefaultValues(HASH.TELEOP);
                }
                break;
            case ENDGAME:
                if(endgameHashMap == null || endgameHashMap.isEmpty()) {
                    endgameHashMap = new LinkedHashMap<>();
                    setDefaultValues(HASH.ENDGAME);
                }
                break;
        }
    }

    /**
     * Resets the application for the next match. <br>
     * Keeps ScouterName and increments MatchNumber.
     */
    public static void setupNextMatch(){
        String scouter = setupHashMap.get("ScouterName");
        String match = setupHashMap.get("MatchNumber");
        int matchNum = 0;
        try {
            matchNum = Integer.parseInt(match);
        } catch (Exception e) {}
        matchNum++;

        initHashMaps();
        setupHashMap.put("ScouterName", scouter);
        setupHashMap.put("MatchNumber", String.valueOf(matchNum));
    }
}
