package com.example.smarttourism;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameEditText, passwordEditText;
    private final String REGISTER_URL = "https://gamifytourism-955657412481.europe-west9.run.app/register";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        usernameEditText = findViewById(R.id.edit_username);
        passwordEditText = findViewById(R.id.edit_password);
        Button registerButton = findViewById(R.id.btn_register);
        Button backButton = findViewById(R.id.btn_back);

        registerButton.setOnClickListener(view -> registerUser());
        backButton.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        new Thread(() -> {
            try {
                URL url = new URL(REGISTER_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                JSONObject jsonInput = new JSONObject();
                jsonInput.put("username", usernameEditText.getText().toString().trim());
                jsonInput.put("password", passwordEditText.getText().toString().trim());

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInput.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();

                BufferedReader br = new BufferedReader(new InputStreamReader(
                        responseCode < 400 ? conn.getInputStream() : conn.getErrorStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line.trim());
                }

                runOnUiThread(() -> {
                    if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                        Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Registration failed: " + response.toString(), Toast.LENGTH_LONG).show();
                    }
                });

                conn.disconnect();

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
}