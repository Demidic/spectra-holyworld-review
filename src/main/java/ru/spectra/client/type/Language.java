package ru.spectra.client.type;

import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.model.Translation;

public enum Language implements DisplayNamed {
    en_US("en_US.json", "English"),
    ru_RU("ru_RU.json", "\u0420\u0443\u0441\u0441\u043A\u0438\u0439");

    public final String source;
    public final String canonical;
    public static final Language PRIMARY = en_US;

    Language(String str, String str2) {
        this.source = str;
        this.canonical = str2;
    }

    public String source() {
        return this.source;
    }

    public String canonical() {
        return this.canonical;
    }

    @Override
    public Translation getDisplayName() {
        return Translation.clearText(this.canonical);
    }
}

