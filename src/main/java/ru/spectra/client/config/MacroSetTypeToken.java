package ru.spectra.client.config;
import ru.spectra.client.model.MacroEntry;

import com.google.gson.reflect.TypeToken;
import java.util.Set;

public class MacroSetTypeToken extends TypeToken<Set<MacroEntry>> {
    MacroSetTypeToken() {
    }
}
