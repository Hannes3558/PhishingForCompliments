package com.example.phishingforcompliments;

import com.example.phishingforcompliments.logic.ChefMessages;
import com.example.phishingforcompliments.logic.GameSession;
import com.example.phishingforcompliments.model.Difficulty;
import com.example.phishingforcompliments.model.GameState;
import com.example.phishingforcompliments.ui.DesktopScreen;
import com.example.phishingforcompliments.ui.TitleScreen;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Selbsttest für das fertige Paket ({@code PhishingForCompliments.exe --selftest}).
 * Startet JavaFX (inkl. nativer Bibliotheken), lädt CSS und Icons und baut Titel- und
 * Desktop-Ansicht in einer unsichtbaren Scene. Es wird kein Fenster angezeigt.
 * Ergebnis: Exit-Code 0/1 und eine Logdatei im Temp-Verzeichnis.
 */
final class SelfTest {

    private SelfTest() {
    }

    static int run() {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        StringBuilder log = new StringBuilder("Phishing for Compliments – Selbsttest\n");
        log.append("Java ").append(System.getProperty("java.version"))
                .append(" / ").append(System.getProperty("java.home")).append('\n');
        try {
            Platform.startup(() -> {
                try {
                    PhishingApp app = new PhishingApp();
                    StackPane root = new StackPane(new TitleScreen(app, false));
                    Scene scene = new Scene(root, PhishingApp.WIDTH, PhishingApp.HEIGHT);
                    scene.getStylesheets().add(PhishingApp.stylesheetUrl());
                    root.applyCss();
                    root.layout();
                    log.append("Titelbildschirm: ok\n");

                    GameSession session = new GameSession(GameState.newGame("Selbsttest", Difficulty.MITTEL, 1L));
                    session.startDay();
                    DesktopScreen desktop = new DesktopScreen(app, session,
                            ChefMessages.dayBriefing(session.state(), null, session.openMails().size()));
                    root.getChildren().setAll(desktop);
                    root.applyCss();
                    root.layout();
                    desktop.dispose();
                    log.append("Desktop mit ").append(session.openMails().size()).append(" Mails: ok\n");

                    for (Image icon : PhishingApp.appIcons()) {
                        if (icon.isError() || icon.getWidth() <= 0) {
                            throw new IllegalStateException("Icon fehlerhaft: " + icon.getUrl());
                        }
                    }
                    log.append("Icons: ok\n");
                } catch (Throwable t) {
                    error.set(t);
                } finally {
                    done.countDown();
                }
            });
            if (!done.await(60, TimeUnit.SECONDS)) {
                error.compareAndSet(null, new IllegalStateException("Zeitüberschreitung beim Selbsttest"));
            }
        } catch (Throwable t) {
            error.compareAndSet(null, t);
        }

        int code = error.get() == null ? 0 : 1;
        if (error.get() != null) {
            StringWriter sw = new StringWriter();
            error.get().printStackTrace(new PrintWriter(sw));
            log.append("FEHLER:\n").append(sw);
        } else {
            log.append("ERGEBNIS: alles ok\n");
        }
        try {
            Files.writeString(Path.of(System.getProperty("java.io.tmpdir"), "phishingforcompliments-selftest.log"),
                    log.toString());
        } catch (IOException ignored) {
            // Log ist nur eine Hilfe, der Exit-Code zählt.
        }
        System.out.print(log);
        Platform.exit();
        return code;
    }
}
