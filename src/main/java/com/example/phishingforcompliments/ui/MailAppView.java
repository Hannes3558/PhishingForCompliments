package com.example.phishingforcompliments.ui;

import com.example.phishingforcompliments.logic.CorporateGuidelines;
import com.example.phishingforcompliments.logic.GameSession;
import com.example.phishingforcompliments.logic.RuleChecker;
import com.example.phishingforcompliments.model.Attachment;
import com.example.phishingforcompliments.model.DecisionResult;
import com.example.phishingforcompliments.model.Email;
import com.example.phishingforcompliments.model.EmailLink;
import com.example.phishingforcompliments.model.Feature;
import com.example.phishingforcompliments.model.Signature;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Das "Mail-Programm" nach Skizze 3: Liste links, Mail rechts mit Logo, Signatur und Code.
 *
 * Geprüft wird über die Werkzeugleiste ("Prüfen: Absender | Link | …") oder per Klick auf
 * Absender, Logo bzw. Signatur. Links und Anhänge werden bewusst NICHT durch Anklicken
 * geprüft – das wäre im echten Leben genau der Fehler, den das Spiel abtrainieren soll.
 */
public class MailAppView extends BorderPane {

    static final String LINK_WARNING = "Links in verdächtigen Mails nie anklicken! Fahre nur mit der Maus darüber "
            + "– das echte Ziel steht unten in der Statusleiste. Zum Vergleichen: \"Prüfen: Link\".";
    static final String ATTACHMENT_WARNING = "Anhänge nicht öffnen, solange die Mail nicht geprüft ist! "
            + "Den Dateinamen vergleichst du mit \"Prüfen: Anhang\".";

    private final GameSession session;
    private final RuleChecker checker;
    private final int day;
    private final Consumer<DecisionResult> onDecision;

    private final ListView<Email> list = new ListView<>();
    private final StackPane detailPane = new StackPane();
    private final Label statusBar = new Label(" ");
    private Label caution;

    public MailAppView(GameSession session, Consumer<DecisionResult> onDecision) {
        this.session = session;
        this.checker = session.checker();
        this.day = session.state().day();
        this.onDecision = onDecision;

        list.setCellFactory(lv -> new MailCell());
        list.setPrefWidth(260);
        list.getStyleClass().add("mail-list");
        list.setPlaceholder(placeholder("Postfach leer.\nZeit für den Feierabend!"));
        list.getSelectionModel().selectedItemProperty().addListener((obs, o, mail) -> {
            if (mail != null) {
                showEmail(mail, null);
            }
        });

        Label inboxTitle = new Label("Posteingang");
        inboxTitle.getStyleClass().add("inbox-title");
        VBox leftBox = new VBox(inboxTitle, list);
        VBox.setVgrow(list, Priority.ALWAYS);
        leftBox.getStyleClass().add("inbox-pane");

        detailPane.getChildren().add(placeholder("Wähle links eine Mail aus."));
        statusBar.getStyleClass().add("status-bar");
        statusBar.setMaxWidth(Double.MAX_VALUE);

        setLeft(leftBox);
        setCenter(detailPane);
        setBottom(statusBar);
        refreshList();
    }

    // ------------------------------------------------------------------
    // Liste
    // ------------------------------------------------------------------

    public void refreshList() {
        Email selected = list.getSelectionModel().getSelectedItem();
        List<Email> mails = new ArrayList<>(session.openMails());
        Collections.reverse(mails); // neueste oben
        list.setItems(FXCollections.observableArrayList(mails));
        if (selected != null && mails.contains(selected)) {
            list.getSelectionModel().select(selected);
        }
        list.refresh();
    }

    private class MailCell extends ListCell<Email> {
        MailCell() {
            // Zelle nie breiter als die Liste: lange Namen werden mit "…" gekürzt statt eine
            // horizontale Scrollleiste zu erzeugen (die Uhrzeit rechts bleibt immer sichtbar).
            setPrefWidth(0);
        }

        @Override
        protected void updateItem(Email mail, boolean empty) {
            super.updateItem(mail, empty);
            if (empty || mail == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            boolean unread = !session.isRead(mail);
            Label name = new Label(mail.senderName());
            name.getStyleClass().add(unread ? "bold" : "normal");
            name.setMinWidth(0);
            Label time = new Label(mail.receivedAt());
            time.getStyleClass().add("muted");
            time.setMinWidth(Region.USE_PREF_SIZE);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox top = new HBox(6, name, spacer, time);
            Label subject = new Label(mail.subject());
            subject.getStyleClass().add("mail-subject");
            subject.setMaxWidth(170);
            VBox texts = new VBox(2, top, subject);
            texts.setMinWidth(0);
            HBox.setHgrow(texts, Priority.ALWAYS);
            Circle dot = new Circle(5, unread ? Color.web("#E53935") : Color.TRANSPARENT);
            HBox row = new HBox(8, avatar(mail.senderName(), 18), texts, dot);
            row.setAlignment(Pos.CENTER_LEFT);
            setGraphic(row);
            setText(null);
        }
    }

    private static StackPane avatar(String name, double radius) {
        String[] parts = name.trim().split("\\s+");
        String initials = parts.length >= 2
                ? ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0))
                : name.substring(0, Math.min(2, name.length()));
        int hue = Math.abs(name.hashCode()) % 360;
        Circle c = new Circle(radius, Color.hsb(hue, 0.45, 0.75));
        Text t = new Text(initials.toUpperCase());
        t.setFill(Color.WHITE);
        t.setStyle("-fx-font-weight: bold; -fx-font-size: " + (radius * 0.8) + "px;");
        return new StackPane(c, t);
    }

    // ------------------------------------------------------------------
    // Detailansicht
    // ------------------------------------------------------------------

    private void showEmail(Email mail, DecisionResult feedback) {
        session.markRead(mail);
        list.refresh();
        statusBar.setText(" ");
        String player = session.state().playerName();

        // Kopf: Absender, Empfänger, Betreff, Logo
        Label senderName = new Label(mail.senderName());
        senderName.getStyleClass().add("bold");
        Label senderAddr = new Label("<" + mail.senderAddress() + ">");
        senderAddr.getStyleClass().add("muted");
        HBox senderLine = new HBox(6, new Label("Von:"), senderName, senderAddr);
        senderLine.setAlignment(Pos.CENTER_LEFT);
        VBox senderRegion = new VBox(senderLine);
        senderRegion.setPadding(new Insets(4, 6, 4, 6));
        makeHot(senderRegion, Feature.ABSENDER, mail);

        Label to = new Label("An: " + player + " <" + player.toLowerCase().replace(' ', '.') + "@"
                + CorporateGuidelines.COMPANY_DOMAIN + ">");
        to.getStyleClass().add("muted");
        Label subject = new Label(mail.subject());
        subject.getStyleClass().add("mail-subject-big");
        subject.setWrapText(true);
        Label time = new Label("Heute, " + mail.receivedAt() + " Uhr");
        time.getStyleClass().add("muted");

        VBox headerLeft = new VBox(4, senderRegion, to, time, subject);
        HBox.setHgrow(headerLeft, Priority.ALWAYS);

        LogoView logo = new LogoView(mail.logo(), 64);
        StackPane logoRegion = new StackPane(logo);
        logoRegion.setPadding(new Insets(6));
        makeHot(logoRegion, Feature.LOGO, mail);

        HBox header = new HBox(12, headerLeft, logoRegion);
        header.setAlignment(Pos.TOP_LEFT);

        caution = new Label();
        caution.getStyleClass().add("caution");
        caution.setWrapText(true);
        caution.setMaxWidth(Double.MAX_VALUE);
        caution.setVisible(false);
        caution.setManaged(false);

        VBox content = new VBox(12);
        content.setPadding(new Insets(14));
        if (feedback != null) {
            content.getChildren().add(feedbackBox(feedback));
        }
        content.getChildren().addAll(caution, header, new Separator(), buildBody(mail));
        Node attachments = buildAttachments(mail);
        if (attachments != null) {
            content.getChildren().add(attachments);
        }
        content.getChildren().add(buildSignature(mail));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("plain-scroll");

        BorderPane detail = new BorderPane(scroll);
        detail.setTop(buildToolbar(mail));
        detailPane.getChildren().setAll(detail);
    }

    /** Werkzeugleiste: ein Prüf-Button je Merkmal (UC-07) und "Als sicher ablegen". */
    private Node buildToolbar(Email mail) {
        Label label = new Label("Prüfen:");
        label.getStyleClass().add("bold");
        label.setMinWidth(Region.USE_PREF_SIZE);
        HBox bar = new HBox(6, label);
        bar.getStyleClass().add("mail-toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 12, 8, 12));

        for (Feature f : Feature.values()) {
            Button b = new Button(f.label());
            b.getStyleClass().add("inspect-button");
            b.setMinWidth(Region.USE_PREF_SIZE);
            b.setUserData(f);
            if (!f.activeOn(day)) {
                b.setDisable(true);
                b.getStyleClass().add("inspect-locked");
            } else if ((f == Feature.LINK && mail.links().isEmpty())
                    || (f == Feature.ANHANG && mail.attachments().isEmpty())) {
                b.setDisable(true);
            } else {
                if (f.introducedOnDay() == day) {
                    b.getStyleClass().add("inspect-new");
                    b.setTooltip(new Tooltip("Neu ab heute: " + f.rule()));
                } else {
                    b.setTooltip(new Tooltip("Mit den Corporate Guidelines vergleichen"));
                }
                b.setOnAction(e -> showComparison(mail, f));
            }
            bar.getChildren().add(b);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button safe = new Button("Als sicher ablegen");
        safe.getStyleClass().add("btn-primary");
        safe.setMinWidth(Region.USE_PREF_SIZE);
        safe.setOnAction(e -> decide(session.fileAsSafe(mail), mail));
        bar.getChildren().addAll(spacer, safe);
        return bar;
    }

    private TextFlow buildBody(Email mail) {
        TextFlow flow = new TextFlow();
        flow.getStyleClass().add("mail-body");
        String[] parts = mail.body().split("\\{link}", -1);
        for (int i = 0; i < parts.length; i++) {
            flow.getChildren().add(new Text(parts[i]));
            if (i < parts.length - 1 && i < mail.links().size()) {
                flow.getChildren().add(buildLink(mail.links().get(i)));
            }
        }
        return flow;
    }

    /** Links zeigen ihr Ziel nur beim Darüberfahren. Ein Klick "öffnet" nichts, sondern warnt. */
    private Hyperlink buildLink(EmailLink link) {
        Hyperlink h = new Hyperlink(link.displayText());
        h.setOnMouseEntered(e -> statusBar.setText("Link-Ziel: " + link.url()));
        h.setOnMouseExited(e -> statusBar.setText(" "));
        h.setOnAction(e -> {
            h.setVisited(false);
            warn(LINK_WARNING);
        });
        return h;
    }

    private Node buildAttachments(Email mail) {
        if (mail.attachments().isEmpty()) {
            return null;
        }
        HBox box = new HBox(10, new Label("📎 Anhang:"));
        box.setAlignment(Pos.CENTER_LEFT);
        for (Attachment a : mail.attachments()) {
            Label chip = new Label(a.fileName() + "  (" + a.sizeKb() + " KB)");
            chip.getStyleClass().add("attachment-chip");
            chip.setOnMouseClicked(e -> warn(ATTACHMENT_WARNING));
            box.getChildren().add(chip);
        }
        box.setPadding(new Insets(6));
        return box;
    }

    private Node buildSignature(Email mail) {
        Signature s = mail.signature();
        VBox box = new VBox(2);
        box.setPadding(new Insets(6));
        if (s.isEmpty()) {
            Label none = new Label("— keine Signatur —");
            none.getStyleClass().add("muted");
            box.getChildren().addAll(new Label("Mit freundlichen Grüßen"), none);
        } else {
            Label name = new Label(s.name());
            name.getStyleClass().add("bold");
            box.getChildren().addAll(new Label("Mit freundlichen Grüßen"), name, new Label(s.department()));
            if (!s.code().isBlank()) {
                Label code = new Label("Mitarbeiter-Code: " + s.code());
                code.getStyleClass().add("code");
                box.getChildren().add(code);
            }
        }
        makeHot(box, Feature.SIGNATUR, mail);
        return box;
    }

    /** Macht Absender, Logo oder Signatur zusätzlich direkt anklickbar (harmlos, nichts wird geöffnet). */
    private void makeHot(Region region, Feature feature, Email mail) {
        if (feature.activeOn(day)) {
            region.getStyleClass().add("feature-hot");
            Tooltip.install(region, new Tooltip("Mit den Guidelines vergleichen: " + feature.label()));
            region.setOnMouseClicked(e -> showComparison(mail, feature));
        } else {
            Tooltip.install(region, new Tooltip("Das Merkmal \"" + feature.label() + "\" wird erst ab Tag "
                    + feature.introducedOnDay() + " geprüft."));
        }
    }

    private void warn(String text) {
        statusBar.setText("⚠ " + text);
        if (caution != null) {
            caution.setText("⚠ " + text);
            caution.setVisible(true);
            caution.setManaged(true);
        }
    }

    private void showComparison(Email mail, Feature feature) {
        statusBar.setText(" ");
        ComparisonView view = new ComparisonView(checker, mail, feature,
                () -> decide(session.reportFeature(mail, feature), mail),
                () -> showEmail(mail, null));
        detailPane.getChildren().setAll(view);
    }

    private void decide(DecisionResult result, Email mail) {
        onDecision.accept(result);
        refreshList();
        if (result.closesEmail()) {
            list.getSelectionModel().clearSelection();
            VBox box = new VBox(14, feedbackBox(result));
            box.setPadding(new Insets(14));
            Label next = new Label(session.openMails().isEmpty()
                    ? "Alle Mails bearbeitet. Du kannst Feierabend machen."
                    : "Wähle links die nächste Mail.");
            next.getStyleClass().add("muted");
            box.getChildren().add(next);
            detailPane.getChildren().setAll(box);
        } else {
            showEmail(mail, result);
        }
    }

    private static Node feedbackBox(DecisionResult r) {
        Label title = new Label((r.isCorrect() ? "✔ " : "✘ ") + (r.points() >= 0 ? "+" : "") + r.points() + " Punkte");
        title.getStyleClass().add("bold");
        Label text = new Label(r.explanation());
        text.setWrapText(true);
        VBox box = new VBox(4, title, text);
        box.setPadding(new Insets(10));
        box.getStyleClass().add(r.isCorrect() ? "feedback-ok" : "feedback-bad");
        return box;
    }

    private static Node placeholder(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("placeholder");
        l.setWrapText(true);
        l.setAlignment(Pos.CENTER);
        return l;
    }
}
