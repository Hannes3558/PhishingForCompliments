package com.example.phishingforcompliments.ui;

import com.example.phishingforcompliments.PhishingApp;
import com.example.phishingforcompliments.model.Difficulty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineJoin;

/** Titelbildschirm nach Skizze 1: Titel, geflügelter Briefumschlag, Schwierigkeit, Start. */
public class TitleScreen extends StackPane {

    public TitleScreen(PhishingApp app, boolean hasSave) {
        getStyleClass().add("title-screen");

        Label title = new Label("Phishing\nfor\nCompliments");
        title.getStyleClass().add("title-text");
        title.setMinWidth(Region.USE_PREF_SIZE);

        HBox hero = new HBox(60, title, buildWingedEnvelope());
        hero.setAlignment(Pos.CENTER);

        TextField nameField = new TextField();
        nameField.setPromptText("Dein Name");
        nameField.setPrefColumnCount(14);

        ComboBox<Difficulty> difficultyBox = new ComboBox<>(FXCollections.observableArrayList(Difficulty.values()));
        difficultyBox.setValue(Difficulty.LEICHT);
        difficultyBox.setPrefWidth(160);

        Label difficultyInfo = new Label();
        difficultyInfo.getStyleClass().add("muted");
        difficultyBox.valueProperty().addListener((obs, o, d) -> difficultyInfo.setText(describe(d)));
        difficultyInfo.setText(describe(difficultyBox.getValue()));

        Label error = new Label();
        error.getStyleClass().add("error-text");

        Button start = new Button("Start");
        start.getStyleClass().add("btn-primary");
        start.setDefaultButton(true);
        start.setOnAction(e -> {
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            if (name.isEmpty()) {
                error.setText("Bitte gib zuerst deinen Namen ein.");
                nameField.requestFocus();
                return;
            }
            app.startNewGame(name, difficultyBox.getValue());
        });

        Button load = new Button("Spiel laden");
        load.setDisable(!hasSave);
        load.setOnAction(e -> app.loadGame());

        Button quit = new Button("Beenden");
        quit.setOnAction(e -> app.exit());

        HBox form = new HBox(12, new Label("Name:"), nameField, new Label("Schwierigkeit:"), difficultyBox);
        form.setAlignment(Pos.CENTER);

        HBox buttons = new HBox(16, start, load, quit);
        buttons.setAlignment(Pos.CENTER);

        VBox card = new VBox(18, hero, form, difficultyInfo, buttons, error);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("title-card");
        card.setPadding(new Insets(40, 60, 40, 60));
        card.setMaxWidth(820);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        Label footer = new Label("Ein Lernspiel über Phishing · Programmierprojekt 3. Semester");
        footer.getStyleClass().add("muted");
        StackPane.setAlignment(footer, Pos.BOTTOM_CENTER);
        StackPane.setMargin(footer, new Insets(0, 0, 16, 0));

        getChildren().addAll(card, footer);
    }

    private static String describe(Difficulty d) {
        return d.label() + ": " + d.mailsForDay(1) + "+ Mails pro Tag · "
                + d.intensityLabel() + " Abweichungen · "
                + d.minManipulations() + "–" + d.maxManipulations() + " Fehler pro Phishing-Mail · "
                + (d.daySeconds() / 60) + " Minuten pro Arbeitstag";
    }

    /** Briefumschlag mit Flügeln, gezeichnet aus einfachen Shapes (wie in der Skizze). */
    private static Node buildWingedEnvelope() {
        Rectangle env = new Rectangle(0, 0, 150, 96);
        env.setFill(Color.WHITE);
        env.setStroke(Color.web("#1F5FBF"));
        env.setStrokeWidth(3);
        env.setArcWidth(8);
        env.setArcHeight(8);

        Polyline flap = new Polyline(0, 0, 75, 52, 150, 0);
        flap.setStroke(Color.web("#1F5FBF"));
        flap.setStrokeWidth(3);
        flap.setStrokeLineJoin(StrokeLineJoin.ROUND);

        Polygon leftWing = wing(false);
        leftWing.setTranslateX(-95);
        leftWing.setTranslateY(-30);
        Polygon rightWing = wing(true);
        rightWing.setTranslateX(150);
        rightWing.setTranslateY(-30);

        Group g = new Group(leftWing, rightWing, env, flap);
        StackPane wrapper = new StackPane(g);
        wrapper.setPadding(new Insets(20, 40, 20, 40));
        return wrapper;
    }

    private static Polygon wing(boolean mirrored) {
        double[] pts = {0, 60, 20, 10, 50, 0, 95, 5, 70, 25, 95, 40, 60, 45, 85, 70, 40, 65, 0, 90};
        if (mirrored) {
            for (int i = 0; i < pts.length; i += 2) {
                pts[i] = 95 - pts[i];
            }
        }
        Polygon p = new Polygon();
        for (double v : pts) {
            p.getPoints().add(v);
        }
        p.setFill(Color.web("#F3F6FB"));
        p.setStroke(Color.web("#1F5FBF"));
        p.setStrokeWidth(2.5);
        p.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return p;
    }
}
