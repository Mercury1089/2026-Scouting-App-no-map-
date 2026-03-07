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

public class HashMapManager{
    private static LinkedHashMap<String, String> settingsHashMap = new LinkedHashMap<>();
    private static LinkedHashMap<String, String> setupHashMap = new LinkedHashMap<>();
    private static LinkedHashMap<String, String> autonHashMap = new LinkedHashMap<>();
    private static LinkedHashMap<String, String> teleopHashMap = new LinkedHashMap<>();
    private static LinkedHashMap<String, String> endgameHashMap = new LinkedHashMap<>();

    /**
     *
     * Enum for reference to each HashMap
     *
     */
    public enum HASH{
        SETTINGS, SETUP, AUTON, TELEOP, ENDGAME, QRCODES
    }

    /**
     *
     * Used to access the setup HashMap from an activity
     *
     */
    private HashMapManager(){
        // Nothing to see here
    }

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
     * @param teleopData    the data to be put in the teleopHashMap
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
     * @param endgameData    the data to be put in the endgameHashMap
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
            String line = reader.readLine();
            int count = 0;
            while(line != null){
                Log.d("QRStuff3", "" + count);
                String[] tempList = qrList;
                qrList = new String[tempList.length + 1];
                for(int i = 0; i < tempList.length; i++)
                    qrList[i] = tempList[i];
                qrList[qrList.length-1] = line;
                line = reader.readLine();
                count ++;
            }
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
        switch(map) {
            case SETTINGS:
                settingsHashMap.put("HashMapName", "Settings");
                settingsHashMap.put("NothingToSeeHere", "0");
                settingsHashMap.put("Slack", "0");
                settingsHashMap.put("DefaultPassword", "abc");
                break;
            case SETUP:
                setupHashMap.put("HashMapName", "Setup");
                setupHashMap.put("ScouterName", "");
                setupHashMap.put("MatchNumber", "");
                setupHashMap.put("TeamNumber", "");
                setupHashMap.put("NoShow", "N");
                setupHashMap.put("PreloadNote", "N");
                setupHashMap.put("PlayedDefense", "N");
                setupHashMap.put("AlliancePartner1", "");
                setupHashMap.put("AlliancePartner2", "");
                setupHashMap.put("AllianceColor", "");
                setupHashMap.put("PreloadedCargo", "0");
                //Note: FellOver is put in setup hashmap because its value may be updated in Auton, Teleop, or Endgame
                setupHashMap.put("FellOver", "N");
                break;
            case AUTON:
                // 2026 Fuel Game - Autonomous Phase Defaults
                autonHashMap.put("HashMapName", "Auton");

                // Game data fields
                autonHashMap.put("Collecting", "0");
                autonHashMap.put("Ferrying", "0");
                autonHashMap.put("Missed", "0");
                autonHashMap.put("StartLevel", "EMPTY");
                autonHashMap.put("StopLevel", "EMPTY");
                autonHashMap.put("AttemptedClimb", "DID NOT ATTEMPT");
                autonHashMap.put("SuccessfulClimbed", "None");
                autonHashMap.put("ClimbLocation", "Null");
                autonHashMap.put("RobotFellOver", "0");

                // CSV Snapshot buffer - initialize with header
                autonHashMap.put("snapshots", "collecting,ferrying,missed,startLevel,stopLevel,attemptedClimb,successfulClimbed,climbLocation,robotFellOver\n");

                break;
            case TELEOP:
                // 2026 Fuel Game - Teleoperated Phase Defaults
                teleopHashMap.put("HashMapName", "Teleop");

                // Game data fields
                teleopHashMap.put("Collecting", "0");
                teleopHashMap.put("Ferrying", "0");
                teleopHashMap.put("Missed", "0");
                teleopHashMap.put("StartLevel", "EMPTY");
                teleopHashMap.put("StopLevel", "EMPTY");
                teleopHashMap.put("AttemptedClimb", "DID NOT ATTEMPT");
                teleopHashMap.put("SuccessfulClimbed", "None");
                teleopHashMap.put("ClimbLocation", "Null");
                teleopHashMap.put("RobotFellOver", "0");

                // CSV Snapshot buffer - initialize with header
                teleopHashMap.put("snapshots", "collecting,ferrying,missed,startLevel,stopLevel,attemptedClimb,successfulClimbed,climbLocation,robotFellOver\n");

                break;
            case ENDGAME:
                // 2026 Fuel Game - Endgame Phase Defaults (same structure as Teleop)
                endgameHashMap.put("HashMapName", "Endgame");

                // Game data fields
                endgameHashMap.put("Collecting", "0");
                endgameHashMap.put("Ferrying", "0");
                endgameHashMap.put("Missed", "0");
                endgameHashMap.put("StartLevel", "EMPTY");
                endgameHashMap.put("StopLevel", "EMPTY");
                endgameHashMap.put("AttemptedClimb", "DID NOT ATTEMPT");
                endgameHashMap.put("SuccessfulClimbed", "None");
                endgameHashMap.put("ClimbLocation", "Null");
                endgameHashMap.put("RobotFellOver", "0");

                // CSV Snapshot buffer - initialize with header
                endgameHashMap.put("snapshots", "collecting,ferrying,missed,startLevel,stopLevel,attemptedClimb,successfulClimbed,climbLocation,robotFellOver\n");

                break;
        }
    }

    /**
     *
     * Checks if the setupHashMap is empty or null
     * if it is null, it instantiates it and calls setDefaultValues()
     * if it is empty, it calls setDefaultValues()
     * @param map   The map to be checked
     *
     */

    public static boolean checkNullOrEmpty(HASH map){
        switch(map){
            case SETTINGS:
                if(settingsHashMap == null)
                    settingsHashMap = new LinkedHashMap<>();
                if(settingsHashMap.isEmpty()) {
                    setDefaultValues(HASH.SETTINGS);
                    return true;
                }
                break;
            case SETUP:
                if(setupHashMap == null)
                    setupHashMap = new LinkedHashMap<>();
                if(setupHashMap.isEmpty()) {
                    setDefaultValues(HASH.SETUP);
                    return true;
                }
                break;
            case AUTON:
                if(autonHashMap == null)
                    autonHashMap = new LinkedHashMap<>();
                if(autonHashMap.isEmpty()) {
                    setDefaultValues(HASH.AUTON);
                    return true;
                }
                break;
            case TELEOP:
                if(teleopHashMap == null)
                    teleopHashMap = new LinkedHashMap<>();
                if(teleopHashMap.isEmpty()) {
                    setDefaultValues(HASH.TELEOP);
                    return true;
                }
                break;
            case ENDGAME:
                if(endgameHashMap == null)
                    endgameHashMap = new LinkedHashMap<>();
                if(endgameHashMap.isEmpty()) {
                    setDefaultValues(HASH.ENDGAME);
                    return true;
                }
                break;
        }
        return false;
    }

    /**
     *
     * resets all of the values of all of the HashMaps except for scouterName
     * It also increments the match number
     *
     */

    public static void setupNextMatch(){
        String scouterName = setupHashMap.get("ScouterName");
        String matchNumber = setupHashMap.get("MatchNumber");
        String allianceColor = setupHashMap.get("AllianceColor");
        setDefaultValues(HASH.SETUP);
        setDefaultValues(HASH.AUTON);
        setDefaultValues(HASH.TELEOP);
        setDefaultValues(HASH.ENDGAME);
        setupHashMap.put("ScouterName", scouterName);
        try {
            setupHashMap.put("MatchNumber", Integer.toString((Integer.parseInt(matchNumber) + 1)));
        } catch(NumberFormatException e){
            setupHashMap.put("MatchNumber", "0");
        }
        setupHashMap.put("AllianceColor", allianceColor);
    }
}