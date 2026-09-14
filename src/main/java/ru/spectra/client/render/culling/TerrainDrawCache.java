package ru.spectra.client.render.culling;

import java.util.IdentityHashMap;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;

/** Reuses complete command buffers only while their geometry and visibility remain valid. */
final class TerrainDrawCache implements AutoCloseable {
    private final IdentityHashMap<RenderRegion, Slot> cache = new IdentityHashMap<>();
    private long frame;

    void beginFrame() {
        this.frame++;
        // Release native buffers for regions that have left the view/world.
        if ((this.frame & 63) == 0) {
            this.cache.values().removeIf(slot -> {
                if (this.frame - slot.seen <= 128) return false;
                slot.builder.close();
                return true;
            });
        }
    }

    TerrainDrawBuilder get(TerrainDrawBuilder source, RenderRegion region, SectionRenderDataStorage storage,
                           ChunkRenderList list, CameraTransform camera, TerrainRenderPass pass, boolean faceCulling) {
        Slot slot = this.cache.computeIfAbsent(region, ignored -> new Slot());
        long revision = ((TerrainRegionAccess) region).spectra$meshRevision();
        boolean sameSections = slot.compareSections(list, pass.isTranslucent());
        boolean unchanged = slot.view == source.viewVersion() && slot.meshes == revision
                && slot.faceCulling == faceCulling && slot.storage == storage && sameSections;
        slot.storage = storage;
        slot.seen = this.frame;
        if (!unchanged) {
            slot.builder.copyView(source);
            slot.builder.build(region, storage, list, camera, pass, faceCulling);
            slot.view = source.viewVersion(); slot.meshes = revision; slot.faceCulling = faceCulling;
        }
        return slot.builder;
    }

    @Override public void close() {
        for (Slot slot : this.cache.values()) slot.builder.close();
        this.cache.clear();
    }

    private static final class Slot {
        final TerrainDrawBuilder builder = new TerrainDrawBuilder(true);
        SectionRenderDataStorage storage;
        final byte[] sections = new byte[256];
        int sectionCount;
        long view = Long.MIN_VALUE, meshes = Long.MIN_VALUE, seen;
        boolean faceCulling;

        boolean compareSections(ChunkRenderList list, boolean reverse) {
            var iterator = list.sectionsWithGeometryIterator(reverse);
            int count = 0;
            boolean same = true;
            if (iterator != null) while (iterator.hasNext()) {
                byte section = (byte) iterator.nextByteAsInt();
                same &= count < this.sectionCount && this.sections[count] == section;
                this.sections[count++] = section;
            }
            same &= count == this.sectionCount;
            this.sectionCount = count;
            return same;
        }
    }
}
