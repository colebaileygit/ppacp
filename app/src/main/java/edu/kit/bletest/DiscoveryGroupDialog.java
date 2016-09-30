package edu.kit.bletest;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.widget.CheckBox;
import android.widget.EditText;

import edu.kit.privateadhocpeering.BeaconManager;
import edu.kit.privateadhocpeering.PeerDatabase;

/**
 * Created by Me on 5/17/2016.
 */
public class DiscoveryGroupDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final BeaconManager beaconManager = BeaconManager.initInstance(getActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Enter new identifiers");
        builder.setView(R.layout.popup_content);
        builder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                EditText setId = (EditText) ((AlertDialog) dialog).findViewById(R.id.editText3);
                EditText peerId = (EditText) ((AlertDialog) dialog).findViewById(R.id.editText4);
                CheckBox checkBox = (CheckBox) ((AlertDialog) dialog).findViewById(R.id.checkBox3);

                String sId = setId.getText().toString();
                String pId = peerId.getText().toString();

                // TODO: merge addAuth and addPeer
                beaconManager.addAdvertisementSet(sId, checkBox.isChecked());

                if (PeerDatabase.getInstance().contains(pId)) {
                    PeerDatabase.getInstance().getPeer(pId).addAdvertisementSet(sId);
                    PeerDatabase.getInstance().getPeer(pId).addDiscoveryKey(getArguments().getString("scanKey"));
                } else {
                    PeerDatabase.getInstance().addPeer(pId, sId, getArguments().getString("scanKey"));
                }

                Intent intent = new Intent(getActivity(), PeerInfoActivity.class);
                intent.putExtra("id", pId);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("Cancel", null);
        return builder.create();
    }
}
