package ru.spectra.client.util;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.internal.DirectionIndexMap;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.model.GlStateSnapshot;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.render.SvgTexture;
import ru.spectra.client.type.ItemSpriteManager;
import ru.spectra.client.render.ItemSpriteTextures;
import ru.spectra.client.type.Mc;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.accessor.NativeMemoryAccessor;
import ru.spectra.client.natives.NativeMemoryBuffer;
import ru.spectra.client.model.ScissorBounds;
import ru.spectra.client.render.ScissorStack;
import ru.spectra.client.render.ShaderProgram;
import ru.spectra.client.render.ShaderUniform;
import ru.spectra.client.type.ShapeType;
import ru.spectra.client.model.SpriteRegion;
import ru.spectra.client.render.TextureSlotAllocator;
import ru.spectra.client.type.VertexAttributeType;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;

public class DrawEngine {

    public final NativeMemoryBuffer vertexBuffer;
    public final NativeMemoryBuffer indexBuffer;

    public final ShaderProgram shaderProgram;

    public final ShaderUniform orthoMatrixUniform;

    public final ShaderUniform textureSamplerUniform;
    public final ShaderUniform resolutionUniform;

    public final TextureSlotAllocator textureSlotAllocator;

    public final ColorStack colorStack;
    // Cache final vertices, not a screenshot: clipping, blending and animation
    // still use the current frame. The font atlas is bound before every replay.
    private record TextMeshKey(MsdfFont font, String text, int textureSlot) { }
    private final java.util.LinkedHashMap<TextMeshKey, RectangleMeshCache> textMeshes =
            new java.util.LinkedHashMap<>(192, 0.75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<TextMeshKey, RectangleMeshCache> entry) {
                    return size() > 192;
                }
            };
    private final float[] vertexAlphaStack = new float[32];
    private int vertexAlphaPointer;
    public final int vertexArrayId;
    public final int vertexBufferId;
    public final int indexBufferId;
    public int indexCount;
    public int vertexCount;

    public GlStateSnapshot stateSnapshot;

    public boolean building;

    public boolean usingStencil;
    public final int maxTextureUnits;
    public final Vector4f tempVector = new Vector4f();
    // Separate render-thread scratch: textured quads must not change tempVector,
    // whose final value is observed by text-mesh replay and other shape paths.
    private final Vector4f texturedQuadVector = new Vector4f();
    private final Matrix4f orthoMatrix = new Matrix4f();

    public final ScissorStack scissorStack = new ScissorStack();
    public float contentScale = 1.0f;
    public int drawCallCount = 0;

    public final ScissorBounds lastBounds = new ScissorBounds();

    public Runnable blendSwitchCallback = null;

    public DrawEngine(ShaderProgram class381Var, int i) {
        this.maxTextureUnits = i;
        NativeMemoryAccessor class014Var = new NativeMemoryAccessor();
        this.vertexBuffer = new NativeMemoryBuffer(class014Var, 8388608L);
        this.indexBuffer = new NativeMemoryBuffer(class014Var, 8388608L);
        this.shaderProgram = class381Var;
        this.orthoMatrixUniform = class381Var.uniform("orthographicMatrix");
        this.textureSamplerUniform = class381Var.uniform("textureSampler");
        this.resolutionUniform = class381Var.uniform("resolution");
        this.textureSlotAllocator = new TextureSlotAllocator(i);
        this.colorStack = new ColorStack();
        List<VertexAttributeType> listOf = List.of(new VertexAttributeType[]{VertexAttributeType.VEC2, VertexAttributeType.VEC2, VertexAttributeType.VEC2, VertexAttributeType.VEC2, VertexAttributeType.VEC4, VertexAttributeType.NORMALIZED_VEC4, VertexAttributeType.NORMALIZED_VEC4, VertexAttributeType.FLOAT, VertexAttributeType.FLOAT, VertexAttributeType.UNSIGNED_BYTE, VertexAttributeType.UNSIGNED_BYTE, VertexAttributeType.UNSIGNED_BYTE});
        this.vertexArrayId = GL33.glGenVertexArrays();
        this.vertexBufferId = GL33.glGenBuffers();
        this.indexBufferId = GL33.glGenBuffers();
        GL33.glBindVertexArray(this.vertexArrayId);
        GL33.glBindBuffer(34962, this.vertexBufferId);
        GL33.glBindBuffer(34963, this.indexBufferId);
        int iSum = listOf.stream().mapToInt((v0) -> {
            return v0.size();
        }).sum();
        int size = 0;
        for (int i2 = 0; i2 < listOf.size(); i2++) {
            VertexAttributeType class075Var = (VertexAttributeType) listOf.get(i2);
            GL33.glEnableVertexAttribArray(i2);
            if (class075Var.integer()) {
                GL33.glVertexAttribIPointer(i2, class075Var.count(), class075Var.type(), iSum, size);
            } else {
                GL33.glVertexAttribPointer(i2, class075Var.count(), class075Var.type(), class075Var.normalized(), iSum, size);
            }
            size += class075Var.size();
        }
    }

    public void begin() {
        if (this.building) {
            throw new RuntimeException("DrawEngine.begin() called while already building.");
        }
        Window window = Mc.INSTANCE.getWindow();
        updateContentScale(window);
        int iMethod007 = scaledWidth(window);
        int iMethod013 = scaledHeight(window);
        this.drawCallCount = 0;
        setupGlState();
        beginScissor(0.0f, 0.0f, iMethod007, iMethod013);
        this.colorStack.begin();
        this.vertexAlphaPointer = 0;
        this.vertexAlphaStack[0] = 1.0f;
        this.building = true;
    }

    public void end() {
        if (!this.building) {
            throw new RuntimeException("DrawEngine.end() called while not building.");
        }
        this.building = false;
        draw();
        restoreGlState();
    }

    public int bindTexture(int i) {
        if (!this.building) {
            throw new RuntimeException("DrawEngine.bindTexture() called while not building.");
        }
        if (!this.textureSlotAllocator.contains(i) && this.textureSlotAllocator.isFull()) {
            draw();
        }
        return this.textureSlotAllocator.bindTexture(i, this.stateSnapshot);
    }

    public void draw() {
        if (this.indexCount == 0 && this.blendSwitchCallback == null) {
            return;
        }
        Window window = Mc.INSTANCE.getWindow();
        Matrix4f matrix4fOrtho2D = this.orthoMatrix.identity()
                .ortho2D(0.0f, window.getFramebufferWidth(), window.getFramebufferHeight(), 0.0f);
        ByteBuffer byteBufferDirectByteBuffer = this.vertexBuffer.directByteBuffer();
        ByteBuffer byteBufferDirectByteBuffer2 = this.indexBuffer.directByteBuffer();
        GL33.glBindVertexArray(this.vertexArrayId);
        GL33.glBindBuffer(34962, this.vertexBufferId);
        GL33.glBindBuffer(34963, this.indexBufferId);
        GL33.glBufferData(34962, MemoryUtil.memSlice(byteBufferDirectByteBuffer), 35040);
        GL33.glBufferData(34963, MemoryUtil.memSlice(byteBufferDirectByteBuffer2), 35040);
        this.shaderProgram.bind();
        this.orthoMatrixUniform.uploadMatrix4f(matrix4fOrtho2D);
        this.textureSamplerUniform.uploadIntBuffer(this.textureSlotAllocator.textureSlots());
        this.resolutionUniform.uploadVec2(window.getFramebufferWidth(), window.getFramebufferHeight());
        boolean z = this.blendSwitchCallback != null;
        if (z) {
            this.blendSwitchCallback.run();
            this.blendSwitchCallback = null;
        }
        GL33.glDrawElements(4, this.indexCount, 5125, 0L);
        if (z) {
            GL33.glBlendFuncSeparate(770, 771, 1, 771);
        }
        this.drawCallCount++;
        this.indexCount = 0;
        this.vertexCount = 0;
        this.shaderProgram.unbind();
        this.textureSlotAllocator.clear();
        this.vertexBuffer.reset();
        this.indexBuffer.reset();
    }

    public void setupGlState() {
        this.stateSnapshot = GlStateSnapshot.createDeferred(this.maxTextureUnits);
        GL33.glEnable(3042);
        GL33.glBlendEquation(32774);
        GL33.glBlendFuncSeparate(770, 771, 1, 771);
        GL33.glEnable(3089);
        GL33.glDisable(2884);
        GL33.glDisable(2929);
        GL33.glDisable(36765);
        GL33.glPolygonMode(1032, 6914);
        GL33.glPixelStorei(3317, 1);
        GL33.glPixelStorei(3333, 1);
    }

    public void restoreGlState() {
        this.scissorStack.end();
        this.stateSnapshot.revert();
    }

    public void switchBlendFuncAndDraw(Runnable runnable) {
        this.blendSwitchCallback = runnable;
        draw();
    }

    public void beginStencil() {
        if (this.usingStencil) {
            throw new RuntimeException("DrawEngine.beginStencil() called while already using stencil.");
        }
        this.usingStencil = true;
        draw();
        StencilBufferUtil.prepareStencil();
    }

    public void prepareStencil(int i) {
        if (!this.usingStencil) {
            throw new RuntimeException("DrawEngine.prepareStencil() called while not using stencil.");
        }
        draw();
        StencilBufferUtil.prepareElement(i);
    }

    public void endStencil() {
        if (!this.usingStencil) {
            throw new RuntimeException("DrawEngine.endStencil() called while not using stencil.");
        }
        draw();
        StencilBufferUtil.cleanup();
        this.usingStencil = false;
    }

    public void beginScissor(float f, float f2, float f3, float f4) {
        Window window = Mc.INSTANCE.getWindow();
        int iRound = Math.round(applyContentScale(f));
        int iRound2 = Math.round(applyContentScale(f2));
        int iRound3 = Math.round(applyContentScale(f3));
        int iRound4 = Math.round(applyContentScale(f4));
        int framebufferHeight = (window.getFramebufferHeight() - iRound2) - iRound4;
        draw();
        this.scissorStack.push(iRound, framebufferHeight, iRound3, iRound4, f, f2, f3, f4);
    }

    public void beginScissor(Matrix4f matrix4f, float f, float f2, float f3, float f4) {
        Window window = Mc.INSTANCE.getWindow();
        float fTransformX = transformX(matrix4f, f);
        float fTransformY = transformY(matrix4f, f2);
        float fTransformX2 = transformX(matrix4f, f + f3);
        float fTransformY2 = transformY(matrix4f, f2 + f4);
        int iRound = Math.round(applyContentScale(fTransformX));
        int iRound2 = Math.round(applyContentScale(fTransformY));
        int iRound3 = Math.round(applyContentScale(fTransformX2 - fTransformX));
        int iRound4 = Math.round(applyContentScale(fTransformY2 - fTransformY));
        int framebufferHeight = (window.getFramebufferHeight() - iRound2) - iRound4;
        draw();
        this.scissorStack.push(iRound, framebufferHeight, iRound3, iRound4, fTransformX, fTransformY, fTransformX2, fTransformY2);
    }

    public void drawDebugDataIfEnabled() {
        int i = this.drawCallCount;
        ArrayList<String> arrayList = new ArrayList();
        arrayList.add("drawcalls " + i);
        arrayList.add(MinecraftClient.getInstance().fpsDebugString);
        MsdfFont class161Var = Fonts.INTER_SEMIBOLD.get();
        Window window = Mc.INSTANCE.getWindow();
        float height = class161Var.getHeight(12.0f);
        int iMethod007 = scaledWidth(window);
        float fMethod013 = (scaledHeight(window) - 45.0f) - (height * arrayList.size());
        Matrix4f positionMatrix = new MatrixStack().peek().getPositionMatrix();
        for (String str : arrayList) {
            msdfFont(positionMatrix, class161Var, str, (iMethod007 - 10.0f) - class161Var.getWidth(str, 12.0f), fMethod013, 12.0f, 0.05f, -1);
            fMethod013 += height;
        }
    }

    public void endScissor() {
        draw();
        this.scissorStack.pop();
    }

    public float transformY(MatrixStack matrixStack, float f) {
        Matrix4f positionMatrix = matrixStack.peek().getPositionMatrix();
        Vector4f vector4f = new Vector4f(0.0f, f, 0.0f, 1.0f);
        positionMatrix.transform(vector4f);
        return vector4f.y();
    }

    public float transformX(MatrixStack matrixStack, float f) {
        Matrix4f positionMatrix = matrixStack.peek().getPositionMatrix();
        Vector4f vector4f = new Vector4f(f, 0.0f, 0.0f, 1.0f);
        positionMatrix.transform(vector4f);
        return vector4f.x();
    }

    public float transformY(Matrix4f matrix4f, float f) {
        Vector4f vector4f = new Vector4f(0.0f, f, 0.0f, 1.0f);
        matrix4f.transform(vector4f);
        return vector4f.y();
    }

    public float transformX(Matrix4f matrix4f, float f) {
        Vector4f vector4f = new Vector4f(f, 0.0f, 0.0f, 1.0f);
        matrix4f.transform(vector4f);
        return vector4f.x();
    }

    public void emitShape(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, float f11, float f12, float f13, float f14, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, ShapeType class328Var) {
        Window window = Mc.INSTANCE.getWindow();
        // Snapshot the render-thread-owned scale once. Calling the tiny Java
        // scaling accessor per coordinate otherwise crosses JNI each time.
        float scale = this.contentScale;
        Vector4f vector4fTransform = matrix4f.transform(this.tempVector.set(f, f2, 0.0f, 1.0f));
        float fX = vector4fTransform.x();
        float fY = vector4fTransform.y();
        float fX2 = fX;
        float fY2 = fY;
        Vector4f vector4fTransform2 = matrix4f.transform(this.tempVector.set(f + f3, f2 + f4, 0.0f, 1.0f));
        float fX3 = vector4fTransform2.x() - fX;
        float fY3 = vector4fTransform2.y() - fY;
        float fX4 = fX3;
        float fY4 = fY3;
        this.lastBounds.setGui(fX, fY, fX3, fY3);
        float fMethod016 = (scale == 1.0f ? fX2 : fX2 * scale);
        float fMethod017 = (scale == 1.0f ? fY2 : fY2 * scale);
        float fMethod018 = (scale == 1.0f ? fX4 : fX4 * scale);
        float fMethod019 = (scale == 1.0f ? fY4 : fY4 * scale);
        float framebufferHeight = (window.getFramebufferHeight() - fMethod019) - fMethod017;
        this.lastBounds.setGl(fMethod016, framebufferHeight, fMethod018, fMethod019);
        float fMax = Math.max(matrix4f.m00(), matrix4f.m11()) * scale;
        float f15 = f9 * fMax;
        float f16 = f10 * fMax;
        float f17 = f11 * fMax;
        float f18 = f12 * fMax;
        float f19 = f13 * scale;
        float f20 = f14 * scale;
        int iMode = class328Var.mode();
        writeTransformedVertex(matrix4f, f, f2 + f4, fMethod016, framebufferHeight, fMethod018, fMethod019, f5, f8, f15, f16, f17, f18, i5, i, f19, f20, i9, iMode, i10);
        writeTransformedVertex(matrix4f, f + f3, f2 + f4, fMethod016, framebufferHeight, fMethod018, fMethod019, f7, f8, f15, f16, f17, f18, i6, i2, f19, f20, i9, iMode, i10);
        writeTransformedVertex(matrix4f, f + f3, f2, fMethod016, framebufferHeight, fMethod018, fMethod019, f7, f6, f15, f16, f17, f18, i7, i3, f19, f20, i9, iMode, i10);
        writeTransformedVertex(matrix4f, f, f2, fMethod016, framebufferHeight, fMethod018, fMethod019, f5, f6, f15, f16, f17, f18, i8, i4, f19, f20, i9, iMode, i10);
        int firstVertex = this.vertexCount;
        writeTriangle(firstVertex, firstVertex + 1, firstVertex + 2);
        writeTriangle(firstVertex, firstVertex + 2, firstVertex + 3);
        this.vertexCount = firstVertex + 4;
        this.indexCount += 6;
    }

    public void emitTexturedQuad(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, float f11, float f12, int i, int i2, int i3, int i4, int i5, int i6, ShapeType class328Var) {
        Window window = Mc.INSTANCE.getWindow();
        // Snapshot the render-thread-owned scale once. Calling the tiny Java
        // scaling accessor per coordinate otherwise crosses JNI each time.
        float scale = this.contentScale;
        Vector4f vector4f = this.texturedQuadVector;
        matrix4f.transform(vector4f.set(f, f2, 0.0f, 1.0f));
        float fX = vector4f.x;
        float fY = vector4f.y;
        matrix4f.transform(vector4f.set(f3, f4, 0.0f, 1.0f));
        float fX2 = vector4f.x;
        float fY2 = vector4f.y;
        matrix4f.transform(vector4f.set(f5, f6, 0.0f, 1.0f));
        float fX3 = vector4f.x;
        float fY3 = vector4f.y;
        matrix4f.transform(vector4f.set(f7, f8, 0.0f, 1.0f));
        float fX4 = vector4f.x;
        float fY4 = vector4f.y;
        float fMin = Math.min(Math.min(fX, fX2), Math.min(fX3, fX4));
        float fMax = Math.max(Math.max(fX, fX2), Math.max(fX3, fX4));
        float fMin2 = Math.min(Math.min(fY, fY2), Math.min(fY3, fY4));
        float fMax2 = Math.max(Math.max(fY, fY2), Math.max(fY3, fY4));
        float f13 = fMax - fMin;
        float f14 = fMax2 - fMin2;
        float framebufferHeight = window.getFramebufferHeight() - (scale == 1.0f ? fMax2 : fMax2 * scale);
        float fMethod016 = (scale == 1.0f ? fX : fX * scale);
        float fMethod017 = (scale == 1.0f ? fY : fY * scale);
        float fMethod018 = (scale == 1.0f ? fX2 : fX2 * scale);
        float fMethod019 = (scale == 1.0f ? fY2 : fY2 * scale);
        float fMethod0110 = (scale == 1.0f ? fX3 : fX3 * scale);
        float fMethod0111 = (scale == 1.0f ? fY3 : fY3 * scale);
        float fMethod0112 = (scale == 1.0f ? fX4 : fX4 * scale);
        float fMethod0113 = (scale == 1.0f ? fY4 : fY4 * scale);
        float fMethod0114 = (scale == 1.0f ? fMin : fMin * scale);
        float fMethod0115 = (scale == 1.0f ? f13 : f13 * scale);
        float fMethod0116 = (scale == 1.0f ? f14 : f14 * scale);
        this.lastBounds.setGui(fMin, fMin2, f13, f14);
        this.lastBounds.setGl(fMethod0114, framebufferHeight, fMethod0115, fMethod0116);
        int iMode = class328Var.mode();
        writeVertex(fMethod016, fMethod017, fMethod0114, framebufferHeight, fMethod0115, fMethod0116, f9, f12, 0.0f, 0.0f, 0.0f, 0.0f, i, -1, 0.0f, 0.0f, i5, iMode, i6);
        writeVertex(fMethod018, fMethod019, fMethod0114, framebufferHeight, fMethod0115, fMethod0116, f11, f12, 0.0f, 0.0f, 0.0f, 0.0f, i2, -1, 0.0f, 0.0f, i5, iMode, i6);
        writeVertex(fMethod0110, fMethod0111, fMethod0114, framebufferHeight, fMethod0115, fMethod0116, f11, f10, 0.0f, 0.0f, 0.0f, 0.0f, i3, -1, 0.0f, 0.0f, i5, iMode, i6);
        writeVertex(fMethod0112, fMethod0113, fMethod0114, framebufferHeight, fMethod0115, fMethod0116, f9, f10, 0.0f, 0.0f, 0.0f, 0.0f, i4, -1, 0.0f, 0.0f, i5, iMode, i6);
        int firstVertex = this.vertexCount;
        writeTriangle(firstVertex, firstVertex + 1, firstVertex + 2);
        writeTriangle(firstVertex, firstVertex + 2, firstVertex + 3);
        this.vertexCount = firstVertex + 4;
        this.indexCount += 6;
    }

    public void quadTexture(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, float f11, float f12, int i, int i2, int i3, int i4, int i5) {
        emitTexturedQuad(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, i2, i3, i4, i5, i, 0, ShapeType.TEXTURE);
    }

    public void rectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, int i, int i2, int i3, int i4) {
        emitShape(matrix4f, f, f2, f3, f4, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1, -1, -1, -1, i, i2, i3, i4, 0, 0, ShapeType.COLOR);
    }

    public void roundedBlur(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, int i, int i2) {
        emitShape(matrix4f, f, f2, f3, f4, 0.0f, 0.0f, 1.0f, 1.0f, f5, f6, f7, f8, f9, f10, -1, -1, -1, -1, i, i, i, i, bindTexture(i2), 0, ShapeType.BLUR);
    }

    public void outerMask(Matrix4f matrix4f, int i, int i2) {
        Window window = Mc.INSTANCE.getWindow();
        emitShape(matrix4f, 0.0f, 0.0f, scaledWidth(window), scaledHeight(window), 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0, 0, 0, 0, 0, 0, 0, 0, bindTexture(i), bindTexture(i2), ShapeType.OUTER_MASK);
    }

    public void alphaMask(Matrix4f matrix4f, int i, int i2) {
        Window window = Mc.INSTANCE.getWindow();
        float fMethod007 = scaledWidth(window);
        float fMethod013 = scaledHeight(window);
        int iBindTexture = bindTexture(i);
        int iArgb = this.colorStack.argb(i2, StencilBufferUtil.STENCIL_MASK, StencilBufferUtil.STENCIL_MASK, StencilBufferUtil.STENCIL_MASK);
        emitShape(matrix4f, 0.0f, 0.0f, fMethod007, fMethod013, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0, 0, 0, 0, iArgb, iArgb, iArgb, iArgb, iBindTexture, 0, ShapeType.ALPHA_MASK);
    }

    public void roundedBlur(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, int i, int i2) {
        roundedBlur(matrix4f, f, f2, f3, f4, f5, f5, f5, f5, 0.0f, 0.0f, i, i2);
    }

    public void roundedBlur(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2) {
        roundedBlur(matrix4f, f, f2, f3, f4, f5, f5, f5, f5, f6, 0.0f, i, i2);
    }

    public void roundedBlur(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, int i, int i2) {
        roundedBlur(matrix4f, f, f2, f3, f4, f5, f5, f5, f5, f6, f7, i, i2);
    }

    public void roundedBlur(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int i, int i2) {
        roundedBlur(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, 0.0f, 0.0f, i, i2);
    }

    public void roundedBlur(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, int i, int i2) {
        roundedBlur(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, f9, 0.0f, i, i2);
    }

    public void rectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, int i) {
        rectangle(matrix4f, f, f2, f3, f4, i, i, i, i);
    }

    public void texture(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int i, int i2, int i3, int i4, int i5) {
        emitShape(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1, -1, -1, -1, i2, i3, i4, i5, i, 0, ShapeType.TEXTURE);
    }

    /**
     * Draws a small, single-pass Gaussian approximation from an already bound
     * texture. This is intended for short UI transitions; unlike the global
     * blur pipeline it only shades the submitted rectangle.
     */
    public void softTexture(Matrix4f matrix4f, float x, float y, float width, float height,
                            float minU, float minV, float maxU, float maxV,
                            int texture, int color, float blurRadius) {
        emitShape(matrix4f, x, y, width, height,
                minU, minV, maxU, maxV,
                0.0f, 0.0f, 0.0f, 0.0f,
                0.0f, Math.max(0.0f, blurRadius),
                -1, -1, -1, -1,
                color, color, color, color,
                texture, 0, ShapeType.SOFT_TEXTURE);
    }

    public void textureMsdf(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int i, int i2, int i3, int i4, int i5) {
        emitShape(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, 0.0f, 0.0f, 0.0f, 0.0f, 0.05f, 0.5f, -1, -1, -1, -1, i2, i3, i4, i5, i, 0, ShapeType.MSDF_FONT);
    }

    public void textureSdf(Matrix4f matrix4f, float x, float y, float width, float height,
                           float minU, float minV, float maxU, float maxV,
                           int texture, int color) {
        emitShape(matrix4f, x, y, width, height, minU, minV, maxU, maxV,
                0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.5f,
                -1, -1, -1, -1, color, color, color, color,
                texture, 0, ShapeType.MSDF_FONT);
    }

    public void itemStack(Matrix4f matrix4f, ItemStack itemStack, float f, float f2, float f3, float f4) {
        ItemSpriteTextures spriteTextures;
        Map<Direction, SpriteRegion> mapSprites;
        float f5;
        if (itemStack == null || itemStack.isEmpty() || (spriteTextures = ItemSpriteManager.INSTANCE.getSpriteTextures(itemStack)) == null || (mapSprites = spriteTextures.sprites()) == null || mapSprites.isEmpty()) {
            return;
        }
        float f6 = 32.0f * f3;
        float f7 = f6 / 2.0f;
        float f8 = f6 / 4.0f;
        float f9 = f6 / 16.0f;
        float f10 = (f2 - f8) - (f9 / 2.0f);
        for (Map.Entry<Direction, SpriteRegion> entry : mapSprites.entrySet()) {
            Direction direction = (Direction) entry.getKey();
            SpriteRegion class220Var = (SpriteRegion) entry.getValue();
            if (class220Var != null) {
                int iBindTexture = bindTexture(Mc.INSTANCE.getTextureManager().getTexture(class220Var.atlasId()).getGlId());
                float fMinU = class220Var.minU() + 1.0E-4f;
                float fMinV = class220Var.minV() + 1.0E-4f;
                float fMaxU = class220Var.maxU() - 1.0E-4f;
                float fMaxV = class220Var.maxV() - 1.0E-4f;
                switch (DirectionIndexMap.directionOrdinals[direction.ordinal()]) {
                    case 1:
                        f5 = 1.0f;
                        break;
                    case 2:
                        f5 = 0.7f;
                        break;
                    case 3:
                        f5 = 0.85f;
                        break;
                    default:
                        f5 = 1.0f;
                        break;
                }
                float f11 = f5;
                int iComputeColor = this.colorStack.computeColor(Math.max(0, Math.min(StencilBufferUtil.STENCIL_MASK, (int) (f4 * 255.0f))), (int) (255.0f * f11), (int) (255.0f * f11), (int) (255.0f * f11));
                if (spriteTextures.type().equals("3D")) {
                    switch (DirectionIndexMap.directionOrdinals[direction.ordinal()]) {
                        case 1:
                            quadTexture(matrix4f, f + f7, f10 + f8, f + f6, f10 + f7, f + f7, (f10 + f6) - f8, f, f10 + f7, fMinU, fMinV, fMaxU, fMaxV, iBindTexture, iComputeColor, iComputeColor, iComputeColor, iComputeColor);
                            break;
                        case 2:
                            float f12 = f + f7;
                            float f13 = f10 + f7 + f8;
                            quadTexture(matrix4f, f + f6, f10 + f7, f + f6, f10 + f6 + f9, f + f7, f10 + f6 + f8 + f9, f12, f13, fMinU, fMinV, fMaxU, fMaxV, iBindTexture, iComputeColor, iComputeColor, iComputeColor, iComputeColor);
                            break;
                        case 3:
                            quadTexture(matrix4f, f, f10 + f6 + f9, f + f7, f10 + f6 + f8 + f9, (f + f6) - f7, f10 + f7 + f8, f, f10 + f7, fMinU, fMinV, fMaxU, fMaxV, iBindTexture, iComputeColor, iComputeColor, iComputeColor, iComputeColor);
                            break;
                    }
                } else {
                    f6 = 32.0f * f3;
                    float f14 = f + f6;
                    float f15 = f2 + f6;
                    int iComputeColor2 = this.colorStack.computeColor(class220Var.color(), f4);
                    quadTexture(matrix4f, f, f15, f14, f15, f14, f2, f, f2, fMinU, fMinV, fMaxU, fMaxV, iBindTexture, iComputeColor2, iComputeColor2, iComputeColor2, iComputeColor2);
                }
            }
        }
    }

    public void drawLine(MatrixStack matrixStack, MsdfFont class161Var, int i, int i2, float f, String str, String str2, int i3, int i4) {
        int width = (int) ((i2 - (class161Var.getWidth(str, i) + class161Var.getWidth(str2, i))) / 2.0f);
        msdfFont(matrixStack.peek().getPositionMatrix(), class161Var, str, width, f, i, 0.0f, i3);
        msdfFont(matrixStack.peek().getPositionMatrix(), class161Var, str2, width + class161Var.getWidth(str, i), f, i, 0.0f, i4);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, int i) {
        circle(matrix4f, f, f2, f3, 0.0f, 0.0f, 360.0f, 0.0f, -1, i, i, i, i);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, float f4, int i) {
        circle(matrix4f, f, f2, f3, f4, 0.0f, 360.0f, 0.0f, -1, i, i, i, i);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, int i, int i2, int i3, int i4) {
        circle(matrix4f, f, f2, f3, 0.0f, 0.0f, 360.0f, 0.0f, -1, i, i2, i3, i4);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, float f4, int i, int i2, int i3, int i4) {
        circle(matrix4f, f, f2, f3, f4, 0.0f, 360.0f, 0.0f, -1, i, i2, i3, i4);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, float f4, int i, int i2) {
        circle(matrix4f, f, f2, f3, 0.0f, 0.0f, 360.0f, f4, i, i2, i2, i2, i2);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, int i, int i2) {
        circle(matrix4f, f, f2, f3, f4, 0.0f, 360.0f, f5, i, i2, i2, i2, i2);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, int i) {
        circle(matrix4f, f, f2, f3, 0.0f, f4, f5, 0.0f, -1, i, i, i, i);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, int i) {
        circle(matrix4f, f, f2, f3, f4, f5, f6, 0.0f, -1, i, i, i, i);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2) {
        circle(matrix4f, f, f2, f3, 0.0f, f4, f5, f6, i, i2, i2, i2, i2);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, int i, int i2) {
        circle(matrix4f, f, f2, f3, f4, f5, f6, f7, i, i2, i2, i2, i2);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2, int i3, int i4, int i5) {
        circle(matrix4f, f, f2, f3, 0.0f, f4, f5, f6, i, i2, i3, i4, i5);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, int i, int i2, int i3, int i4, int i5) {
        float f8 = f3 * 2.0f;
        float fRad = MathUtil.rad(f5);
        float fRad2 = MathUtil.rad(f6);
        float f9 = f7 > 0.0f ? 1.0f : 0.0f;
        float f10 = (f7 > 0.0f ? f7 * 0.5f : 0.0f) + (f7 > 0.0f ? f9 : f9 + 1.0f) + 1.0f;
        float f11 = (f3 * 2.0f) + (f10 * 2.0f);
        emitShape(matrix4f, (f - f3) - f10, (f2 - f3) - f10, f11, f11, fRad, fRad2, fRad, fRad2, f3, f4, 0.0f, 0.0f, f7, f9, i, i, i, i, i2, i3, i4, i5, 0, 0, ShapeType.CIRCLE);
    }

    public void msdfFont(Matrix4f matrix4f, MsdfFont class161Var, String str, float f, float f2, float f3, float f4, int i) {
        int slot = bindTexture(class161Var.getTextureId());
        RectangleMeshCache mesh = null;
        if (!str.isEmpty() && str.length() <= 64 && str.indexOf('\u00a7') < 0) {
            TextMeshKey key = new TextMeshKey(class161Var, str, slot);
            mesh = this.textMeshes.get(key);
            if (mesh == null) {
                mesh = new RectangleMeshCache();
                this.textMeshes.put(key, mesh);
            }
            if (mesh.replayOrBegin(this, matrix4f, f, f2, f3, f4, 0, 0, 0,
                    i, Mc.INSTANCE.getWindow().getFramebufferHeight())) return;
        }
        class161Var.applyGlyphs(matrix4f, this, slot, str, f3, 0.0f, 0.0f, f, f2 + ((class161Var.metrics().ascent() != 0.0f ? class161Var.metrics().ascent() : class161Var.metrics().lineHeight()) * f3), i);
        if (mesh != null) mesh.finish(this);
    }

    public void msdfText(Matrix4f matrix4f, MsdfFont class161Var, Text text, float f, float f2, float f3, float f4, float f5, int i) {
        int iBindTexture = bindTexture(class161Var.getTextureId());
        float fAscent = class161Var.metrics().ascent() != 0.0f ? class161Var.metrics().ascent() : class161Var.metrics().lineHeight();
        float height = class161Var.getHeight(f3);
        int i2 = (i >> 24) & StencilBufferUtil.STENCIL_MASK;
        float[] fArr = {0.0f};
        float[] fArr2 = {0.0f};
        int[] iArr = {i};
        boolean[] zArr = {false};
        int[] iArrGlyphCodes = class161Var.glyphCodes();
        ThreadLocalRandom threadLocalRandomCurrent = ThreadLocalRandom.current();
        int[] iArr2 = {-1};
        text.asOrderedText().accept((charIndex, style, codePoint) -> {
            int i3;
            char c = (char) codePoint;
            if (c == '\n') {
                fArr[0] = 0.0f;
                fArr2[0] = fArr2[0] + height;
                iArr2[0] = -1;
                return true;
            }
            if (style.getColor() != null) {
                iArr[0] = (i2 << 24) | (style.getColor().getRgb() & 16777215);
                zArr[0] = true;
            } else if (zArr[0]) {
                iArr[0] = i;
                zArr[0] = false;
            }
            int i4 = c;
            if (style.isObfuscated() && !Character.isWhitespace(c)) {
                if (iArrGlyphCodes.length == 0) {
                    return true;
                }
                int i5 = 0;
                do {
                    i3 = iArrGlyphCodes[threadLocalRandomCurrent.nextInt(iArrGlyphCodes.length)];
                    i5++;
                    if (class161Var.glyphWidth(i3, f3) > 0.01f) {
                        break;
                    }
                } while (i5 < 20);
                i4 = i3;
            }
            int iGlyphIndex = class161Var.glyphIndex(i4);
            if (iGlyphIndex == -1) {
                iArr2[0] = -1;
                return true;
            }
            if (iArr2[0] != -1) {
                fArr[0] = fArr[0] + (class161Var.kerningAdvance(iArr2[0], i4) * f3);
            }
            fArr[0] = fArr[0] + class161Var.applyGlyph(matrix4f, this, iGlyphIndex, f3, f + fArr[0], f2 + fArr2[0] + (fAscent * f3), iArr[0], iBindTexture) + f4 + f5;
            iArr2[0] = i4;
            return true;
        });
    }

    public void msdfText(Matrix4f matrix4f, MsdfFont class161Var, Text text, float f, float f2, float f3, int i) {
        msdfText(matrix4f, class161Var, text, f, f2, f3, 0.05f, 0.0f, i);
    }

    public void hollowCircle(Matrix4f matrix4f, float f, float f2, float f3, float f4, int i) {
        circle(matrix4f, f, f2, f3, Math.max(0.0f, f3 - f4), 0.0f, 360.0f, 0.0f, -1, i, i, i, i);
    }

    public void arc(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, int i) {
        circle(matrix4f, f, f2, f3, Math.max(0.0f, f3 - f6), f4, f5, 0.0f, -1, i, i, i, i);
    }

    public void circleChecker(Matrix4f matrix4f, float f, float f2, float f3, int i, int i2) {
        roundedChecker(matrix4f, f - f3, f2 - f3, f3 * 2.0f, f3 * 2.0f, f3, 2.0f, i, i2);
    }

    public void roundedChecker(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2) {
        emitShape(matrix4f, f, f2, f3, f4, 0.0f, 0.0f, 1.0f, 1.0f, f5, f5, f5, f5, 0.0f, f6, i, i, i, i, i2, i2, i2, i2, 0, 0, ShapeType.CHECKER);
    }

    public void msdfFontHorizontalC(Matrix4f matrix4f, MsdfFont class161Var, String str, float f, float f2, float f3, float f4, int i) {
        msdfFont(matrix4f, class161Var, str, f - Math.round(class161Var.getWidth(str, f3) / 2.0f), f2, f3, f4, i);
    }

    public void msdfFontVerticalC(Matrix4f matrix4f, MsdfFont class161Var, String str, float f, float f2, float f3, float f4, int i) {
        msdfFont(matrix4f, class161Var, str, f, f2 - Math.round(class161Var.getHeight(f3) / 2.0f), f3, f4, i);
    }

    public void msdfFontVerticalCHorizontalC(Matrix4f matrix4f, MsdfFont class161Var, String str, float f, float f2, float f3, float f4, int i) {
        msdfFont(matrix4f, class161Var, str, f - Math.round(class161Var.getWidth(str, f3) / 2.0f), Math.round(f2 - (class161Var.getHeight(f3) / 2.0f)), f3, f4, i);
    }

    public void msdfFontWithHorizontalGradient(Matrix4f matrix4f, MsdfFont class161Var, String str, float f, float f2, float f3, float f4, int i, int i2) {
        class161Var.applyGlyphsWithHorizontalGradient(matrix4f, this, bindTexture(class161Var.getTextureId()), str, f3, 0.0f, 0.0f, f, f2 + ((class161Var.metrics().ascent() != 0.0f ? class161Var.metrics().ascent() : class161Var.metrics().lineHeight()) * f3), i, i2);
    }

    public void textureMsdf(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int i, int i2) {
        textureMsdf(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, i, i2, i2, i2, i2);
    }

    public void texture(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int i, int i2) {
        texture(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, i, i2, i2, i2, i2);
    }

    public void texture(Matrix4f matrix4f, float f, float f2, float f3, float f4, int i, int i2, int i3, int i4, int i5) {
        texture(matrix4f, f, f2, f3, f4, 0.0f, 0.0f, 1.0f, 1.0f, i, i2, i3, i4, i5);
    }

    public void textureVerticalC(Matrix4f matrix4f, GlTexture class073Var, float f, float f2, int i, int i2, int i3) {
        texture(matrix4f, f, f2 - (MathHelper.ceil(i2) / 2.0f), i, i2, bindTexture(class073Var.textureWithSTB()), i3);
    }

    public void textureVerticalC(Matrix4f matrix4f, SvgTexture texture, float x, float centerY,
                                 int width, int height, int color) {
        texture(matrix4f, x, centerY - (MathHelper.ceil(height) / 2.0f), width, height,
                bindTexture(texture.id()), color);
    }

    public void textureVerticalCHorizontalC(Matrix4f matrix4f, GlTexture class073Var, float f, float f2, int i, int i2, int i3) {
        texture(matrix4f, f - (MathHelper.ceil(i) / 2.0f), f2 - (MathHelper.ceil(i2) / 2.0f), i, i2, bindTexture(class073Var.textureWithSTB()), i3);
    }

    public void texture(Matrix4f matrix4f, float f, float f2, float f3, float f4, int i, int i2) {
        texture(matrix4f, f, f2, f3, f4, 0.0f, 0.0f, 1.0f, 1.0f, i, i2, i2, i2, i2);
    }

    public void texture(Matrix4f matrix4f, float f, float f2, float f3, float f4, int i, int i2, boolean z) {
        if (z) {
            texture(matrix4f, f, f2, f3, f4, 0.0f, 1.0f, 1.0f, 0.0f, i, i2, i2, i2, i2);
        } else {
            texture(matrix4f, f, f2, f3, f4, 0.0f, 0.0f, 1.0f, 1.0f, i, i2, i2, i2, i2);
        }
    }

    public void roundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        emitShape(matrix4f, f, f2, f3, f4, 0.0f, 0.0f, 1.0f, 1.0f, f5, f6, f7, f8, f9, f10, i, i2, i3, i4, i5, i6, i7, i8, 0, 0, ShapeType.ROUNDED_RECTANGLE);
    }

    public void glassPanel(Matrix4f matrix4f, float x, float y, float width, float height,
                           float radius, float thickness, int outlineColor, int fillColor,
                           int blurTexture) {
        int textureIndex = blurTexture > 0 ? bindTexture(blurTexture) : 0;
        int hasBlur = blurTexture > 0 ? 1 : 0;
        emitShape(matrix4f, x, y, width, height,
                0.0f, 0.0f, 1.0f, 1.0f,
                radius, radius, radius, radius,
                thickness, 0.0f,
                outlineColor, outlineColor, outlineColor, outlineColor,
                fillColor, fillColor, fillColor, fillColor,
                textureIndex, hasBlur, ShapeType.GLASS_PANEL);
    }

    public void radialRoundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, int i, int i2) {
        emitShape(matrix4f, f, f2, f3, f4, 0.0f, 0.0f, 1.0f, 1.0f, f5, f5, f5, f5, 0.0f, 0.0f, i2, i2, i2, i2, i, i, i, i, 0, 0, ShapeType.RADIAL_ROUNDED_RECTANGLE);
    }

    public void roundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, int i) {
        roundedRectangle(matrix4f, f, f2, f3, f4, f5, f5, f5, f5, 0.0f, 0.0f, -1, -1, -1, -1, i, i, i, i);
    }

    public void roundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2) {
        roundedRectangle(matrix4f, f, f2, f3, f4, f5, f5, f5, f5, f6, 1.0f, i, i, i, i, i2, i2, i2, i2);
    }

    public void roundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        roundedRectangle(matrix4f, f, f2, f3, f4, f5, f5, f5, f5, f6, 1.0f, i, i2, i3, i4, i5, i6, i7, i8);
    }

    public void roundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, int i, int i2, int i3, int i4) {
        roundedRectangle(matrix4f, f, f2, f3, f4, f5, f5, f5, f5, 0.0f, 0.0f, -1, -1, -1, -1, i, i2, i3, i4);
    }

    public void roundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2, int i3, int i4, int i5) {
        roundedRectangle(matrix4f, f, f2, f3, f4, f5, f5, f5, f5, f6, 1.0f, i, i, i, i, i2, i3, i4, i5);
    }

    public void roundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, int i, int i2, int i3, int i4, int i5) {
        roundedRectangle(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, f9, 1.0f, i, i, i, i, i2, i3, i4, i5);
    }

    public void roundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int i) {
        roundedRectangle(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, 0.0f, 0.0f, -1, -1, -1, -1, i, i, i, i);
    }

    public void roundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, int i, int i2) {
        roundedRectangle(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, f9, f10, i, i, i, i, i2, i2, i2, i2);
    }

    public void roundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, int i) {
        roundedRectangle(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, 0.0f, f9, -1, -1, -1, -1, i, i, i, i);
    }

    public void roundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, int i, int i2) {
        roundedRectangle(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, f9, 0.0f, i, i, i, i, i2, i2, i2, i2);
    }

    public void roundedTexture(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int i, int i2, int i3, int i4, int i5) {
        emitShape(matrix4f, f, f2, f3, f4, 0.0f, 0.0f, 1.0f, 1.0f, f5, f6, f7, f8, 0.0f, 0.0f, -1, -1, -1, -1, i2, i3, i4, i5, i, 0, ShapeType.ROUNDED_TEXTURE);
    }

    public void roundedTexture(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, float f11, float f12, int i, int i2, int i3, int i4, int i5) {
        emitShape(matrix4f, f, f2, f3, f4, f9, f10, f11, f12, f5, f6, f7, f8, 0.0f, 0.0f, -1, -1, -1, -1, i2, i3, i4, i5, i, 0, ShapeType.ROUNDED_TEXTURE);
    }

    public void roundedTexture(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int i, int i2) {
        roundedTexture(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, i, i2, i2, i2, i2);
    }

    public void roundedTexture(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, int i, int i2, int i3, int i4, int i5) {
        roundedTexture(matrix4f, f, f2, f3, f4, f5, f5, f5, f5, i, i2, i3, i4, i5);
    }

    public void roundedTexture(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, int i, int i2) {
        roundedTexture(matrix4f, f, f2, f3, f4, f5, f5, f5, f5, i, i2, i2, i2, i2);
    }

    public void roundedTexture(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, int i, int i2) {
        roundedTexture(matrix4f, f, f2, f3, f4, f5, f5, f5, f5, f6, f7, f8, f9, i, i2, i2, i2, i2);
    }

    // Keep the buffer writes in one Java call per vertex. Geometry/material
    // decisions stay in the native emitters; the buffer still owns allocation.
    private void writeTransformedVertex(Matrix4f matrix, float x, float y,
            float bx, float by, float bw, float bh, float u, float v,
            float r0, float r1, float r2, float r3, int color, int border,
            float p0, float p1, int texture, int mode, int extra) {
        writeTransformedPosition(matrix, x, y);
        writeVertexAttributes(bx, by, bw, bh, u, v, r0, r1, r2, r3,
                color, border, p0, p1, texture, mode, extra);
    }

    private void writeVertex(float x, float y,
            float bx, float by, float bw, float bh, float u, float v,
            float r0, float r1, float r2, float r3, int color, int border,
            float p0, float p1, int texture, int mode, int extra) {
        writeVec2(x, y);
        writeVertexAttributes(bx, by, bw, bh, u, v, r0, r1, r2, r3,
                color, border, p0, p1, texture, mode, extra);
    }

    private void writeVertexAttributes(float bx, float by, float bw, float bh,
            float u, float v, float r0, float r1, float r2, float r3,
            int color, int border, float p0, float p1, int texture, int mode, int extra) {
        writeVec2(bx, by);
        writeVec2(bw, bh);
        writeVec2(u, v);
        writeVec4(r0, r1, r2, r3);
        writeColor(color);
        writeColor(border);
        writeFloat(p0);
        writeFloat(p1);
        writeByte(texture);
        writeByte(mode);
        writeByte(extra);
    }

    public void writeTransformedPosition(Matrix4f matrix4f, float f, float f2) {
        Vector4f vector4fTransform = matrix4f.transform(this.tempVector.set(f, f2, 0.0f, 1.0f));
        writeVec2(applyContentScale(vector4fTransform.x()), applyContentScale(vector4fTransform.y()));
    }

    public void updateContentScale(Window window) {
        if (WindowUtil.isMac()) {
            this.contentScale = WindowUtil.getWindowContentScale(window.getHandle());
        } else {
            this.contentScale = 1.0f;
        }
    }

    public float applyContentScale(float f) {
        return this.contentScale == 1.0f ? f : f * this.contentScale;
    }

    public int scaledWidth(Window window) {
        return Math.max(1, Math.round(window.getFramebufferWidth() / this.contentScale));
    }

    public int scaledHeight(Window window) {
        return Math.max(1, Math.round(window.getFramebufferHeight() / this.contentScale));
    }

    public void writeVec2(float f, float f2) {
        this.vertexBuffer.requireMoreFreeBytes(8L);
        long jEffectiveAddress = this.vertexBuffer.effectiveAddress();
        this.vertexBuffer.writeFloat(jEffectiveAddress, f);
        this.vertexBuffer.writeFloat(jEffectiveAddress + 4, f2);
        this.vertexBuffer.offset(8L);
    }

    public void writeTriangle(int i, int i2, int i3) {
        this.indexBuffer.requireMoreFreeBytes(12L);
        long jEffectiveAddress = this.indexBuffer.effectiveAddress();
        this.indexBuffer.writeInt(jEffectiveAddress, i);
        this.indexBuffer.writeInt(jEffectiveAddress + 4, i2);
        this.indexBuffer.writeInt(jEffectiveAddress + 8, i3);
        this.indexBuffer.offset(12L);
    }

    public void writeVec4(float f, float f2, float f3, float f4) {
        this.vertexBuffer.requireMoreFreeBytes(16L);
        long jEffectiveAddress = this.vertexBuffer.effectiveAddress();
        this.vertexBuffer.writeFloat(jEffectiveAddress, f);
        this.vertexBuffer.writeFloat(jEffectiveAddress + 4, f2);
        this.vertexBuffer.writeFloat(jEffectiveAddress + 8, f3);
        this.vertexBuffer.writeFloat(jEffectiveAddress + 12, f4);
        this.vertexBuffer.offset(16L);
    }

    public void writeColorBytes(int i, int i2, int i3, int i4) {
        this.vertexBuffer.requireMoreFreeBytes(4L);
        long jEffectiveAddress = this.vertexBuffer.effectiveAddress();
        this.vertexBuffer.writeByte(jEffectiveAddress, i);
        this.vertexBuffer.writeByte(jEffectiveAddress + 1, i2);
        this.vertexBuffer.writeByte(jEffectiveAddress + 2, i3);
        this.vertexBuffer.writeByte(jEffectiveAddress + 3, i4);
        this.vertexBuffer.offset(4L);
    }

    public void writeInt(int i) {
        this.vertexBuffer.requireMoreFreeBytes(4L);
        this.vertexBuffer.writeInt(this.vertexBuffer.effectiveAddress(), i);
        this.vertexBuffer.offset(4L);
    }

    public void writeFloat(float f) {
        this.vertexBuffer.requireMoreFreeBytes(4L);
        this.vertexBuffer.writeFloat(this.vertexBuffer.effectiveAddress(), f);
        this.vertexBuffer.offset(4L);
    }

    public void writeByte(int i) {
        this.vertexBuffer.requireMoreFreeBytes(1L);
        this.vertexBuffer.writeByte(this.vertexBuffer.effectiveAddress(), i);
        this.vertexBuffer.offset(1L);
    }

    public void writeColor(int i) {
        int alpha = MathUtil.clamp(Math.round(
                ((i >> 24) & StencilBufferUtil.STENCIL_MASK)
                        * this.vertexAlphaStack[this.vertexAlphaPointer]
        ), 0, StencilBufferUtil.STENCIL_MASK);
        writeColorBytes((i >> 16) & StencilBufferUtil.STENCIL_MASK,
                (i >> 8) & StencilBufferUtil.STENCIL_MASK,
                i & StencilBufferUtil.STENCIL_MASK, alpha);
    }

    public float vertexAlpha() {
        return this.vertexAlphaStack[this.vertexAlphaPointer];
    }

    public void pushVertexAlpha(float alpha) {
        int next = this.vertexAlphaPointer + 1;
        if (next >= this.vertexAlphaStack.length) {
            throw new IllegalStateException("DrawEngine vertex alpha stack overflow");
        }
        this.vertexAlphaStack[next] = this.vertexAlphaStack[this.vertexAlphaPointer]
                * MathUtil.clamp(alpha, 0.0f, 1.0f);
        this.vertexAlphaPointer = next;
    }

    public void popVertexAlpha() {
        if (this.vertexAlphaPointer == 0) {
            throw new IllegalStateException("DrawEngine vertex alpha stack underflow");
        }
        this.vertexAlphaPointer--;
    }

    public ColorStack colorStack() {
        return this.colorStack;
    }

    public ScissorStack scissorStack() {
        return this.scissorStack;
    }

    public boolean building() {
        return this.building;
    }

    public ScissorBounds lastBounds() {
        return this.lastBounds;
    }
}
