package edu.kit.bletest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import edu.kit.privateadhocpeering.DiscoveryService;
import edu.kit.privateadhocpeering.Peer;
import edu.kit.privateadhocpeering.PeerDatabase;
import edu.kit.privateadhocpeering.PeerStatus;

/**
 * A fragment representing a list of Items.
 * <p/>
 */
public class PeerFragment extends Fragment {

    private MyPeerRecyclerViewAdapter recyclerViewAdapter;

    private MyReceiver.StatusChangedListener listener;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PeerFragment() {
        listener = new MyReceiver.StatusChangedListener() {
            @Override
            public void onStatusChanged(Peer peer) {
                if (recyclerViewAdapter != null) {
                    recyclerViewAdapter.refreshItems(PeerDatabase.getInstance().getPeers());
                }
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        if (recyclerViewAdapter != null) {
            recyclerViewAdapter.refreshItems(PeerDatabase.getInstance().getPeers());
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyReceiver.addListener(listener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MyReceiver.removeListener(listener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_peer_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;

            recyclerView.setLayoutManager(new LinearLayoutManager(context));

            recyclerViewAdapter = new MyPeerRecyclerViewAdapter(getContext(), PeerDatabase.getInstance().getPeers());
            recyclerView.setAdapter(recyclerViewAdapter);
        }
        return view;
    }


}
