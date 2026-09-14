package ru.spectra.client.render;

import com.google.common.base.Suppliers;
import java.util.function.Supplier;

public class Fonts {
    /** Called only during render-thread startup, before the first interactive frame. */
    public static void preloadTextures() {
        MENU_ICON.get().getTextureId();
        INTER_SEMIBOLD.get().getTextureId();
        INTER_BOLD.get().getTextureId();
        INTER_MEDIUM.get().getTextureId();
        INTER_EXTRA_BOLD.get().getTextureId();
    }

    public static final Supplier<MsdfFont> MENU_ICON = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("menuicon").data("menuicon").build();
    });
    public static final Supplier<MsdfFont> INTER_SEMIBOLD = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("inter-semi").data("inter-semi").build();
    });
    public static final Supplier<MsdfFont> INTER_BOLD = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("inter-bold").data("inter-bold").build();
    });
    public static final Supplier<MsdfFont> INTER_MEDIUM = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("inter-medium").data("inter-medium").build();
    });
    public static final Supplier<MsdfFont> INTER_EXTRA_BOLD = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("inter-extrabold").data("inter-extrabold").build();
    });
}
