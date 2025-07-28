package com.example.smarttourism;

public class LocationModel {
    private String name;
    private double latitude;
    private double longitude;
    private int points;
    private double weight;

    public LocationModel(String name, double latitude, double longitude, int points, double weight) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.points = points;
        this.weight = weight;
    }

    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getPoints() { return points; }
    public double getWeight() { return weight; }
}