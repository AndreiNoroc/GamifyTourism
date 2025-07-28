package com.example.smarttourism;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText, passwordEditText;
    private final String LOGIN_URL = "https://gamifytourism-955657412481.europe-west9.run.app/login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameEditText = findViewById(R.id.edit_username);
        passwordEditText = findViewById(R.id.edit_password);
        Button loginButton = findViewById(R.id.btn_login);
        Button backButton = findViewById(R.id.btn_back);

        loginButton.setOnClickListener(view -> loginUser());
        backButton.setOnClickListener(v -> finish());
    }

    private void loginUser() {
        new Thread(() -> {
            try {
                URL url = new URL(LOGIN_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                JSONObject jsonInput = new JSONObject();
                jsonInput.put("username", usernameEditText.getText().toString().trim());
                jsonInput.put("password", passwordEditText.getText().toString().trim());

                try (OutputStream os = new BufferedOutputStream(conn.getOutputStream())) {
                    os.write(jsonInput.toString().getBytes("UTF-8"));
                    os.flush();
                }

                int responseCode = conn.getResponseCode();

                BufferedReader br = new BufferedReader(new InputStreamReader(
                        responseCode < 400 ? conn.getInputStream() : conn.getErrorStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line.trim());
                }
                br.close();

                runOnUiThread(() -> {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
                        prefs.edit().putString("username", usernameEditText.getText().toString()).apply();

                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Invalid credentials: " + response.toString(), Toast.LENGTH_LONG).show();
                    }
                });

                conn.disconnect();

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}