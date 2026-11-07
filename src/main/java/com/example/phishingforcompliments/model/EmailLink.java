package com.example.phishingforcompliments.model;

/** Ein Link im Mailtext: angezeigter Text und tatsächliches Ziel. */
public record EmailLink(String displayText, String url) {

    /**
     * Die echte Ziel-Domain. Berücksichtigt den Trick "https://komplimento.de@boese.ru/" –
     * alles vor dem @ ist nur ein Benutzername, das Ziel ist boese.ru.
     */
    public String host() {
        String authority = authority();
        int at = authority.lastIndexOf('@');
        if (at >= 0) {
            authority = authority.substring(at + 1);
        }
        int colon = authority.indexOf(':');
        if (colon >= 0) {
            authority = authority.substring(0, colon);
        }
        return authority.toLowerCase();
    }

    /** true, wenn vor der eigentlichen Domain ein "user@" steht. */
    public boolean hasUserInfo() {
        return authority().contains("@");
    }

    private String authority() {
        String s = url;
        int schemeIdx = s.indexOf("://");
        if (schemeIdx >= 0) {
            s = s.substring(schemeIdx + 3);
        }
        int slash = s.indexOf('/');
        return slash >= 0 ? s.substring(0, slash) : s;
    }
}
