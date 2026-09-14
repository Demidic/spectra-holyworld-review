package ru.spectra.client.config;

import ru.spectra.client.util.AtomicFileWriter;
import ru.spectra.client.Spectra;
import ru.spectra.client.type.Language;
import ru.spectra.client.type.SurfaceStyle;
import ru.spectra.client.model.PreferencesData;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;

public class LocalPreferencesFile {
    static final Gson gson = new GsonBuilder().setPrettyPrinting().setLenient().create();
    public final Path filePath;
    public PreferencesData data = new PreferencesData();

    public void load() {
        if (Files.exists(this.filePath, new LinkOption[0])) {
            PreferencesData class083Var;
            try {
                class083Var = (PreferencesData) gson.fromJson(Files.readString(this.filePath, StandardCharsets.UTF_8), PreferencesData.class);
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            if (class083Var != null) {
                this.data = class083Var;
                ensureCollections();
                applyLanguage();
                applyDpiScale();
                applyHudScale();
                applyMenuOpenKey();
            }
        }
    }

    public void save() throws IOException {
        ensureCollections();
        AtomicFileWriter.writeBytes(this.filePath, gson.toJson(this.data).getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    public boolean isModuleFavorite(String str) {
        if (str == null) {
            return false;
        }
        ensureCollections();
        return this.data.favoriteModules.contains(str);
    }

    public void setModuleFavorite(String str, boolean z) {
        if (str == null) {
            return;
        }
        ensureCollections();
        if (z) {
            this.data.favoriteModules.add(str);
        } else {
            this.data.favoriteModules.remove(str);
        }
    }

    public boolean isConfigFavorite(String str) {
        if (str == null) {
            return false;
        }
        ensureCollections();
        return this.data.favoriteConfigs.contains(str);
    }

    public void setConfigFavorite(String str, boolean z) {
        if (str == null) {
            return;
        }
        ensureCollections();
        if (z) {
            this.data.favoriteConfigs.add(str);
        } else {
            this.data.favoriteConfigs.remove(str);
        }
    }

    public void setLanguage(Language class313Var) {
        if (class313Var == null) {
            return;
        }
        this.data.language = class313Var.name();
    }

    public void setDpiScale(boolean z, float f) {
        this.data.autoDpiScale = Boolean.valueOf(z);
        this.data.dpiScaleFactor = Float.valueOf(f);
    }

    public void setHudScale(float scale) {
        this.data.hudScaleFactor = scale;
    }

    public void setMenuOpenKey(int key) {
        this.data.menuOpenKey = key;
    }

    public boolean isMenuDimBackgroundEnabled() {
        return this.data.menuDimBackground == null || this.data.menuDimBackground;
    }

    public void setMenuDimBackgroundEnabled(boolean enabled) {
        this.data.menuDimBackground = enabled;
    }

    public boolean isMenuBlurBackgroundEnabled() {
        return Boolean.TRUE.equals(this.data.menuBlurBackground);
    }

    public void setMenuBlurBackgroundEnabled(boolean enabled) {
        this.data.menuBlurBackground = enabled;
    }

    public SurfaceStyle hudSurfaceStyle() {
        return surfaceStyle(this.data.hudSurfaceStyle, SurfaceStyle.BLURRED);
    }

    public void setHudSurfaceStyle(SurfaceStyle style) {
        if (style != null) {
            this.data.hudSurfaceStyle = style.name();
        }
    }

    public SurfaceStyle menuSurfaceStyle() {
        return surfaceStyle(this.data.menuSurfaceStyle, SurfaceStyle.BLURRED);
    }

    public void setMenuSurfaceStyle(SurfaceStyle style) {
        if (style != null) {
            this.data.menuSurfaceStyle = style.name();
        }
    }

    public boolean isConfigAutoLoadEnabled() {
        return this.data.configAutoLoad == null || this.data.configAutoLoad;
    }

    public void setConfigAutoLoadEnabled(boolean enabled) {
        this.data.configAutoLoad = enabled;
    }

    public void applyLanguage() {
        if (this.data.language == null || this.data.language.isBlank()) {
            return;
        }
        try {
            Spectra.INSTANCE.languages().language(Language.valueOf(this.data.language));
        } catch (IllegalArgumentException e) {
            Spectra.LOGGER.warn("Unknown saved language: {}", this.data.language);
        }
    }

    public void applyDpiScale() {
        if (this.data.autoDpiScale == null && this.data.dpiScaleFactor == null) {
            return;
        }
        if (Boolean.TRUE.equals(this.data.autoDpiScale)) {
            Spectra.INSTANCE.windowController().enableAutoDpiScale();
        } else if (this.data.dpiScaleFactor != null) {
            Spectra.INSTANCE.windowController().setManualDpiScaleFactor(this.data.dpiScaleFactor.floatValue());
        }
    }

    public void applyHudScale() {
        if (this.data.hudScaleFactor != null) {
            ru.spectra.client.ui.WidgetStack.setHudScale(this.data.hudScaleFactor);
        }
    }

    public void applyMenuOpenKey() {
        if (this.data.menuOpenKey != null && Spectra.INSTANCE.menuWindow() != null) {
            Spectra.INSTANCE.menuWindow().setOpenKey(this.data.menuOpenKey);
        }
    }

    public boolean isAutoSaveDisabled(String str) {
        if (str == null) {
            return false;
        }
        ensureCollections();
        return this.data.autoSaveDisabled.contains(str);
    }

    public void setAutoSaveDisabled(String str, boolean z) {
        if (str == null) {
            return;
        }
        ensureCollections();
        if (z) {
            this.data.autoSaveDisabled.add(str);
        } else {
            this.data.autoSaveDisabled.remove(str);
        }
    }

    public void ensureCollections() {
        if (this.data.favoriteModules == null) {
            this.data.favoriteModules = new HashSet();
        }
        if (this.data.favoriteConfigs == null) {
            this.data.favoriteConfigs = new HashSet();
        }
        if (this.data.autoSaveDisabled == null) {
            this.data.autoSaveDisabled = new HashSet();
        }
    }

    private static SurfaceStyle surfaceStyle(String value, SurfaceStyle fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return SurfaceStyle.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    public LocalPreferencesFile(Path path) {
        this.filePath = path;
    }
}
