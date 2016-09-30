package edu.kit.bletest;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.List;

import edu.kit.privateadhocpeering.AdvertisementSet;
import edu.kit.privateadhocpeering.BeaconManager;
import edu.kit.privateadhocpeering.CallbackReceiver;
import edu.kit.privateadhocpeering.DiscoveryService;
import edu.kit.privateadhocpeering.Peer;
import edu.kit.privateadhocpeering.PeerDatabase;

public class MainActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private Intent serviceIntent;
    private Intent testingIntent;

    private CallbackReceiver myReceiver;

    public static final int REQUEST_ENABLE_BT = 97;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        serviceIntent = new Intent(this, DiscoveryService.class);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, 3);
        } else {
            startService(serviceIntent);
        }

        myReceiver = new MyReceiver();
        myReceiver.register(this);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        assert mViewPager != null;
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        assert tabLayout != null;
        tabLayout.setupWithViewPager(mViewPager);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;

        final AppCompatActivity activity = this;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final CharSequence options[] = new CharSequence[] {"Discovery Group", "Peer" };

                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setTitle("Create a new..");
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            startActivity(new Intent(getApplicationContext(), NewGroupActivity.class));
                        } else {
                            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                                    != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(activity,
                                        new String[]{Manifest.permission.CAMERA}, 1);
                            } else {
                                scan();
                            }

//                            Intent intent = new Intent(getApplicationContext(), PeerInfoActivity.class);
//                            intent.putExtra("id", )
                        }
                    }
                });
                builder.show();
            }
        });
    }

    private void scan() {
        IntentIntegrator ii = new IntentIntegrator(this);
        ii.setPrompt("Scan a trusted friend's QR code!");
        ii.setOrientationLocked(false);
        ii.initiateScan();
    }

    @Override
    protected void onDestroy() {
        stopService(serviceIntent);
        myReceiver.unregister(this);
        if (testingIntent != null) stopService(testingIntent);

        super.onDestroy();
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
                if (strs[0].equals("Peer")) {
                    PeerDialog p = new PeerDialog();
                    Bundle b = new Bundle();
                    b.putString("authenticationKey", strs[1]);
                    b.putString("discoveryKey", strs[2]);
                    p.setArguments(b);
                    p.show(getFragmentManager(), "peer");


                } else if (strs[0].equals("Set")) {
                    final String scanKey = strs[1];

                    DiscoveryGroupDialog f = new DiscoveryGroupDialog();
                    Bundle b = new Bundle();
                    b.putString("scanKey", scanKey);
                    f.setArguments(b);
                    f.show(getFragmentManager(), "set");
                }
            }
        } else {
            // This is important, otherwise the result will not be passed to the fragment
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scan();
            }
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, CompatibilityActivity.class));
            return true;
        } else if (id == R.id.action_testmode) {
            final CharSequence options[] = new CharSequence[] {"Alice", "Bob", "Carl" };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Pick a user");
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    PeerDatabase.getInstance().initTestMode(getApplicationContext(), options[which].toString());
                }
            });
            builder.show();
        } else if (id == R.id.action_energyuse) {
            myReceiver.unregister(this);
            myReceiver = new ServerReceiver();
            myReceiver.register(this);
            for (AdvertisementSet set : BeaconManager.initInstance(this).getAdvertisementSets()) {
                set.setProximityAware(true);
            }
            serviceIntent.setAction(DiscoveryService.LOG_BATTERY_LEVELS);
            startService(serviceIntent);
            requestWritePermission();
        } else if (id == R.id.action_latency) {
            startLatencyTesting();
            requestWritePermission();
        } else if (id == R.id.action_save) {
            requestWritePermission();
            if (testingIntent != null) {
                stopService(testingIntent);
                startService(testingIntent);
            } else {
                stopService(serviceIntent);
                startService(serviceIntent);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void requestWritePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 16);
        }
    }


    private void startLatencyTesting() {
        stopService(serviceIntent);
        myReceiver.unregister(this);

        testingIntent = new Intent(this, TestingService.class);
        startService(testingIntent);
    }





    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new PeerFragment();
                case 1:
                    return new BeaconFragment();
            }

            return new PeerFragment();
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "PEERS";
                case 1:
                    return "BEACONS";
                case 2:
                    return "SERVICES";
            }
            return null;
        }
    }
}
