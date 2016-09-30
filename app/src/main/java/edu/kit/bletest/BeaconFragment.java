package edu.kit.bletest;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import edu.kit.bletest.dummy.Beacon;
import edu.kit.privateadhocpeering.AdvertisementSet;
import edu.kit.privateadhocpeering.BeaconManager;
import edu.kit.privateadhocpeering.Peer;
import edu.kit.privateadhocpeering.PeerDatabase;

/**
 * A fragment representing a list of Items.
 * <p/>
 */
public class BeaconFragment extends Fragment {

    private MyBeaconRecyclerViewAdapter recyclerViewAdapter;

    private MyReceiver.StatusChangedListener statusListener;

    private BeaconManager.StatusChangedListener beaconStatusListener;

    private BeaconListener listener = new BeaconListener() {
        @Override
        public void beaconSelected(String keyIdentifier) {
            Intent intent = new Intent(getContext(), GroupInfoActivity.class);
            intent.putExtra("id", keyIdentifier);
            startActivity(intent);
        }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public BeaconFragment() {
        statusListener = new MyReceiver.StatusChangedListener() {
            @Override
            public void onStatusChanged(Peer peer) {
                if (recyclerViewAdapter != null) {
                    recyclerViewAdapter.refreshItems(createBeacons());
                }
            }
        };

        beaconStatusListener = new BeaconManager.StatusChangedListener() {
            @Override
            public void onStatusChanged(String keyIdentifier, boolean active) {
                if (recyclerViewAdapter != null) {
                    recyclerViewAdapter.refreshItems(createBeacons());
                }
            }
        };
    }

    private List<Beacon> createBeacons() {
        List<Beacon> beacons = new ArrayList<>();
        BeaconManager beaconManager = BeaconManager.initInstance(getContext());
        for (AdvertisementSet set : beaconManager.getAdvertisementSets()) {
            Beacon.BeaconStatus status = beaconManager.isBroadcasting(set.getIdentifier()) ?
                    Beacon.BeaconStatus.ACTIVE : Beacon.BeaconStatus.INACTIVE;
            beacons.add(new Beacon(set.getIdentifier(), status));
        }

        return beacons;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (recyclerViewAdapter != null) {
            recyclerViewAdapter.refreshItems(createBeacons());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyReceiver.addListener(statusListener);
        BeaconManager.initInstance(getContext()).addListener(beaconStatusListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MyReceiver.removeListener(statusListener);
        BeaconManager.initInstance(getContext()).removeListener(beaconStatusListener);
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

            recyclerViewAdapter = new MyBeaconRecyclerViewAdapter(createBeacons(), listener);
            recyclerView.setAdapter(recyclerViewAdapter);
        }
        return view;
    }

    public interface BeaconListener {
        void beaconSelected(String keyIdentifier);
    }


}
