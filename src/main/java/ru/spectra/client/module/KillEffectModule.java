package ru.spectra.client.module;

import ru.spectra.client.Spectra;
import ru.spectra.client.event.AttackEntityEvent;
import ru.spectra.client.event.WorldRenderEvent;
import ru.spectra.client.model.PopChamsModel;
import ru.spectra.client.model.Translation;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.NumberSetting;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class KillEffectModule extends Module {
    private static final long KILL_WINDOW = 3_000L;
    public final NumberSetting lifetime = new NumberSetting(Translation.clearText("Lifetime"))
            .currentValue(1.6f).range(0.5f, 4.0f).step(0.1f);
    private final Map<UUID, Long> attacked = new HashMap<>();
    private final Map<UUID, Boolean> deathLatch = new HashMap<>();
    private final List<Soul> souls = new ArrayList<>();

    public KillEffectModule() {
        super(ModuleTab.RENDER, "Kill Effect");
        addSettings(lifetime);
        register(AttackEntityEvent.class, event -> {
            if (isState() && event.attacker() instanceof OtherClientPlayerEntity target) {
                attacked.put(target.getUuid(), System.currentTimeMillis());
            }
        });
        register(WorldRenderEvent.class, this::render);
    }

    private void render(WorldRenderEvent event) {
        if (!isState() || !Mc.INSTANCE.isWorldLoaded()) {
            reset();
            return;
        }
        long now = System.currentTimeMillis();
        attacked.entrySet().removeIf(entry -> now - entry.getValue() > KILL_WINDOW);
        PopChamsModule renderer = Spectra.INSTANCE.moduleRepository().get(PopChamsModule.class);
        for (PlayerEntity player : Mc.INSTANCE.getWorld().getPlayers()) {
            if (!(player instanceof OtherClientPlayerEntity target)) {
                continue;
            }
            boolean dead = !target.isAlive() || target.getHealth() <= 0.0f || target.deathTime > 0;
            if (dead && attacked.containsKey(target.getUuid())
                    && !deathLatch.getOrDefault(target.getUuid(), false)) {
                PopChamsModel snapshot = renderer.createModel(target,
                        Spectra.INSTANCE.theme().palette().accent().argb(), true);
                souls.add(new Soul(snapshot, now, Math.round(lifetime.currentValue() * 1000.0f)));
                deathLatch.put(target.getUuid(), true);
                attacked.remove(target.getUuid());
            } else if (!dead) {
                deathLatch.remove(target.getUuid());
            }
        }
        souls.removeIf(soul -> now - soul.startedAt >= soul.lifetimeMs);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        try {
            for (Soul soul : souls) {
                float progress = Math.min(1.0f, (now - soul.startedAt) / (float) soul.lifetimeMs);
                float fadeIn = Math.min(1.0f, progress / 0.16f);
                float fadeOut = Math.min(1.0f, (1.0f - progress) / 0.28f);
                float alpha = 170.0f / 255.0f * Math.min(1.0f - (float) Math.pow(1.0f - fadeIn, 3.0), fadeOut);
                float rise = 0.7f + (2.3f - 0.7f) * (1.0f - (float) Math.pow(1.0f - progress, 3.0));
                int tint = Spectra.INSTANCE.theme().palette().accent().argb();
                renderer.renderModel(event.matrixStack(), soul.model, alpha, rise, tint);
            }
        } finally {
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }
    }

    private void reset() {
        attacked.clear();
        deathLatch.clear();
        souls.clear();
    }

    @Override
    public void deactivate() {
        reset();
        super.deactivate();
    }

    private record Soul(PopChamsModel model, long startedAt, long lifetimeMs) {
    }
}
