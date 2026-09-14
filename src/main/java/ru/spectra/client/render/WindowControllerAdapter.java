package ru.spectra.client.render;
import ru.spectra.client.model.CursorMoveInput;
import ru.spectra.client.event.InputEvent;
import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.model.PixelPoint;
import ru.spectra.client.util.WindowController;
import org.lwjgl.glfw.GLFW;

public class WindowControllerAdapter {
    public final WindowController controller;

    public PixelPoint mousePosition = new PixelPoint(0, 0);
    private boolean renderingSuspended;

    public void preBlitFramebufferToBackbuffer(long j) throws MatchException {
        // Minecraft (and Dynamic FPS) can keep producing occasional frames while
        // the native window is unfocused or iconified. Running our off-screen
        // blur/composite passes in that state makes them sample an invalid/empty
        // backbuffer, which alternates a black frame with the last valid frame.
        // Keep the last valid UI buffers untouched until the window is active.
        if (!isWindowRenderable(j)) {
            this.renderingSuspended = true;
            return;
        }
        if (this.renderingSuspended) {
            this.renderingSuspended = false;
            this.controller.resumeRendering();
        }
        this.controller.draw(j);
        handleInput(new CursorMoveInput(this.controller.window().determineMousePosition()));
    }

    private static boolean isWindowRenderable(long handle) {
        return GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_FALSE
                && GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
    }

    public void handleResize(int i, int i2) {
        this.controller.handleResize(i, i2);
    }

    public boolean handleInput(InputEvent class691Var) throws MatchException {
        if (class691Var instanceof CursorMoveInput) {
            try {
                this.mousePosition = ((CursorMoveInput) class691Var).mousePosition();
            } catch (Throwable th) {
                throw new MatchException(th.toString(), th);
            }
        }
        return this.controller.handleInput(new InputEventContext(class691Var, this.mousePosition, this.controller.dpiScaleFactor()));
    }

    public boolean interceptKeyboard() {
        return this.controller.interceptKeyboardIfScreenPresent() || this.controller.interceptKeyboard();
    }

    public boolean interceptMouse() {
        return this.controller.interceptCursorIfScreenNotPresent();
    }

    public WindowControllerAdapter(WindowController class686Var) {
        this.controller = class686Var;
    }
}
