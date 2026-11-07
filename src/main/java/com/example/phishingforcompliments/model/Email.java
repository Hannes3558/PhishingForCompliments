package com.example.phishingforcompliments.model;

import java.util.List;

/**
 * Eine E-Mail im Postfach des Spielers. Reines Datenobjekt; ob sie Phishing ist,
 * entscheidet ausschließlich {@code RuleChecker} anhand der Corporate Guidelines.
 *
 * @param senderCompany die Firma, in deren Namen die Mail auftritt (Komplimento AG oder ein Partner) –
 *                      unabhängig davon, ob die Absenderadresse echt ist
 */
public record Email(
        String id,
        String senderName,
        String senderAddress,
        String senderCompany,
        String subject,
        String body,
        List<EmailLink> links,
        LogoSpec logo,
        Signature signature,
        List<Attachment> attachments,
        String receivedAt) {

    public Email {
        links = List.copyOf(links);
        attachments = List.copyOf(attachments);
    }

    public String senderDomain() {
        int at = senderAddress.lastIndexOf('@');
        return at < 0 ? "" : senderAddress.substring(at + 1).toLowerCase();
    }
}
