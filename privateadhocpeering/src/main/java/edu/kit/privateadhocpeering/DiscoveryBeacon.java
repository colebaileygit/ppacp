package edu.kit.privateadhocpeering;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.graphics.Color;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.Pair;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DiscoveryBeacon {

    final BluetoothAdapter bluetoothAdapter;
    static final ParcelUuid SERVICE_UUID =
            ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");
    AdvertiseCallback callback;
    private Context context;
    private AuthenticationGATT server;
    private AdvertisementSet set;

    DiscoveryBeacon(Context context, AdvertisementSet set) {
        this.context = context;
        BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        this.set = set;
    }

    void broadcast(AdvertiseCallback callback) {
        this.callback = callback;

        byte[] advData = set.advertisementKey.getAdvertisingData();
        byte[] bytes = new byte[advData.length + 1];
        bytes[0] = (byte)0x30;                                          // eID frame type
        System.arraycopy(advData, 0, bytes, 1, advData.length);
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .setTimeout(0)
                .build();

        AdvertiseData.Builder builder = new AdvertiseData.Builder()
                .setIncludeDeviceName( false )
                .setIncludeTxPowerLevel( false )
                .addServiceUuid( SERVICE_UUID )
                .addServiceData( SERVICE_UUID, bytes );


        AdvertiseData data = builder.build();

        bluetoothAdapter.setName("nil");

        if (bluetoothAdapter.getBluetoothLeAdvertiser() == null) return;        // Bluetooth disabled or not supported


        bluetoothAdapter.getBluetoothLeAdvertiser().stopAdvertising(callback);
        bluetoothAdapter.getBluetoothLeAdvertiser().startAdvertising(settings, data, callback);

        server = new AuthenticationGATT(context, set);
        server.openServer();
    }

    void cancel() {
        if (callback != null) {
            BluetoothLeAdvertiser advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            if (advertiser != null) advertiser.stopAdvertising(callback);
            server.closeServer();
        } else {
            Log.e("BEACON", "Could not cancel because callback null.");
        }
    }

    void writeMessage(String peerIdentifier) {
        if (server != null) {
            server.sendData(peerIdentifier);
        } else {        // trying to write message to peer with no connection
            PeerDatabase.getInstance().getPeer(peerIdentifier).lost();
        }
    }


}
