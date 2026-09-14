package ru.spectra.client.ui;
import ru.spectra.client.render.AnimatedFloat;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.model.DragRenderContext;
import ru.spectra.client.util.DrawEngine;
import ru.spectra.client.math.Easings;
import ru.spectra.client.ui.setting.ExpandableSetting;
import ru.spectra.client.Spectra;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.type.HotkeyCategory;
import ru.spectra.client.model.HotkeyEntry;
import ru.spectra.client.util.KeyboardUtil;
import ru.spectra.client.Lang;
import ru.spectra.client.type.Mc;
import ru.spectra.client.type.ModuleCategory;
import ru.spectra.client.module.Module;
import ru.spectra.client.model.MouseButtonInput2;
import ru.spectra.client.model.MouseMoveInput;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.ui.setting.MultiSelectSetting;
import ru.spectra.client.ui.setting.Setting;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.util.WeightedEngine;
import ru.spectra.client.util.ClientLocalization;
import ru.spectra.client.model.WidgetBounds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.client.util.math.MatrixStack;

public class HotkeysWidget extends Draggable {
    private static final float PANEL_WIDTH = 160.0f;
    private static final float ROW_START = 34.0f;
    private static final float ROW_HEIGHT = 20.0f;
    private static final float BOTTOM_PADDING = 8.0f;
    public final MsdfFont semiBoldFont;
    public final MsdfFont boldFont;
    public final GlTexture starsTexture;
    public final GlTexture buttonsIcon;
    public final MsdfFont menuIconFont;
    public final AnimatedFloat opacityAnimation;
    private final AnimatedFloat[] previewRowAnimations;
    public final List<HotkeyEntry> entries;
    public final Map<String, HotkeyEntry> entryMap;
    private List<DisplayRow> frameRows = List.of();
    public final WidgetBounds headerBounds;
    public final MultiSelectSetting<HotkeyCategory> hiddenCategoriesSetting;
    public float width;
    public float height;

    public HotkeysWidget(BooleanSupplier booleanSupplier) {
        super("Hotkeys", booleanSupplier);
        this.semiBoldFont = Fonts.INTER_SEMIBOLD.get();
        this.boldFont = Fonts.INTER_EXTRA_BOLD.get();
        this.starsTexture = new GlTexture(new ClasspathResource("/textures/stars.png"));
        this.buttonsIcon = new GlTexture(new ClasspathResource("/icons/menu/new/keyboard.png"));
        this.menuIconFont = Fonts.MENU_ICON.get();
        this.opacityAnimation = new AnimatedFloat(200, Easings.LINEAR);
        this.previewRowAnimations = new AnimatedFloat[] {
                new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC),
                new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC),
                new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC)
        };
        this.entries = new ArrayList();
        this.entryMap = new HashMap();
        this.headerBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 19.0f);
        this.hiddenCategoriesSetting = new MultiSelectSetting(Lang.WIDGET_HOTKEYS_HIDDEN_CATEGORIES).values(HotkeyCategory.class);
        this.width = PANEL_WIDTH;
        this.height = 206.0f;
        this.x = 30.0f;
        this.y = 228.0f;
    }

    @Override
    public float width() {
        return this.width;
    }

    @Override
    public float height() {
        return this.height;
    }

    @Override
    public boolean click(MouseButtonInput2 class807Var, boolean z) {
        return false;
    }

    @Override
    public boolean cursor(MouseMoveInput class808Var, boolean z) {
        return z && !class808Var.intercepted();
    }

    @Override
    public void layout(DragRenderContext class809Var) {
        if (isVisible()) {
            this.width = PANEL_WIDTH;
            float contentHeight = BOTTOM_PADDING;
            this.frameRows = displayRows();
            for (DisplayRow row : this.frameRows) {
                float progress = row.progress();
                if (progress <= 0.01f) {
                    continue;
                }
                contentHeight += ROW_HEIGHT * progress;
            }
            this.height = ROW_START + contentHeight;
            this.headerBounds.withSize(this.width - 18.0f, 19.0f)
                    .withPosition(this.x + 9.0f, this.y);
        }
    }

    @Override
    public void render(DragRenderContext class809Var) {
        if (!isVisible() || this.opacityAnimation.isZero()) {
            return;
        }
        DrawEngine class154VarDrawEngine = class809Var.drawEngine();
        MatrixStack matrixStack = class809Var.matrixStack();
        ColorStack class115VarColorStack = class154VarDrawEngine.colorStack();
        int clientAccent = class809Var.theme().palette().accent().argb();
        float fAnimatedValue = this.opacityAnimation.animatedValue();
        class115VarColorStack.push();
        class115VarColorStack.alpha(fAnimatedValue);
        SpectraHudStyle.panel(class154VarDrawEngine, matrixStack, this.x, this.y, this.width, this.height, 8.0f);
        SpectraHudStyle.header(
                class154VarDrawEngine, matrixStack,
                this.semiBoldFont, "Keybinds", this.menuIconFont, "\u041B",
                this.x, this.y, this.width,
                class115VarColorStack.computeColor(SpectraHudStyle.TEXT),
                class115VarColorStack.computeColor(0xFFFFFFFF)
        );

        float rowY = this.y + ROW_START;
        for (DisplayRow row : this.frameRows) {
            float rowAlpha = row.progress();
            if (rowAlpha <= 0.01f) {
                continue;
            }
            class115VarColorStack.push();
            class115VarColorStack.alpha(rowAlpha);
            float keyWidth = this.boldFont.getWidth(row.key(), 10.0f) + 14.0f;
            float keyX = this.x + this.width - 9.0f - keyWidth;
            float animatedRowY = rowY + (1.0f - rowAlpha) * 6.0f;
            float keyY = animatedRowY + 2.0f;
            if (row.category() != null) {
                class154VarDrawEngine.msdfFontVerticalC(
                        matrixStack.peek().getPositionMatrix(),
                        this.menuIconFont,
                        menuSectionGlyph(row.category()),
                        this.x + 9.0f,
                        animatedRowY + 10.5f,
                        12.0f,
                        0.05f,
                        class115VarColorStack.computeColor(clientAccent)
                );
            }
            class154VarDrawEngine.msdfFont(matrixStack.peek().getPositionMatrix(), this.semiBoldFont, row.name(), this.x + 27.0f, animatedRowY + 4.0f, 12.0f, 0.05f, class115VarColorStack.computeColor(SpectraHudStyle.TEXT));
            SpectraHudStyle.outlinedRect(class154VarDrawEngine, matrixStack,
                    keyX, keyY, keyWidth, 17.0f, 6.0f,
                    class115VarColorStack.computeColor(SpectraHudStyle.BORDER),
                    SpectraHudStyle.panelColor(class115VarColorStack, SpectraHudStyle.PANEL_DARK));
            class154VarDrawEngine.msdfFont(matrixStack.peek().getPositionMatrix(), this.boldFont, row.key(), keyX + 7.0f, keyY + 8.5f - this.boldFont.getHeight(10.0f) / 2.0f, 10.0f, 0.05f, class115VarColorStack.computeColor(SpectraHudStyle.MUTED));
            class115VarColorStack.pop();
            rowY += ROW_HEIGHT * rowAlpha;
        }
        class115VarColorStack.pop();
    }

    private List<DisplayRow> displayRows() {
        ArrayList<DisplayRow> rows = new ArrayList<>();
        for (HotkeyEntry entry : this.entries) {
            float progress = normalizedProgress(entry.anim);
            if (progress > 0.01f) {
                rows.add(new DisplayRow(entry.category, entry.name, KeyboardUtil.formatCombination(entry.keys), progress));
            }
        }
        if (rows.isEmpty()) {
            ModuleCategory[] categories = {
                    ModuleCategory.INTERFACE,
                    ModuleCategory.VISUALIZATION,
                    ModuleCategory.UTILITIES
            };
            long tick = previewTick();
            int first = Math.floorMod((int) tick, categories.length);
            String[] keys = {"R", "G", "V"};
            for (int slot = 0; slot < this.previewRowAnimations.length; slot++) {
                float progress = this.previewRowAnimations[slot].animatedValue();
                if (progress > 0.01f) {
                    rows.add(new DisplayRow(categories[(first + slot) % categories.length],
                            ClientLocalization.text("Example Keybind", "\u041F\u0440\u0438\u043C\u0435\u0440 \u0431\u0438\u043D\u0434\u0430"), keys[slot], progress));
                }
            }
        }
        return rows;
    }

    private static float normalizedProgress(ru.spectra.client.render.ToggleAnimator animator) {
        return Math.min(1.0f, animator.smoothAnimation() / animator.maxValueScale);
    }

    private record DisplayRow(ModuleCategory category, String name, String key, float progress) {
    }

    /** Uses the same three icons as the Display, Visuals and Utilities menu sections. */
    private static String menuSectionGlyph(ModuleCategory category) {
        return switch (category) {
            case INTERFACE -> "j";
            case VISUALIZATION, WORLD -> "q";
            default -> "W";
        };
    }

    private static String categoryGlyph(ModuleCategory category) {
        return switch (category) {
            case ATTACK -> "\u0445";
            case DEFENSE -> "\u0410";
            case AUTOMATION -> "\u044B";
            case EXPLOITS -> "\u0431";
            case BOOST -> "\u0422";
            case ANTI_LIMITS -> "N";
            case CONVENIENCE -> "\u0436";
            case VISUALIZATION -> "q";
            case INTERFACE -> "j";
            case WORLD -> "B";
            case UTILITIES -> "D";
        };
    }

    @Override
    public void animate(WeightedEngine class141Var) {
        if (Mc.INSTANCE.getPlayer() != null) {
            this.entries.forEach(class640Var -> class640Var.anim.animate(class141Var));
        }
        boolean chatOpen = HudEditorScreen.isEditing();
        boolean realRowsVisible = this.entries.stream().anyMatch(entry -> !entry.anim.isZero());
        int previewCount = chatOpen && !realRowsVisible ? previewRowCount() : 0;
        for (int slot = 0; slot < this.previewRowAnimations.length; slot++) {
            this.previewRowAnimations[slot].destination(slot < previewCount ? 1.0f : 0.0f).animate(class141Var);
        }
        if (chatOpen) {
            this.opacityAnimation.destination(1.0f);
        }
        this.opacityAnimation.animate(class141Var);
    }

    @Override
    protected boolean isContentVisible() {
        return this.opacityAnimation.destination() > 0.5f;
    }

    @Override
    public void update() {
        if (isVisible()) {
            HashSet hashSet = new HashSet();
            boolean zMethod007 = false;
            for (Module class605Var : Spectra.INSTANCE.moduleRepository().getModules()) {
                HotkeyCategory class641VarMethod002 = HotkeyCategory.fromTab(class605Var.getModuleTab());
                if (class605Var.isVisibleInMenu() && class605Var.hasKeyBind()) {
                    zMethod007 |= updateEntry(hashSet, "m:" + class605Var.getName(), () -> {
                        return new HotkeyEntry(class605Var, class605Var.getName(), class605Var.getKeyBind());
                    }, class605Var.getKeyBind(), class605Var.isState());
                }
            }
            this.entryMap.keySet().retainAll(hashSet);
            this.entries.clear();
            Stream<HotkeyEntry> streamSorted = this.entryMap.values().stream().sorted(Comparator.comparing(class640Var -> {
                return class640Var.name;
            }, String.CASE_INSENSITIVE_ORDER));
            List<HotkeyEntry> list = this.entries;
            Objects.requireNonNull(list);
            streamSorted.forEach((v1) -> {
                list.add(v1);
            });
            this.opacityAnimation.destination(((this.entries.isEmpty() || !zMethod007) && !HudEditorScreen.isEditing()) ? 0.0f : 1.0f);
        }
    }

    private static int previewRowCount() {
        int[] pattern = {1, 2, 3, 1, 2, 1, 3};
        return pattern[Math.floorMod((int) previewTick(), pattern.length)];
    }

    private static long previewTick() {
        return Math.floorDiv(System.currentTimeMillis() + 137L, 1000L);
    }

    public boolean updateEntry(Set<String> set, String str, Supplier<HotkeyEntry> supplier, List<Integer> list, boolean z) {
        set.add(str);
        HotkeyEntry class640VarComputeIfAbsent = this.entryMap.computeIfAbsent(str, str2 -> {
            return (HotkeyEntry) supplier.get();
        });
        class640VarComputeIfAbsent.keys.clear();
        if (list != null) {
            class640VarComputeIfAbsent.keys.addAll(list);
        }
        class640VarComputeIfAbsent.anim.state(z);
        return z;
    }
}
