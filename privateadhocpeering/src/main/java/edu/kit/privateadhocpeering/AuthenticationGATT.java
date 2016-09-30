package edu.kit.privateadhocpeering;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class AuthenticationGATT {

    static final String TAG = "GATT_SERVER";


    private Context mContext;
    static final UUID base_uuid = UUID.fromString("9f4a76ec-adbb-42c0-ac30-615c43ff98db");
    private UUID service_uuid;
    static final UUID requestUuid     = UUID.fromString("f0e4d791-a72b-425b-bd66-8a4dbac83c37");
    static final UUID responseUuid    = UUID.fromString("f0e4d792-a72b-425b-bd66-8a4dbac83c37");
    static final UUID confirmUuid     = UUID.fromString("f0e4d793-a72b-425b-bd66-8a4dbac83c37");
    static final UUID postDataUuid    = UUID.fromString("f0e4d794-a72b-425b-bd66-8a4dbac83c37");
    static final UUID notifyDataUuid  = UUID.fromString("f0e4d795-a72b-425b-bd66-8a4dbac83c37");

    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private Map<Integer, List<Byte>> writtenData;

    private Handler mHandler;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGattServer gattServer;
    private BluetoothGattCharacteristic responseCharacteristic;
    private BluetoothGattCharacteristic notifyCharacteristic;
    private final AdvertisementSet set;

    private Map<String, GattClient> clients;

    private boolean isSending;

    private BluetoothGattServerCallback serverCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Device connected: " + device.getAddress());
                if (!clients.containsKey(device.getAddress())) {
                    clients.put(device.getAddress(), new GattClient());
                }
                GattClient client = clients.get(device.getAddress());
                client.peerDevice = device;
                if (client.peer != null) client.peer.connected();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                GattClient client = clients.get(device.getAddress());
                if (client != null && client.peer != null) client.peer.lost();
                clients.remove(device.getAddress());
                Log.d(TAG, "Device disconnected: " + device.getAddress());
                gattServer.cancelConnection(device);
            }

            super.onConnectionStateChange(device, status, newState);
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d(TAG, "Characteristic read request: " + characteristic.getUuid());
            gattServer.sendResponse(device, requestId, 0, offset, new byte[0]);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.d(TAG, "Characteristic write request: " + characteristic.getUuid() + " prepared: " + preparedWrite);

            // TODO: consider offset correctly
            List<Byte> data = writtenData.get(requestId - 1);
            if (data == null) data = new ArrayList<>();
            for (byte b : value) {
                data.add(b);
            }



            writtenData.put(requestId, data);

            int status = BluetoothGatt.GATT_SUCCESS;

            GattClient client = clients.get(device.getAddress());

            if (!preparedWrite) {
                if (characteristic.getUuid().equals(requestUuid)) {
                    interpretAuthenticationRequest(device, requestId);
                } else if (characteristic.getUuid().equals(confirmUuid)) {
                    interpretAuthenticationConfirmation(device, requestId);
                    status = client.peer.getStatus() == PeerStatus.AUTHENTICATED ?
                            BluetoothGatt.GATT_SUCCESS : BluetoothGatt.GATT_FAILURE;
                } else if (characteristic.getUuid().equals(postDataUuid)) {
                    if (client.peer.getStatus() != PeerStatus.AUTHENTICATED) {
                        status = BluetoothGatt.GATT_FAILURE;
                    } else {
                        byte[] decrypted = client.peer.authenticationKey.decrypt(client.clientSessionIV, ByteArrayHelper.toArray(writtenData.remove(requestId)));
                        Log.i(TAG, "Message received: " + ByteArrayHelper.toString(decrypted));
                        if (decrypted != null) {
                            status = BluetoothGatt.GATT_SUCCESS;
                            client.peer.addReceivedMessage(decrypted);
                        } else {
                            status = BluetoothGatt.GATT_FAILURE;
                        }
                    }
                }
            }


            if (responseNeeded) {

                gattServer.sendResponse(device, requestId, status, offset, value);
            }

        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);
            Log.d(TAG, "Execute write: " + execute);

            int status = BluetoothGatt.GATT_FAILURE;

            GattClient client = clients.get(device.getAddress());
            // TODO: handle client does not exist

            if (client.peer.getStatus() == PeerStatus.AUTHENTICATED && execute) {
                byte[] decrypted = client.peer.authenticationKey.decrypt(client.clientSessionIV, ByteArrayHelper.toArray(writtenData.remove(requestId - 1)));
                Log.i(TAG, "Message received: " + ByteArrayHelper.toString(decrypted));
                if (decrypted != null) {
                    status = BluetoothGatt.GATT_SUCCESS;
                    client.peer.addReceivedMessage(decrypted);
                }
            }

            gattServer.sendResponse(device, requestId, status, 0, new byte[0]);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            Log.d(TAG, "Notification sent: " + status);

            GattClient client = clients.get(device.getAddress());

            // resend failed notify
            if (status != BluetoothGatt.GATT_SUCCESS) {
                if (client.peer.getStatus() != PeerStatus.AUTHENTICATED) {
                    notifyResponseCharacteristic(device, client.authenticationData);
                } else {
                    sendDataNotification(client.peerDevice, client.dataQueue.getLastPacket());
                }
                return;
            }

            // continue sending data
            if (client.peer.getStatus() == PeerStatus.AUTHENTICATED && isSending) {
                if (client.dataQueue.messageAvailable()) {
                    sendDataNotification(client.peerDevice, client.dataQueue.getBytes(20));
                } else {
                    client.peer.confirmMessageSent();
                }

                if (client.peer != null && client.peer.hasData()){
                    sendMessage(client);
                } else {
                    isSending = false;
                    // update broadcasts once payload delivered
                    if (client.peer != null) {
                        // TODO: enter delay between cancelling broadcasts to accommodate for responses from client
                        BeaconManager instance = BeaconManager.tryGetInstance();
                        if (instance != null) instance.updateBroadcasts();
                    }

                }
            }
        }
    };

    AuthenticationGATT(Context context, AdvertisementSet set) {
        mContext = context;
        bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        writtenData = new HashMap<>();
        this.set = set;
        mHandler = new Handler(Looper.getMainLooper());
        clients = new HashMap<>();
    }

    public boolean openServer() {
        gattServer = bluetoothManager.openGattServer(mContext, serverCallback);

        for (BluetoothDevice device : bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER)) {
            Log.i("CONN_DEVICES", device.toString());
        }

        service_uuid = set.advertisementKey.getServiceUUID();

        Log.i(TAG, "Broadcasted service id: " + service_uuid);
        BluetoothGattService gattService = new BluetoothGattService(service_uuid, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic requestCharacteristic = new BluetoothGattCharacteristic(requestUuid,
                BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);

        responseCharacteristic = new BluetoothGattCharacteristic(responseUuid,
                BluetoothGattCharacteristic.PROPERTY_INDICATE, BluetoothGattCharacteristic.PERMISSION_READ);

        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID, BluetoothGattDescriptor.PERMISSION_READ);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);

        responseCharacteristic.addDescriptor(descriptor);

        BluetoothGattCharacteristic confirmCharacteristic = new BluetoothGattCharacteristic(confirmUuid,
                BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattCharacteristic postCharacteristic = new BluetoothGattCharacteristic(postDataUuid,
                BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);

        notifyCharacteristic = new BluetoothGattCharacteristic(notifyDataUuid,
                BluetoothGattCharacteristic.PROPERTY_INDICATE, BluetoothGattCharacteristic.PERMISSION_READ);
        notifyCharacteristic.addDescriptor(descriptor);

        gattService.addCharacteristic(requestCharacteristic);
        gattService.addCharacteristic(responseCharacteristic);
        gattService.addCharacteristic(confirmCharacteristic);
        gattService.addCharacteristic(postCharacteristic);
        gattService.addCharacteristic(notifyCharacteristic);

        gattServer.addService(gattService);

        return true;
    }



    public void closeServer() {
        for (GattClient client : new ArrayList<>(clients.values())) {
            if (client.peer != null) client.peer.lost();

            if (client.peerDevice != null) {
                Log.i(TAG, "Closing device: " + client.peerDevice.getAddress());
                gattServer.cancelConnection(client.peerDevice);
            }
        }

        gattServer.removeService(new BluetoothGattService(service_uuid, BluetoothGattService.SERVICE_TYPE_PRIMARY));

        gattServer.close();

    }

    private void interpretAuthenticationRequest(BluetoothDevice device, int requestId) {
        List<Byte> data = writtenData.remove(requestId);

        if (data == null || data.size() < 16) {
            Log.e("AUTH", "Input not long enough.");
            return;
        }

        GattClient client = clients.get(device.getAddress());

        byte[] eid = new byte[8];
        for (int i = 0; i < 8; i++) {
            eid[i] = data.get(i);
        }
        byte[] token = new byte[16];
        for (int i = 8; i < 16; i++) {
            token[i - 8] = data.get(i);
        }

        byte[] nonce = new byte[8];
        try {
            SecureRandom.getInstance("SHA1PRNG").nextBytes(nonce);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Log.e(TAG, "SHA1PRNG not supported");
            return;
        }

        System.arraycopy(nonce, 0, token, 8, 8);
        client.serverSessionIV = new byte[4];
        System.arraycopy(nonce, 0, client.serverSessionIV, 0, 4);
        client.clientSessionIV = new byte[4];
        System.arraycopy(nonce, 4, client.clientSessionIV, 0, 4);

        Log.i(TAG, "Authentication nonce: " + ByteArrayHelper.toString(token));

        byte[] reverseToken = new byte[16];
        System.arraycopy(nonce, 0, reverseToken, 0, 8);
        System.arraycopy(token, 0, reverseToken, 8, 8);

        client.expectedAuthenticationToken = ByteArrayHelper.toString(reverseToken);

        // find correct peer
        PeerDatabase peerDb = PeerDatabase.getInstance();
        String identifier = ByteArrayHelper.toString(eid);
        for (String id : set.getAuthorizedPeers()) {
            Peer temp = peerDb.getPeer(id);
            String advData = ByteArrayHelper.toString(temp.authenticationKey.getAdvertisingData());
            if (advData.equals(identifier)) {
                client.peer = temp;
                client.peer.connected();
                break;
            }
        }

        if (client.peer == null) {
            Log.e(TAG, "No peer found");
            gattServer.cancelConnection(device);
            return;
        }

        byte[] encrypted = client.peer.authenticationKey.encryptUniqueData(token);

        notifyResponseCharacteristic(device, encrypted);

    }

    private void interpretAuthenticationConfirmation(BluetoothDevice device, int requestId) {
        List<Byte> data = writtenData.remove(requestId);

        if (data.size() < 16) {
            Log.e("AUTH", "Input nonce not long enough.");
            return;
        }

        GattClient client = clients.get(device.getAddress());

        byte[] encrypted = ByteArrayHelper.toArray(data);
        byte[] token = client.peer.authenticationKey.decryptUniqueData(encrypted);

        String checkToken = ByteArrayHelper.toString(token);
        Log.i(TAG, "Authentication confirmation: " + checkToken);

        if (Objects.equals(checkToken, client.expectedAuthenticationToken)) {
            Log.i(TAG, "Authenticated peer: " + client.peer.getIdentifier());
            client.peer.authenticated();

            client.messageCounter = new ByteCounter();

            Log.i(TAG, "Peer has data: " + client.peer.hasData());
            sendMessage(client);
        }
    }

    private void notifyResponseCharacteristic(final BluetoothDevice device, byte[] data) {
        Log.i(TAG, "Notification data: " + ByteArrayHelper.toString(data));
        GattClient client = clients.get(device.getAddress());
        client.authenticationData = data;
        responseCharacteristic.setValue(data);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (device != null && responseCharacteristic != null)
                    try {
                        gattServer.notifyCharacteristicChanged(device, responseCharacteristic, true);
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
            }
        });
    }

    void sendData(String peerId) {
        GattClient foundClient = null;
        for (GattClient client : clients.values()) {
            if (client.peer != null && client.peer.getIdentifier().equals(peerId)) {
                foundClient = client;
                break;
            }
        }
        if (foundClient == null || foundClient.peer == null ||
                !Objects.equals(foundClient.peer.getIdentifier(), peerId) || !foundClient.peer.hasData()) return;

        sendMessage(foundClient);
    }

    private void sendMessage(GattClient client) {
        if (client.peer.getStatus() == PeerStatus.AUTHENTICATED && client.peer.hasData() && !isSending) {
            byte[] encrypted = client.peer.authenticationKey.encrypt(client.messageCounter.getNextCount(),
                    client.serverSessionIV, client.peer.getNextOutgoingMessage());
            client.dataQueue.addMessage(encrypted);

            byte[] packet = client.dataQueue.getBytes(20);
            sendDataNotification(client.peerDevice, packet);
        }
    }

    private void sendDataNotification(final BluetoothDevice device, byte[] data) {
        Log.i(TAG, "Sending payload data: " + ByteArrayHelper.toString(data));
        isSending = true;
        notifyCharacteristic.setValue(data);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
            if (device != null && notifyCharacteristic != null)
                gattServer.notifyCharacteristicChanged(device, notifyCharacteristic, true);

            }
        });
    }



}
