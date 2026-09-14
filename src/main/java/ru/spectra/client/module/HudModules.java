package ru.spectra.client.module;

import ru.spectra.client.Spectra;
import ru.spectra.client.ui.CoordsWidget;
import ru.spectra.client.ui.CooldownsWidget;
import ru.spectra.client.ui.Draggable;
import ru.spectra.client.ui.HotkeysWidget;
import ru.spectra.client.ui.InventoryHudWidget;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.NotificationWidget;
import ru.spectra.client.ui.PotionListWidget;
import ru.spectra.client.ui.TargetHudWidget;
import ru.spectra.client.ui.StructuresWidget;
import ru.spectra.client.ui.WatermarkWidget;
import ru.spectra.client.ui.setting.Setting;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * One module per HUD element. The old Widgets module remains only as a hidden
 * compatibility bridge for existing configs.
 */
public final class HudModules {
    private HudModules() {
    }

    public static float watermarkBossBarOffset() {
        if (Spectra.INSTANCE == null || Spectra.INSTANCE.moduleRepository() == null) {
            return 0.0f;
        }
        return Spectra.INSTANCE.moduleRepository().find(Watermark.class)
                .filter(Module::isState)
                .map(module -> module.widget().bossBarOffset())
                .orElse(0.0f);
    }

    public abstract static class WidgetModule<T extends Draggable> extends Module {
        private final T widget;

        protected WidgetModule(String name, Function<BooleanSupplier, T> widgetFactory) {
            super(ModuleTab.RENDER, name);
            this.widget = widgetFactory.apply(this::isState);
            this.widget.availability = this::isAvailable;
            Spectra.INSTANCE.widgetStack().newWidget(this.widget);
            if (!this.widget.getSettings().isEmpty()) {
                addSettings(this.widget.getSettings().toArray(Setting[]::new));
            }
        }

        public T widget() {
            return this.widget;
        }
    }

    public static final class Watermark extends WidgetModule<WatermarkWidget> {
        public Watermark() {
            super("Watermark", WatermarkWidget::new);
        }
    }

    public static final class ActiveEffects extends WidgetModule<PotionListWidget> {
        public ActiveEffects() {
            super("Active Effects", PotionListWidget::new);
        }
    }

    public static final class Keybinds extends WidgetModule<HotkeysWidget> {
        public Keybinds() {
            super("Keybinds", HotkeysWidget::new);
        }
    }

    public static final class TargetHud extends WidgetModule<TargetHudWidget> {
        public TargetHud() {
            super("Target HUD", TargetHudWidget::new);
        }
    }

    public static final class Coordinates extends WidgetModule<CoordsWidget> {
        public Coordinates() {
            super("Coordinates", CoordsWidget::new);
        }
    }

    public static final class Notifications extends WidgetModule<NotificationWidget> {
        public Notifications() {
            super("Notifications", NotificationWidget::new);
        }
    }

    public static final class Structures extends WidgetModule<StructuresWidget> {
        public Structures() {
            super("Structures", StructuresWidget::new);
        }
    }

    public static final class Inventory extends WidgetModule<InventoryHudWidget> {
        public Inventory() {
            super("Inventory", InventoryHudWidget::new);
        }
    }

    public static final class Cooldowns extends WidgetModule<CooldownsWidget> {
        public Cooldowns() {
            super("Cooldowns", CooldownsWidget::new);
        }
    }
}
