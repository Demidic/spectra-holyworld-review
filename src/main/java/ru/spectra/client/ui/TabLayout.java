package ru.spectra.client.ui;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.util.WeightedEngine;

public interface TabLayout {
    default void initialize(MenuTabElement class732Var) {
    }

    void render(DrawCtx class699Var);

    default void renderOverlays(DrawCtx class699Var) {
    }

    void layout(LayoutScaleContext class698Var);

    void animation(WeightedEngine class141Var);

    boolean handleInput(InputEventContext class688Var, boolean z);

    float getContentHeight();

    void positionFrames();

    float width();

    float height();
}
