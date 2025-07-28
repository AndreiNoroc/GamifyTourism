package com.example.smarttourism;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity; import androidx.recyclerview.widget.LinearLayoutManager; import androidx.recyclerview.widget.RecyclerView;

import com.example.smarttourism.adapters.LeaderboardAdapter; import com.example.smarttourism.models.UserScore;

import org.json.JSONArray; import org.json.JSONObject;

import java.io.BufferedReader; import java.io.InputStreamReader; import java.net.HttpURLConnection; import java.net.URL; import java.util.ArrayList;

public class LeaderboardActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private LeaderboardAdapter adapter;
    private ArrayList<UserScore> userList = new ArrayList<>();
    private final String LEADERBOARD_URL = "https://gamifytourism-955657412481.europe-west9.run.app/leaderboard";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        recyclerView = findViewById(R.id.recycler_leaderboard);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new LeaderboardAdapter(userList);
        recyclerView.setAdapter(adapter);

        Button backButton = findViewById(R.id.btn_back);
        backButton.setOnClickListener(v -> finish());

        fetchLeaderboard();
    }

    private void fetchLeaderboard() {
        new Thread(() -> {
            try {
                URL url = new URL(LEADERBOARD_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader br = new BufferedReader(new InputStreamReader(
                        conn.getInputStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line.trim());
                }

                JSONArray array = new JSONArray(response.toString());
                userList.clear();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject user = array.getJSONObject(i);
                    String username = user.getString("username");
                    int score = user.optInt("score", 0);
                    userList.add(new UserScore(username, score));
                }

                runOnUiThread(() -> adapter.notifyDataSetChanged());

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error loading leaderboard: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}