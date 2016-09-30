package edu.kit.bletest;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.widget.EditText;

import java.util.List;

import edu.kit.privateadhocpeering.AdvertisementSet;
import edu.kit.privateadhocpeering.BeaconManager;
import edu.kit.privateadhocpeering.PeerDatabase;

public class PeerDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final EditText edit = new EditText(getActivity());

        final BeaconManager beaconManager = BeaconManager.initInstance(getActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Enter unique peer id");
        builder.setView(edit);
        builder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String pId = edit.getText().toString();

                List< AdvertisementSet > strings = beaconManager.getAdvertisementSets();
                final CharSequence options[] = new CharSequence[strings.size()];
                int i = 0;
                for (AdvertisementSet set : strings) {
                    options[i++] = set.getIdentifier();
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Which group does this peer belong to?");
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String id = options[which].toString();
                        PeerDatabase.getInstance().addPeer(pId, id, getArguments().getString("discoveryKey"), getArguments().getString("authenticationKey"));
                    }
                });
                builder.show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        return builder.create();
    }
}
