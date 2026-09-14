package ru.spectra.client.model;
import ru.spectra.client.render.ColorToneScale;
import ru.spectra.client.render.ColorValue;
import ru.spectra.client.type.ConfigOrigin;
import ru.spectra.client.Spectra;
import ru.spectra.client.render.Theme;
import ru.spectra.client.type.ThemeMode;
import ru.spectra.client.render.ThemePalette;

import java.time.Instant;
import java.util.Map;

public class ThemeData {
    public static ThemePalette createDarkPalette() {
        return new ThemePalette(ColorValue.fromHex("8B87FF"), ColorValue.fromHex("A29FFF"), ColorValue.fromHex("FDC95A"), ColorValue.fromHex("0B0B0C"), new ColorToneScale(Map.ofEntries(Map.entry(900, ColorValue.fromHex("323238")), Map.entry(800, ColorValue.fromHex("505058")), Map.entry(700, ColorValue.fromHex("686872")), Map.entry(600, ColorValue.fromHex("888894")), Map.entry(500, ColorValue.fromHex("AAAAB4")), Map.entry(400, ColorValue.fromHex("C0C0C9")), Map.entry(300, ColorValue.fromHex("D0D0D8")), Map.entry(200, ColorValue.fromHex("E5E5EC")), Map.entry(100, ColorValue.fromHex("F0F0F5")), Map.entry(50, ColorValue.fromHex("F6F6FA")))), new ColorToneScale(Map.ofEntries(Map.entry(900, ColorValue.fromHex("ED4561")), Map.entry(500, ColorValue.fromHex("EE5871")), Map.entry(300, ColorValue.fromHex("EF6179")))), new ColorToneScale(Map.ofEntries(Map.entry(700, ColorValue.fromHex("151518")), Map.entry(600, ColorValue.fromHex("1A1A1E")), Map.entry(500, ColorValue.fromHex("202024")), Map.entry(400, ColorValue.fromHex("28282D")), Map.entry(300, ColorValue.fromHex("34343A")), Map.entry(50, ColorValue.fromHex("73737E")))), new ColorToneScale(Map.ofEntries(Map.entry(900, ColorValue.fromHex("080809")), Map.entry(801, ColorValue.fromHex("0A0A0B")), Map.entry(800, ColorValue.fromHex("0D0D0F")), Map.entry(700, ColorValue.fromHex("101012")), Map.entry(600, ColorValue.fromHex("121215")), Map.entry(500, ColorValue.fromHex("17171B")), Map.entry(400, ColorValue.fromHex("1D1D22")), Map.entry(300, ColorValue.fromHex("25252B")), Map.entry(200, ColorValue.fromHex("3A3A42")))));
    }

    public static Theme defaultDark() {
        ThemePalette class764VarMethod001 = createDarkPalette();
        Instant instantNow = Instant.now();
        return Theme.of("spectra-dark", "Spectra", "Spectra", ConfigOrigin.OFFICIAL, ThemeMode.DARK, class764VarMethod001, instantNow, instantNow);
    }
}
