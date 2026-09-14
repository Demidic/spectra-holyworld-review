package ru.spectra.mixin;

import ru.spectra.client.Spectra;
import ru.spectra.client.render.WindowControllerAdapter;
import ru.spectra.client.model.MouseButtonInput;
import ru.spectra.client.model.CharInput;
import ru.spectra.client.model.CursorMoveInput;
import ru.spectra.client.model.KeyInput;
import ru.spectra.client.model.ScrollInput;
import ru.spectra.client.type.KeyInputAction;
import ru.spectra.client.model.KeyModifiers;
import ru.spectra.client.type.MouseButtonAction;
import ru.spectra.client.model.MouseModifiers;
import ru.spectra.client.model.PixelPoint;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharModsCallbackI;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWDropCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({InputUtil.class})
public class InputUtilMixin {
    @Inject(method = {"setKeyboardCallbacks"}, at = {@At("HEAD")}, cancellable = true)
    private static void setupKeyboardCallbacks(long j, GLFWKeyCallbackI gLFWKeyCallbackI, GLFWCharModsCallbackI gLFWCharModsCallbackI, CallbackInfo callbackInfo) {
        GLFW.glfwSetKeyCallback(j, (j2, i, i2, i3, i4) -> {
            WindowControllerAdapter class687VarWindowControllerAdapter = Spectra.INSTANCE.windowControllerAdapter();
            boolean zInterceptKeyboard = class687VarWindowControllerAdapter.interceptKeyboard();
            boolean zHandleInput = class687VarWindowControllerAdapter.handleInput(new KeyInput(i, i2, new KeyInputAction(i3), new KeyModifiers(i4)));
            if (zInterceptKeyboard || zHandleInput) {
                return;
            }
            gLFWKeyCallbackI.invoke(j2, i, i2, i3, i4);
        });
        GLFW.glfwSetCharModsCallback(j, (j3, i5, i6) -> {
            WindowControllerAdapter class687VarWindowControllerAdapter = Spectra.INSTANCE.windowControllerAdapter();
            boolean zInterceptKeyboard = class687VarWindowControllerAdapter.interceptKeyboard();
            if (class687VarWindowControllerAdapter.handleInput(new CharInput(i5, i6)) || zInterceptKeyboard) {
                return;
            }
            gLFWCharModsCallbackI.invoke(j3, i5, i6);
        });
        callbackInfo.cancel();
    }

    @Inject(method = {"setMouseCallbacks"}, at = {@At("HEAD")}, cancellable = true)
    private static void setupMouseCallbacks(long j, GLFWCursorPosCallbackI gLFWCursorPosCallbackI, GLFWMouseButtonCallbackI gLFWMouseButtonCallbackI, GLFWScrollCallbackI gLFWScrollCallbackI, GLFWDropCallbackI gLFWDropCallbackI, CallbackInfo callbackInfo) {
        GLFW.glfwSetCursorPosCallback(j, (j2, d, d2) -> {
            WindowControllerAdapter class687VarWindowControllerAdapter = Spectra.INSTANCE.windowControllerAdapter();
            boolean zInterceptMouse = class687VarWindowControllerAdapter.interceptMouse();
            if (class687VarWindowControllerAdapter.handleInput(new CursorMoveInput(new PixelPoint(Math.round(d), Math.round(d2)))) || zInterceptMouse) {
                return;
            }
            gLFWCursorPosCallbackI.invoke(j2, d, d2);
        });
        GLFW.glfwSetMouseButtonCallback(j, (j3, i, i2, i3) -> {
            if (i2 == GLFW.GLFW_PRESS
                    && Spectra.INSTANCE.systemStatusOverlay()
                    .handleCurrentPointerClick(i == GLFW.GLFW_MOUSE_BUTTON_LEFT)) {
                return;
            }
            WindowControllerAdapter class687VarWindowControllerAdapter = Spectra.INSTANCE.windowControllerAdapter();
            boolean zInterceptMouse = class687VarWindowControllerAdapter.interceptMouse();
            if (class687VarWindowControllerAdapter.handleInput(new MouseButtonInput(i, new MouseButtonAction(i2), new MouseModifiers(i3))) || zInterceptMouse) {
                return;
            }
            gLFWMouseButtonCallbackI.invoke(j3, i, i2, i3);
        });
        GLFW.glfwSetScrollCallback(j, (j4, d3, d4) -> {
            WindowControllerAdapter class687VarWindowControllerAdapter = Spectra.INSTANCE.windowControllerAdapter();
            boolean zInterceptMouse = class687VarWindowControllerAdapter.interceptMouse();
            if (class687VarWindowControllerAdapter.handleInput(new ScrollInput(d4)) || zInterceptMouse) {
                return;
            }
            gLFWScrollCallbackI.invoke(j4, d3, d4);
        });
        GLFW.glfwSetDropCallback(j, gLFWDropCallbackI);
        callbackInfo.cancel();
    }
}
