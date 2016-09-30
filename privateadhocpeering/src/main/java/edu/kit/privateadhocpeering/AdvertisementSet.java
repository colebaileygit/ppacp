package edu.kit.privateadhocpeering;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


public class AdvertisementSet {

    private String identifier;

    // TODO: make not public
    public Key advertisementKey;

    List<String> authorizedPeers;

    private boolean proximityAware;

    AdvertisementSet(String identifier, boolean proximityAware) {
        this(identifier, proximityAware, new Key().toString(), new ArrayList<String>());
    }

    AdvertisementSet(String identifier, boolean proximityAware, String key) {
        this(identifier, proximityAware, key, new ArrayList<String>());
    }

    AdvertisementSet(String identifier, boolean proximityAware, String key, List<String> authorizedPeers) {
        this.identifier = identifier;
        this.authorizedPeers = authorizedPeers;
        advertisementKey = new Key(UUID.fromString(key));
        this.proximityAware = proximityAware;
    }

    public boolean isActive() {
        if (proximityAware) {
            return true;
        } else {
            BeaconManager bm = BeaconManager.tryGetInstance();
            if (bm == null) return false;

            // activate set if it is the smallest set for one of its peers which has data to transmit
            for (String s : new ArrayList<>(authorizedPeers)) {
                Peer p = PeerDatabase.getInstance().getPeer(s);

                if (p.hasData()) {
                    AdvertisementSet smallestSet = bm.getAdvertisementSet(p.getAdvertisementSets().get(0));
                    if (smallestSet.getIdentifier().equals(identifier)) {
                        // check that no other set for this peer is proximity aware
                        if (!hasProximityAwareBeacon(p, bm)) return true;
                    }
                }
            }

            return false;
        }
    }

    private boolean hasProximityAwareBeacon(Peer peer, BeaconManager bm) {
        for (String id : peer.getAdvertisementSets()) {
            if (bm.getAdvertisementSet(id).isProximityAware()) {
                return true;
            }
        }

        return false;
    }


    public String getIdentifier() {
        return identifier;
    }

    public String getAdvertisementKey() { return advertisementKey.toString(); }

    public List<String> getAuthorizedPeers() { return new ArrayList<>(authorizedPeers); }

    boolean removeAuthorization(String peerIdentifier) {
        return authorizedPeers.remove(peerIdentifier);
    }

    boolean addAuthorization(String peerIdentifier) {
        boolean b = authorizedPeers.contains(peerIdentifier);
        if (!PeerDatabase.getInstance().contains(peerIdentifier)) return false;
        if (!b) authorizedPeers.add(peerIdentifier);
        return !b;
    }

    public boolean isProximityAware() {
        return proximityAware;
    }

    public void setProximityAware(boolean proximityAware) {
        this.proximityAware = proximityAware;
        BeaconManager beaconManager = BeaconManager.tryGetInstance();
        if (beaconManager != null) {
            beaconManager.updateBroadcasts();
            beaconManager.save();
        }
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o.getClass() == this.getClass() && Objects.equals(((AdvertisementSet) o).identifier, this.identifier);
    }
}
