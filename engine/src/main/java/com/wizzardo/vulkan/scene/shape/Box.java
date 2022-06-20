package com.wizzardo.vulkan.scene.shape;

import com.wizzardo.vulkan.Material;
import com.wizzardo.vulkan.Mesh;
import com.wizzardo.vulkan.Vertex;
import com.wizzardo.vulkan.scene.Geometry;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Box extends Geometry {

    public static final Mesh MESH;

    static {
        Vector3f center = new Vector3f(0, 0, 0);

        float half = 0.5f;
        Vector3f[] positions = {
                new Vector3f(center).add(-half, -half, -half),
                new Vector3f(center).add(half, -half, -half),
                new Vector3f(center).add(half, half, -half),
                new Vector3f(center).add(-half, half, -half),
                new Vector3f(center).add(half, -half, half),
                new Vector3f(center).add(-half, -half, half),
                new Vector3f(center).add(half, half, half),
                new Vector3f(center).add(-half, half, half),
        };

        Vector4f color = new Vector4f(1, 1, 1, 1);

        Vertex[] vertices = {
                // back
                new Vertex(positions[0], color, new Vector2f(1, 0), new Vector3f(0, 0, -1)),
                new Vertex(positions[1], color, new Vector2f(0, 0), new Vector3f(0, 0, -1)),
                new Vertex(positions[2], color, new Vector2f(0, 1), new Vector3f(0, 0, -1)),
                new Vertex(positions[3], color, new Vector2f(1, 1), new Vector3f(0, 0, -1)),

                // right
                new Vertex(positions[1], color, new Vector2f(1, 0), new Vector3f(1, 0, 0)),
                new Vertex(positions[4], color, new Vector2f(0, 0), new Vector3f(1, 0, 0)),
                new Vertex(positions[6], color, new Vector2f(0, 1), new Vector3f(1, 0, 0)),
                new Vertex(positions[2], color, new Vector2f(1, 1), new Vector3f(1, 0, 0)),

                // front
                new Vertex(positions[4], color, new Vector2f(1, 0), new Vector3f(0, 0, 1)),
                new Vertex(positions[5], color, new Vector2f(0, 0), new Vector3f(0, 0, 1)),
                new Vertex(positions[7], color, new Vector2f(0, 1), new Vector3f(0, 0, 1)),
                new Vertex(positions[6], color, new Vector2f(1, 1), new Vector3f(0, 0, 1)),

                // left
                new Vertex(positions[5], color, new Vector2f(1, 0), new Vector3f(-1, 0, 0)),
                new Vertex(positions[0], color, new Vector2f(0, 0), new Vector3f(-1, 0, 0)),
                new Vertex(positions[3], color, new Vector2f(0, 1), new Vector3f(-1, 0, 0)),
                new Vertex(positions[7], color, new Vector2f(1, 1), new Vector3f(-1, 0, 0)),

                // top
                new Vertex(positions[2], color, new Vector2f(1, 0), new Vector3f(0, 1, 0)),
                new Vertex(positions[6], color, new Vector2f(0, 0), new Vector3f(0, 1, 0)),
                new Vertex(positions[7], color, new Vector2f(0, 1), new Vector3f(0, 1, 0)),
                new Vertex(positions[3], color, new Vector2f(1, 1), new Vector3f(0, 1, 0)),

                // bottom
                new Vertex(positions[0], color, new Vector2f(1, 0), new Vector3f(0, -1, 0)),
                new Vertex(positions[5], color, new Vector2f(0, 0), new Vector3f(0, -1, 0)),
                new Vertex(positions[4], color, new Vector2f(0, 1), new Vector3f(0, -1, 0)),
                new Vertex(positions[1], color, new Vector2f(1, 1), new Vector3f(0, -1, 0)),
        };

        int[] indices = {
                2, 1, 0, 3, 2, 0, // back
                6, 5, 4, 7, 6, 4, // right
                10, 9, 8, 11, 10, 8, // front
                14, 13, 12, 15, 14, 12, // left
                18, 17, 16, 19, 18, 16, // top
                22, 21, 20, 23, 22, 20  // bottom
        };

        MESH = new Mesh(vertices, indices);
    }

    public Box(Material material) {
        this.mesh = MESH;
        this.material = material;
    }
}
