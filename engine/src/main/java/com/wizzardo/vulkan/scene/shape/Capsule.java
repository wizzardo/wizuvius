package com.wizzardo.vulkan.scene.shape;

import com.wizzardo.vulkan.Material;
import com.wizzardo.vulkan.Mesh;
import com.wizzardo.vulkan.Vertex;
import com.wizzardo.vulkan.scene.Geometry;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Capsule extends Geometry {

    public Capsule(Material material) {
        this.mesh = createMesh();
        this.material = material;
    }

    public static Mesh createMesh() {
        return createMesh(0.5f, 0.25f, 16, 1, 16, (float) (Math.PI * 2));
    }

    public static Mesh createMesh(
            float height,
            float radius,
            int nx,
            int ny,
            int roundSegments,
            float phi
    ) {
        Vector4f color = new Vector4f(1, 1, 1, 1);

        int ringsBody = ny + 1;
        int ringsCap = roundSegments * 2;
        int ringsTotal = ringsCap + ringsBody;

        int size = ringsTotal * nx;

        Vertex[] vertices = new Vertex[size];
        int[] indices = new int[(ringsTotal - 1) * (nx - 1) * 6];

        int vertexIndex = 0;
        int cellIndex = 0;

        double ringIncrement = 1.0 / (ringsCap - 1);
        double bodyIncrement = 1.0 / (ringsBody - 1);

        for (int r = 0; r < roundSegments; r++) {
            vertexIndex = computeRing(
                    Math.sin(Math.PI * r * ringIncrement),
                    Math.sin(Math.PI * (r * ringIncrement - 0.5)),
                    -0.5,
                    height,
                    radius,
                    nx,
                    phi,
                    vertexIndex,
                    vertices,
                    color
            );
        }
        for (int r = 0; r < ringsBody; r++) {
            vertexIndex = computeRing(
                    1,
                    0,
                    r * bodyIncrement - 0.5,
                    height,
                    radius,
                    nx,
                    phi,
                    vertexIndex,
                    vertices,
                    color
            );
        }
        for (int r = roundSegments; r < ringsCap; r++) {
            vertexIndex = computeRing(
                    Math.sin(Math.PI * r * ringIncrement),
                    Math.sin(Math.PI * (r * ringIncrement - 0.5)),
                    0.5,
                    height,
                    radius,
                    nx,
                    phi,
                    vertexIndex,
                    vertices,
                    color
            );
        }

        for (int r = 0; r < ringsTotal - 1; r++) {
            for (int s = 0; s < nx - 1; s++) {
                int a = r * nx;
                int b = (r + 1) * nx;
                int s1 = s + 1;
                indices[cellIndex] = a + s;
                indices[cellIndex + 1] = a + s1;
                indices[cellIndex + 2] = b + s1;

                indices[cellIndex + 3] = a + s;
                indices[cellIndex + 4] = b + s1;
                indices[cellIndex + 5] = b + s;

                cellIndex += 6;
            }
        }

        return new Mesh(vertices, indices);
    }

    static int computeRing(
            double r,
            double y,
            double dy,
            float height,
            float radius,
            int nx,
            double phi,
            int vertexIndex,
            Vertex[] vertices,
            Vector4f color
    ) {
        double segmentIncrement = 1.0 / (nx - 1);
        for (int s = 0; s < nx; s++, vertexIndex++) {
            double x = -Math.cos(s * segmentIncrement * phi) * r;
            double z = Math.sin(s * segmentIncrement * phi) * r;

            double py = (radius * y + height * dy);

            vertices[vertexIndex] = new Vertex(
//                    new Vector3f((float) (radius * x), (float) py, (float) (radius * z)),
                    new Vector3f((float) (radius * x), (float) (radius * z), (float) py),
                    color,
                    new Vector2f((float) (s * segmentIncrement), (float) (1 - (0.5 - py / (2 * radius + height)))),
//                    new Vector3f((float) x, (float) y, (float) z)
                    new Vector3f((float) x, (float) z, (float) y)
            );
        }
        return vertexIndex;
    }
}
