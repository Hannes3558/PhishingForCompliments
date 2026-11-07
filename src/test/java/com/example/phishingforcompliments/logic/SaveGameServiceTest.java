package com.example.phishingforcompliments.logic;

import com.example.phishingforcompliments.model.Difficulty;
import com.example.phishingforcompliments.model.GameState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaveGameServiceTest {

    @Test
    void roundTrip(@TempDir Path dir) throws IOException {
        SaveGameService service = new SaveGameService(dir.resolve("sub").resolve("save.properties"));
        assertFalse(service.exists());
        assertTrue(service.load().isEmpty());

        GameState state = new GameState("Kim Müller", Difficulty.SCHWER, 3, 85, 1, 123456789L);
        service.save(state);

        assertTrue(service.exists());
        Optional<GameState> loaded = service.load();
        assertTrue(loaded.isPresent());
        assertEquals(state, loaded.get());

        service.delete();
        assertFalse(service.exists());
    }

    @Test
    void corruptFileYieldsEmpty(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("save.properties");
        Files.writeString(file, "difficulty=UNBEKANNT\nday=abc\n");
        assertTrue(new SaveGameService(file).load().isEmpty());
    }
}
