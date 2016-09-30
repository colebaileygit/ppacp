package edu.kit.privateadhocpeering;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import au.com.bytecode.opencsv.CSVWriter;

public class Logger {

    private List<String[]> lines;
    private File currentFile;

    public Logger() {
        lines = new ArrayList<>();
        currentFile = createFile();
    }

    public void addLine(String[] fields) {
        lines.add(fields);
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(currentFile, true));
            writer.writeNext(fields);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static File getLogsStorageDir() {
        // Get the directory for the app's private documents directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), "BLE_TEST");
        if(!file.exists()) {
            if (!file.mkdirs()) Log.i("FILE_MANAGEMENT", "Directory not created: Files");
        }
        return file;
    }

    public void saveLogs() {
        File file = createFile();
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(file));
            for (String[] entry : lines) {
                writer.writeNext(entry);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        emptyLogs();
    }

    private File createFile() {
        String fileName = "LOG_" + new SimpleDateFormat("dd.MM_HH:mm:ss", Locale.ROOT).format(new Date()) + ".txt";
        File file = new File(getLogsStorageDir(), fileName);
        try {
            if (!file.createNewFile()) {
                Log.e("FILE_MANAGEMENT", "File not created");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return file;
    }


    public void emptyLogs() {
        lines.clear();
    }
}
