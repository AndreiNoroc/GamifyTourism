package com.example.smarttourism;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private TextView textScore;
    private TextView textLoggedIn;
    private LinearLayout authOptions;
    private FloatingActionButton fabLeaderboard, fabLogin, fabRegister, fabLogout, fabAuth, fabVoucher;
    private List<Marker> markerList = new ArrayList<>();
    BeaconScanner beaconScanner;
    private FloatingActionButton fabVisitedLocations;
    private Handler recommendationHandler = new Handler();
    private Runnable recommendationRunnable;
    private static final long RECOMMENDATION_INTERVAL = 1 * 30 * 1000; // 5 minute în milisecunde
    private static final String RECOMMENDATION_URL = "https://gamifytourism-955657412481.europe-west9.run.app/recommend-location";
    private List<String> visitedLocationsList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        beaconScanner = new BeaconScanner(this);

        SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String loggedUser = prefs.getString("username", null);

        if (loggedUser != null) {
            // Rulează când vrei să începi scanarea (ex: onCreate sau pe un buton)
            beaconScanner.startScanning();
        }

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        textLoggedIn = findViewById(R.id.text_logged_in);
        textScore = findViewById(R.id.text_score);

        fabLeaderboard = findViewById(R.id.fab_leaderboard);
        fabVisitedLocations = findViewById(R.id.fab_visited_locations);
        fabLogin = findViewById(R.id.fab_login);
        fabRegister = findViewById(R.id.fab_register);
        fabLogout = findViewById(R.id.fab_logout);
        authOptions = findViewById(R.id.auth_options);
        fabAuth = findViewById(R.id.fab_auth_main);
        fabVoucher = findViewById(R.id.fab_voucher);
        LinearLayout authOptions = findViewById(R.id.auth_options);

        // Toggle auth buttons
        fabAuth.setOnClickListener(v -> {
            if (authOptions.getVisibility() == View.VISIBLE) {
                authOptions.setVisibility(View.GONE);
            } else {
                authOptions.setVisibility(View.VISIBLE);
            }
        });

        FloatingActionButton fabVoucher = findViewById(R.id.fab_voucher);
        fabVoucher.setOnClickListener(v -> {
            SharedPreferences prefs2 = getSharedPreferences("user_session", Context.MODE_PRIVATE);
            String loggedUser1 = prefs2.getString("username", null);
            if (loggedUser1 != null) {
                startActivity(new Intent(this, VoucherActivity.class));
            } else {
                Toast.makeText(this, "You need to log in to access vouchers", Toast.LENGTH_SHORT).show();
            }
        });

        fabLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
        fabRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        fabLogout.setOnClickListener(v -> logoutUser());

        fabLeaderboard.setOnClickListener(v -> {
            SharedPreferences prefs1 = getSharedPreferences("user_session", Context.MODE_PRIVATE);
            String username = prefs1.getString("username", null);
            if (username != null) {
                startActivity(new Intent(this, LeaderboardActivity.class));
            } else {
                Toast.makeText(this, "You must be logged in to view the leaderboard.", Toast.LENGTH_SHORT).show();
            }
        });

        fabVisitedLocations.setOnClickListener(v -> {
            SharedPreferences prefs3 = getSharedPreferences("user_session", Context.MODE_PRIVATE);
            String username = prefs3.getString("username", null);
            if (username != null) {
                // TODO: deschidem activitatea de visited locations
                startActivity(new Intent(this, VisitedLocationsActivity.class));

            } else {
                Toast.makeText(this, "You must be logged in to view visited locations.", Toast.LENGTH_SHORT).show();
            }
        });

        IntentFilter filter = new IntentFilter("com.example.smarttourism.SCORE_COLLECTED");
        registerReceiver(scoreCollectedReceiver, filter);

        startPeriodicRecommendation();

        // Apel la refresh UI (login / logout etc.)
        refreshUI();
    }

    private void startPeriodicRecommendation() {
        recommendationRunnable = new Runnable() {
            @Override
            public void run() {
                fetchRecommendation();
                recommendationHandler.postDelayed(this, RECOMMENDATION_INTERVAL);
            }
        };
        recommendationHandler.postDelayed(recommendationRunnable, RECOMMENDATION_INTERVAL);
    }

    private void fetchRecommendation() {
        AsyncTask.execute(() -> {
            try {
                URL url = new URL(RECOMMENDATION_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                SharedPreferences prefs5 = getSharedPreferences("user_session", Context.MODE_PRIVATE);
                String username = prefs5.getString("username", null);

                if (username == null) {
                    Log.e("Recommendation", "User not logged in");
                    return;
                }

                String jsonInputString = "{\"username\":\"" + username + "\"}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONObject responseJson = new JSONObject(response.toString());
                    String recommendation = responseJson.getString("recommendation");

                    runOnUiThread(() -> showRecommendationSnackbar(recommendation));
                } else {
                    Log.e("Recommendation", "Server returned non-OK status: " + responseCode);
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e("Recommendation", "❌ Error fetching recommendation", e);
            }
        });
    }


    private void showRecommendationSnackbar(String recommendation) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                "🌍 Recommended for you:\n" + recommendation,
                Snackbar.LENGTH_INDEFINITE);

        snackbar.setDuration(10000); // 10 secunde

        View snackbarView = snackbar.getView();
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setMaxLines(5);

        snackbar.setBackgroundTint(getResources().getColor(R.color.purple_500));
        snackbar.setTextColor(getResources().getColor(android.R.color.white));

        // 🔥 Detectăm tap pe snackbar
        snackbarView.setOnClickListener(v -> {
            showRecommendationDialog(recommendation); // deschidem dialog la tap
        });

        snackbar.show();
    }

    private void showRecommendationDialog(String recommendation) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Inflăm manual un layout ca să punem butonul X sus
        View customView = getLayoutInflater().inflate(R.layout.dialog_recommendation, null);

        TextView recommendationText = customView.findViewById(R.id.recommendation_text);
        ImageView closeButton = customView.findViewById(R.id.close_button);

        recommendationText.setText(recommendation);

        AlertDialog dialog = builder.setView(customView)
                .setCancelable(true)
                .create();

        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private BroadcastReceiver scoreCollectedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.smarttourism.SCORE_COLLECTED".equals(intent.getAction())) {

                SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
                String username = prefs.getString("username", null);

                if (username == null) {
                    Log.e("ergerg", "❌ User not logged in. Ignoring beacon signal.");
                    return;
                }

                String visitedLocationName = intent.getStringExtra("visited_location_name");
                if (visitedLocationName != null && !visitedLocationName.isEmpty()) {
                    showVisitedLocationToast(visitedLocationName);
                    markLocationAsVisited(visitedLocationName);
                }
            }
        }
    };



    private void markLocationAsVisited(String locationName) {
        if (locationName == null || mMap == null || markerList.isEmpty()) return;

        // 🔥 Dacă locația nu e în visitedLocationsList, o adăugăm imediat
        if (!visitedLocationsList.contains(locationName)) {
            visitedLocationsList.add(locationName);
        }

        // 🔥 Căutăm markerul corect și îl colorăm verde
        for (Marker marker : markerList) {
            if (marker.getTitle() != null && marker.getTitle().equals(locationName)) {
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                marker.showInfoWindow(); // afișăm infoWindow să știe userul că a vizitat
                break; // Găsit și colorat, ieșim
            }
        }
    }





    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            String lastVisitedLocation = data.getStringExtra("last_visited_location");
            if (lastVisitedLocation != null) {
                showVisitedLocationToast(lastVisitedLocation);
            }
        }
    }

    private void showVisitedLocationToast(String locationName) {
        Toast toast = Toast.makeText(this,
                "✅ You have visited: " + locationName,
                Toast.LENGTH_LONG);
        toast.setGravity(android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL, 0, 200);
        toast.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(scoreCollectedReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }

    private void refreshUI() {
        SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String username = prefs.getString("username", null);

        if (username != null) {
            // User logat
            fabAuth.setVisibility(View.GONE);
            authOptions.setVisibility(View.GONE);
            fabLogout.setVisibility(View.VISIBLE);
            fabLeaderboard.setVisibility(View.VISIBLE);
            fabVisitedLocations.setVisibility(View.VISIBLE);
            textLoggedIn.setVisibility(View.VISIBLE);
            textScore.setVisibility(View.VISIBLE);
            fabVoucher.setVisibility(View.VISIBLE);

            textLoggedIn.setText("Logged in as: " + username);
            fetchScoreFromBackend();
            fetchVisitedLocationsAndMarkers();
        } else {
            // User nelogat
            fabAuth.setVisibility(View.VISIBLE);
            authOptions.setVisibility(View.GONE);
            fabLogout.setVisibility(View.GONE);
            fabLeaderboard.setVisibility(View.GONE);
            fabVisitedLocations.setVisibility(View.GONE);
            textLoggedIn.setVisibility(View.GONE);
            textScore.setVisibility(View.GONE);
            fabVoucher.setVisibility(View.GONE);
            visitedLocationsList.clear();
            refreshMarkers();
        }
    }

    private void logoutUser() {
        SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        visitedLocationsList.clear(); // 🔥 Ștergem lista de locuri vizitate

        refreshUI(); // Actualizăm butoanele/logica de UI

        refreshMarkers(); // 🔥 Refacem toți markerii portocalii
    }

    private void refreshMarkers() {
        if (mMap == null) return;

        for (Marker marker : markerList) {
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
        }
    }

    private void fetchScoreFromBackend() {
        new Thread(() -> { try { SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE); String username = prefs.getString("username", null);
            if (username == null) {
                runOnUiThread(() -> {
                    TextView scoreText = findViewById(R.id.text_score);
                    scoreText.setText("Score: N/A (no user)");
                });
                return;
            }

            URL url = new URL("https://gamifytourism-955657412481.europe-west9.run.app/score?username=" + URLEncoder.encode(username, "UTF-8"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                conn.disconnect();

                JSONObject jsonResponse = new JSONObject(response.toString());

                if (jsonResponse.has("score")) {
                    int score = jsonResponse.getInt("score");
                    runOnUiThread(() -> {
                        TextView scoreText = findViewById(R.id.text_score);
                        scoreText.setText("Score: " + score);
                    });
                } else {
                    runOnUiThread(() -> {
                        TextView scoreText = findViewById(R.id.text_score);
                        scoreText.setText("Score: N/A (missing key)");
                    });
                }
            } else {
                runOnUiThread(() -> {
                    TextView scoreText = findViewById(R.id.text_score);
                    scoreText.setText("Score: N/A (bad response)");
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                TextView scoreText = findViewById(R.id.text_score);
                scoreText.setText("Score: N/A (exception)");
            });
        }
        }).start();
    }

    private float distanceBetween(LatLng latLng1, LatLng latLng2) {
        float[] results = new float[1];
        Location.distanceBetween(
                latLng1.latitude, latLng1.longitude,
                latLng2.latitude, latLng2.longitude,
                results
        );
        return results[0];
    }

    private Marker findNearestMarker(LatLng point) {
        Marker nearest = null;
        float minDistance = Float.MAX_VALUE;

        for (Marker marker : markerList) {
            float distance = distanceBetween(point, marker.getPosition());
            if (distance < minDistance) {
                minDistance = distance;
                nearest = marker;
            }
        }
        return nearest;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        mMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 15f));
                    }
                });

        fetchVisitedLocationsAndMarkers();

        mMap.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            return true;
        });

        mMap.setOnMapClickListener(latLng -> {
            Marker nearestMarker = findNearestMarker(latLng);
            if (nearestMarker != null && distanceBetween(latLng, nearestMarker.getPosition()) < 80) {
                nearestMarker.showInfoWindow();
            }
        });

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true);
    }

    private void fetchVisitedLocationsAndMarkers() {
        new Thread(() -> {
            try {
                SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
                String username = prefs.getString("username", null);

                if (username == null) {
                    // User nelogat -> desenează doar markerii normali
                    runOnUiThread(this::fetchAndDisplayMarkers);
                    return;
                }

                URL url = new URL("https://gamifytourism-955657412481.europe-west9.run.app/get-visited-locations");
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
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder responseBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    reader.close();

                    JSONObject responseJson = new JSONObject(responseBuilder.toString());
                    JSONArray visitedArray = responseJson.getJSONArray("visitedLocations");

                    visitedLocationsList.clear();
                    for (int i = 0; i < visitedArray.length(); i++) {
                        visitedLocationsList.add(visitedArray.getString(i));
                    }

                    runOnUiThread(this::fetchAndDisplayMarkers);
                } else {
                    Log.e("MapActivity", "Failed to fetch visited locations. Code: " + responseCode);
                    runOnUiThread(this::fetchAndDisplayMarkers); // fallback
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e("MapActivity", "Error fetching visited locations", e);
                runOnUiThread(this::fetchAndDisplayMarkers); // fallback
            }
        }).start();
    }


    private void fetchAndDisplayMarkers() {
        new Thread(() -> {
            try {
                URL url = new URL("https://gamifytourism-955657412481.europe-west9.run.app/locations");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder jsonBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jsonBuilder.append(line);
                    }
                    reader.close();

                    JSONArray locations = new JSONArray(jsonBuilder.toString());

                    // Afișăm markerii pe UI thread
                    runOnUiThread(() -> {
                        try {
                            for (int i = 0; i < locations.length(); i++) {
                                JSONObject location = locations.getJSONObject(i);
                                String name = location.getString("name");
                                double latitude = location.getDouble("latitude");
                                double longitude = location.getDouble("longitude");
                                int score = location.getInt("score");

                                LatLng position = new LatLng(latitude, longitude);

                                MarkerOptions markerOptions = new MarkerOptions()
                                        .position(position)
                                        .title(name)
                                        .snippet("Score: " + score);

                                if (visitedLocationsList.contains(name)) {
                                    // 🔥 Dacă locația a fost vizitată => verde
                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                                } else {
                                    // 🔥 Dacă nu, portocaliu
                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                                }

                                Marker marker = mMap.addMarker(markerOptions);
                                if (marker != null) {
                                    markerList.add(marker);
                                }


                                if (marker != null) {
                                    markerList.add(marker);
                                    Log.d("MapActivity", "Marker added: " + name + " at " + position);
                                }
                            }

                            Log.d("MapActivity", "Total markers: " + markerList.size());

                        } catch (Exception e) {
                            Log.e("MapActivity", "Error processing markers", e);
                        }
                    });

                } else {
                    Log.e("MapActivity", "Failed to fetch locations. Code: " + responseCode);
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e("MapActivity", "Exception in fetchAndDisplayMarkers", e);
            }
        }).start();
    }




    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) onMapReady(mMap);
        }
    }
}
