package ru.spectra.client.module;

import ru.spectra.client.Spectra;
import ru.spectra.client.Lang;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.event.ClientTickEvent;
import ru.spectra.client.event.MouseButtonEvent2;
import ru.spectra.client.event.Render2DEvent;
import ru.spectra.client.event.StatusEffectOverlayEvent;
import ru.spectra.client.event.WindowResizeEvent;
import ru.spectra.client.type.HudWidgetType;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.Draggable;
import ru.spectra.client.ui.HudEditorScreen;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.WatermarkWidget;
import ru.spectra.client.ui.WidgetStack;
import ru.spectra.client.ui.setting.MultiSelectSetting;

/**
 * Hidden HUD controller and compatibility bridge for old configs.
 *
 * Each visible HUD element now has its own Display module in {@link HudModules}.
 * Keeping this class avoids breaking old "Widgets" configs and gives the widget
 * stack one central render/input listener.
 */
@Aliases(aliases = {"HUD", "Widgets", "Overlay"})
public class WidgetsModule extends Module {
    public final MultiSelectSetting<HudWidgetType> elements;
    private boolean legacyConfigChecked;
    private boolean spectraHudLayoutMigrated;

    public WidgetsModule() {
        super(ModuleTab.RENDER, "Widgets");
        this.elements = new MultiSelectSetting(Lang.WIDGETS_ELEMENTS, Lang.WIDGETS_ELEMENTS_DESC)
                .values(HudWidgetType.class);
        addSettings(this.elements);

        WidgetStack stack = Spectra.INSTANCE.widgetStack();
        register(Render2DEvent.class, event -> {
            if (Mc.INSTANCE.isWorldLoaded()
                    && event.isPost()
                    && !HudEditorScreen.isAdvancedOpen()
                    && stack.widgets().stream().anyMatch(Draggable::isVisible)) {
                stack.draw();
            }
        });
        register(ClientTickEvent.class, event -> {
            if (Mc.INSTANCE.isWorldLoaded()) {
                migrateLegacyConfig(stack);
                stack.update();
            }
        });
        register(WindowResizeEvent.class, event -> {
            stack.handleResize(event.width(), event.height());
        });
        register(MouseButtonEvent2.class, event -> {
            if (HudEditorScreen.isEditing()) {
                stack.click(event);
            }
        });
        register(StatusEffectOverlayEvent.class, event -> Spectra.INSTANCE.moduleRepository()
                .find(HudModules.ActiveEffects.class)
                .filter(Module::isState)
                .ifPresent(module -> event.cancel()));
    }

    public MultiSelectSetting<HudWidgetType> getElements() {
        return this.elements;
    }

    private void migrateLegacyConfig(WidgetStack stack) {
        if (this.legacyConfigChecked || stack.resolution == null) {
            return;
        }
        this.legacyConfigChecked = true;
        if (!isState()) {
            return;
        }

        setModuleState(HudModules.Watermark.class, HudWidgetType.WATERMARK);
        setModuleState(HudModules.ActiveEffects.class, HudWidgetType.POTION_LIST);
        setModuleState(HudModules.Keybinds.class, HudWidgetType.HOTKEYS);
        setModuleState(HudModules.TargetHud.class, HudWidgetType.TARGET_HUD);
        setModuleState(HudModules.Coordinates.class, HudWidgetType.COORDS);
        setModuleState(HudModules.Notifications.class, HudWidgetType.NOTIFICATION);
        setModuleState(HudModules.Structures.class, HudWidgetType.TRAP_TIMER);
        setModuleState(HudModules.Inventory.class, HudWidgetType.INVENTORY);
        setModuleState(HudModules.Cooldowns.class, HudWidgetType.COOLDOWNS);

        boolean oldLayout = !this.elements.isSelected(HudWidgetType.INVENTORY)
                || !this.elements.isSelected(HudWidgetType.COOLDOWNS)
                || this.elements.isSelected(HudWidgetType.COORDS)
                || this.elements.isSelected(HudWidgetType.STAFF_LIST)
                || this.elements.isSelected(HudWidgetType.TRAP_TIMER)
                || hasLegacySpectraPositions(stack);
        setStateSilent(false);
        if (!oldLayout) {
            return;
        }

        this.spectraHudLayoutMigrated = true;
        float screenWidth = stack.resolution.screenWidth();
        float screenHeight = stack.resolution.screenHeight();
        for (Draggable widget : stack.widgets()) {
            switch (widget.getName()) {
                case "Watermark" -> {
                    widget.applySavedPosition(30.0f, 30.0f);
                    if (widget instanceof WatermarkWidget watermark) {
                        watermark.scale.setCurrentValue(1.0f);
                    }
                }
                case "PotionList" -> widget.applySavedPosition(30.0f, 82.0f);
                case "Hotkeys" -> widget.applySavedPosition(30.0f, 228.0f);
                case "Inventory" -> widget.applySavedPosition(35.0f, screenHeight - 210.0f);
                case "Cooldowns" -> widget.applySavedPosition(screenWidth - 235.0f, 304.0f);
                case "Notifications" -> widget.applySavedPosition(
                        (screenWidth - 400.0f) / 2.0f, screenHeight - 490.0f);
                case "ItemBind" -> widget.applySavedPosition(
                        (screenWidth - 292.0f) / 2.0f, screenHeight - 164.0f);
                case "TargetHud" -> widget.applySavedPosition(
                        (screenWidth - 204.0f) / 2.0f, screenHeight - 94.0f);
                default -> {
                }
            }
        }
    }

    private <T extends Module> void setModuleState(Class<T> moduleClass, HudWidgetType widgetType) {
        Spectra.INSTANCE.moduleRepository().find(moduleClass)
                .ifPresent(module -> module.setStateSilent(this.elements.isSelected(widgetType)));
    }

    private boolean hasLegacySpectraPositions(WidgetStack stack) {
        float screenWidth = stack.resolution.screenWidth();
        float screenHeight = stack.resolution.screenHeight();
        int displaced = 0;
        for (Draggable widget : stack.widgets()) {
            float expectedX;
            float expectedY;
            switch (widget.getName()) {
                case "Watermark" -> {
                    expectedX = 30.0f;
                    expectedY = 30.0f;
                }
                case "PotionList" -> {
                    expectedX = 30.0f;
                    expectedY = 82.0f;
                }
                case "Hotkeys" -> {
                    expectedX = 30.0f;
                    expectedY = 228.0f;
                }
                case "Inventory" -> {
                    expectedX = 35.0f;
                    expectedY = screenHeight - 210.0f;
                }
                case "Cooldowns" -> {
                    expectedX = screenWidth - 235.0f;
                    expectedY = 304.0f;
                }
                case "Notifications" -> {
                    expectedX = (screenWidth - 400.0f) / 2.0f;
                    expectedY = screenHeight - 490.0f;
                }
                case "ItemBind" -> {
                    expectedX = (screenWidth - 292.0f) / 2.0f;
                    expectedY = screenHeight - 164.0f;
                }
                case "TargetHud" -> {
                    expectedX = (screenWidth - 204.0f) / 2.0f;
                    expectedY = screenHeight - 94.0f;
                }
                default -> {
                    continue;
                }
            }
            if (Math.abs(widget.getX() - expectedX) > 2.0f
                    || Math.abs(widget.getY() - expectedY) > 2.0f) {
                displaced++;
            }
        }
        return displaced >= 3;
    }
}
