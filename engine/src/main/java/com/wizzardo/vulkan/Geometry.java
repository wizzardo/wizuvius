package com.wizzardo.vulkan;

public class Geometry extends Spatial {
    private Mesh mesh;
    private Material material;

    public Geometry() {
    }

    public Geometry(Mesh mesh, Material material) {
        this.mesh = mesh;
        this.material = material;
    }

    public Mesh getMesh() {
        return mesh;
    }

    public void setMesh(Mesh mesh) {
        this.mesh = mesh;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }
}
