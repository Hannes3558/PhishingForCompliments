package com.example.phishingforcompliments.logic;

import com.example.phishingforcompliments.logic.MailTemplates.Template;
import com.example.phishingforcompliments.model.Difficulty;
import com.example.phishingforcompliments.model.Email;
import com.example.phishingforcompliments.model.Feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Erzeugt das Postfach für einen Tag. Deterministisch über Seed + Tag, damit ein
 * geladener Spielstand dieselben Mails zeigt.
 *
 * Phishing-Mails entstehen aus sauberen Vorlagen, in die je nach Schwierigkeit
 * 1–3 Fehler aus dem {@link Manipulations}-Katalog eingebaut werden – ausschließlich
 * bei Merkmalen, deren Regel an diesem Tag schon gilt. An dem Tag, an dem ein Merkmal
 * neu eingeführt wird, enthalten mindestens {@link #NEW_FEATURE_MIN_MAILS} Mails genau
 * diesen einen Fehler, damit die neue Regel wirklich geübt wird.
 *
 * Ob eine Mail Phishing ist, entscheidet danach immer der {@link RuleChecker}.
 */
public final class MailGenerator {

    /** Mindestanzahl Mails, deren einziger Fehler das an diesem Tag neue Merkmal ist. */
    public static final int NEW_FEATURE_MIN_MAILS = 2;

    private final RuleChecker checker = new RuleChecker();

    /** Anzahl der Phishing-Mails an einem Tag. */
    public static int phishingCount(Difficulty difficulty, int day) {
        int total = difficulty.mailsForDay(day);
        int min = Feature.introducedOn(day) != null ? NEW_FEATURE_MIN_MAILS : 1;
        int count = (int) Math.round(total * difficulty.phishingShare());
        return Math.max(min, Math.min(total - 1, count));
    }

    public List<Email> generate(Difficulty difficulty, int day, long seed, String playerName) {
        Random rng = new Random(seed * 1_000_003L + day * 7919L);
        int total = difficulty.mailsForDay(day);
        int phishingCount = phishingCount(difficulty, day);
        int legitCount = total - phishingCount;
        Set<String> usedTemplates = new HashSet<>();

        List<Email> result = new ArrayList<>();

        // Legitime Mails: Mischung aus internen und Partner-Mails
        List<Template> legitPool = new ArrayList<>(MailTemplates.LEGIT_INTERNAL);
        legitPool.addAll(MailTemplates.LEGIT_PARTNER);
        for (int i = 0; i < legitCount; i++) {
            Template t = pickTemplate(legitPool, usedTemplates, null, rng);
            result.add(new MailDraft(t, rng, playerName, false).toEmail());
        }

        // Phishing-Mails
        Feature newFeature = Feature.introducedOn(day);
        List<Feature> active = Feature.activeFeatures(day);
        for (int i = 0; i < phishingCount; i++) {
            boolean focusOnNewFeature = newFeature != null && i < NEW_FEATURE_MIN_MAILS;
            Template t = phishingBase(difficulty, focusOnNewFeature ? newFeature : null, usedTemplates, rng);
            boolean sloppy = rng.nextDouble() >= difficulty.subtleShare();
            MailDraft draft = new MailDraft(t, rng, playerName, sloppy);

            List<Feature> features = focusOnNewFeature
                    ? List.of(newFeature)
                    : chooseFeatures(difficulty, active, draft.isPartner(), rng);
            for (Feature f : features) {
                Manipulations.apply(draft, f, rng.nextDouble() < difficulty.subtleShare(), rng);
            }

            Email email = draft.toEmail();
            if (!checker.isPhishing(email, day)) {
                // Sicherheitsnetz: Absender ist ab Tag 1 immer prüfbar.
                Manipulations.apply(draft, Feature.ABSENDER, false, rng);
                email = draft.toEmail();
            }
            result.add(email);
        }

        Collections.shuffle(result, rng);
        return assignIdsAndTimes(result, day, rng);
    }

    private List<Feature> chooseFeatures(Difficulty difficulty, List<Feature> active, boolean partner, Random rng) {
        List<Feature> applicable = new ArrayList<>(active.stream()
                .filter(f -> Manipulations.appliesTo(f, partner)).toList());
        Collections.shuffle(applicable, rng);
        int n = difficulty.minManipulations()
                + rng.nextInt(difficulty.maxManipulations() - difficulty.minManipulations() + 1);
        n = Math.max(1, Math.min(applicable.size(), n));
        return applicable.subList(0, n);
    }

    /**
     * Basis für eine Phishing-Mail: meist ein typischer Köder (Druck, Geld, Passwort).
     * Je subtiler die Schwierigkeit, desto öfter eine völlig harmlos klingende Vorlage –
     * dann verrät der Text nichts und nur die Merkmale zählen.
     */
    private Template phishingBase(Difficulty difficulty, Feature mustSupport, Set<String> used, Random rng) {
        boolean innocent = rng.nextDouble() < difficulty.subtleShare() * 0.5;
        List<Template> pool = new ArrayList<>();
        if (innocent) {
            pool.addAll(MailTemplates.LEGIT_INTERNAL);
            pool.addAll(MailTemplates.LEGIT_PARTNER);
        } else {
            pool.addAll(MailTemplates.LURES);
            pool.addAll(MailTemplates.PARTNER_LURES);
        }
        return pickTemplate(pool, used, mustSupport, rng);
    }

    /** Wählt eine Vorlage, die heute noch nicht benutzt wurde (falls möglich). */
    private Template pickTemplate(List<Template> pool, Set<String> used, Feature mustSupport, Random rng) {
        List<Template> candidates = pool.stream()
                .filter(t -> mustSupport == null || Manipulations.appliesTo(mustSupport, t.isPartner()))
                .toList();
        List<Template> unused = candidates.stream().filter(t -> !used.contains(t.key())).toList();
        List<Template> choice = unused.isEmpty() ? candidates : unused;
        Template t = choice.get(rng.nextInt(choice.size()));
        used.add(t.key());
        return t;
    }

    private List<Email> assignIdsAndTimes(List<Email> mails, int day, Random rng) {
        List<Integer> minutes = new ArrayList<>();
        for (int i = 0; i < mails.size(); i++) {
            minutes.add(8 * 60 + 30 + rng.nextInt(3 * 60));
        }
        Collections.sort(minutes);
        List<Email> out = new ArrayList<>();
        for (int i = 0; i < mails.size(); i++) {
            Email m = mails.get(i);
            String time = String.format("%02d:%02d", minutes.get(i) / 60, minutes.get(i) % 60);
            out.add(new Email("t" + day + "-" + (i + 1), m.senderName(), m.senderAddress(), m.senderCompany(),
                    m.subject(), m.body(),
                    m.links(), m.logo(), m.signature(), m.attachments(), time));
        }
        return out;
    }
}
