package ru.spectra.client.module;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.Spectra;
import ru.spectra.client.type.ClientSoundType;
import ru.spectra.client.type.UiSoundTarget;
import ru.spectra.client.Lang;
import ru.spectra.client.event.ModuleStateEvent;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.ModeSetting;
import ru.spectra.client.ui.setting.MultiSelectSetting;
import ru.spectra.client.ui.setting.NumberSetting;
import ru.spectra.client.type.SettingUnit;
import ru.spectra.client.util.WavSoundPlayer;

@Aliases(aliases = {"Client Sounds", "Module Sounds", "Sound Effects", "Sounds", "Toggle Sounds"})
public class SoundsModule extends Module {
    private static final long SLIDER_SOUND_INTERVAL_NANOS = 45_000_000L;

    public final MultiSelectSetting<UiSoundTarget> soundTargets;
    public final ModeSetting<ClientSoundType> soundTypeSetting;
    public final NumberSetting volumeSetting;
    private long lastSliderSoundNanos;

    public SoundsModule() {
        super(ModuleTab.MISC, "Sounds");
        this.soundTargets = new MultiSelectSetting<UiSoundTarget>(
                Lang.CLIENTSOUNDS_TARGETS, Lang.CLIENTSOUNDS_TARGETS_DESC
        ).values(UiSoundTarget.class).select(
                UiSoundTarget.MODULE_TOGGLE,
                UiSoundTarget.SETTING_TOGGLE,
                UiSoundTarget.SLIDER_MOVE
        );
        this.soundTypeSetting = new ModeSetting<ClientSoundType>(
                Lang.CLIENTSOUNDS_TYPE,
                ru.spectra.client.model.Translation.unformatted("misc.clientsounds.type.desc")
        ).values(ClientSoundType.class);
        this.volumeSetting = new NumberSetting(
                Lang.CLIENTSOUNDS_VOLUME,
                Lang.CLIENTSOUNDS_VOLUME_DESC
        ).currentValue(70.0f).range(1.0f, 100.0f).step(1.0f).unit(SettingUnit.PERCENTS);
        addSettings(this.soundTargets, this.soundTypeSetting, this.volumeSetting);
        register(ModuleStateEvent.class, event -> {
            boolean finalDisableSound = event.module() == this && !event.moduleState();
            if ((isState() || finalDisableSound)
                    && this.soundTargets.isSelected(UiSoundTarget.MODULE_TOGGLE)) {
                playToggle(event.moduleState());
            }
        });
    }

    public static void playSettingToggle(boolean enabled) {
        SoundsModule sounds = activeSounds();
        if (sounds == null || !sounds.soundTargets.isSelected(UiSoundTarget.SETTING_TOGGLE)) {
            return;
        }
        sounds.playToggle(enabled);
    }

    public static void playSliderMove() {
        SoundsModule sounds = activeSounds();
        if (sounds == null || !sounds.soundTargets.isSelected(UiSoundTarget.SLIDER_MOVE)) {
            return;
        }
        long now = System.nanoTime();
        if (now - sounds.lastSliderSoundNanos < SLIDER_SOUND_INTERVAL_NANOS) {
            return;
        }
        sounds.lastSliderSoundNanos = now;
        sounds.play("ui_slider_tick", 0.82f);
    }

    private void playToggle(boolean enabled) {
        int soundIndex = this.soundTypeSetting.selectedIndex() + 1;
        play((enabled ? "module_enable_" : "module_disable_") + soundIndex, 1.0f);
    }

    private void play(String resource, float volumeMultiplier) {
        WavSoundPlayer.INSTANCE.playSound(
                resource,
                Math.min(93.0f, Math.max(1.0f,
                        this.volumeSetting.currentValue() * volumeMultiplier)),
                false
        );
    }

    private static SoundsModule activeSounds() {
        if (Spectra.INSTANCE == null || Spectra.INSTANCE.moduleRepository() == null) {
            return null;
        }
        SoundsModule sounds = Spectra.INSTANCE.moduleRepository().get(SoundsModule.class);
        return sounds != null && sounds.isState() ? sounds : null;
    }
}
