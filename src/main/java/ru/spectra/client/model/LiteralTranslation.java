package ru.spectra.client.model;
import ru.spectra.client.util.StringLookup;
import ru.spectra.client.util.StringUtil;
import ru.spectra.client.util.ClientLocalization;

public class LiteralTranslation implements Translation {
    String firstLetterUppercase;
    final String original;

    LiteralTranslation(String str) {
        this.original = str;
    }

    @Override
    public void lookupFromDictionary(StringLookup class045Var) {
        this.firstLetterUppercase = StringUtil.firstLetterUppercase(this.original);
    }

    @Override
    public String original() {
        return this.original;
    }

    @Override
    public String effective() {
        return ClientLocalization.literal(this.original);
    }

    @Override
    public String firstLetterUppercase() {
        return StringUtil.firstLetterUppercase(effective());
    }
}

