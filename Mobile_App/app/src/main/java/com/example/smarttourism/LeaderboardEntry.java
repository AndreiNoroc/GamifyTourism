package com.example.smarttourism;

public class LeaderboardEntry {
    private final String name;
    private final int score;

    public LeaderboardEntry(String name, int score) {
        this.name = name;
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }
}
