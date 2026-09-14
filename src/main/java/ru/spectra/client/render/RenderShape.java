package ru.spectra.client.render;
import ru.spectra.client.type.RenderShapeState;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;

public abstract class RenderShape {
    protected final RenderShapeState renderState = new RenderShapeState();

    public abstract void emit(MatrixStack matrixStack, BufferBuilder bufferBuilder);

    public final RenderShapeState state() {
        return this.renderState;
    }
}
