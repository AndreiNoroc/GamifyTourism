package com.example.smarttourism;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class VisitedLocationsActivity extends AppCompatActivity {

    private RecyclerView visitedLocationsRecyclerView;
    private static final String ENDPOINT_URL = "https://gamifytourism-955657412481.europe-west9.run.app/get-visited-locations";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visited_locations);

        visitedLocationsRecyclerView = findViewById(R.id.visited_locations_recyclerview);
        visitedLocationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        Button backButton = findViewById(R.id.btn_back);
        backButton.setOnClickListener(v -> finish());

        fetchVisitedLocations();
    }

    private void fetchVisitedLocations() {
        SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String username = prefs.getString("username", null);

        if (username == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        AsyncTask.execute(() -> {
            try {
                URL url = new URL(ENDPOINT_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String jsonInputString = "{\"username\":\"" + username + "\"}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONObject responseJson = new JSONObject(response.toString());
                    JSONArray locationsArray = responseJson.getJSONArray("visitedLocations");

                    ArrayList<String> locationsList = new ArrayList<>();
                    for (int i = 0; i < locationsArray.length(); i++) {
                        locationsList.add(locationsArray.getString(i));
                    }

                    runOnUiThread(() -> {
                        com.example.smarttourism.VisitedLocationAdapter adapter = new com.example.smarttourism.VisitedLocationAdapter(locationsList);
                        visitedLocationsRecyclerView.setAdapter(adapter);
                    });

                    if (!locationsList.isEmpty()) {
                        String lastVisited = locationsList.get(locationsList.size() - 1);

                        // Pregătim rezultat pentru MapActivity
                        Intent intent = new Intent("com.example.smarttourism.LOCATION_VISITED");
                        intent.putExtra("last_visited_location", lastVisited);
                        sendBroadcast(intent);
                    }

                } else {
                    Log.e("VisitedLocations", "Server returned non-OK status: " + responseCode);
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e("VisitedLocations", "Error fetching visited locations", e);
            }
        });
    }
}
