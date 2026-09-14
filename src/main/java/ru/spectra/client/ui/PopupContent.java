package ru.spectra.client.ui;

import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.model.WidgetBounds;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.util.WeightedEngine;

/** Content contract for reusable menu popups. */
public interface PopupContent {
    float preferredWidth();

    float preferredHeight();

    default boolean showCloseButton() {
        return true;
    }

    void render(DrawCtx context, WidgetBounds contentBounds);

    default void layout(LayoutScaleContext context, WidgetBounds contentBounds) {
    }

    default boolean handleInput(InputEventContext context, boolean consumed,
                                WidgetBounds contentBounds) {
        return false;
    }

    default void animation(WeightedEngine engine) {
    }

    default void onOpen() {
    }

    default void onClose() {
    }
}
