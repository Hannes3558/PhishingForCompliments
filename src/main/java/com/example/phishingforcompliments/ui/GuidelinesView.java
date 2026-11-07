package com.example.phishingforcompliments.ui;

import com.example.phishingforcompliments.logic.CorporateGuidelines;
import com.example.phishingforcompliments.model.Employee;
import com.example.phishingforcompliments.model.Feature;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TextField;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.function.Function;

/** Die Corporate Guidelines als "Programm" auf dem Desktop: Regeln, Logo, Domains, Mitarbeiterverzeichnis. */
public class GuidelinesView extends ScrollPane {

    public GuidelinesView(int day) {
        VBox content = new VBox(16);
        content.setPadding(new Insets(16));

        Label title = new Label("Corporate Guidelines – " + CorporateGuidelines.COMPANY_NAME);
        title.getStyleClass().add("h1");
        Label sub = new Label("Stand: Tag " + day + ". Neue Regeln werden von der Geschäftsführung bekannt gegeben.");
        sub.getStyleClass().add("muted");
        content.getChildren().addAll(title, sub);

        // Regeln
        VBox rules = new VBox(8);
        for (Feature f : Feature.values()) {
            boolean active = f.activeOn(day);
            Label mark = new Label(active ? "✔" : "🔒");
            mark.setMinWidth(24);
            Label name = new Label(f.label() + (active ? "" : "  (gilt ab Tag " + f.introducedOnDay() + ")"));
            name.getStyleClass().add("bold");
            Label rule = new Label(active ? f.rule() : "Noch nicht freigeschaltet.");
            rule.setWrapText(true);
            VBox text = new VBox(2, name, rule);
            HBox row = new HBox(8, mark, text);
            row.setAlignment(Pos.TOP_LEFT);
            if (!active) {
                row.setOpacity(0.45);
            }
            rules.getChildren().add(row);
        }
        content.getChildren().add(section("Prüfregeln", rules));

        // Logo
        HBox logoRow = new HBox(16, new LogoView(CorporateGuidelines.OFFICIAL_LOGO, 72),
                wrapped("Offizielles Logo: " + CorporateGuidelines.OFFICIAL_LOGO.describe()
                        + ".\nAbweichende Farbe, Form, Buchstabe oder fehlender Stern = Fälschung."));
        logoRow.setAlignment(Pos.CENTER_LEFT);
        content.getChildren().add(section("Firmenlogo", logoRow));

        // Domains
        VBox domains = new VBox(4,
                wrapped("Firmendomain: " + CorporateGuidelines.COMPANY_DOMAIN + " (inklusive Subdomains wie intranet."
                        + CorporateGuidelines.COMPANY_DOMAIN + ")"),
                wrapped("Partnerfirmen: " + String.join(", ", CorporateGuidelines.PARTNER_DOMAINS)),
                wrapped("Partnerfirmen verwenden eigene Logos und Signaturen – diese werden nicht geprüft."));
        content.getChildren().add(section("Vertrauenswürdige Domains", domains));

        // Anhänge
        content.getChildren().add(section("Anhänge", wrapped("Erlaubte Dateitypen: "
                + String.join(", ", CorporateGuidelines.ALLOWED_ATTACHMENT_EXTENSIONS.stream().map(e -> "." + e).toList())
                + ".\nNicht erlaubt: Programme und Skripte (.exe, .bat, .msi, .js, .vbs …), Archive und Disk-Images "
                + "(.zip, .rar, .iso …), Office-Dateien mit Makros (.docm, .xlsm), HTML-Dateien und Doppel-Endungen "
                + "wie .pdf.exe – dort zählt immer die letzte Endung.")));

        // Mitarbeiterverzeichnis mit Suche (Name, Abteilung oder Code)
        FilteredList<Employee> filtered = new FilteredList<>(FXCollections.observableArrayList(CorporateGuidelines.EMPLOYEES));
        SortedList<Employee> sorted = new SortedList<>(filtered);
        TableView<Employee> table = new TableView<>(sorted);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.getColumns().add(column("Name", Employee::name, 170));
        table.getColumns().add(column("Abteilung", Employee::department, 150));
        table.getColumns().add(column("Mitarbeiter-Code", Employee::code, 130));
        table.getColumns().add(column("E-Mail", Employee::email, 240));
        table.setPrefHeight(32 + 12 * 28);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Niemand gefunden – steht die Person wirklich im Verzeichnis?"));

        TextField search = new TextField();
        search.setPromptText("Suchen: Name, Abteilung oder Code");
        search.getStyleClass().add("directory-search");
        search.textProperty().addListener((obs, old, query) -> {
            String q = query == null ? "" : query.trim().toLowerCase();
            filtered.setPredicate(e -> q.isEmpty()
                    || e.name().toLowerCase().contains(q)
                    || e.department().toLowerCase().contains(q)
                    || e.code().toLowerCase().contains(q));
        });
        Label count = new Label(CorporateGuidelines.EMPLOYEES.size() + " Einträge");
        count.getStyleClass().add("muted");
        filtered.predicateProperty().addListener((obs, o, n) -> count.setText(filtered.size() + " Einträge"));
        HBox searchRow = new HBox(10, search, count);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(search, javafx.scene.layout.Priority.ALWAYS);
        content.getChildren().add(section("Mitarbeiterverzeichnis", new VBox(8, searchRow, table)));

        setContent(content);
        setFitToWidth(true);
        getStyleClass().add("plain-scroll");
    }

    private static TableColumn<Employee, String> column(String title, Function<Employee, String> getter, double width) {
        TableColumn<Employee, String> col = new TableColumn<>(title);
        col.setCellValueFactory(c -> new ReadOnlyStringWrapper(getter.apply(c.getValue())));
        col.setPrefWidth(width);
        return col;
    }

    private static VBox section(String heading, javafx.scene.Node body) {
        Label h = new Label(heading);
        h.getStyleClass().add("h2");
        VBox box = new VBox(8, h, body);
        box.getStyleClass().add("section");
        box.setPadding(new Insets(10));
        return box;
    }

    private static Label wrapped(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        return l;
    }
}
