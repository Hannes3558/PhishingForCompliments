package com.example.phishingforcompliments.ui;

import com.example.phishingforcompliments.logic.CorporateGuidelines;
import com.example.phishingforcompliments.logic.RuleChecker;
import com.example.phishingforcompliments.model.Email;
import com.example.phishingforcompliments.model.Feature;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * UC-07: grafischer Vergleich eines Merkmals – links wie es in der Mail steht,
 * rechts was die Corporate Guidelines vorgeben. Der Spieler entscheidet selbst.
 */
public class ComparisonView extends VBox {

    public ComparisonView(RuleChecker checker, Email email, Feature feature, Runnable onReport, Runnable onBack) {
        setSpacing(14);
        setPadding(new Insets(16));

        Label title = new Label("Merkmal vergleichen: " + feature.label());
        title.getStyleClass().add("h2");

        Label rule = new Label("Regel: " + feature.rule());
        rule.setWrapText(true);
        rule.getStyleClass().add("rule-text");

        VBox left = column("In der E-Mail", actualNode(checker, email, feature));
        VBox right = column("Laut Corporate Guidelines", expectedNode(checker, email, feature));
        Label arrows = new Label("⇄");
        arrows.getStyleClass().add("compare-arrows");
        arrows.setMinWidth(Region.USE_PREF_SIZE);

        HBox columns = new HBox(12, left, arrows, right);
        columns.setAlignment(Pos.CENTER);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        VBox.setVgrow(columns, Priority.ALWAYS);

        Label question = new Label("Weicht die E-Mail bei diesem Merkmal von den Guidelines ab?");
        question.getStyleClass().add("bold");

        Button report = new Button("Abweichung melden");
        report.getStyleClass().add("btn-danger");
        report.setOnAction(e -> onReport.run());
        Button back = new Button("Zurück zur Mail");
        back.setOnAction(e -> onBack.run());
        HBox buttons = new HBox(12, report, back);
        buttons.setAlignment(Pos.CENTER);

        getChildren().addAll(title, rule, columns, question, buttons);
    }

    private static VBox column(String heading, Node content) {
        Label h = new Label(heading);
        h.getStyleClass().add("compare-heading");
        VBox box = new VBox(10, h, content);
        box.getStyleClass().add("compare-column");
        box.setPadding(new Insets(12));
        box.setAlignment(Pos.TOP_CENTER);
        box.setMinWidth(220);
        return box;
    }

    private static Node actualNode(RuleChecker checker, Email email, Feature feature) {
        if (feature == Feature.LOGO) {
            return new VBox(8, new LogoView(email.logo(), 96), text(email.logo().describe()));
        }
        return text(checker.describeActual(feature, email));
    }

    private static Node expectedNode(RuleChecker checker, Email email, Feature feature) {
        if (feature == Feature.LOGO && !checker.isPartnerMail(email)) {
            return new VBox(8, new LogoView(CorporateGuidelines.OFFICIAL_LOGO, 96),
                    text(CorporateGuidelines.OFFICIAL_LOGO.describe()));
        }
        return text(checker.describeExpected(feature, email));
    }

    private static Label text(String s) {
        Label l = new Label(s);
        l.setWrapText(true);
        l.getStyleClass().add("compare-text");
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }
}
