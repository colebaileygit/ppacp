package edu.kit.privateadhocpeering;


import android.bluetooth.BluetoothGattCharacteristic;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

class MessageQueue {

    private int counter;

    private List<byte[]> messages;

    private int messageOffset;

    private byte[] lastPacket;

    MessageQueue() {
        counter = 1;
        messages = new ArrayList<>();
    }

    void addMessage(byte[] data) {
        messages.add(data);
    }

    boolean messageAvailable() {
        return !messages.isEmpty();
    }

    int incrementMessageCounter() {
        return counter++;
    }

    byte[] getBytes(int payloadSize) {
        if (messages.size() == 0) return null;

        int packetSize = Math.min(payloadSize, messages.get(0).length - messageOffset + 2);
        lastPacket = new byte[packetSize];

        // determine if final packet or not
        int byteCount = packetSize - 2;
        byte[] byteNumber = new byte[2];
        boolean isFinal = messages.get(0).length - messageOffset <= payloadSize - 2;
        if (isFinal) {
            byteNumber = new byte[] { (byte)0xFF, (byte)0xFF };
        } else {
            byte[] bytes = ByteArrayHelper.fromLong(messageOffset);
            byteNumber = new byte[] { bytes[1], bytes[0] };
        }

        // construct packet
        System.arraycopy(byteNumber, 0, lastPacket, 0, 2);

        System.arraycopy(messages.get(0), messageOffset, lastPacket, 2, byteCount);

        // update queue state
        if (isFinal) {
            messageOffset = 0;
            messages.remove(0);
        } else {
            messageOffset += byteCount;
        }

        return lastPacket;
    }

    byte[] getLastPacket() {
        return lastPacket;
    }
}
