package ru.spectra.client.render.culling;

public interface TerrainSectionAccess {
    TerrainMeshData spectra$terrainMesh(Object pass);
    void spectra$terrainMesh(Object pass, TerrainMeshData mesh);
}
