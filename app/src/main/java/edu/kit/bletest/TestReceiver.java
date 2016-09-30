package edu.kit.bletest;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import junit.framework.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import edu.kit.privateadhocpeering.CallbackReceiver;
import edu.kit.privateadhocpeering.Logger;
import edu.kit.privateadhocpeering.PeerDatabase;


public class TestReceiver extends CallbackReceiver {

    private Logger logger;
    private TestingService service;
    private boolean scheduled;

    public TestReceiver(TestingService service) {
        super();
        logger = new Logger();
        this.service = service;
    }

    public void logCycleStarted(boolean advertising) {
        String event = advertising ? "Advertising cycle begun." : "Scanning cycle begun.";
        logger.addLine(constructFields(event));
    }

    public void saveLogs() {
        logger.saveLogs();
    }

    @Override
    public void onProximityDetected(Context context, String peerId) {
        logger.addLine(constructFields("Peer scanned"));
        scheduled = false;
    }

    @Override
    public void onPeerLost(Context context, String peerId) {
        logger.addLine(constructFields("Peer disconnected"));

        if (service != null && !scheduled) {
            scheduled = true;
            service.scheduleTestCycle();
        }
    }

    @Override
    public void onConnecting(Context context, String peerId) {
        logger.addLine(constructFields("Peer connecting"));
    }

    @Override
    public void onConnected(Context context, String peerId) {
        logger.addLine(constructFields("Peer connected"));
    }

    @Override
    public void onAuthenticated(Context context, String peerId) {
        logger.addLine(constructFields("Peer authenticated"));
    }

    @Override
    public void onMessageReceived(Context context, String peerId) {
        logger.addLine(constructFields("Message received"));

        if (service != null && !scheduled) {
            scheduled = true;
            service.scheduleTestCycle();
        }
    }

    private String[] constructFields(String event) {
        String[] result = {
                new SimpleDateFormat("dd.MM_HH:mm:ss:SSSS", Locale.ROOT).format(new Date()),
                System.currentTimeMillis() + "",
                event
        };
        Log.i("Logged", event);
        return result;
    }
}
