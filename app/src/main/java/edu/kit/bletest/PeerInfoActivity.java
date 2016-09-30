package edu.kit.bletest;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.Arrays;
import java.util.List;

import edu.kit.privateadhocpeering.AdvertisementSet;
import edu.kit.privateadhocpeering.BeaconManager;
import edu.kit.privateadhocpeering.Peer;
import edu.kit.privateadhocpeering.PeerDatabase;
import edu.kit.privateadhocpeering.PeerStatus;

public class PeerInfoActivity extends AppCompatActivity {

    private TextView identifierView;
    private TextView statusView;
    private ImageView qrView;
    private EditText messageInputView;
    private Button sendButton;
    private String id;
    private MyReceiver.StatusChangedListener statusListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peer_info);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Peer Information");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        identifierView = (TextView) findViewById(R.id.textView);

        statusView = (TextView) findViewById(R.id.textView2);

        qrView = (ImageView) findViewById(R.id.imageView);

        messageInputView = (EditText) findViewById(R.id.editText2);

        sendButton = (Button) findViewById(R.id.button);

        id = getIntent().getStringExtra("id");

        if (id == null) {
            finish();
            return;
        }

        final Peer peer = PeerDatabase.getInstance().getPeer(id);
        final String advertisingKey = BeaconManager.initInstance(this).getAdvertisementSet(peer.getAdvertisementSets().get(0)).getAdvertisementKey();


        String text = id;
        identifierView.setText(text);


        statusView.setText(peer.getStatus().toString());
        statusView.setTextColor(getStatusColor(peer.getStatus()));

        statusListener = new MyReceiver.StatusChangedListener() {
            @Override
            public void onStatusChanged(Peer peer) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Peer peer = PeerDatabase.getInstance().getPeer(id);
                        if (peer == null) return;
                        statusView.setText(peer.getStatus().toString());
                        statusView.setTextColor(getStatusColor(peer.getStatus()));
                    }
                });
            }
        };

        MyReceiver.addListener(statusListener);

        qrView.post(new Runnable() {
            @Override
            public void run() {
                String contents = "Peer<b>" + peer.getAuthenticationKey() + "<b>" + advertisingKey;
                int width = qrView.getWidth() * 3 / 4;
                QRCodeWriter qrCodeWriter = new QRCodeWriter();
                BitMatrix bitMatrix = new BitMatrix(width, width);
                try {
                    bitMatrix = qrCodeWriter.encode(contents, BarcodeFormat.QR_CODE, width,
                            width);
                } catch (WriterException e) {
                    e.printStackTrace();
                    finish();
                }


                int[] pixels = new int[width * width];
                for (int y = 0; y < width; y++)
                {
                    int offset = y * width;
                    for (int x = 0; x < width; x++)
                    {
                        pixels[offset + x] = bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
                    }
                }

                Bitmap bitmap = Bitmap.createBitmap(width, width, Bitmap.Config.RGB_565);
                bitmap.setPixels(pixels, 0, width, 0, 0, width, width);

                qrView.setImageBitmap(bitmap);
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = messageInputView.getText().toString();
                if (msg == null || msg.length() == 0) return;

                peer.sendData(getApplicationContext(), msg.getBytes());

                messageInputView.setText("");
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyReceiver.removeListener(statusListener);
    }

    private int getStatusColor(PeerStatus status) {
        switch (status) {
            case AUTHENTICATED:
                return Color.BLUE;
            case CONNECTED:
                return Color.GREEN;
            case DISCOVERED:
                return Color.DKGRAY;
            case OUT_OF_RANGE:
                return Color.RED;
        }

        return Color.RED;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_delete, menu);

        menu.findItem(R.id.action_add_advertisement).setVisible(true);
        menu.findItem(R.id.action_add_scanning).setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final int itemId = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (itemId == R.id.action_delete) {
            PeerDatabase.getInstance().removePeer(this.id);
            finish();
            return true;
        } else if (itemId == R.id.action_add_advertisement) {
            List< AdvertisementSet > strings = BeaconManager.initInstance(this).getAdvertisementSets();
            final CharSequence options[] = new CharSequence[strings.size()];
            int i = 0;
            for (AdvertisementSet set : strings) {
                options[i++] = set.getIdentifier();
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Which group does this peer belong to?");
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String setId = options[which].toString();
                    PeerDatabase.getInstance().getPeer(id).addAdvertisementSet(setId);
                }
            });
            builder.show();
        } else if (itemId == R.id.action_add_scanning) {
            scan();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Log.d("MainActivity", "Cancelled scan");
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                Log.d("MainActivity", "Scanned");
                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();

                // TODO: cleanup callback hell
                final String[] strs = result.getContents().split("<b>");
                if (strs[0].equals("Set")) {
                    final String scanKey = strs[1];

                    Peer peer = PeerDatabase.getInstance().getPeer(id);
                    peer.addDiscoveryKey(scanKey);
                } else {
                    Log.d("PeerInfoActivity", "Error. False QR Code.");
                    Toast.makeText(this, "Error. False QR Code.", Toast.LENGTH_LONG).show();
                }
            }
        } else {
            // This is important, otherwise the result will not be passed to the fragment
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void scan() {
        IntentIntegrator ii = new IntentIntegrator(this);
        ii.setPrompt("Scan a trusted friend's QR code!");
        ii.setOrientationLocked(false);
        ii.initiateScan();
    }

}
