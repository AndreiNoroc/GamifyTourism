package com.example.smarttourism.models;

public class UserScore {
    private String username;
    private int score;

    public UserScore(String username, int score) {
        this.username = username;
        this.score = score;
    }

    public String getUsername() {
        return username;
    }

    public int getScore() {
        return score;
    }

}
