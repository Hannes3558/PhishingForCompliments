package com.example.phishingforcompliments;

import javafx.application.Application;

import java.util.Arrays;

/**
 * Einstiegspunkt ohne JavaFX-Superklasse (nötig für das Starten aus einem Fat-JAR / der IDE
 * und als Hauptklasse der .exe). {@code --selftest} prüft das Paket, ohne ein Fenster zu öffnen.
 */
public class Launcher {
    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--selftest")) {
            System.exit(SelfTest.run());
        }
        Application.launch(PhishingApp.class, args);
    }
}
