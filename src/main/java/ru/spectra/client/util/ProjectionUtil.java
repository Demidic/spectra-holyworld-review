package ru.spectra.client.util;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

public final class ProjectionUtil {
    public static final AtomicReference<Matrix4f> LAST_PROJECTION = new AtomicReference<>();
    public static final AtomicReference<Matrix4f> LAST_MODEL_VIEW = new AtomicReference<>();

    public static Optional<Vector2f> worldToScreen(Vec3d vec3d) {
        Matrix4f matrix4f = LAST_PROJECTION.get();
        Matrix4f matrix4f2 = LAST_MODEL_VIEW.get();
        if (matrix4f == null || matrix4f2 == null) {
            return Optional.empty();
        }
        Matrix4f matrix4fMul = new Matrix4f(matrix4f).mul(matrix4f2);
        Vec3d vec3dSubtract = vec3d.subtract(MinecraftClient.getInstance().gameRenderer.getCamera().getPos());
        Vector4f vector4f = new Vector4f((float) vec3dSubtract.x, (float) vec3dSubtract.y, (float) vec3dSubtract.z, 1.0f);
        matrix4fMul.transform(vector4f);
        if (vector4f.w <= 0.0f || Float.isNaN(vector4f.w)) {
            return Optional.empty();
        }
        float f = vector4f.x / vector4f.w;
        float f2 = vector4f.y / vector4f.w;
        Window window = MinecraftClient.getInstance().getWindow();
        float framebufferWidth = (f + 1.0f) * 0.5f * window.getFramebufferWidth();
        float framebufferHeight = (1.0f - f2) * 0.5f * window.getFramebufferHeight();
        return (Float.isNaN(framebufferWidth) || Float.isNaN(framebufferHeight) || Float.isInfinite(framebufferWidth) || Float.isInfinite(framebufferHeight)) ? Optional.empty() : Optional.of(new Vector2f(framebufferWidth, framebufferHeight));
    }

    public static boolean isOutOfScreen(Vector4f vector4f) {
        return vector4f == null || (vector4f.x < 0.0f && vector4f.z < 1.0f) || (vector4f.y < 0.0f && vector4f.w < 1.0f);
    }

    public static Optional<Vector4f> boxToScreen(Box box) {
        Matrix4f projection = LAST_PROJECTION.get();
        Matrix4f modelView = LAST_MODEL_VIEW.get();
        if (projection == null || modelView == null) return Optional.empty();
        MinecraftClient client = MinecraftClient.getInstance();
        Window window = client.getWindow();
        return projectBox(box, projection, modelView, client.gameRenderer.getCamera().getPos(),
                window.getFramebufferWidth(), window.getFramebufferHeight());
    }

    static Optional<Vector4f> projectBox(Box box, Matrix4f projection, Matrix4f modelView,
                                       Vec3d camera, int width, int height) {
        Matrix4f viewProjection = new Matrix4f(projection).mul(modelView);
        Vector4f corner = new Vector4f();
        float f = Float.POSITIVE_INFINITY;
        float f2 = Float.POSITIVE_INFINITY;
        float f3 = Float.NEGATIVE_INFINITY;
        float f4 = Float.NEGATIVE_INFINITY;
        int i = 0;
        for (int index = 0; index < 8; index++) {
            double x = (index & 4) == 0 ? box.minX : box.maxX;
            double y = (index & 2) == 0 ? box.minY : box.maxY;
            double z = (index & 1) == 0 ? box.minZ : box.maxZ;
            corner.set((float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z), 1.0f);
            viewProjection.transform(corner);
            if (corner.w > 0.0f) {
                float f5 = (corner.x / corner.w + 1.0f) * 0.5f * width;
                float f6 = (1.0f - corner.y / corner.w) * 0.5f * height;
                if (Float.isFinite(f5) && Float.isFinite(f6)) {
                    i++;
                    if (f5 < f) {
                        f = f5;
                    }
                    if (f6 < f2) {
                        f2 = f6;
                    }
                    if (f5 > f3) {
                        f3 = f5;
                    }
                    if (f6 > f4) {
                        f4 = f6;
                    }
                }
            }
        }
        if (i == 0) {
            return Optional.empty();
        }
        float framebufferWidth = width;
        float framebufferHeight = height;
        if (f3 < 0.0f || f > framebufferWidth || f4 < 0.0f || f2 > framebufferHeight) {
            return Optional.empty();
        }
        float f7 = f;
        float f8 = f2;
        float fMax = Math.max(0.0f, f3 - f7);
        float fMax2 = Math.max(0.0f, f4 - f8);
        return (fMax == 0.0f && fMax2 == 0.0f) ? Optional.empty() : Optional.of(new Vector4f(Math.round(f7), Math.round(f8), Math.round(fMax), Math.round(fMax2)));
    }

    public static float centerX(Vector4f vector4f) {
        return vector4f.x + (vector4f.z / 2.0f);
    }

    public ProjectionUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
