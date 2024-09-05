package com.wizzardo.vulkan.scene.shape;

import com.wizzardo.vulkan.Material;
import com.wizzardo.vulkan.Mesh;
import com.wizzardo.vulkan.Vertex;
import com.wizzardo.vulkan.scene.Geometry;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class PlaneXZ extends Geometry {

    public static final Mesh MESH;

    static {
        Vector4f color = new Vector4f(1, 1, 1, 1);

        Vector3f[] positions = {
                new Vector3f(0, 0, 0),
                new Vector3f(1, 0, 0),
                new Vector3f(1, 0, 1),
                new Vector3f(0, 0, 1),
        };

        Vertex[] vertices = {
                new Vertex(positions[0], color, new Vector2f(1, 0), new Vector3f(0, 0, 1)),
                new Vertex(positions[1], color, new Vector2f(0, 0), new Vector3f(0, 0, 1)),
                new Vertex(positions[2], color, new Vector2f(0, 1), new Vector3f(0, 0, 1)),
                new Vertex(positions[3], color, new Vector2f(1, 1), new Vector3f(0, 0, 1))
        };

        int[] indices = {
                2, 1, 0, 3, 2, 0, // back
        };

        MESH = new Mesh(vertices, indices);
    }

    public PlaneXZ(Material material) {
        this.mesh = MESH;
        this.material = material;
    }
}
