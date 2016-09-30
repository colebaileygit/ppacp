package edu.kit.privateadhocpeering;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.telecom.Call;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DiscoveryService extends Service {

    static final String STOP_SCANNING = "edu.kit.blescanning.stop";
    public static final String RESET_SCANNING = "edu.kit.blescanning.reset";
    static final String RESET_ADVERTISING = "edu.kit.bleadvertising.reset";
    static final String SILENT_DURATION = "edu.kit.bleadvertising.silentperiod";

    public static final String LOG_BATTERY_LEVELS = "edu.kit.logging.batterylevel";

    private PeerDatabase peerDatabase;
    private BleScanner bleScanner;
    private BeaconManager beaconManager;
    private Handler mHandler;

    private Logger batteryLogger;

    static String ACTION_SCANNING_KEYS_CHANGED = "edu.kit.keys.changed";


    public DiscoveryService() {
        peerDatabase = PeerDatabase.getInstance();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        bleScanner = new BleScanner(this);
        bleScanner.startScan();
        beaconManager = BeaconManager.initInstance(this);
        peerDatabase.setContext(this);
        mHandler = new Handler(Looper.myLooper());

   //     TimingReceiver.setNextAlarm(this);
    }


    @Override
    public void onDestroy() {
        peerDatabase.removeContext();
        bleScanner.stopScan();
        beaconManager.deactivateBroadcasts();

        if (batteryLogger != null) batteryLogger.saveLogs();

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(RESET_SCANNING)) {
                if (bleScanner != null) {
                    restartScanning();
                }
            } else if (intent.getAction().equals(RESET_ADVERTISING)) {
                final BeaconManager bm = BeaconManager.initInstance(this);
                bm.deactivateBroadcasts();
                long delay = intent.getLongExtra(SILENT_DURATION, 0);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        bm.activateBroadcasts();
                        restartScanning();
                    }
                }, delay);

                if (batteryLogger != null) {
                    logBatteryLevel(delay);
                }
            } else if (intent.getAction().equals(STOP_SCANNING)) {
                if (bleScanner != null) {
                    bleScanner.stopScan();
                }
            } else if (intent.getAction().equals(LOG_BATTERY_LEVELS)) {
                batteryLogger = new Logger();
                logBatteryLevel(0);
            }

        }

        return START_STICKY;
    }

    private void logBatteryLevel(long delay) {
        Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            float batteryPct = level / (float) scale;
            String[] fields = {
                    new SimpleDateFormat("dd.MM_HH:mm:ss", Locale.ROOT).format(new Date()),
                    batteryPct + "",
                    delay + " delay"
            };
            Log.d("Logger", batteryPct + "");
            batteryLogger.addLine(fields);
        }
    }

    private void restartScanning() {
        Log.i("BLE_SCANNING", "Restarting scan");
        bleScanner.stopScan();
        PeerDatabase.getInstance().refreshEphemeralKeys();
        bleScanner.startScan();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
