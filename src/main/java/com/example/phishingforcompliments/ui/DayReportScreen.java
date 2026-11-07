package com.example.phishingforcompliments.ui;

import com.example.phishingforcompliments.PhishingApp;
import com.example.phishingforcompliments.logic.ChefMessages;
import com.example.phishingforcompliments.logic.GameSession;
import com.example.phishingforcompliments.model.DayReport;
import com.example.phishingforcompliments.model.DecisionResult;
import com.example.phishingforcompliments.model.GameState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Tagesbericht am Abend: jede Entscheidung mit Erklärung, Punkte, Verwarnungen, weiter / Ende. */
public class DayReportScreen extends BorderPane {

    public DayReportScreen(PhishingApp app, GameSession session, DayReport report) {
        getStyleClass().add("report-screen");
        GameState state = session.state();

        Label title = new Label("Tagesbericht – Tag " + report.day());
        title.getStyleClass().add("h1");
        Label summary = new Label(
                "Richtig: " + report.correctCount() + " von " + report.results().size() + " Entscheidungen   ·   "
                        + "Punkte heute: " + signed(report.pointsToday()) + "   ·   "
                        + "Gesamt: " + report.totalScore() + "   ·   "
                        + "Verwarnungen: " + report.warnings() + " / " + GameState.MAX_WARNINGS);
        summary.getStyleClass().add("report-summary");
        VBox header = new VBox(6, title, summary);
        header.setPadding(new Insets(20, 24, 10, 24));

        VBox rows = new VBox(10);
        rows.setPadding(new Insets(10, 24, 10, 24));
        for (DecisionResult r : report.results()) {
            rows.getChildren().add(row(r));
        }
        ScrollPane scroll = new ScrollPane(rows);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("plain-scroll");

        VBox bottom = new VBox(12);
        bottom.setPadding(new Insets(14, 24, 20, 24));
        bottom.setAlignment(Pos.CENTER);
        if (report.fired() || report.campaignFinished()) {
            Label verdict = new Label(ChefMessages.finalVerdict(state, report.fired()));
            verdict.setWrapText(true);
            verdict.getStyleClass().add(report.fired() ? "verdict-bad" : "verdict-ok");
            verdict.setMaxWidth(820);
            Button toTitle = new Button("Zum Hauptmenü");
            toTitle.getStyleClass().add("btn-primary");
            toTitle.setOnAction(e -> app.finishCampaign());
            bottom.getChildren().addAll(verdict, toTitle);
        } else {
            Button next = new Button("Nächster Tag (Tag " + state.day() + ")");
            next.getStyleClass().add("btn-primary");
            next.setOnAction(e -> app.nextDay());
            Button saveExit = new Button("Speichern & Beenden");
            saveExit.setOnAction(e -> app.saveAndExit());
            HBox buttons = new HBox(14, next, saveExit);
            buttons.setAlignment(Pos.CENTER);
            Label saved = new Label("Der Spielstand wurde automatisch gespeichert.");
            saved.getStyleClass().add("muted");
            bottom.getChildren().addAll(buttons, saved);
        }

        setTop(header);
        setCenter(scroll);
        setBottom(bottom);
    }

    private static VBox row(DecisionResult r) {
        Label mark = new Label(r.isCorrect() ? "✔" : "✘");
        mark.getStyleClass().add(r.isCorrect() ? "mark-ok" : "mark-bad");
        mark.setMinWidth(24);
        Label subject = new Label(r.email().subject());
        subject.getStyleClass().add("bold");
        Label sender = new Label("von " + r.email().senderName() + " <" + r.email().senderAddress() + ">");
        sender.getStyleClass().add("muted");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label outcome = new Label(outcomeText(r));
        Label points = new Label(signed(r.points()));
        points.getStyleClass().add("bold");
        points.setMinWidth(44);
        points.setAlignment(Pos.CENTER_RIGHT);
        HBox top = new HBox(10, mark, subject, sender, spacer, outcome, points);
        top.setAlignment(Pos.CENTER_LEFT);
        Label explanation = new Label(r.explanation());
        explanation.setWrapText(true);
        explanation.setPadding(new Insets(0, 0, 0, 34));
        VBox box = new VBox(4, top, explanation);
        box.setPadding(new Insets(10));
        box.getStyleClass().add(r.isCorrect() ? "result-ok" : "result-bad");
        return box;
    }

    private static String outcomeText(DecisionResult r) {
        return switch (r.outcome()) {
            case REPORTED_CORRECT -> "Abweichung gemeldet";
            case REPORTED_WRONG -> "Falsches Merkmal gemeldet";
            case FILED_CORRECT -> "Als sicher abgelegt";
            case FILED_WRONG -> "Phishing durchgewinkt";
            case UNPROCESSED -> "Nicht bearbeitet";
        };
    }

    private static String signed(int points) {
        return (points > 0 ? "+" : "") + points;
    }
}
