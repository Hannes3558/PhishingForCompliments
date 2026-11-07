package com.example.phishingforcompliments.logic;

import com.example.phishingforcompliments.logic.MailTemplates.Template;
import com.example.phishingforcompliments.model.Attachment;
import com.example.phishingforcompliments.model.Email;
import com.example.phishingforcompliments.model.EmailLink;
import com.example.phishingforcompliments.model.Employee;
import com.example.phishingforcompliments.model.LogoSpec;
import com.example.phishingforcompliments.model.Signature;

import java.util.List;
import java.util.Random;

/**
 * Veränderbarer Entwurf einer Mail. Entsteht sauber aus einer {@link Template},
 * wird von {@link Manipulations} verfälscht und am Ende zur unveränderlichen {@link Email}.
 */
final class MailDraft {

    String senderName;
    String localPart;
    String address;
    String subject;
    String body;
    String linkText;
    String linkUrl;
    LogoSpec logo;
    Signature signature;
    String attachment;

    /** Domain der Partnerfirma oder null bei internen Mails. */
    final String partnerDomain;
    private final Random rng;

    MailDraft(Template t, Random rng, String playerName, boolean sloppy) {
        this.rng = rng;
        this.subject = t.subject();
        String text = (sloppy && t.sloppyBody() != null) ? t.sloppyBody() : t.body();
        this.body = text.replace("{player}", playerName);
        this.linkText = t.linkText();
        this.linkUrl = t.linkUrl();
        this.attachment = t.attachment();
        this.partnerDomain = t.partnerDomain();
        if (t.isPartner()) {
            this.senderName = t.senderName();
            this.localPart = t.localPart();
            this.address = t.localPart() + "@" + t.partnerDomain();
            this.logo = t.externalLogo();
            this.signature = t.externalSignature();
        } else {
            List<Employee> candidates = CorporateGuidelines.byDepartment(t.department());
            Employee signer = candidates.isEmpty()
                    ? CorporateGuidelines.EMPLOYEES.get(rng.nextInt(CorporateGuidelines.EMPLOYEES.size()))
                    : candidates.get(rng.nextInt(candidates.size()));
            // Persönliche Mails kommen aus dem Postfach der signierenden Person,
            // Funktionspostfächer (it-support@, poststelle@ …) sind in der Vorlage fest vorgegeben.
            this.senderName = t.senderName() != null ? t.senderName() : signer.name();
            this.localPart = t.localPart() != null
                    ? t.localPart() : signer.email().substring(0, signer.email().indexOf('@'));
            this.address = localPart + "@" + CorporateGuidelines.COMPANY_DOMAIN;
            this.logo = CorporateGuidelines.OFFICIAL_LOGO;
            this.signature = new Signature(signer.name(), signer.department(), signer.code());
        }
    }

    boolean isPartner() {
        return partnerDomain != null;
    }

    /** Die echte Domain, die gefälscht werden soll ("komplimento.de" oder die Partnerdomain). */
    String baseDomain() {
        return isPartner() ? partnerDomain : CorporateGuidelines.COMPANY_DOMAIN;
    }

    /** Domain ohne Endung, z.B. "komplimento" oder "nordlicht-logistik". */
    String stem() {
        String d = baseDomain();
        int dot = d.lastIndexOf('.');
        return dot < 0 ? d : d.substring(0, dot);
    }

    /** Subdomain, wie sie echte Links benutzen ("intranet." bzw. "www."). */
    String linkSubdomain() {
        return isPartner() ? "www." : "intranet.";
    }

    /** Pfad des vorhandenen Links ohne führenden Schrägstrich, sonst "login". */
    String linkPath() {
        if (linkUrl == null) {
            return "login";
        }
        String s = linkUrl.substring(linkUrl.indexOf("://") + 3);
        int slash = s.indexOf('/');
        return slash < 0 || slash == s.length() - 1 ? "login" : s.substring(slash + 1);
    }

    void setSenderDomain(String domain) {
        address = localPart + "@" + domain;
    }

    /** Sorgt dafür, dass ein Link existiert, der manipuliert werden kann. */
    void ensureLink() {
        if (linkText == null) {
            body = body + "\n\nAlle Details finden Sie hier: {link}";
            linkText = "Zum Portal";
            linkUrl = "https://" + linkSubdomain() + baseDomain() + "/login";
        }
    }

    /** Sorgt dafür, dass ein Anhang existiert, und liefert dessen Namen ohne Endung. */
    String ensureAttachmentBase() {
        if (attachment == null) {
            body = body + "\n\nDie Unterlagen finden Sie im Anhang.";
            String[] names = {"Unterlagen", "Rechnung", "Details", "Dokument"};
            return names[rng.nextInt(names.length)];
        }
        int dot = attachment.indexOf('.');
        return dot < 0 ? attachment : attachment.substring(0, dot);
    }

    Email toEmail() {
        List<EmailLink> links = linkText == null ? List.of() : List.of(new EmailLink(linkText, linkUrl));
        List<Attachment> attachments = attachment == null
                ? List.of() : List.of(new Attachment(attachment, 40 + rng.nextInt(900)));
        String company = isPartner()
                ? CorporateGuidelines.PARTNER_NAMES.get(partnerDomain) : CorporateGuidelines.COMPANY_NAME;
        return new Email("tmp", senderName, address, company, subject, body, links,
                logo == null ? LogoSpec.NONE : logo,
                signature == null ? Signature.NONE : signature,
                attachments, "00:00");
    }
}
