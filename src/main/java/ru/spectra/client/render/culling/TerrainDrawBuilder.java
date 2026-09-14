package ru.spectra.client.render.culling;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gl.device.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataUnsafe;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortBehavior;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;
import ru.spectra.client.Spectra;
import ru.spectra.client.module.OptimizerModule;

/** Only rewrites draw ranges. Vertex/index buffers, chunk tasks and lighting stay owned by Sodium. */
public final class TerrainDrawBuilder implements AutoCloseable {
    private final Matrix4f clip = new Matrix4f();
    private final TerrainFrustum frustum = new TerrainFrustum();
    private final Matrix4f relativeClip = new Matrix4f();
    private final Matrix4f checkedClip = new Matrix4f().zero();
    private double checkedX = Double.NaN, checkedY, checkedZ;
    private GuardedTerrainFrustum guard;
    private CameraTransform anchor;
    private long viewVersion;
    private double cameraX = Double.NaN, cameraY, cameraZ;
    private final ShaderCullingGuard shaders = new ShaderCullingGuard();
    private MultiDrawBatch batch;
    private boolean indexed;
    private int previousBase, previousElements;
    private long previousPointer;
    private long inputQuads;
    private long emittedQuads;
    private long fallbackQuads;
    private TerrainDrawCache cache;
    private java.util.IdentityHashMap<TerrainRenderPass, TerrainDrawCache> caches;
    private final boolean cachedBuilder;
    private long sourceView = Long.MIN_VALUE;

    public TerrainDrawBuilder() { this(false); }
    TerrainDrawBuilder(boolean cachedBuilder) { this.cachedBuilder = cachedBuilder; }
    public boolean isCachedBuilder() { return this.cachedBuilder; }
    long viewVersion() { return this.viewVersion; }

    void copyView(TerrainDrawBuilder source) {
        if (this.sourceView != source.viewVersion) {
            this.sourceView = source.viewVersion;
            this.viewVersion++;
            this.frustum.copyFrom(source.frustum);
            this.anchor = source.anchor;
        }
    }

    public boolean begin(ChunkRenderMatrices matrices, TerrainRenderPass pass, CameraTransform camera) {
        this.cache = null;
        Spectra client = Spectra.INSTANCE;
        if (client == null || client.moduleRepository() == null) return false;
        OptimizerModule optimizer = client.moduleRepository().get(OptimizerModule.class);
        if (!optimizer.isState() || !optimizer.worldCulling.isValue() || !TerrainFrameView.allowCaching()
                || this.shaders.requiresNativeCulling()) return false;
        // With sorting disabled Sodium uses a different, shared index layout.
        if (pass.isTranslucent() && SodiumClientMod.options().debug.getSortBehavior() == SortBehavior.OFF) return false;
        this.clip.set(matrices.projection()).mul(matrices.modelView());
        if (!this.clip.isFinite() || this.clip.determinant() == 0.0f) return false;
        updateView(camera);
        if (this.caches == null) this.caches = new java.util.IdentityHashMap<>(3);
        this.cache = this.caches.computeIfAbsent(pass, ignored -> new TerrainDrawCache());
        this.cache.beginFrame();
        return true;
    }

    private void updateView(CameraTransform camera) {
        if (this.checkedX == camera.x && this.checkedY == camera.y && this.checkedZ == camera.z
                && this.checkedClip.equals(this.clip)) return;
        this.checkedClip.set(this.clip);
        this.checkedX = camera.x; this.checkedY = camera.y; this.checkedZ = camera.z;
        this.relativeClip.set(this.clip).translate((float) (this.cameraX - camera.x),
                (float) (this.cameraY - camera.y), (float) (this.cameraZ - camera.z));
        if (this.guard == null || !GuardedTerrainFrustum.sameFaceCell(this.anchor.intX, camera.intX)
                || !GuardedTerrainFrustum.sameFaceCell(this.anchor.intY, camera.intY) || !GuardedTerrainFrustum.sameFaceCell(this.anchor.intZ, camera.intZ)
                || !this.guard.contains(this.relativeClip)) {
            this.anchor = camera;
            this.cameraX = camera.x; this.cameraY = camera.y; this.cameraZ = camera.z;
            this.viewVersion++;
            this.guard = new GuardedTerrainFrustum(this.clip, 2);
            this.frustum.set(this.relativeClip.set(this.clip).scaleLocal(0.9f, 0.9f, 1));
            this.frustum.expand(2);
        }
    }

    public MultiDrawBatch build(RenderRegion region, SectionRenderDataStorage storage,
                               ChunkRenderList list, CameraTransform camera,
                               TerrainRenderPass pass, boolean faceCulling) {
        if (this.cache != null) {
            TerrainDrawBuilder prepared = this.cache.get(this, region, storage, list, camera, pass, faceCulling);
            this.inputQuads = prepared.inputQuads;
            this.emittedQuads = prepared.emittedQuads;
            this.fallbackQuads = prepared.fallbackQuads;
            return prepared.batch;
        }
        ensureCapacity(1);
        this.batch.clear();
        this.indexed = pass.isTranslucent();
        this.inputQuads = this.emittedQuads = this.fallbackQuads = 0;
        var iterator = list.sectionsWithGeometryIterator(pass.isTranslucent());
        if (iterator == null) return this.batch;
        // Match the shader's region-offset arithmetic, including its split camera
        // coordinates. No loss of precision at the world border.
        float regionX = (float) (region.getOriginX() - this.anchor.intX) - this.anchor.fracX;
        float regionY = (float) (region.getOriginY() - this.anchor.intY) - this.anchor.fracY;
        float regionZ = (float) (region.getOriginZ() - this.anchor.intZ) - this.anchor.fracZ;
        int regionPlanes = this.frustum.classifyBox(regionX - 8, regionY - 8, regionZ - 8,
                regionX + RenderRegion.REGION_WIDTH * 16 + 8,
                regionY + RenderRegion.REGION_HEIGHT * 16 + 8,
                regionZ + RenderRegion.REGION_LENGTH * 16 + 8, 15);
        while (iterator.hasNext()) {
            int index = iterator.nextByteAsInt();
            RenderSection section = region.getSection(index);
            long meshPointer = storage.getDataPointer(index);
            int mask = SectionRenderDataUnsafe.getSliceMask(meshPointer);
            if (faceCulling) mask &= visibleFaces(camera, section);
            if (mask == 0) continue;
            TerrainMeshData data = ((TerrainSectionAccess) section).spectra$terrainMesh(pass);
            float x = regionX + (section.getOriginX() - region.getOriginX());
            float y = regionY + (section.getOriginY() - region.getOriginY());
            float z = regionZ + (section.getOriginZ() - region.getOriginZ());
            int planes = data == null || (this.indexed && !data.hasIndexOrder()) ? 15 : regionPlanes <= 0 ? regionPlanes
                    : data.classify(this.frustum, x, y, z, regionPlanes);
            long elementOffset = pass.isTranslucent() ? SectionRenderDataUnsafe.getBaseElement(meshPointer) : 0;
            for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
                int elements = (int) SectionRenderDataUnsafe.getElementCount(meshPointer, facing);
                long offset = elementOffset;
                if (pass.isTranslucent()) elementOffset += Integer.toUnsignedLong(elements);
                if ((mask & (1 << facing)) == 0 || elements == 0) continue;
                int baseVertex = (int) SectionRenderDataUnsafe.getVertexOffset(meshPointer, facing);
                int quads = elements / 6;
                this.inputQuads += quads;
                if (planes < 0) continue;
                if (planes == 0) {
                    append(baseVertex, elements, pass.isTranslucent() ? offset * 4 : 0);
                    this.emittedQuads += quads;
                    continue;
                }
                boolean supported = data != null && (!this.indexed || data.hasIndexOrder());
                if (!supported || data.quadCount(facing) != quads || elements % 6 != 0) {
                    // A missing/new/unsupported mesh must remain visible.
                    append(baseVertex, elements, pass.isTranslucent() ? offset * 4 : 0);
                    this.emittedQuads += quads;
                    this.fallbackQuads += quads;
                    continue;
                }
                if (data.isOutside(facing, this.frustum, x, y, z, planes)) continue;
                // Keep intersecting slices intact: no per-quad scans or fragmented draws.
                append(baseVertex, elements, pass.isTranslucent() ? offset * 4 : 0);
                this.emittedQuads += quads;
            }
        }
        return this.batch;
    }

    private static int visibleFaces(CameraTransform camera, RenderSection section) {
        // Preserve Sodium 0.6.13's original directional face mask exactly.
        int x = section.getOriginX(), y = section.getOriginY(), z = section.getOriginZ();
        int mask = 1 << ModelQuadFacing.UNASSIGNED.ordinal();
        if (camera.intX > x - 3) mask |= 1 << ModelQuadFacing.POS_X.ordinal();
        if (camera.intY > y - 3) mask |= 1 << ModelQuadFacing.POS_Y.ordinal();
        if (camera.intZ > z - 3) mask |= 1 << ModelQuadFacing.POS_Z.ordinal();
        if (camera.intX < x + 19) mask |= 1 << ModelQuadFacing.NEG_X.ordinal();
        if (camera.intY < y + 19) mask |= 1 << ModelQuadFacing.NEG_Y.ordinal();
        if (camera.intZ < z + 19) mask |= 1 << ModelQuadFacing.NEG_Z.ordinal();
        return mask;
    }

    private void append(int baseVertex, int elements, long elementPointer) {
        // Coalesce only physically consecutive ranges. This preserves both the
        // quad order and sorted transparency, without reintroducing culled faces.
        if (this.batch.size > 0 && elements % 6 == 0 && this.previousElements % 6 == 0
                && (long) this.previousElements + elements <= Integer.MAX_VALUE
                && (this.indexed
                ? this.previousBase == baseVertex && this.previousPointer + this.previousElements * 4L == elementPointer
                : (long) this.previousBase + this.previousElements / 6L * 4 == baseVertex)) {
            this.previousElements += elements;
            MemoryUtil.memPutInt(this.batch.pElementCount + (this.batch.size - 1) * 4L, this.previousElements);
            return;
        }
        ensureCapacity(this.batch.size + 1);
        int index = this.batch.size++;
        MemoryUtil.memPutInt(this.batch.pBaseVertex + index * 4L, baseVertex);
        MemoryUtil.memPutInt(this.batch.pElementCount + index * 4L, elements);
        MemoryUtil.memPutAddress(this.batch.pElementPointer + (long) index * Pointer.POINTER_SIZE, elementPointer);
        this.previousBase = baseVertex;
        this.previousElements = elements;
        this.previousPointer = elementPointer;
    }

    private void ensureCapacity(int required) {
        if (this.batch != null && this.batch.capacity() >= required) return;
        MultiDrawBatch replacement = new MultiDrawBatch(Math.max(2048,
                Math.max(required, this.batch == null ? 0 : this.batch.capacity() * 2)));
        if (this.batch != null) {
            int size = this.batch.size;
            MemoryUtil.memCopy(this.batch.pBaseVertex, replacement.pBaseVertex, size * 4L);
            MemoryUtil.memCopy(this.batch.pElementCount, replacement.pElementCount, size * 4L);
            MemoryUtil.memCopy(this.batch.pElementPointer, replacement.pElementPointer, (long) size * Pointer.POINTER_SIZE);
            replacement.size = size;
            this.batch.delete();
        }
        this.batch = replacement;
    }

    public long inputQuads() { return this.inputQuads; }
    public long emittedQuads() { return this.emittedQuads; }
    public long fallbackQuads() { return this.fallbackQuads; }

    @Override
    public void close() {
        if (this.caches != null) {
            this.caches.values().forEach(TerrainDrawCache::close);
            this.caches.clear(); this.caches = null; this.cache = null;
        }
        if (this.batch != null) {
            this.batch.delete();
            this.batch = null;
        }
    }
}



