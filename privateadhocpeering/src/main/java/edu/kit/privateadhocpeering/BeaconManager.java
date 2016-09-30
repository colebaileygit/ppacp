package edu.kit.privateadhocpeering;

import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class BeaconManager {

    private final Context context;
    private PeerDatabase peerDatabase;
    private static int RETRY_ATTEMPTS = 3;
    private static String TAG = "BEACONS";

    private Map<String, AdvertisementSet> advertisementSets;
    private Map<String, DiscoveryBeacon> activeBeacons;

    private static BeaconManager instance;
    private List<StatusChangedListener> listeners;

    private boolean active;

    static BeaconManager tryGetInstance() {
        return instance;
    }

    public static BeaconManager initInstance(Context context) {
        if (instance == null) {
            instance = new BeaconManager(context);
            instance.activateBroadcasts();
        }

        return instance;
    }

    BeaconManager(Context context) {
        this.context = context;
        peerDatabase = PeerDatabase.getInstance();
        activeBeacons = new HashMap<>();
        advertisementSets = new HashMap<>();
        listeners = new ArrayList<>();

        read();
    }

    public List<AdvertisementSet> getAdvertisementSets() {
        return new ArrayList<>(advertisementSets.values());
    }

    public AdvertisementSet getAdvertisementSet(String setIdentifier) {
        return advertisementSets.get(setIdentifier);
    }

    public boolean addAdvertisementSet(String setIdentifier, boolean proximityAware) {
        return addAdvertisementSet(setIdentifier, proximityAware, new Key().toString());
    }

    public boolean addAdvertisementSet(String setIdentifier, boolean proximityAware, String key) {
        if (advertisementSets.containsKey(setIdentifier)) return false;

        AdvertisementSet set = new AdvertisementSet(setIdentifier, proximityAware, key);

        advertisementSets.put(setIdentifier, set);
        save();

        updateBroadcasts();

        return true;
    }

    public boolean removeAdvertisementSet(String setIdentifier) {
        if (!advertisementSets.containsKey(setIdentifier)) return false;

        AdvertisementSet set = advertisementSets.remove(setIdentifier);
        for (String peer : set.getAuthorizedPeers()) {
            PeerDatabase.getInstance().removePeer(peer);
        }

        save();

        return true;
    }

    public boolean containsAdvertisementSet(String setIdentifier) {
        return advertisementSets.containsKey(setIdentifier);
    }

    public boolean isBroadcasting(String setIdentifier) {
        return activeBeacons.containsKey(setIdentifier);
    }

    void updateBroadcasts() {
        if (!active) return;

        for (AdvertisementSet set : new ArrayList<>(advertisementSets.values())) {
            if (set.isActive() && !activeBeacons.containsKey(set.getIdentifier())) {
                beginBroadcast(set.getIdentifier());
            } else if (!set.isActive() && activeBeacons.containsKey(set.getIdentifier())) {
                endBroadcast(set.getIdentifier());
            }
        }
    }

    boolean activateBroadcasts() {
        active = true;

        Log.i(TAG, "Starting advertising: " + System.currentTimeMillis() / (1000 * Key.TICK_LENGTH));

        for (AdvertisementSet set : new ArrayList<>(advertisementSets.values())) {
            if (set.isActive()) {
                beginBroadcast(set.getIdentifier());
            }
        }

        return true;
    }

    private boolean beginBroadcast(final String setIdentifier) {
        if (!containsAdvertisementSet(setIdentifier) || activeBeacons.containsKey(setIdentifier)) return false;

        AdvertisementSet set = getAdvertisementSet(setIdentifier);

        final DiscoveryBeacon beacon = new DiscoveryBeacon(context, set);
        AdvertiseCallback callback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                activeBeacons.put(setIdentifier, beacon);
                notifyListeners(setIdentifier, true);
            }

            @Override
            public void onStartFailure(int errorCode) {
                switch (errorCode) {
                    case ADVERTISE_FAILED_ALREADY_STARTED:
                        Log.i(TAG, "Advertise failed: already started");
                        break;
                    case ADVERTISE_FAILED_DATA_TOO_LARGE:
                        Log.i(TAG, "Advertise failed: data too large");
                        break;
                    case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                        Log.i(TAG, "Advertise failed: feature unsupported");
                        break;
                    case ADVERTISE_FAILED_INTERNAL_ERROR:
                        Log.i(TAG, "Advertise failed: internal error");
                        break;
                    case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                        Log.i(TAG, "Advertise failed: too many advertisers");
                        break;
                    default:
                        Log.i(TAG, "Advertise failed: UNKNOWN");
                        break;
                }

                notifyListeners(setIdentifier, false);

                // TODO: implement delayed retry
            }
        };

        beacon.broadcast(callback);

        return true;
    }

    boolean deactivateBroadcasts() {
        active = false;

        Log.i(TAG, "Stopping broadcasts: " + System.currentTimeMillis() / (1000 * Key.TICK_LENGTH));

        for (String id : new ArrayList<>(activeBeacons.keySet())) {
            endBroadcast(id);
        }

        return true;
    }

    private boolean endBroadcast(String setIdentifier) {
        if (!activeBeacons.containsKey(setIdentifier)) return false;

        activeBeacons.remove(setIdentifier).cancel();

        notifyListeners(setIdentifier, false);

        return true;
    }

    void writeMessage(String peerIdentifier) {
        Peer peer = PeerDatabase.getInstance().getPeer(peerIdentifier);
        List<String> sets = peer.getAdvertisementSets();

        // check if a valid beacon for the selected peer is active
        for (String s : sets) {
            if (activeBeacons.containsKey(s)) {
                activeBeacons.get(s).writeMessage(peerIdentifier);
                return;
            }
        }

        // otherwise, activate beacon with smallest peer set (set of keys is sorted)
        if (!sets.isEmpty()) {
            AdvertisementSet smallestSet = advertisementSets.get(sets.get(0));
            beginBroadcast(smallestSet.getIdentifier());
        }
    }

    public void addListener(StatusChangedListener listener) {
        listeners.add(listener);
    }

    public void removeListener(StatusChangedListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(String keyIdentifier, boolean active) {
        List<StatusChangedListener> listenerCopy = new ArrayList<>(listeners);
        for (StatusChangedListener listener : listenerCopy) {
            try {
                listener.onStatusChanged(keyIdentifier, active);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void save() {
        SharedPreferences prefs = context.getSharedPreferences(
                "edu.kit.beaconmanager", Context.MODE_PRIVATE);

        SharedPreferences.Editor edit = prefs.edit();

        edit.clear();

        Set<String> ids = advertisementSets.keySet();
        edit.putStringSet("ids", ids);
        for (String id : ids) {
            AdvertisementSet set = getAdvertisementSet(id);
            edit.putBoolean(id + "proximityAware", set.isProximityAware());
            edit.putString(id + "key", set.getAdvertisementKey());
            // TODO: save messages
        }

        edit.commit();
    }

    private void read() {
        SharedPreferences prefs = context.getSharedPreferences(
                "edu.kit.beaconmanager", Context.MODE_PRIVATE);

        Set<String> ids = prefs.getStringSet("ids", new HashSet<String>());
        for (String id : ids) {
            boolean proximityAware = prefs.getBoolean(id + "proximityAware", false);
            String key = prefs.getString(id + "key", "");
            if (Objects.equals(key, "")) {
                Log.e("ERROR", "Advertisement group " + id + " could not be decoded!");
                continue;
            }
            AdvertisementSet set = new AdvertisementSet(id, proximityAware, key);
            advertisementSets.put(id, set);
        }

        updateBroadcasts();
    }



    public interface StatusChangedListener {
        void onStatusChanged(String keyIdentifier, boolean active);
    }
}
