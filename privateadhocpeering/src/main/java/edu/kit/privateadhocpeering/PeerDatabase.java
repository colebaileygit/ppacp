package edu.kit.privateadhocpeering;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.telecom.Call;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.SynchronousQueue;

public class PeerDatabase {

    private Map<String, Peer> peers;
    private long tickId;
    private Map<List<Byte>, String> ephemeralKeys;
    private Map<List<Byte>, Key> scanningKeys;

    private static PeerDatabase instance;
    private Context context;

    final List<Peer> discoveredPeers;

    public static PeerDatabase getInstance() {
        if (instance == null) {
            instance = new PeerDatabase();
        }

        return instance;
    }

    private PeerDatabase() {
        peers = new HashMap<>();
        ephemeralKeys = new HashMap<>();
        scanningKeys = new HashMap<>();
        discoveredPeers = new ArrayList<>();
    }

    private void save(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                "edu.kit.peerdatabase", Context.MODE_PRIVATE);

        SharedPreferences.Editor edit = prefs.edit();

        edit.clear();

        edit.putStringSet("ids", getPeerIdentifiers());
        for (String id : getPeerIdentifiers()) {
            Peer p = getPeer(id);
            Set<String> set = new HashSet<>();
            for (Key k : p.getDiscoveryKeys()) {
                set.add(k.toString());
            }
            Set<String> advSet = new HashSet<>(p.getAdvertisementSets());
            edit.putStringSet(id + "scanKeys", set);
            edit.putStringSet(id + "advSets", advSet);
            edit.putString(id + "authKey", p.getAuthenticationKey());
        }

        edit.commit();
    }

    private void read(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                "edu.kit.peerdatabase", Context.MODE_PRIVATE);

        Set<String> ids = prefs.getStringSet("ids", new HashSet<String>());
        for (String id : ids) {
            Set<String> scanKeys = prefs.getStringSet(id + "scanKeys", new HashSet<String>());
            Set<String> advSets = prefs.getStringSet(id + "advSets", new HashSet<String>());
            String authKey = prefs.getString(id + "authKey", "");
            if (scanKeys.isEmpty() || advSets.isEmpty() || Objects.equals(authKey, "")) {
                Log.e("ERROR", "Peer " + id + " could not be decoded!");
                continue;
            }
            Set<Key> keys = new HashSet<>();
            for (String s : scanKeys) {
                keys.add(new Key(UUID.fromString(s)));
            }
            Peer peer = insertPeer(id, advSets, keys, authKey);
        }

        resetScanning();
    }

    public void initTestMode(Context context, String id) {
        switch (id) {
            case "Alice":
                // advertising keys
                BeaconManager.initInstance(context).addAdvertisementSet("Bob", false, "48427f2a-8521-41d6-a37c-3e552e92cbf2");
                BeaconManager.initInstance(context).addAdvertisementSet("All", true, "58427f2a-8521-41d6-a37c-3e552e92cbf3");

                // scanning keys
                PeerDatabase.getInstance().addPeer("Bob", "Bob", "18427f2a-8521-41d6-a37c-3e552e92cbf2", "a7427f2a-8521-41d6-a37c-3e552e92cbf2");
                PeerDatabase.getInstance().addPeer("Carl", "All", "28427f2a-8521-41d6-a37c-3e552e92cbf2", "b7427f2a-8521-41d6-a37c-3e552e92cbf2");
                PeerDatabase.getInstance().addPeer("BobGroup", "All", "38427f2a-8521-41d6-a37c-3e552e92cbf2", "c7427f2a-8521-41d6-a37c-3e552e92cbf2");

                notifyStatusListeners(getPeer("Bob"));
                notifyStatusListeners(getPeer("Carl"));
                notifyStatusListeners(getPeer("BobGroup"));
                break;
            case "Bob":
                // advertising keys
                BeaconManager.initInstance(context).addAdvertisementSet("Alice", false, "18427f2a-8521-41d6-a37c-3e552e92cbf2");
                BeaconManager.initInstance(context).addAdvertisementSet("All", true, "38427f2a-8521-41d6-a37c-3e552e92cbf2");

                // scanning keys
                PeerDatabase.getInstance().addPeer("Alice", "Alice", "48427f2a-8521-41d6-a37c-3e552e92cbf2", "a7427f2a-8521-41d6-a37c-3e552e92cbf2");
                PeerDatabase.getInstance().addPeer("Carl", "All", "28427f2a-8521-41d6-a37c-3e552e92cbf2", "d7427f2a-8521-41d6-a37c-3e552e92cbf2");
                PeerDatabase.getInstance().addPeer("AliceGroup", "All", "58427f2a-8521-41d6-a37c-3e552e92cbf3", "c7427f2a-8521-41d6-a37c-3e552e92cbf2");

                notifyStatusListeners(getPeer("Alice"));
                notifyStatusListeners(getPeer("Carl"));
                notifyStatusListeners(getPeer("AliceGroup"));
                break;
            case "Carl":
                // advertising keys
                BeaconManager.initInstance(context).addAdvertisementSet("All", true, "28427f2a-8521-41d6-a37c-3e552e92cbf2");

                // scanning keys
                PeerDatabase.getInstance().addPeer("Alice", "All", "58427f2a-8521-41d6-a37c-3e552e92cbf3", "d7427f2a-8521-41d6-a37c-3e552e92cbf2");
                PeerDatabase.getInstance().addPeer("Bob", "All", "38427f2a-8521-41d6-a37c-3e552e92cbf2", "b7427f2a-8521-41d6-a37c-3e552e92cbf2");

                notifyStatusListeners(getPeer("Bob"));
                notifyStatusListeners(getPeer("Alice"));
                break;
        }
    }


    public Set<String> getPeerIdentifiers() {
        return peers.keySet();
    }

    public List<Peer> getPeers() { return new ArrayList<>(peers.values()); }

    public Peer getPeer(String peerIdentifier) {
        return peers.get(peerIdentifier);
    }

    public boolean addPeer(String peerIdentifier, String discoveryGroupIdentifier, String discoveryKey) {
        return addPeer(peerIdentifier, discoveryGroupIdentifier, discoveryKey, new Key().toString());
    }

    public boolean addPeer(String peerIdentifier, String discoveryGroupIdentifier, String discoveryKey, String authenticationKey) {
        if (peers.containsKey(peerIdentifier)) return false;

        Peer peer = insertPeer(peerIdentifier, discoveryGroupIdentifier, discoveryKey, authenticationKey);
        if (context != null) save(context);

        resetScanning();

        if (peer != null) notifyStatusListeners(peer);

        return true;
    }

    private Peer insertPeer(String peerIdentifier, String discoveryGroupIdentifier, String discoveryKey, String authenticationKey) {
        Set<Key> keys = new HashSet<>();
        keys.add(new Key(UUID.fromString(discoveryKey)));
        Set<String> ids = new HashSet<>();
        ids.add(discoveryGroupIdentifier);
        return insertPeer(peerIdentifier, ids, keys, authenticationKey);
    }

    private Peer insertPeer(String peerIdentifier, Set<String> advertisementSetIdentifiers, Set<Key> discoveryKeys, String authenticationKey) {
        Key authentKey = new Key(UUID.fromString(authenticationKey));
        try {
            Peer peer = new Peer(peerIdentifier, advertisementSetIdentifiers, discoveryKeys, authentKey);
            peers.put(peerIdentifier, peer);
            insertEphemeralIdentifier(peer);

            // add authorization for advertisement sets
            for (String id : advertisementSetIdentifiers) {
                BeaconManager.tryGetInstance().getAdvertisementSet(id).addAuthorization(peer.getIdentifier());
            }
            return peer;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }



    private void insertEphemeralIdentifier(Peer peer) {
        for (Key k : peer.getDiscoveryKeys()) {
            List<Byte> bytes = ByteArrayHelper.toList(k.getAdvertisingData());
            ephemeralKeys.put(bytes, peer.getIdentifier());
            scanningKeys.put(bytes, k);
        }
    }


    public boolean removePeer(String peerIdentifier) {
        Peer peer = peers.remove(peerIdentifier);

        if (peer == null) return false;

        BeaconManager beaconManager = BeaconManager.tryGetInstance();

        // remove authorizations for the peer's discovery sets
        for (String s : peer.getAdvertisementSets()) {
            if (beaconManager != null && beaconManager.containsAdvertisementSet(s)) {
                beaconManager.getAdvertisementSet(s).removeAuthorization(peer.getIdentifier());
            }
        }

        if (context != null) {
            save(context);
        }

        resetScanning();
        return true;
    }

    public boolean contains(String peerIdentifier) {
        return peers.containsKey(peerIdentifier);
    }

    String getIdentifier(byte[] ephemeralKey) {
        if (ephemeralKey == null) return "";

        refreshEphemeralKeys();
        List<Byte> bytes = ByteArrayHelper.toList(ephemeralKey);

        return ephemeralKeys.get(bytes);
    }

    Key getScanningKey(byte[] ephemeralKey) {
        if (ephemeralKey == null) return null;

        refreshEphemeralKeys();
        List<Byte> bytes = ByteArrayHelper.toList(ephemeralKey);

        return scanningKeys.get(bytes);
    }

    List<byte[]> getCurrentEphemeralIdentifiers() {
        refreshEphemeralKeys();
        Set<List<Byte>> keys = ephemeralKeys.keySet();
        List<byte[]> result = new ArrayList<>();
        for (List<Byte> bytes : keys) {
            result.add(ByteArrayHelper.toArray(bytes));
        }

        return result;
    }

    void refreshEphemeralKeys() {
        long tick = System.currentTimeMillis() / (1000 * Key.TICK_LENGTH);
        if (tick == tickId) {
            return;
        }

        tickId = tick;
        ephemeralKeys.clear();
        scanningKeys.clear();

        for (Peer p : getPeers()) {
            insertEphemeralIdentifier(p);
        }
    }

    void notifyStatusListeners(Peer peer) {
        String intentAction = "";
        switch (peer.getStatus()) {
            case DISCOVERED:
                intentAction = CallbackReceiver.PROXIMITY_NEAR;
                break;
            case OUT_OF_RANGE:
                intentAction = CallbackReceiver.PROXIMITY_LOST;
                updateQueue(peer);
                break;
            case CONNECTING:
                intentAction = CallbackReceiver.CONNECTING;
                break;
            case CONNECTED:
                intentAction = CallbackReceiver.CONNECTED;
                break;
            case AUTHENTICATED:
                intentAction = CallbackReceiver.AUTHENTICATED;
                updateQueue(peer);
                break;
        }

        Intent intent = new Intent(intentAction);
        intent.putExtra(CallbackReceiver.KEY_IDENTIFIER, peer.getIdentifier());
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void updateQueue(Peer peer) {
        synchronized (discoveredPeers) {
            discoveredPeers.remove(peer);
            if (!discoveredPeers.isEmpty()) {
                try {
                    if (context != null) {
                        Log.i("PEER", "Authentication started. Peer next in queue.");
                        discoveredPeers.get(0).authenticate(context);
                    }
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    discoveredPeers.remove(peer);
                }
            }
        }



    }

    void notifyMessageListeners(Peer peer) {
        Intent intent = new Intent(CallbackReceiver.NEW_MESSAGE);
        intent.putExtra(CallbackReceiver.KEY_IDENTIFIER, peer.getIdentifier());
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    void setContext(Context c) {
        context = c;
        BeaconManager.initInstance(c);
        read(c);
    }

    void removeContext() {
        save(context);
        context = null;
    }

    private void resetScanning() {
        if (context == null) return;
        Intent i = new Intent(context, DiscoveryService.class);
        i.setAction(DiscoveryService.RESET_SCANNING);
        context.startService(i);
    }

}
