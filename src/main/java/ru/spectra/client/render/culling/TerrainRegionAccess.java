package ru.spectra.client.render.culling;

/** Render-thread revision of one region's uploaded geometry and GPU addresses. */
public interface TerrainRegionAccess {
    long spectra$meshRevision();
    void spectra$meshChanged();
}
