package com.example.phishingforcompliments;

import com.example.phishingforcompliments.logic.ChefMessages;
import com.example.phishingforcompliments.logic.GameSession;
import com.example.phishingforcompliments.logic.SaveGameService;
import com.example.phishingforcompliments.model.DayReport;
import com.example.phishingforcompliments.model.Difficulty;
import com.example.phishingforcompliments.model.GameState;
import com.example.phishingforcompliments.ui.DayReportScreen;
import com.example.phishingforcompliments.ui.DesktopScreen;
import com.example.phishingforcompliments.ui.TitleScreen;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import javafx.scene.image.Image;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JavaFX-Anwendung und Szenenwechsel: Titelbildschirm → Desktop (ein Arbeitstag)
 * → Tagesbericht → nächster Tag … bis Kampagnenende oder Kündigung.
 */
public class PhishingApp extends Application {

    public static final double WIDTH = 1180;
    public static final double HEIGHT = 740;

    private final StackPane root = new StackPane();
    private final SaveGameService saves = new SaveGameService();

    private GameSession session;
    private GameState dayStartState;
    private DayReport lastReport;
    private DesktopScreen desktop;

    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.getStylesheets().add(stylesheetUrl());
        stage.setTitle("Phishing for Compliments");
        stage.getIcons().setAll(appIcons());
        stage.setMinWidth(960);
        stage.setMinHeight(620);
        stage.setScene(scene);
        showTitle();
        stage.show();
    }

    // ------------------------------------------------------------------
    // Navigation
    // ------------------------------------------------------------------

    public void showTitle() {
        disposeDesktop();
        setRoot(new TitleScreen(this, saves.exists()));
    }

    /** UC-01 / UC-04 */
    public void startNewGame(String playerName, Difficulty difficulty) {
        session = new GameSession(GameState.newGame(playerName, difficulty, System.nanoTime()));
        lastReport = null;
        beginDay();
    }

    /** UC-03 */
    public boolean loadGame() {
        Optional<GameState> loaded = saves.load();
        if (loaded.isEmpty()) {
            showError("Es wurde kein gültiger Spielstand gefunden.");
            return false;
        }
        session = new GameSession(loaded.get());
        lastReport = null;
        beginDay();
        return true;
    }

    private void beginDay() {
        disposeDesktop();
        session.startDay();
        dayStartState = session.state();
        saveQuietly();
        String briefing = ChefMessages.dayBriefing(session.state(), lastReport, session.openMails().size());
        desktop = new DesktopScreen(this, session, briefing);
        setRoot(desktop);
    }

    public void endDay() {
        disposeDesktop();
        lastReport = session.endDay();
        dayStartState = session.state();
        saveQuietly();
        setRoot(new DayReportScreen(this, session, lastReport));
    }

    public void nextDay() {
        beginDay();
    }

    /** Kampagne vorbei (fertig oder gekündigt): Spielstand entfernen, zurück zum Titel. */
    public void finishCampaign() {
        try {
            saves.delete();
        } catch (IOException e) {
            showError("Spielstand konnte nicht gelöscht werden: " + e.getMessage());
        }
        showTitle();
    }

    // ------------------------------------------------------------------
    // Speichern / Beenden (UC-02)
    // ------------------------------------------------------------------

    /** Speichert immer den Stand vom Tagesbeginn – der laufende Tag startet beim Laden neu. */
    public boolean saveQuietly() {
        if (dayStartState == null) {
            return false;
        }
        try {
            saves.save(dayStartState);
            return true;
        } catch (IOException e) {
            showError("Spielstand konnte nicht gespeichert werden:\n" + e.getMessage());
            return false;
        }
    }

    public void saveAndExit() {
        saveQuietly();
        exit();
    }

    public void exit() {
        disposeDesktop();
        Platform.exit();
    }

    public GameState dayStartState() {
        return dayStartState;
    }

    // ------------------------------------------------------------------
    // Hilfen
    // ------------------------------------------------------------------

    /** NFR-03: Fehler verständlich anzeigen statt abzustürzen. */
    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("Da ist etwas schiefgelaufen");
        alert.showAndWait();
    }

    private void setRoot(Node node) {
        root.getChildren().setAll(node);
    }

    private void disposeDesktop() {
        if (desktop != null) {
            desktop.dispose();
            desktop = null;
        }
    }

    static String stylesheetUrl() {
        return Objects.requireNonNull(PhishingApp.class.getResource("styles.css"), "styles.css fehlt").toExternalForm();
    }

    /** Fenster-Icons in mehreren Größen (erzeugt von packaging/make-icon.ps1). */
    static List<Image> appIcons() {
        List<Image> icons = new ArrayList<>();
        for (int size : new int[] {16, 32, 48, 256}) {
            URL url = PhishingApp.class.getResource("icon-" + size + ".png");
            if (url != null) {
                icons.add(new Image(url.toExternalForm()));
            }
        }
        return icons;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
