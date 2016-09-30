package edu.kit.privateadhocpeering;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Authenticator {

    private String TAG = "BLE_TESTING";
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private UUID service_uuid;
    private int mConnectionState;
    private Handler mHandler;

    private byte[] authenticationNonce;
    private byte[] serverSessionIV;
    private byte[] clientSessionIV;
    private ByteCounter messageCounter;
    private byte[] message;

    private String peerIdentifier;
    private Context context;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private boolean writeInProgress;
    private byte[] authData;
    private byte[] postedMessage;

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            Peer peer = PeerDatabase.getInstance().getPeer(peerIdentifier);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (mConnectionState == STATE_CONNECTED) return;

                mConnectionState = STATE_CONNECTED;
                Log.i(TAG, "Connected to GATT server.");
                if (peer != null) peer.connected();

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Attempts to discover services after successful connection.
                    Log.i(TAG, "Attempting to start service discovery:" +
                            gatt.discoverServices());
                    }
                }, 0);


            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
      //          intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server: status " + status);
                if (peer != null) peer.lost();
                gatt.disconnect();
                gatt.close();
//                Intent i = new Intent(context, DiscoveryService.class);
//                i.setAction(DiscoveryService.RESET_SCANNING);
//                context.startService(i);


       //         broadcastUpdate(intentAction);
            }
        }

        private boolean refreshDeviceCache(BluetoothGatt gatt){
            try {
                BluetoothGatt localBluetoothGatt = gatt;
                Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
                if (localMethod != null) {
                    boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                    return bool;
                }
            }
            catch (Exception localException) {
                Log.e(TAG, "An exception occured while refreshing device");
            }
            return false;
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
    //            broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                Log.d(TAG, "Gatt Services discovered");

                gatt.getServices();
                Log.d(TAG, "Services found.");
                initiateAuthentication(gatt);

            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Characteristic read: " + ByteArrayHelper.toString(characteristic.getValue()));
    //            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, "Characteristic write: " + ByteArrayHelper.toString(characteristic.getValue()) + " status: " + status);
            writeInProgress = false;
            // TODO: handle status
            if (characteristic.getUuid().equals(AuthenticationGATT.confirmUuid)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Peer peer = PeerDatabase.getInstance().getPeer(peerIdentifier);
                    peer.authenticated();
                    if (peer.hasData()) {
                        postedMessage = peer.getNextOutgoingMessage();
                        if (postedMessage != null) writeMessage(postedMessage);
                    }
                }
            } else if (characteristic.getUuid().equals(AuthenticationGATT.requestUuid)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (authData != null) {
                        confirmAuthentication(gatt, authData);
                    }
                }
            } else if (characteristic.getUuid().equals(AuthenticationGATT.postDataUuid)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Peer peer = PeerDatabase.getInstance().getPeer(peerIdentifier);
                    peer.confirmMessageSent();
                    if (peer.hasData()) {
                        postedMessage = peer.getNextOutgoingMessage();
                        if (postedMessage != null) writeMessage(postedMessage);
                    } else {
                        BeaconManager instance = BeaconManager.tryGetInstance();
                        if (instance != null) instance.updateBroadcasts();
                    }
                } else {
                    if (postedMessage != null) writeMessage(postedMessage);
                }
            }

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.i(TAG, "Characteristic changed: " + ByteArrayHelper.toString(characteristic.getValue()));
            if (characteristic.getUuid().equals(AuthenticationGATT.responseUuid)) {
                if (writeInProgress) {

                    authData = characteristic.getValue();
                } else {
                    confirmAuthentication(gatt, characteristic.getValue());
                }
            } else {
                processData(characteristic.getValue());
            }
        }
    };

    Authenticator(Context context, String peerIdentifier, Key scanningKey) {
        this.context = context;
        this.peerIdentifier = peerIdentifier;
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        mHandler = new Handler(Looper.getMainLooper());
        service_uuid = scanningKey.getServiceUUID();
    }

    private void initiateAuthentication(final BluetoothGatt gatt) {
        writeInProgress = true;

        BluetoothGattService service = gatt.getService(service_uuid);

        if (service == null) {
            Log.e(TAG, "No service discovered.");
            gatt.disconnect();
            return;
        }

        Log.d(TAG, "Service found: " + service.getUuid().toString());
        service.getCharacteristics();

        gatt.setCharacteristicNotification(service.getCharacteristic(AuthenticationGATT.responseUuid), true);
        gatt.setCharacteristicNotification(service.getCharacteristic(AuthenticationGATT.notifyDataUuid), true);

        final BluetoothGattCharacteristic characteristic = service.getCharacteristic(AuthenticationGATT.requestUuid);

        authenticationNonce = new byte[8];
        try {
            SecureRandom.getInstance("SHA1PRNG").nextBytes(authenticationNonce);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Log.e(TAG, "SHA1PRNG not supported");
            return;
        }

        Log.d(TAG, "Auth nonce: " + ByteArrayHelper.toString(authenticationNonce));


        Peer peer = PeerDatabase.getInstance().getPeer(peerIdentifier);
        byte[] data = peer.authenticationKey.getAdvertisingData();

        byte[] request = new byte[16];
        System.arraycopy(data, 0, request, 0, 8);
        System.arraycopy(authenticationNonce, 0, request, 8, 8);

        Log.d(TAG, "Characteristic found: " + characteristic.getUuid().toString());
        characteristic.setValue(request);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                gatt.writeCharacteristic(characteristic);
                Log.d(TAG, "Characteristic writing..");
            }
        });
    }

    private void confirmAuthentication(final BluetoothGatt gatt, byte[] data) {
        writeInProgress = true;
        BluetoothGattService service = gatt.getService(service_uuid);
        Log.d(TAG, "Service found: " + service.getUuid().toString());
        service.getCharacteristics();

        final BluetoothGattCharacteristic characteristic = service.getCharacteristic(AuthenticationGATT.confirmUuid);

        Peer peer = PeerDatabase.getInstance().getPeer(peerIdentifier);
        byte[] decrypted = peer.authenticationKey.decryptUniqueData(data);

        byte[] nonce = new byte[8];
        System.arraycopy(decrypted, 0, nonce, 0, 8);
        serverSessionIV = new byte[4];
        System.arraycopy(decrypted, 8, serverSessionIV, 0, 4);
        clientSessionIV = new byte[4];
        System.arraycopy(decrypted, 12, clientSessionIV, 0, 4);

        String recvToken = ByteArrayHelper.toString(nonce);
        String authToken = ByteArrayHelper.toString(authenticationNonce);
        if (!authToken.equals(recvToken)) {
            Log.e(TAG, "Token does not match: " + recvToken);
            gatt.disconnect();
            return;
        }

        messageCounter = new ByteCounter();

        byte[] temp = new byte[16];
        System.arraycopy(decrypted, 8, temp, 0, 8);
        System.arraycopy(decrypted, 0, temp, 8, 8);

        byte[] payload = peer.authenticationKey.encryptUniqueData(temp);

        Log.d(TAG, "Characteristic found: " + characteristic.getUuid().toString());
        characteristic.setValue(payload);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                gatt.writeCharacteristic(characteristic);
                Log.d(TAG, "Characteristic writing..");
            }
        });
    }

    private void processData(byte[] data) {
        int byteNumber = getByteNumber(data);
        // first packet from message
        if (this.message == null) {

            if (byteNumber != 0x0 && byteNumber != 0xFFFF) {
                Log.e(TAG, "Error first message packet had invalid byte number.");
                message = null;
                return;
            }

            message = new byte[data.length - 2];
            System.arraycopy(data, 2, message, 0, data.length - 2);

        } else {

            if (byteNumber != message.length && byteNumber != 0xFFFF) {
                Log.e(TAG, "Error followup message packet had invalid byte number.");
                message = null;
                return;
            }

            byte[] temp = message;
            message = new byte[temp.length + data.length - 2];
            System.arraycopy(temp, 0, message, 0, temp.length);
            System.arraycopy(data, 2, message, temp.length, data.length - 2);
        }

        // end of stream -> forward message and reset state
        if (byteNumber == 0xFFFF) {
            processMessage(message);
            message = null;
        }
    }

    private int getByteNumber(byte[] data) {
        return ByteBuffer.allocate(4)
                .put((byte) 0).put((byte) 0)
                .put(data[0]).put(data[1])
                .getInt(0);
    }

    private void processMessage(byte[] message) {
        Peer peer = PeerDatabase.getInstance().getPeer(peerIdentifier);
        Log.i(TAG, "Message received (encrypted): " + ByteArrayHelper.toString(message));

        byte[] decrypted = peer.authenticationKey.decrypt(serverSessionIV, message);
        if (decrypted == null) {
            Log.e(TAG, "Message could not be decoded.");
            return;
        }
        Log.i(TAG, "Message received: " + ByteArrayHelper.toString(decrypted));

        peer.addReceivedMessage(decrypted);
    }


    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param device The destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final BluetoothDevice device) {
        if (bluetoothAdapter == null || device == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (mConnectionState != STATE_DISCONNECTED) {
            return true;
        }
        // Previously connected device.  Try to reconnect.
//        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
//                && mBluetoothGatt != null) {
//            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
//            if (mBluetoothGatt.connect()) {
//                mConnectionState = STATE_CONNECTING;
//                return true;
//            } else {
//                return false;
//            }
//          }

        PeerDatabase.getInstance().getPeer(peerIdentifier).connecting();

        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt = null;
        }
//        Intent i = new Intent(context, DiscoveryService.class);
//        i.setAction(DiscoveryService.STOP_SCANNING);
//        context.startService(i);
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBluetoothGatt = device.connectGatt(context, false, mGattCallback);
            }
        });


        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = device.getAddress();
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public void notifyDataAvailable() {
        Peer peer = PeerDatabase.getInstance().getPeer(peerIdentifier);
        if (peer.getStatus() == PeerStatus.AUTHENTICATED && !writeInProgress) {
            postedMessage = peer.getNextOutgoingMessage();
            if (postedMessage != null) writeMessage(postedMessage);
        }
    }

    private void writeMessage(byte[] data) {
        writeInProgress = true;
        BluetoothGattService service = mBluetoothGatt.getService(service_uuid);
        Log.d(TAG, "Service found: " + service.getUuid().toString());
        service.getCharacteristics();

        final BluetoothGattCharacteristic characteristic = service.getCharacteristic(AuthenticationGATT.postDataUuid);

        Peer peer = PeerDatabase.getInstance().getPeer(peerIdentifier);
        byte[] encrypted = peer.authenticationKey.encrypt(messageCounter.getNextCount(), clientSessionIV, data);

        Log.d(TAG, "Characteristic found: " + characteristic.getUuid().toString());
        characteristic.setValue(encrypted);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBluetoothGatt.writeCharacteristic(characteristic);
                Log.d(TAG, "Characteristic writing..");
            }
        });

    }
}
