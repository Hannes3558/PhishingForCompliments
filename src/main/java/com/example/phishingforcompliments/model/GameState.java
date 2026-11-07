package com.example.phishingforcompliments.model;

/**
 * Persistierbarer Spielstand (UC-02 / UC-03). Wird nur an Tagesgrenzen gespeichert,
 * deshalb reicht Tag, Punkte und Verwarnungen.
 */
public record GameState(String playerName, Difficulty difficulty, int day, int score, int warnings, long seed) {

    public static final int MAX_WARNINGS = 3;
    public static final int CAMPAIGN_DAYS = 5;

    public static GameState newGame(String playerName, Difficulty difficulty, long seed) {
        return new GameState(playerName, difficulty, 1, 0, 0, seed);
    }

    public GameState withDay(int newDay) {
        return new GameState(playerName, difficulty, newDay, score, warnings, seed);
    }

    public GameState withScore(int newScore) {
        return new GameState(playerName, difficulty, day, newScore, warnings, seed);
    }

    public GameState withWarnings(int newWarnings) {
        return new GameState(playerName, difficulty, day, score, newWarnings, seed);
    }

    public boolean isFired() {
        return warnings >= MAX_WARNINGS;
    }

    public boolean isCampaignFinished() {
        return day > CAMPAIGN_DAYS;
    }
}
