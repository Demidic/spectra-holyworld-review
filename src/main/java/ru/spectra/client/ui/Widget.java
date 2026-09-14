package ru.spectra.client.ui;
import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.event.LayoutCallback;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.util.OverlayCommandQueue;
import ru.spectra.client.model.PixelPoint;
import ru.spectra.client.util.RenderCommandQueue;
import ru.spectra.client.util.WeightedEngine;
import ru.spectra.client.model.WidgetBounds;

public abstract class Widget implements LayoutCallback {
    public WidgetContainer parent;
    public WidgetBounds bounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public boolean visible = true;

    public void setPosition(float f, float f2) {
        this.bounds = this.bounds.withPosition(Math.round(f), Math.round(f2));
    }

    public void setSize(float f, float f2) {
        this.bounds = this.bounds.withSize(f, f2);
    }

    public float x() {
        return this.bounds.x();
    }

    public float y() {
        return this.bounds.y();
    }

    public float width() {
        return this.bounds.width();
    }

    public float height() {
        return this.bounds.height();
    }

    public void handleClose() {
    }

    public void onMenuDrag(boolean z) {
    }

    public boolean handleInput(InputEventContext class688Var, boolean z) {
        return false;
    }

    public void animation(WeightedEngine class141Var) {
    }

    public void collectBlurElements(OverlayCommandQueue class677Var) {
    }

    public void collectBloomElements(RenderCommandQueue class676Var) {
    }

    public boolean hitTest(PixelPoint class708Var, LayoutScaleContext class698Var) {
        return this.bounds.containsPhysical(class708Var.x(), class708Var.y(), class698Var.scaleFactor());
    }

    public WidgetContainer parent() {
        return this.parent;
    }

    public Widget parent(WidgetContainer class683Var) {
        this.parent = class683Var;
        return this;
    }

    public boolean visible() {
        return this.visible;
    }

    public Widget visible(boolean z) {
        this.visible = z;
        return this;
    }
}
