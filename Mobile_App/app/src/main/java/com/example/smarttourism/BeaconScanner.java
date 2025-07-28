package com.example.smarttourism;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.content.SharedPreferences;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.UUID;

public class BeaconScanner extends AppCompatActivity {

    private static final String TAG = "BeaconScanner";
    private BluetoothLeScanner bluetoothLeScanner;
    private final Context context;

    private static final UUID BEACON_UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef0");

    public BeaconScanner(Context context) {
        this.context = context;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter != null) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            ScanRecord scanRecord = result.getScanRecord();
            if (scanRecord != null && advertisementHasExpectedUUID(scanRecord)) {
                Log.d(TAG, "✅ Expected beacon detected!");
                if (bluetoothLeScanner != null) {
                    bluetoothLeScanner.stopScan(this);
                }
                updateUserScore();
            }
        }
    };

    public void startScanning() {
//        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
//            Log.e(TAG, "Bluetooth scan permission not granted!");
//            return;
//        }

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        bluetoothLeScanner.startScan(null, scanSettings, scanCallback);
        Log.d(TAG, "Started BLE scanning...");
    }

    private boolean advertisementHasExpectedUUID(ScanRecord scanRecord) {
        byte[] manufacturerData = scanRecord.getManufacturerSpecificData(0x004C); // Apple ID
        if (manufacturerData == null || manufacturerData.length < 23) {
            return false;
        }
        byte[] uuidBytes = new byte[16];
        System.arraycopy(manufacturerData, 2, uuidBytes, 0, 16);
        UUID detectedUuid = bytesToUuid(uuidBytes);
        return BEACON_UUID.equals(detectedUuid);
    }

    private UUID bytesToUuid(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long high = buffer.getLong();
        long low = buffer.getLong();
        return new UUID(high, low);
    }

    private void updateUserScore() {
        AsyncTask.execute(() -> {
            try {
                URL url = new URL("https://gamifytourism-955657412481.europe-west9.run.app/visit-location");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                String loggedUser = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
                        .getString("username", "guest");

                Log.i("USERNAME", loggedUser);
                String jsonInputString = "{\"username\":\"" + loggedUser + "\",\"locationName\":\"" + "Facultatea de Automatica si Calculatoare" + "\"}";

                OutputStream os = conn.getOutputStream();
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "✅ Score collected successfully!");

                    // 🔥 TRIMITEM BROADCAST imediat după ce colectăm scorul
                    Intent intent = new Intent("com.example.smarttourism.SCORE_COLLECTED");
                    intent.putExtra("visited_location_name", "Facultatea de Automatica si Calculatoare"); // exemplu
                    context.sendBroadcast(intent);
                } else {
                    Log.e(TAG, "Server returned non-OK status: " + code);
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "❌ Error sending data to server", e);
            }
        });
        Log.d(TAG, "🏆 User score updated!");
    }
}
