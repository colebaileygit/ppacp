package edu.kit.privateadhocpeering;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BleScanner {

    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private String TAG = "BLE_SCANNING";

    private boolean legacy_scanning = false;

    private static long TIMEOUT_MILLIS = 15000;

    private long lastScanResult;
    private Map<List<Byte>, Long> scanTimestamps;

    private ScanCallback scanCallback;

    private ScanCallback getScanCallback() {
        return new ScanCallback() {
            @Override
            public void onScanFailed(int errorCode) {
                Log.e(TAG, "Scan failed: " + errorCode);
                if (errorCode == ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED) {
                    Log.i("BLE_SCANNING", "Restarting failed scan.");
                    legacy_scanning = true;
                    startScan();
                }
            }
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                // TODO: investigate callbackTypes. only notify on first and lost scan? (ANDROID M only)
                long timestamp = System.currentTimeMillis();

                updateScanResults(result, timestamp);
                checkLostBeacons(timestamp);
                lastScanResult = timestamp;
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                long timestamp = System.currentTimeMillis();
                Log.i(TAG, "Scan batch results: " + results.size());

                for (ScanResult result : results) {
                    updateScanResults(result, timestamp);
                }

                checkLostBeacons(timestamp);
                lastScanResult = timestamp;
            }
        };
    }

    private void checkLostBeacons(long timestamp) {
        Map<List<Byte>, Long> copy = new HashMap<>(scanTimestamps);
        for (List<Byte> data : copy.keySet()) {
            if (timestamp - copy.get(data) > TIMEOUT_MILLIS) {
                scanTimestamps.remove(data);
                String id = PeerDatabase.getInstance().getIdentifier(ByteArrayHelper.toArray(data));
                if (id == null) return;
                Peer peer = PeerDatabase.getInstance().getPeer(id);
                if (peer != null && peer.getStatus() == PeerStatus.DISCOVERED) peer.lost();
            }
        }
    }

    // TODO: use ScanResult timestamp
    private void updateScanResults(ScanResult result, long timestamp) {
        byte[] array = getData(result);
        List<Byte> data = ByteArrayHelper.toList(array);

        boolean b = scanTimestamps.containsKey(data);
        Long previous = scanTimestamps.put(data, timestamp);
        String id = PeerDatabase.getInstance().getIdentifier(array);
        if (id == null) {
            Log.e(TAG, "Scan result: " + result.getDevice().getAddress() + " no id found for " + Arrays.toString(array));
            return;
        }
        Peer peer = PeerDatabase.getInstance().getPeer(id);
        if (peer == null) {
            Log.e(TAG, "Scan result: " + result.getDevice().getAddress() + " no peer found.");
            return;
        }
        Key key = PeerDatabase.getInstance().getScanningKey(array);
        if (key == null) {
            Log.e(TAG, "No scanning key found.");
            return;
        }

        if (!b || peer.getStatus() == PeerStatus.OUT_OF_RANGE
                || peer.getStatus() == PeerStatus.DISCOVERED) {
            peer.discovered(context, result, key);
        }
    }

    private byte[] getData(ScanResult result) {
        ScanRecord record = result.getScanRecord();
        byte[] serviceData = new byte[0];
        if (record != null) {
            byte[] scanData = record.getServiceData(DiscoveryBeacon.SERVICE_UUID);
            if (scanData != null) {
                // Filter out eID frame type
                serviceData = new byte[scanData.length - 1];
                System.arraycopy(scanData, 1, serviceData, 0, scanData.length - 1);
            }
        }

        return serviceData;
    }

    public BleScanner(Context context) {
        this.context = context;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        scanTimestamps = new HashMap<>();
        scanCallback = getScanCallback();
    }

    public void startScan() {
        Log.i(TAG, "Starting scan: " + System.currentTimeMillis() / (1000 * Key.TICK_LENGTH));
        if (!bluetoothAdapter.isOffloadedScanBatchingSupported() || legacy_scanning) {
            if (scanCallback == null) scanCallback = getScanCallback();
            BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
            if (scanner != null) scanner.startScan(scanCallback);
            return;
        }

        List<byte[]> filters = new ArrayList<>();
        for (byte[] data : PeerDatabase.getInstance().getCurrentEphemeralIdentifiers()) {
            byte[] bytes = new byte[data.length + 1];
            bytes[0] = (byte)0x30;                                          // eID frame type
            System.arraycopy(data, 0, bytes, 1, data.length);
            filters.add(bytes);
        }
        startScan(filters);
    }

    private void startScan(List<byte[]> filters) {
        List<ScanFilter> scanFilters = new ArrayList<>();
        for (byte[] data : filters) {
            ScanFilter filter = new ScanFilter.Builder()
                    .setServiceData(DiscoveryBeacon.SERVICE_UUID, data)
                    .build();
            scanFilters.add(filter);
        }
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();
        if (scanCallback == null) scanCallback = getScanCallback();
        bluetoothAdapter.getBluetoothLeScanner().startScan(scanFilters, settings, scanCallback);

//        final BleScanner bleScanner = this;

//        new Handler(Looper.myLooper()).postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (System.currentTimeMillis() - lastScanResult > 15000) {
//                    Log.i("BLE_SCANNING", "Restarting scan. Timeout reached.");
//                    bleScanner.startScan();
//                }
//            }
//        }, 15000);
    }

    public void stopScan() {
        Log.i(TAG, "Stopping scan: " + System.currentTimeMillis() / (1000 * Key.TICK_LENGTH));
        if (scanCallback == null) scanCallback = getScanCallback();
        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner != null) scanner.stopScan(scanCallback);
    }



}
