package ru.spectra.client.render.culling;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

/** A wider volume for conservative culling and containment-checked reuse across frames. */
public final class GuardedTerrainFrustum {
    private final Vector4f[] sides = {new Vector4f(), new Vector4f(), new Vector4f(), new Vector4f()};
    private final Matrix4f inverse = new Matrix4f();
    private final Vector4f point = new Vector4f();

    public GuardedTerrainFrustum(Matrix4fc clip) {
        this(clip, 0);
    }

    public GuardedTerrainFrustum(Matrix4fc clip, float margin) {
        Matrix4f expanded = new Matrix4f(clip).scaleLocal(0.9f, 0.9f, 1);
        for (int p = 0; p < 4; p++) {
            expanded.frustumPlane(p, this.sides[p]);
            this.sides[p].w += margin;
        }
    }

    /** Convex containment, not a guessed angle/delay threshold. */
    public boolean contains(Matrix4fc current) {
        this.inverse.set(current).invert();
        if (!this.inverse.isFinite()) return false;
        for (int corner = 0; corner < 8; corner++) {
            this.inverse.transform(this.point.set((corner & 1) == 0 ? -1 : 1,
                    (corner & 2) == 0 ? -1 : 1, (corner & 4) == 0 ? -1 : 1, 1));
            if (!(this.point.w > 0)) return false;
            for (Vector4f side : this.sides) {
                if (side.dot(this.point) < 0.0001f * this.point.w) return false;
            }
        }
        return true;
    }

    public boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        for (Vector4f p : this.sides) {
            if (p.x * (p.x >= 0 ? maxX : minX) + p.y * (p.y >= 0 ? maxY : minY)
                    + p.z * (p.z >= 0 ? maxZ : minZ) + p.w < -0.002f) return false;
        }
        return true;
    }

    /** Sodium's directional mask changes only at section origin - 2 and origin + 19. */
    static boolean sameFaceCell(int previous, int current) {
        return Math.floorDiv(previous + 2, 16) == Math.floorDiv(current + 2, 16)
                && Math.floorDiv(previous - 3, 16) == Math.floorDiv(current - 3, 16);
    }
}
