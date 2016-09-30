package edu.kit.bletest;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import edu.kit.privateadhocpeering.AdvertisementSet;
import edu.kit.privateadhocpeering.BeaconManager;
import edu.kit.privateadhocpeering.DiscoveryService;
import edu.kit.privateadhocpeering.Peer;
import edu.kit.privateadhocpeering.PeerDatabase;

public class TestingService extends Service {

    private Intent serviceIntent;
    private TestReceiver myReceiver;

    private long queuedTimestamp;

    @Override
    public void onCreate() {
        super.onCreate();
        serviceIntent = new Intent(this, DiscoveryService.class);
        serviceIntent.setAction(DiscoveryService.RESET_SCANNING);

        myReceiver = new TestReceiver(this);
        myReceiver.register(this);

        scheduleTestCycle();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        myReceiver.saveLogs();
        myReceiver.unregister(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void scheduleTestCycle() {
        if (System.currentTimeMillis() < queuedTimestamp) return;

        stopService(serviceIntent);

        final BeaconManager bm = BeaconManager.initInstance(this);

        long delay = 5 * 5 * 1000;

        queuedTimestamp = System.currentTimeMillis() + delay;

        new Handler(Looper.myLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {

                for (AdvertisementSet set : bm.getAdvertisementSets()) {
                    set.setProximityAware(false);
                }

                if (myReceiver != null) {
                    myReceiver.logCycleStarted(false);
                } else {
                    Log.i("Logged", "Receiver is null.");
                }

                startService(serviceIntent);
            }
        }, delay);
    }
}
