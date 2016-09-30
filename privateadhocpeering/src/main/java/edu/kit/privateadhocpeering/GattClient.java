package edu.kit.privateadhocpeering;

import android.bluetooth.BluetoothDevice;

public class GattClient {
    Peer peer;
    BluetoothDevice peerDevice;
    String expectedAuthenticationToken;
    byte[] serverSessionIV;
    byte[] clientSessionIV;
    ByteCounter messageCounter;

    byte[] authenticationData;

    MessageQueue dataQueue;

    public GattClient() {
        messageCounter = new ByteCounter();
        dataQueue = new MessageQueue();

    }

}
