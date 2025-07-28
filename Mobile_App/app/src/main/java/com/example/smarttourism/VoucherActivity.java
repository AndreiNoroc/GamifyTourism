package com.example.smarttourism;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

public class VoucherActivity extends AppCompatActivity {

    private LinearLayout voucherContainer;

    private int userScore = 0;
    private RecyclerView recyclerView;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voucher);

        recyclerView = findViewById(R.id.recycler_vouchers);
        btnBack = findViewById(R.id.btn_back);
        voucherContainer = findViewById(R.id.voucher_container);


        btnBack.setOnClickListener(v -> finish());  // Inchide activitatea cand apesi


        fetchUserScore();
    }

    private void fetchUserScore() {
        new Thread(() -> {
            try {
                SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
                String username = prefs.getString("username", null);
                if (username == null) return;

                URL url = new URL("https://gamifytourism-955657412481.europe-west9.run.app/score?username=" + username);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder jsonBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jsonBuilder.append(line);
                    }
                    reader.close();
                    JSONObject obj = new JSONObject(jsonBuilder.toString());
                    userScore = obj.getInt("score");

                    runOnUiThread(this::fetchVouchers);
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void fetchVouchers() {
        new Thread(() -> {
            try {
                URL url = new URL("https://gamifytourism-955657412481.europe-west9.run.app/vouchers");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder jsonBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jsonBuilder.append(line);
                    }
                    reader.close();

                    JSONArray vouchers = new JSONArray(jsonBuilder.toString());

                    runOnUiThread(() -> {
                        for (int i = 0; i < vouchers.length(); i++) {
                            try {
                                JSONObject voucher = vouchers.getJSONObject(i);
                                String name = voucher.getString("description");
                                int points = voucher.getInt("points");

                                Button button = new Button(this);
                                button.setText(name + " (" + points + " points)");

                                if (userScore >= points) {
                                    button.setEnabled(true);
                                    button.setOnClickListener(v -> claimVoucher(name, points, button));
                                } else {
                                    button.setEnabled(false);
                                }

                                voucherContainer.addView(button);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void claimVoucher(String voucherName, int voucherPoints, Button voucherButton) {
        new Thread(() -> {
            try {
                SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
                String username = prefs.getString("username", null);
                if (username == null) return;

                URL url = new URL("https://gamifytourism-955657412481.europe-west9.run.app/claim-voucher");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("username", username);
                body.put("voucherName", voucherName); // 🔥 Trimitem numele voucherului

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = body.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    runOnUiThread(() -> {
                        showSuccessDialog(voucherName);

                        // 🔥 Dezactivăm butonul voucherului
                        voucherButton.setEnabled(false);
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Failed to claim voucher", Toast.LENGTH_SHORT).show());
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error claiming voucher", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void showSuccessDialog(String name) {
        new AlertDialog.Builder(this)
                .setTitle("Voucher Collected!")
                .setMessage("You successfully collected: " + name)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
