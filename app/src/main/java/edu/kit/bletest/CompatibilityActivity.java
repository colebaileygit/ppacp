package edu.kit.bletest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.nio.charset.Charset;
import java.util.UUID;

import edu.kit.privateadhocpeering.AuthenticationGATT;

public class CompatibilityActivity extends AppCompatActivity {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    TextView permissions;
    TextView bleCapable;
    TextView blEnabled;
    TextView beaconCapable;
    TextView scanTested;
    TextView beaconTested;
    TextView secondBeaconTested;

    public CompatibilityActivity() {
    }

    public void setBlActivated(boolean activated) {
        if (!activated) {
            blEnabled.setText(R.string.bluetoothNotEnabled);
            blEnabled.setTextColor(Color.RED);
        } else {
            blEnabled.setText(R.string.bluetoothEnabled);
            blEnabled.setTextColor(Color.GREEN);
            testBluetoothFunctions();
        }
    }

    private void testBluetoothFunctions() {

        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bluetoothAdapter.getBluetoothLeScanner().startScan(new ScanCallback() {
                    @Override
                    public void onScanFailed(int errorCode) {
                        scanTested.setText(R.string.bleScanFail);
                        scanTested.setTextColor(Color.RED);
                    }
                    @SuppressLint("NewApi")
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        scanTested.setText(R.string.bleScanSuccess);
                        scanTested.setTextColor(Color.GREEN);
                        bluetoothAdapter.getBluetoothLeScanner().stopScan(this);
                    }

                });

                scanTested.setText(R.string.bleScanSuccess);
                scanTested.setTextColor(Color.GREEN);
                //           bluetoothAdapter.getBluetoothLeScanner().stopScan(null);
            } else {
                bluetoothAdapter.startLeScan(new BluetoothAdapter.LeScanCallback() {
                    @Override
                    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                        scanTested.setText(R.string.bleScanSuccess);
                        scanTested.setTextColor(Color.GREEN);
                        bluetoothAdapter.stopLeScan(this);
                    }

                });

                scanTested.setText(R.string.bleScanSuccess);
                scanTested.setTextColor(Color.GREEN);
                bluetoothAdapter.stopLeScan(null);
            }
        } catch (Exception e) {
            scanTested.setText(R.string.bleScanFail);
            scanTested.setTextColor(Color.RED);
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AdvertiseSettings settings = new AdvertiseSettings.Builder()
                        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                        .setConnectable(false)
                        .setTimeout(5000)
                        .build();
                ParcelUuid pUuid = new ParcelUuid( UUID.fromString( "CDB7950D-73F1-4D4D-8E47-C090502DBD63" ) );
                AdvertiseData data = new AdvertiseData.Builder()
                        .setIncludeDeviceName( false )
                        .addServiceUuid( pUuid )
                        .addServiceData( pUuid, "Data".getBytes( Charset.forName( "UTF-8" ) ) )
                        .build();
                AdvertiseCallback callback = new AdvertiseCallback() {
                    @Override
                    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                        beaconTested.setText(R.string.beaconBroadcastSuccess);
                        beaconTested.setTextColor(Color.GREEN);
                        broadcastSecondBeacon(bluetoothAdapter);
                    }

                    @Override
                    public void onStartFailure(int errorCode) {
                        if (beaconTested.getCurrentTextColor() != Color.GREEN) {
                            beaconTested.setText(R.string.beaconBroadcastFailed);
                            beaconTested.setTextColor(Color.RED);
                        }
                    }
                };
                bluetoothAdapter.getBluetoothLeAdvertiser().stopAdvertising(callback);
                bluetoothAdapter.getBluetoothLeAdvertiser().startAdvertising(settings, data, callback);
            } else {
                beaconTested.setText(R.string.beaconBroadcastFailed);
                beaconTested.setTextColor(Color.RED);
            }
        } catch (Exception e) {
            beaconTested.setText(R.string.beaconBroadcastFailed);
            beaconTested.setTextColor(Color.RED);
        }
    }

    private void broadcastSecondBeacon(BluetoothAdapter bluetoothAdapter) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AdvertiseSettings settings = new AdvertiseSettings.Builder()
                        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                        .setConnectable(false)
                        .setTimeout(5000)
                        .build();
                ParcelUuid pUuid = new ParcelUuid( UUID.fromString( "0000FEAA-0000-1000-8000-00805F9B34FB" ) );
                AdvertiseData data = new AdvertiseData.Builder()
                        .setIncludeDeviceName( false )
                        .setIncludeTxPowerLevel(false)
                        .addServiceUuid( pUuid )
                        .addServiceData( pUuid, "Data".getBytes( Charset.forName( "UTF-8" ) ) )
                        .build();
                AdvertiseCallback callback = new AdvertiseCallback() {
                    @Override
                    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                        secondBeaconTested.setText(R.string.secondaryBeaconSuccess);
                        secondBeaconTested.setTextColor(Color.GREEN);
                    }

                    @Override
                    public void onStartFailure(int errorCode) {
                        if (secondBeaconTested.getCurrentTextColor() != Color.GREEN) {
                            secondBeaconTested.setText(R.string.secondaryBeaconFail);
                            secondBeaconTested.setTextColor(Color.RED);
                        }
                    }
                };
                bluetoothAdapter.getBluetoothLeAdvertiser().stopAdvertising(callback);
                bluetoothAdapter.getBluetoothLeAdvertiser().startAdvertising(settings, data, callback);
            } else {
                secondBeaconTested.setText(R.string.secondaryBeaconFail);
                secondBeaconTested.setTextColor(Color.RED);
            }
        } catch (Exception e) {
            secondBeaconTested.setText(R.string.secondaryBeaconFail);
            secondBeaconTested.setTextColor(Color.RED);
        }
    }

    public void setBlPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.setText(R.string.permissionsNotGranted);
            permissions.setTextColor(Color.RED);
        } else {
            permissions.setText(R.string.permissionsGranted);
            permissions.setTextColor(Color.GREEN);
            checkBluetoothEnabled();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 65633:
            case MainActivity.REQUEST_ENABLE_BT:
                setBlActivated(resultCode == RESULT_OK);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        setBlPermissions();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_main);
        permissions = (TextView) findViewById(R.id.permissionText);
        bleCapable = (TextView) findViewById(R.id.bleCapableText);
        blEnabled = (TextView) findViewById(R.id.bleEnabledText);
        beaconCapable = (TextView) findViewById(R.id.beaconCapableText);
        scanTested = (TextView) findViewById(R.id.scanTestText);
        beaconTested = (TextView) findViewById(R.id.beaconTestText);
        secondBeaconTested = (TextView) findViewById(R.id.secondBeaconTestText);

        setBlPermissions();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            bleCapable.setText(R.string.bleSupported);
            bleCapable.setTextColor(Color.RED);
        } else {
            bleCapable.setText(R.string.bleNotSupported);
            bleCapable.setTextColor(Color.GREEN);
        }
    }

    private void checkBluetoothEnabled() {
        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter != null) setBlActivated(bluetoothAdapter.isEnabled());
        // Ensures Bluetooth is available on the device and it is enabled. If not,
// displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, MainActivity.REQUEST_ENABLE_BT);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && bluetoothAdapter != null
                && bluetoothAdapter.isMultipleAdvertisementSupported()) {
            beaconCapable.setText(R.string.bleBeaconSupported);
            beaconCapable.setTextColor(Color.GREEN);
        } else {
            beaconCapable.setText(R.string.bleBeaconNotSupported);
            beaconCapable.setTextColor(Color.RED);
        }

    }

}
