package ru.spectra.client.module;

import ru.spectra.client.Spectra;
import ru.spectra.client.event.WorldRenderEvent;
import ru.spectra.client.model.Translation;
import ru.spectra.client.render.ShapeRenderer;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.ui.setting.ColorSetting;
import ru.spectra.client.util.ColorUtil;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Deque;

public final class TrailsModule extends Module {
    private static final int MAX_POINTS = 32;
    private static final double MIN_DISTANCE_SQ = 0.0025;
    public final BooleanSetting clientColor =
            new BooleanSetting(Translation.clearText("Client color")).setValue(true);
    public final ColorSetting color = new ColorSetting(Translation.clearText("Custom color"))
            .visible(() -> !clientColor.isValue());
    public final BooleanSetting gradient =
            new BooleanSetting(Translation.clearText("Gradient")).setValue(true);
    private final Deque<Point> points = new ArrayDeque<>();

    public TrailsModule() {
        super(ModuleTab.RENDER, "Trails");
        color.setColor(0xFF91ADEB);
        addSettings(clientColor, color, gradient);
        register(WorldRenderEvent.class, this::render);
    }

    private void render(WorldRenderEvent event) {
        if (!isState() || !Mc.INSTANCE.isWorldLoaded()) {
            points.clear();
            return;
        }
        long now = System.currentTimeMillis();
        while (!points.isEmpty() && now - points.peekFirst().createdAt > 500L) {
            points.removeFirst();
        }
        Vec3d current = Mc.INSTANCE.getPlayer().getLerpedPos(Mc.INSTANCE.getTickDelta());
        if (points.isEmpty() || points.peekLast().position.squaredDistanceTo(current) > MIN_DISTANCE_SQ) {
            points.addLast(new Point(current, now));
            while (points.size() > MAX_POINTS) {
                points.removeFirst();
            }
        }
        if (Mc.INSTANCE.getGameOptions().getPerspective() == Perspective.FIRST_PERSON || points.size() < 2) {
            return;
        }
        int base = clientColor.isValue()
                ? Spectra.INSTANCE.theme().palette().accent().argb()
                : color.getColor();
        Point previous = null;
        int index = 0;
        for (Point point : points) {
            if (previous != null) {
                float progress = index / (float) Math.max(1, points.size() - 1);
                int segment = trailColor(base, progress);
                Vec3d bottomA = previous.position;
                Vec3d bottomB = point.position;
                Vec3d topA = bottomA.add(0.0, Mc.INSTANCE.getPlayer().getHeight(), 0.0);
                Vec3d topB = bottomB.add(0.0, Mc.INSTANCE.getPlayer().getHeight(), 0.0);
                ShapeRenderer.INSTANCE.addLine(event.matrixStack().peek().getPositionMatrix(),
                        bottomA, bottomB, ColorUtil.replAlpha(segment, Math.round(progress * 255.0f)), 3.0f);
                ShapeRenderer.INSTANCE.addLine(event.matrixStack().peek().getPositionMatrix(),
                        topA, topB, ColorUtil.replAlpha(segment, Math.round(progress * 255.0f)), 3.0f);
                ShapeRenderer.INSTANCE.addLine(event.matrixStack().peek().getPositionMatrix(),
                        bottomB, topB, ColorUtil.replAlpha(segment, Math.round(progress * 128.0f)), 1.5f);
            }
            previous = point;
            index++;
        }
    }

    private int trailColor(int base, float progress) {
        if (!gradient.isValue()) {
            return base;
        }
        float brightness = progress < 0.5f ? progress * 2.0f : 1.0f + (progress - 0.5f) * 0.5f;
        return ColorUtil.argb(255,
                Math.min(255, Math.round(ColorUtil.red(base) * brightness)),
                Math.min(255, Math.round(ColorUtil.green(base) * brightness)),
                Math.min(255, Math.round(ColorUtil.blue(base) * brightness)));
    }

    @Override
    public void deactivate() {
        points.clear();
        super.deactivate();
    }

    private record Point(Vec3d position, long createdAt) {
    }
}
