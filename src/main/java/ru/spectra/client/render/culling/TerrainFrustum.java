package ru.spectra.client.render.culling;

import org.joml.Matrix4fc;
import org.joml.Vector4f;

/** Tests region, mesh and slice bounds, propagating only intersecting planes. */
public final class TerrainFrustum {
    private static final float UNIT = 32.0f / 65536.0f;
    private static final float EDGE_SLACK = 0.002f;
    private final float[] planes = new float[24];
    private final float[] scaled = new float[18];
    private final int[] corners = new int[18];
    private final float[] farDistances = new float[6];
    private final float[] nearDistances = new float[6];
    private final Vector4f scratch = new Vector4f();
    private float originX = Float.NaN, originY, originZ;

    void copyFrom(TerrainFrustum other) {
        System.arraycopy(other.planes, 0, this.planes, 0, this.planes.length);
        System.arraycopy(other.scaled, 0, this.scaled, 0, this.scaled.length);
        System.arraycopy(other.corners, 0, this.corners, 0, this.corners.length);
        this.originX = Float.NaN;
    }

    public void set(Matrix4fc clip) {
        for (int plane = 0; plane < 6; plane++) {
            clip.frustumPlane(plane, this.scratch);
            int p = plane * 4;
            this.planes[p] = this.scratch.x;
            this.planes[p + 1] = this.scratch.y;
            this.planes[p + 2] = this.scratch.z;
            this.planes[p + 3] = this.scratch.w;
            for (int axis = 0; axis < 3; axis++) {
                this.scaled[plane * 3 + axis] = this.planes[p + axis] * UNIT;
                this.corners[plane * 3 + axis] = axis + (this.planes[p + axis] >= 0 ? 3 : 0);
            }
        }
        this.originX = Float.NaN;
    }

    public void atOrigin(float x, float y, float z) {
        if (this.originX == x && this.originY == y && this.originZ == z) return;
        this.originX = x; this.originY = y; this.originZ = z;
        for (int plane = 0; plane < 6; plane++) {
            int p = plane * 4;
            float nx = this.planes[p], ny = this.planes[p + 1], nz = this.planes[p + 2];
            float d = nx * (x - 8) + ny * (y - 8) + nz * (z - 8) + this.planes[p + 3];
            float slack = (Math.abs(nx) + Math.abs(ny) + Math.abs(nz)) * EDGE_SLACK;
            this.farDistances[plane] = d + slack + UNIT * (Math.max(0, nx) + Math.max(0, ny) + Math.max(0, nz));
            this.nearDistances[plane] = d - slack + UNIT * (Math.min(0, nx) + Math.min(0, ny) + Math.min(0, nz));
        }
    }

    public void expand(float distance) {
        for (int plane = 0; plane < 6; plane++) this.planes[plane * 4 + 3] += distance;
        this.originX = Float.NaN;
    }

    /** -1 outside, 0 fully inside, otherwise the planes still intersecting the box. */
    public int classify(char[] bounds, int i, int planeMask) {
        int intersecting = planeMask;
        for (int remaining = planeMask; remaining != 0; remaining &= remaining - 1) {
            int plane = Integer.numberOfTrailingZeros(remaining);
            int p = plane * 3;
            float nx = this.scaled[p], ny = this.scaled[p + 1], nz = this.scaled[p + 2];
            int cx = this.corners[p], cy = this.corners[p + 1], cz = this.corners[p + 2];
            float far = nx * bounds[i + cx] + ny * bounds[i + cy] + nz * bounds[i + cz] + this.farDistances[plane];
            if (far < 0) return -1;
            float near = nx * bounds[i + (3 - cx)] + ny * bounds[i + (5 - cy)]
                    + nz * bounds[i + (7 - cz)] + this.nearDistances[plane];
            if (near >= 0) intersecting &= ~(1 << plane);
        }
        return intersecting;
    }

    /** Leaves only need rejection; computing inner corners and remaining planes is wasted work. */
    public boolean isOutside(char[] bounds, int i, int planeMask) {
        for (int remaining = planeMask; remaining != 0; remaining &= remaining - 1) {
            int plane = Integer.numberOfTrailingZeros(remaining), p = plane * 3;
            float far = this.scaled[p] * bounds[i + this.corners[p]]
                    + this.scaled[p + 1] * bounds[i + this.corners[p + 1]]
                    + this.scaled[p + 2] * bounds[i + this.corners[p + 2]] + this.farDistances[plane];
            if (far < 0) return true;
        }
        return false;
    }

    public int classifyBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int mask) {
        int intersecting = mask;
        for (int remaining = mask; remaining != 0; remaining &= remaining - 1) {
            int plane = Integer.numberOfTrailingZeros(remaining), p = plane * 4;
            float nx = this.planes[p], ny = this.planes[p + 1], nz = this.planes[p + 2], d = this.planes[p + 3];
            float far = nx * (nx >= 0 ? maxX : minX) + ny * (ny >= 0 ? maxY : minY) + nz * (nz >= 0 ? maxZ : minZ) + d;
            if (far < -EDGE_SLACK) return -1;
            float near = nx * (nx >= 0 ? minX : maxX) + ny * (ny >= 0 ? minY : maxY) + nz * (nz >= 0 ? minZ : maxZ) + d;
            if (near > EDGE_SLACK) intersecting &= ~(1 << plane);
        }
        return intersecting;
    }
}
