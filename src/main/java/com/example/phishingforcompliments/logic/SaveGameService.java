package com.example.phishingforcompliments.logic;

import com.example.phishingforcompliments.model.Difficulty;
import com.example.phishingforcompliments.model.GameState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/**
 * Speichert und lädt den Spielstand (UC-02 / UC-03) als Properties-Datei
 * unter {@code ~/.phishingforcompliments/savegame.properties}.
 */
public final class SaveGameService {

    private final Path file;

    public SaveGameService() {
        this(Path.of(System.getProperty("user.home"), ".phishingforcompliments", "savegame.properties"));
    }

    public SaveGameService(Path file) {
        this.file = file;
    }

    public Path file() {
        return file;
    }

    public boolean exists() {
        return Files.exists(file);
    }

    public void save(GameState state) throws IOException {
        Properties p = new Properties();
        p.setProperty("playerName", state.playerName());
        p.setProperty("difficulty", state.difficulty().name());
        p.setProperty("day", Integer.toString(state.day()));
        p.setProperty("score", Integer.toString(state.score()));
        p.setProperty("warnings", Integer.toString(state.warnings()));
        p.setProperty("seed", Long.toString(state.seed()));
        Files.createDirectories(file.getParent());
        try (OutputStream out = Files.newOutputStream(file)) {
            p.store(out, "Phishing for Compliments – Spielstand");
        }
    }

    public Optional<GameState> load() {
        if (!exists()) {
            return Optional.empty();
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
            return Optional.of(new GameState(
                    p.getProperty("playerName", "Spieler"),
                    Difficulty.valueOf(p.getProperty("difficulty", Difficulty.LEICHT.name())),
                    Integer.parseInt(p.getProperty("day", "1")),
                    Integer.parseInt(p.getProperty("score", "0")),
                    Integer.parseInt(p.getProperty("warnings", "0")),
                    Long.parseLong(p.getProperty("seed", "1"))));
        } catch (IOException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public void delete() throws IOException {
        Files.deleteIfExists(file);
    }
}
