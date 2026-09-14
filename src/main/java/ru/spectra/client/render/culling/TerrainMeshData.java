package ru.spectra.client.render.culling;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/** Compact bounds of seven complete draw slices; independent of native buffer lifetime. */
public final class TerrainMeshData {
    private static final int COMPACT_STRIDE = 20;
    private static final int FACES = 7;
    private final char[] bounds = new char[(FACES + 1) * 6];
    private final int[] counts;
    private boolean indexOrderValid;

    private TerrainMeshData(int[] counts) {
        this.counts = counts.clone();
        for (int face = 0; face <= FACES; face++) {
            Arrays.fill(this.bounds, face * 6, face * 6 + 3, Character.MAX_VALUE);
        }
    }

    public int classify(TerrainFrustum frustum, float x, float y, float z, int planes) {
        frustum.atOrigin(x, y, z);
        return frustum.classify(this.bounds, FACES * 6, planes);
    }

    public int quadCount(int face) { return this.counts[face] / 4; }

    public boolean isOutside(int face, TerrainFrustum frustum, float x, float y, float z, int planes) {
        frustum.atOrigin(x, y, z);
        return this.counts[face] == 0 || frustum.isOutside(this.bounds, face * 6, planes);
    }

    public static TerrainMeshData decode(ByteBuffer vertices, int[] vertexCounts) {
        if (vertexCounts.length != FACES) return null;
        long vertexCount = 0;
        for (int count : vertexCounts) {
            if (count < 0 || count % 4 != 0) return null;
            vertexCount += count;
        }
        if (vertexCount * COMPACT_STRIDE != vertices.remaining()) return null;
        ByteBuffer input = vertices.duplicate().order(ByteOrder.nativeOrder());
        TerrainMeshData mesh = new TerrainMeshData(vertexCounts);
        int position = input.position();
        for (int face = 0; face < FACES; face++) {
            for (int v = 0; v < vertexCounts[face]; v++, position += COMPACT_STRIDE) {
                int hi = input.getInt(position), lo = input.getInt(position + 4);
                for (int axis = 0; axis < 3; axis++) {
                    int shift = axis * 10;
                    int encoded = (((hi >>> shift) & 1023) << 10) | ((lo >>> shift) & 1023);
                    char coordinate = (char) (encoded >>> 4);
                    mesh.include(face * 6, axis, coordinate);
                    mesh.include(FACES * 6, axis, coordinate);
                }
            }
        }
        return mesh;
    }

    private void include(int offset, int axis, char coordinate) {
        this.bounds[offset + axis] = (char) Math.min(this.bounds[offset + axis], coordinate);
        this.bounds[offset + axis + 3] = (char) Math.max(this.bounds[offset + axis + 3], coordinate);
    }

    public boolean hasIndexOrder() { return this.indexOrderValid; }

    public void reuseOrder(TerrainMeshData previous) {
        this.indexOrderValid = previous != null && previous.indexOrderValid && Arrays.equals(this.counts, previous.counts);
    }

    /** Whole slices do not need reordered bounds, but unexpected index layouts must fail open. */
    public void updateIndices(ByteBuffer indices) {
        this.indexOrderValid = false;
        long vertices = 0;
        for (int count : this.counts) vertices += count;
        if (indices.remaining() != vertices * 6) return;
        ByteBuffer input = indices.duplicate().order(ByteOrder.nativeOrder());
        for (int face = 0; face < FACES; face++) {
            for (int q = 0; q < quadCount(face); q++) {
                int first = input.getInt();
                if (first < 0 || first >= this.counts[face]) return;
                for (int i = 1; i < 6; i++) {
                    int vertex = input.getInt();
                    if (vertex < 0 || vertex / 4 != first / 4) return;
                }
            }
        }
        this.indexOrderValid = true;
    }
}
