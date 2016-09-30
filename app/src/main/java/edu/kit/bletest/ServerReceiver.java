package edu.kit.bletest;

import android.content.Context;

import edu.kit.privateadhocpeering.CallbackReceiver;
import edu.kit.privateadhocpeering.Peer;
import edu.kit.privateadhocpeering.PeerDatabase;

/**
 * Created by Me on 6/15/2016.
 */
public class ServerReceiver extends CallbackReceiver {
    @Override
    public void onProximityDetected(Context context, String peerId) {

    }

    @Override
    public void onPeerLost(Context context, String peerId) {

    }

    @Override
    public void onConnecting(Context context, String peerId) {

    }

    @Override
    public void onConnected(Context context, String peerId) {

    }

    @Override
    public void onAuthenticated(Context context, String peerId) {
        Peer peer = PeerDatabase.getInstance().getPeer(peerId);
        peer.sendData(context, "Test message. Hello.".getBytes());
    }

    @Override
    public void onMessageReceived(Context context, String peerId) {

    }
}
