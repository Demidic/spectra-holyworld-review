package ru.spectra.client.model;
import ru.spectra.client.render.FontMetrics;
import ru.spectra.client.render.KerningPair;
import ru.spectra.client.config.SpriteRenderConfig;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FontData {

    @SerializedName("atlas")
    public SpriteRenderConfig atlas;

    @SerializedName("metrics")
    public FontMetrics metrics;

    @SerializedName("glyphs")
    public List<GlyphData> glyphs;

    @SerializedName("kerning")
    public List<KerningPair> kernings;

    public SpriteRenderConfig atlas() {
        return this.atlas;
    }

    public FontMetrics metrics() {
        return this.metrics;
    }

    public List<GlyphData> glyphs() {
        return this.glyphs;
    }

    public List<KerningPair> kernings() {
        return this.kernings;
    }
}
