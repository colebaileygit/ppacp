package edu.kit.privateadhocpeering;


import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;

public class Peer {

    private String identifier;

    Key authenticationKey;
    private PeerStatus status;

    Key scannedKey;
    ScanResult result;

    private List<Key> scanningKeys;
    private List<String> advertisementSets;

    private List<byte[]> outgoingQueue;
    private List<byte[]> incomingQueue;
    private Authenticator authenticator;

    Peer(String identifier, Set<String> advertisementSetIds, Set<Key> scanKeys, Key authKey) {
        this.identifier = identifier;
        this.authenticationKey = authKey;
        status = PeerStatus.OUT_OF_RANGE;
        outgoingQueue = new ArrayList<>();
        incomingQueue = new ArrayList<>();
        scanningKeys = new ArrayList<>(scanKeys);
        advertisementSets = new ArrayList<>(advertisementSetIds);
    }



    void discovered(Context context, ScanResult result, Key scanningKey) {
        if (!scanningKeys.contains(scanningKey)) {
            Log.e("PEER", "Discovered scanning key incorrect.");
            return;
        }

        status = PeerStatus.DISCOVERED;
        this.result = result;
        this.scannedKey = scanningKey;

        PeerDatabase db = PeerDatabase.getInstance();
        db.notifyStatusListeners(this);

        synchronized (db.discoveredPeers) {
            if (db.discoveredPeers.size() == 0) {
                Log.i("PEER", "Authentication started. Discovery queue was empty.");
                authenticate(context);
            }

            if (!db.discoveredPeers.contains(this)) db.discoveredPeers.add(this);
        }
    }

    void lost() {
        status = PeerStatus.OUT_OF_RANGE;
        this.result = null;
        authenticator = null;
        PeerDatabase.getInstance().notifyStatusListeners(this);

    }

    void connecting() {
        status = PeerStatus.CONNECTING;
        PeerDatabase.getInstance().notifyStatusListeners(this);
    }


    /**
     * @throws IllegalStateException If no device information to perform authentication.
     * @param context
     */
    void authenticate(Context context) {
        if (result == null) throw new IllegalStateException("Device not nearby. Cannot authenticate.");

        authenticator = new Authenticator(context, identifier, scannedKey);

        authenticator.connect(result.getDevice());
    }

    void connected() {
        status = PeerStatus.CONNECTED;
        PeerDatabase.getInstance().notifyStatusListeners(this);
    }

    void authenticated() {
        status = PeerStatus.AUTHENTICATED;
        PeerDatabase.getInstance().notifyStatusListeners(this);
    }

    boolean hasData() {
        return !outgoingQueue.isEmpty();
    }

    byte[] getNextOutgoingMessage() {
        if (!hasData()) return null;

        return outgoingQueue.get(0);
    }

    void confirmMessageSent() {
        outgoingQueue.remove(0);
    }


    public void sendData(Context context, byte[] data) {
        outgoingQueue.add(data);
        if (status == PeerStatus.AUTHENTICATED) {
            if (authenticator != null) {
                authenticator.notifyDataAvailable();
            } else {
                BeaconManager.initInstance(context).writeMessage(identifier);
            }
        } else if (status == PeerStatus.OUT_OF_RANGE) {
            BeaconManager.initInstance(context).updateBroadcasts();
        }
    }

    public boolean isMessageAvailable() {
        return !incomingQueue.isEmpty();
    }

    public byte[] getNextReceivedMessage() {
        if (!isMessageAvailable()) return null;

        return incomingQueue.remove(0);
    }

    void addReceivedMessage(byte[] data) {
        incomingQueue.add(data);
        PeerDatabase.getInstance().notifyMessageListeners(this);
    }

    public String getIdentifier() {
        return identifier;
    }

    public PeerStatus getStatus() {
        return status;
    }

    public String getAuthenticationKey() { return authenticationKey.toString(); }


    public List<Key> getDiscoveryKeys() {
        return new ArrayList<>(scanningKeys);
    }

    public boolean addDiscoveryKey(String keyData) {
        Key newKey = new Key(UUID.fromString(keyData));
        return !scanningKeys.contains(newKey) && scanningKeys.add(newKey);
    }

    public boolean removeDiscoveryKey(String keyData) {
        return scanningKeys.remove(new Key(UUID.fromString(keyData)));
    }

    /**
     * Returns a sorted list of advertisement sets that can be used to discover the peer.
     * The list is sorted based on the size of its beacons' addressed sets of peers.
     * @return Sorted list of keys.
     */
    public List<String> getAdvertisementSets() {
        Collections.sort(advertisementSets, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                BeaconManager bm = BeaconManager.tryGetInstance();
                if (bm == null) return 0;           // if beacon manager not available, all keys are equal

                int lSize = bm.getAdvertisementSet(lhs).getAuthorizedPeers().size();
                int rSize = bm.getAdvertisementSet(rhs).getAuthorizedPeers().size();
                return lSize - rSize;
            }
        });
        return new ArrayList<>(advertisementSets);
    }

    public boolean addAdvertisementSet(String setIdentifier) {
        BeaconManager.tryGetInstance().getAdvertisementSet(setIdentifier).addAuthorization(identifier);
        return advertisementSets.add(setIdentifier);
    }

    public boolean removeAdvertisementSet(String setIdentifier) {
        BeaconManager.tryGetInstance().getAdvertisementSet(setIdentifier).removeAuthorization(identifier);
        return advertisementSets.remove(setIdentifier);
    }


    @Override
    public boolean equals(Object o) {
        Peer other = (Peer) o;

        return o.getClass() == Peer.class && Objects.equals(other.identifier, this.identifier);
    }
}
