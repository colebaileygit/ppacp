package edu.kit.privateadhocpeering;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

public abstract class CallbackReceiver extends BroadcastReceiver {

    static final String KEY_IDENTIFIER = "edu.kit.key.id";
    static final String NEW_MESSAGE = "edu.kit.peer.newmessage";
    static final String PROXIMITY_NEAR = "edu.kit.peer.nearby";
    static final String PROXIMITY_LOST = "edu.kit.peer.lost";
    static final String CONNECTING = "edu.kit.peer.connecting";
    static final String CONNECTED = "edu.kit.peer.connected";
    static final String AUTHENTICATED = "edu.kit.peer.authenticated";

    public void register(Context context) {
        LocalBroadcastManager.getInstance(context).registerReceiver(this, new IntentFilter(NEW_MESSAGE));
        LocalBroadcastManager.getInstance(context).registerReceiver(this, new IntentFilter(PROXIMITY_NEAR));
        LocalBroadcastManager.getInstance(context).registerReceiver(this, new IntentFilter(PROXIMITY_LOST));
        LocalBroadcastManager.getInstance(context).registerReceiver(this, new IntentFilter(CONNECTING));
        LocalBroadcastManager.getInstance(context).registerReceiver(this, new IntentFilter(CONNECTED));
        LocalBroadcastManager.getInstance(context).registerReceiver(this, new IntentFilter(AUTHENTICATED));

    }

    public void unregister(Context context) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case NEW_MESSAGE:
                onMessageReceived(context, intent.getStringExtra(KEY_IDENTIFIER));
                break;
            case PROXIMITY_LOST:
                onPeerLost(context, intent.getStringExtra(KEY_IDENTIFIER));
                break;
            case PROXIMITY_NEAR:
                onProximityDetected(context, intent.getStringExtra(KEY_IDENTIFIER));
                break;
            case CONNECTING:
                onConnecting(context, intent.getStringExtra(KEY_IDENTIFIER));
                break;
            case CONNECTED:
                onConnected(context, intent.getStringExtra(KEY_IDENTIFIER));
                break;
            case AUTHENTICATED:
                onAuthenticated(context, intent.getStringExtra(KEY_IDENTIFIER));
                break;
        }
    }

    public abstract void onProximityDetected(Context context, String peerId);

    public abstract void onPeerLost(Context context, String peerId);

    public abstract void onConnecting(Context context, String peerId);

    public abstract void onConnected(Context context, String peerId);

    public abstract void onAuthenticated(Context context, String peerId);

    public abstract void onMessageReceived(Context context, String peerId);
}
