package com.example.phishingforcompliments.logic;

import com.example.phishingforcompliments.model.DayReport;
import com.example.phishingforcompliments.model.DecisionResult;
import com.example.phishingforcompliments.model.Feature;
import com.example.phishingforcompliments.model.GameState;

/** Texte der Chefin: tägliche Anweisung, Verwarnungen, Abschlussbewertung. */
public final class ChefMessages {

    public static final String BOSS_NAME = CorporateGuidelines.BOSS.name();
    public static final String BOSS_TITLE = CorporateGuidelines.BOSS.department();

    private ChefMessages() {
    }

    /** Anweisung zu Tagesbeginn. {@code previous} ist am ersten Tag null. */
    public static String dayBriefing(GameState state, DayReport previous, int mailCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("Guten Morgen ").append(state.playerName()).append(",\n\n");

        if (previous == null) {
            sb.append("willkommen bei der ").append(CorporateGuidelines.COMPANY_NAME)
                    .append("! Sie übernehmen ab heute die Vorprüfung unseres Postfachs. ")
                    .append("Jede eingehende Mail wird von Ihnen geprüft, bevor sie an die Kolleginnen und Kollegen geht.\n\n")
                    .append("So funktioniert es: Öffnen Sie eine Mail, klicken Sie auf ein Merkmal (z.B. den Absender) ")
                    .append("und vergleichen Sie es mit den Corporate Guidelines. Weicht es ab, melden Sie es. ")
                    .append("Ist alles in Ordnung, legen Sie die Mail als sicher ab.\n\n");
        } else {
            sb.append(feedback(previous)).append("\n\n");
        }

        Feature newFeature = Feature.introducedOn(state.day());
        if (newFeature != null) {
            sb.append("NEU AB HEUTE – Merkmal \"").append(newFeature.label()).append("\":\n")
                    .append(newFeature.rule()).append("\n")
                    .append("Die Regel steht ab sofort in den Corporate Guidelines. Achten Sie heute besonders darauf – ")
                    .append("uns liegen Hinweise auf gezielte Angriffe vor, die genau dieses Merkmal fälschen.\n\n");
        }

        sb.append("Heute liegen ").append(mailCount).append(" Mails im Postfach. Bis 17:00 Uhr muss alles ")
                .append("bearbeitet sein – was liegen bleibt, kostet Punkte.");
        if (state.warnings() > 0) {
            sb.append("\n\nZur Erinnerung: Sie haben ").append(state.warnings()).append(" von ")
                    .append(GameState.MAX_WARNINGS).append(" Verwarnungen. Bei ").append(GameState.MAX_WARNINGS)
                    .append(" ist Schluss.");
        }
        sb.append("\n\nMit freundlichen Grüßen\n").append(BOSS_NAME).append("\n").append(BOSS_TITLE);
        return sb.toString();
    }

    private static String feedback(DayReport previous) {
        double acc = previous.accuracy();
        long correct = previous.correctCount();
        int total = previous.results().size();
        if (acc >= 0.9) {
            return "Kompliment! Gestern haben Sie " + correct + " von " + total + " Mails richtig eingeschätzt. "
                    + "Genau so stelle ich mir das vor – weiter so.";
        }
        if (acc >= 0.6) {
            return "Gestern lagen Sie bei " + correct + " von " + total + " Mails richtig. Solide, aber da ist "
                    + "noch Luft nach oben. Nehmen Sie sich für jede Mail die Zeit, die Merkmale wirklich zu vergleichen.";
        }
        return "Gestern lagen Sie nur bei " + correct + " von " + total + " Mails richtig. Das reicht nicht. "
                + "Schauen Sie ins Handbuch und vergleichen Sie jedes Merkmal mit den Guidelines, bevor Sie entscheiden.";
    }

    /** Text der Verwarnung, wenn eine Phishing-Mail als sicher abgelegt wurde. */
    public static String warning(GameState state, DecisionResult result) {
        int w = state.warnings();
        String intro = w >= GameState.MAX_WARNINGS
                ? "Das war Ihre " + w + ". Verwarnung. Ich habe keine Wahl mehr – räumen Sie Ihren Schreibtisch."
                : "Verwarnung " + w + " von " + GameState.MAX_WARNINGS + ".";
        return intro + "\n\nSie haben eine Phishing-Mail als sicher durchgewinkt (\"" + result.email().subject()
                + "\").\n\n" + result.explanation() + "\n\n" + BOSS_NAME;
    }

    /** Abschlussbewertung nach Kampagnenende oder Kündigung. */
    public static String finalVerdict(GameState state, boolean fired) {
        if (fired) {
            return "Hiermit kündige ich das Arbeitsverhältnis mit " + state.playerName() + " fristlos.\n\n"
                    + "Drei durchgewinkte Phishing-Mails sind drei zu viel. Endstand: " + state.score() + " Punkte.\n\n"
                    + "Tipp für den nächsten Job: Absender, Link-Ziele und Signaturen immer mit den Guidelines vergleichen.\n\n"
                    + BOSS_NAME;
        }
        String grade;
        if (state.score() >= 250) {
            grade = "Herausragend. Sie bekommen die Leitung des Security-Awareness-Programms.";
        } else if (state.score() >= 150) {
            grade = "Gut gemacht. Die Probezeit ist bestanden.";
        } else if (state.score() >= 50) {
            grade = "Es ging so. Wir behalten Sie – vorerst.";
        } else {
            grade = "Das war knapp. Bitte wiederholen Sie die Schulung.";
        }
        return "Liebe/r " + state.playerName() + ",\n\ndie erste Woche ist geschafft. Endstand: " + state.score()
                + " Punkte bei " + state.warnings() + " Verwarnung(en).\n\n" + grade + "\n\n" + BOSS_NAME;
    }
}
