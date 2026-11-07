package com.example.phishingforcompliments.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Halbtransparente Pause-Ebene über dem Desktop (UC-05). */
public class PauseOverlay extends StackPane {

    public PauseOverlay(Runnable onResume) {
        getStyleClass().add("pause-overlay");
        Label title = new Label("PAUSE");
        title.getStyleClass().add("pause-title");
        Label hint = new Label("Die Uhr steht still. Esc oder \"Weiter\" zum Fortsetzen.");
        hint.getStyleClass().add("pause-hint");
        Button resume = new Button("Weiter");
        resume.getStyleClass().add("btn-primary");
        resume.setOnAction(e -> onResume.run());
        VBox box = new VBox(14, title, hint, resume);
        box.setAlignment(Pos.CENTER);
        getChildren().add(box);
        setVisible(false);
    }
}
