package ru.spectra.client.module;

import ru.spectra.client.Spectra;
import ru.spectra.client.Lang;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.event.PlayerTickEvent;
import ru.spectra.client.event.Render2DEvent;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.render.ScreenResolution;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.util.DrawEngine;
import ru.spectra.client.util.IteratorUtil;
import ru.spectra.client.util.MineHelperCountdown;
import ru.spectra.client.util.StencilBufferUtil;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.ArmorStandEntity;

/**
 * Read-only mine schedule display. It does not automate inventory, mining,
 * rotation, item switching, or block breaking.
 */
@Aliases(aliases = {"Fun Time", "Mine Helper"})
public class MineHelperModule extends Module {
    private static final Pattern TIME_PATTERN = Pattern.compile("\\d{2}:\\d{2}");

    private final Mc mc = Mc.INSTANCE;
    private MineHelperCountdown countdown;

    public MineHelperModule() {
        super(ModuleTab.MISC, "Mine Helper");

        register(Render2DEvent.class, event -> {
            if (!isState() || !this.mc.isWorldLoaded() || this.countdown == null) {
                return;
            }
            MatrixStack matrices = event.matrixStack();
            DrawEngine draw = Spectra.INSTANCE.drawEngine();
            ColorStack colors = draw.colorStack();
            int screenWidth = ScreenResolution.resolution().screenWidth();
            MsdfFont font = Fonts.INTER_MEDIUM.get();
            float lineHeight = font.getHeight(14.0f);
            float y = this.mc.getInGameHud().getBossBarHud().bossBars.size() * 45.0f;

            draw.begin();
            draw.drawLine(
                    matrices, font, 14, screenWidth, y,
                    Lang.MINE_HELPER_NEXT_MINE_LABEL.effective(),
                    this.countdown.nextType(),
                    colors.computeColor(230, 230, 230, StencilBufferUtil.STENCIL_MASK),
                    colors.computeColor(getRarityColor(this.countdown.nextType()))
            );
            draw.drawLine(
                    matrices, font, 14, screenWidth, y + lineHeight,
                    Lang.MINE_HELPER_TIME_LEFT_LABEL.effective(),
                    this.countdown.formattedTime(),
                    colors.computeColor(230, 230, 230, StencilBufferUtil.STENCIL_MASK),
                    colors.computeColor(getTimeLeftColor(this.countdown.getSecondsLeft()))
            );
            draw.end();
        });

        register(PlayerTickEvent.class, event -> {
            if (!isState() || !this.mc.isWorldLoaded() || !event.isPre()) {
                return;
            }
            if (this.countdown != null && this.countdown.isExpired()) {
                this.countdown = null;
            }
            if (this.countdown != null) {
                return;
            }
            List<ArmorStandEntity> labels = IteratorUtil.toList(this.mc.getWorld().getEntities().iterator())
                    .stream()
                    .filter(ArmorStandEntity.class::isInstance)
                    .map(ArmorStandEntity.class::cast)
                    .filter(stand -> {
                        String name = getArmorStandName(stand);
                        if (name.contains("\u0410\u0432\u0442\u043e-\u0428\u0430\u0445\u0442\u0430")
                                || name.contains("\u0421\u043b\u0435\u0434\u0443\u044e\u0449\u0430\u044f:")
                                || name.contains("\u041e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u0435 \u0447\u0435\u0440\u0435\u0437:")) {
                            return true;
                        }
                        return name.contains("Авто-Шахта")
                                || name.contains("Следующая:")
                                || name.contains("Обновление через:")
                                || TIME_PATTERN.matcher(name).matches();
                    })
                    .sorted(Comparator.comparingDouble(stand -> -stand.getY()))
                    .toList();
            parseCountdown(labels).ifPresent(value -> this.countdown = value);
        });
    }

    private Optional<MineHelperCountdown> parseCountdown(List<ArmorStandEntity> labels) {
        if (labels.size() < 4) {
            return Optional.empty();
        }
        String title = getArmorStandName(labels.get(0));
        String next = getArmorStandName(labels.get(1));
        String update = getArmorStandName(labels.get(2));
        String time = getArmorStandName(labels.get(3));
        if (title.contains("\u0410\u0432\u0442\u043e-\u0428\u0430\u0445\u0442\u0430")
                && next.contains("\u0421\u043b\u0435\u0434\u0443\u044e\u0449\u0430\u044f:")
                && update.contains("\u041e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u0435 \u0447\u0435\u0440\u0435\u0437:")
                && TIME_PATTERN.matcher(time).matches()) {
            String[] localizedParts = time.split(":");
            return Optional.of(new MineHelperCountdown(
                    next.replace("\u0421\u043b\u0435\u0434\u0443\u044e\u0449\u0430\u044f:", "").trim(),
                    Integer.parseInt(localizedParts[0]) * 60 + Integer.parseInt(localizedParts[1]),
                    System.currentTimeMillis()
            ));
        }
        if (!title.contains("Авто-Шахта")
                || !next.contains("Следующая:")
                || !update.contains("Обновление через:")
                || !TIME_PATTERN.matcher(time).matches()) {
            return Optional.empty();
        }
        String[] parts = time.split(":");
        if (parts.length != 2) {
            return Optional.empty();
        }
        return Optional.of(new MineHelperCountdown(
                next.replace("Следующая:", "").trim(),
                Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]),
                System.currentTimeMillis()
        ));
    }

    private static String getArmorStandName(ArmorStandEntity stand) {
        return stand.getCustomName() == null ? "" : stand.getCustomName().getString();
    }

    private static int getTimeLeftColor(int seconds) {
        if (seconds <= 10) {
            return -43691;
        }
        return seconds <= 60 ? -171 : -11141291;
    }

    private static int getRarityColor(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("\u043c\u0438\u0444\u0438\u0447\u0435\u0441")) {
            return -5614081;
        }
        if (lower.contains("\u043b\u0435\u0433\u0435\u043d\u0434\u0430\u0440\u043d")) {
            return -22016;
        }
        if (lower.contains("мифическ")) {
            return -5614081;
        }
        return lower.contains("легендарн") ? -22016 : -5592406;
    }
}
