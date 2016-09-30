package edu.kit.bletest;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.Arrays;

import edu.kit.privateadhocpeering.AdvertisementSet;
import edu.kit.privateadhocpeering.BeaconManager;
import edu.kit.privateadhocpeering.PeerDatabase;

public class GroupInfoActivity extends AppCompatActivity {

    private TextView identifierView;
    private TextView statusView;
    private ImageView qrView;
    private CheckBox proximityAwareView;
    private TextView peersView;

    private String id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Discovery Group");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        identifierView = (TextView) findViewById(R.id.textView3);

        statusView = (TextView) findViewById(R.id.textView4);

        qrView = (ImageView) findViewById(R.id.imageView2);

        proximityAwareView = (CheckBox) findViewById(R.id.checkBox2);

        peersView = (TextView) findViewById(R.id.textView5);

        id = getIntent().getStringExtra("id");

        if (id == null || id.length() == 0) {
            finish();
            return;
        }

        final BeaconManager beaconManager = BeaconManager.initInstance(this);
        final AdvertisementSet set = beaconManager.getAdvertisementSet(id);

        identifierView.setText(id);

        updateStatus(set);
        beaconManager.addListener(new BeaconManager.StatusChangedListener() {
            @Override
            public void onStatusChanged(String keyIdentifier, boolean active) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateStatus(beaconManager.getAdvertisementSet(id));
                    }
                });
            }
        });


        qrView.post(new Runnable() {
            @Override
            public void run() {
                String contents = "Set<b>" + set.getAdvertisementKey();
                int size = qrView.getWidth() * 3 / 4;
                QRCodeWriter qrCodeWriter = new QRCodeWriter();
                BitMatrix bitMatrix = new BitMatrix(size, size);
                try {
                    bitMatrix = qrCodeWriter.encode(contents, BarcodeFormat.QR_CODE, size,
                            size);
                } catch (WriterException e) {
                    e.printStackTrace();
                    finish();
                }


                int[] pixels = new int[size * size];
                for (int y = 0; y < size; y++)
                {
                    int offset = y * size;
                    for (int x = 0; x < size; x++)
                    {
                        pixels[offset + x] = bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
                    }
                }

                Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
                bitmap.setPixels(pixels, 0, size, 0, 0, size, size);

                qrView.setImageBitmap(bitmap);
            }
        });

        proximityAwareView.setChecked(set.isProximityAware());
        proximityAwareView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                set.setProximityAware(isChecked);
            }
        });

        String peers = Arrays.toString(set.advertisementKey.getAdvertisingData()) + "\nAuthorized Peers:";
        for (String s : set.getAuthorizedPeers()) {
            peers += "\n" + s;
        }

        peersView.setText(peers);
    }

    private void updateStatus(AdvertisementSet set) {
        String text = BeaconManager.initInstance(getApplicationContext()).isBroadcasting(set.getIdentifier()) ?
                "ACTIVE" : "INACTIVE";
        int color = (set.isActive()) ? Color.GREEN : Color.RED;
        statusView.setText(text);
        statusView.setTextColor(color);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_delete, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_delete) {
            BeaconManager.initInstance(this).removeAdvertisementSet(this.id);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
