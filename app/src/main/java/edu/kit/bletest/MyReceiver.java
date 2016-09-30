package edu.kit.bletest;

import android.content.Context;
import android.net.wifi.WifiConfiguration;

import java.util.ArrayList;
import java.util.List;

import edu.kit.privateadhocpeering.CallbackReceiver;
import edu.kit.privateadhocpeering.Peer;
import edu.kit.privateadhocpeering.PeerDatabase;

public class MyReceiver extends CallbackReceiver {

    private static List<StatusChangedListener> listeners = new ArrayList<>();

    @Override
    public void onProximityDetected(Context context, String peerId) {
        notifyListeners(peerId);
    }

    @Override
    public void onPeerLost(Context context, String peerId) {
        notifyListeners(peerId);
    }

    @Override
    public void onConnecting(Context context, String peerId) {
        notifyListeners(peerId);
    }

    @Override
    public void onConnected(Context context, String peerId) {
        notifyListeners(peerId);
    }

    @Override
    public void onAuthenticated(Context context, String peerId) {
        notifyListeners(peerId);
    }

    @Override
    public void onMessageReceived(Context context, String peerId) {
        Peer peer = PeerDatabase.getInstance().getPeer(peerId);
        byte[] msg = peer.getNextReceivedMessage();
        if (msg != null && msg.length > 0) {
            NewMessageNotification.notify(context, peer.getIdentifier(), new String(msg));
        }
    }

    public static void addListener(StatusChangedListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(StatusChangedListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(String peerId) {
        List<StatusChangedListener> copy = new ArrayList<>(listeners);
        Peer peer = PeerDatabase.getInstance().getPeer(peerId);
        for (StatusChangedListener listener : copy) {
            listener.onStatusChanged(peer);
        }
    }


    public interface StatusChangedListener {
        void onStatusChanged(Peer peer);
    }
}
