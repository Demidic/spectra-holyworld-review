package ru.spectra.client.module;

import ru.spectra.client.Spectra;
import ru.spectra.client.event.FovEvent;
import ru.spectra.client.event.KeyInputEvent;
import ru.spectra.client.event.MouseButtonEvent;
import ru.spectra.client.event.VelocityEvent;
import ru.spectra.client.event.WorldRenderEvent;
import ru.spectra.client.math.DeltaTimeTracker;
import ru.spectra.client.math.Easings;
import ru.spectra.client.model.Translation;
import ru.spectra.client.render.AnimatedFloat;
import ru.spectra.client.render.AnimationStack2;
import ru.spectra.client.type.ButtonAction;
import ru.spectra.client.type.KeyPressState;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.KeybindSetting;
import ru.spectra.client.util.MathUtil;
import ru.spectra.client.util.WeightedEngine;
import net.minecraft.client.option.GameOptions;

public class ZoomModule extends Module {
    public final KeybindSetting zoomKey;
    private final AnimatedFloat fovAnimator =
            new AnimatedFloat(150, Easings.LINEAR);
    private final DeltaTimeTracker deltaTracker = new DeltaTimeTracker();
    private final AnimationStack2 animationStack = new AnimationStack2();
    private final Mc mc = Mc.INSTANCE;
    private float targetFov = 70.0f;
    private boolean zooming;

    public ZoomModule() {
        super(ModuleTab.RENDER, "Zoom");
        this.zoomKey = new KeybindSetting(
                Translation.clearText("Zoom key"),
                Translation.clearText("Hold this key to zoom the camera")
        );
        addSettings(this.zoomKey);

        register(KeyInputEvent.class, event -> {
            if (!isState() || !this.mc.isWorldLoaded() || event.key() != this.zoomKey.getKey()) {
                return;
            }
            setZooming(event.action() == KeyPressState.PRESS);
        });
        register(MouseButtonEvent.class, event -> {
            if (!isState() || !this.mc.isWorldLoaded() || event.button() != this.zoomKey.getKey()) {
                return;
            }
            setZooming(event.action() == ButtonAction.PRESS);
        });
        register(WorldRenderEvent.class, event -> {
            if (!isState() || !this.mc.isWorldLoaded()) {
                return;
            }
            if (!this.zooming) {
                this.targetFov = currentFov();
            }
            WeightedEngine engine = new WeightedEngine(
                    this.deltaTracker.elapsedUnit(), this.animationStack);
            this.animationStack.begin();
            this.fovAnimator.destination(this.targetFov);
            this.fovAnimator.animate(engine);
            this.animationStack.end();
        });
        register(VelocityEvent.class, event -> {
            if (!isState() || !this.mc.isWorldLoaded() || !this.zooming) {
                return;
            }
            this.targetFov = (float) MathUtil.clamp(
                    this.targetFov - event.getVertical() * 10.0d,
                    10.0d,
                    currentFov()
            );
            event.cancel();
        });
        register(FovEvent.class, event -> {
            if (!isState() || !this.mc.isWorldLoaded()) {
                return;
            }
            event.setFov((int) MathUtil.clamp(
                    this.fovAnimator.animatedValue(), 10.0f, currentFov()));
            event.cancel();
        });
    }

    private void setZooming(boolean zooming) {
        this.zooming = zooming;
        this.targetFov = zooming
                ? Math.min(30.0f, currentFov() - 20.0f)
                : currentFov();
    }

    private float currentFov() {
        GameOptions options = this.mc.getGameOptions();
        return ((Integer) options.getFov().getValue()).floatValue();
    }

    @Override
    public void activate() {
        this.targetFov = currentFov();
        this.fovAnimator.set(this.targetFov);
        super.activate();
    }

    @Override
    public void deactivate() {
        this.zooming = false;
        this.targetFov = currentFov();
        this.fovAnimator.set(this.targetFov);
        super.deactivate();
    }
}
