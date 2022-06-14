package com.example.gui_quad;

import com.example.AbstractSampleApp;
import com.wizzardo.vulkan.*;
import com.wizzardo.vulkan.material.predefined.UnshadedTexture;
import com.wizzardo.vulkan.scene.Geometry;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class SampleApp extends AbstractSampleApp {

    public static void main(String[] args) {
        new SampleApp().start();
    }

    @Override
    protected void initApp() {
        getMainViewport().getCamera().setLocation(new Vector3f(2, 2, 2));
        getMainViewport().getCamera().lookAt(Vectors.ZERO, Vectors.UNIT_Z);

        {

            Vertex[] vertices = {
//                    new Vertex(new Vector3f(-0.5f, -0.5f, 0f), new Vector3f(0.5f, 0.0f, 0.0f), new Vector2f(1.0f, 0.0f)),
//                    new Vertex(new Vector3f(0.5f, -0.5f, 0f), new Vector3f(0.0f, 0.5f, 0.0f), new Vector2f(0.0f, 0.0f)),
//                    new Vertex(new Vector3f(0.5f, 0.5f, 0f), new Vector3f(0.0f, 0.0f, 0.0f), new Vector2f(0.0f, 1.0f)),
//                    new Vertex(new Vector3f(-0.5f, 0.5f, 0f), new Vector3f(0.5f, 0.5f, 0.0f), new Vector2f(1.0f, 1.0f))
                    new Vertex(new Vector3f(0, 0, 0f), new Vector3f(1, 0.0f, 0.0f), new Vector2f(1.0f, 0.0f)),
                    new Vertex(new Vector3f(1, 0, 0f), new Vector3f(0.0f, 1, 0.0f), new Vector2f(0.0f, 0.0f)),
                    new Vertex(new Vector3f(1, 1, 0f), new Vector3f(0.0f, 0.0f, 0.0f), new Vector2f(0.0f, 1.0f)),
                    new Vertex(new Vector3f(0, 1, 0f), new Vector3f(1, 1, 0.0f), new Vector2f(1.0f, 1.0f))
            };

            int[] indices = {
                    2, 1, 0,
                    0, 3, 2
            };

            TextureImage textureImage = createTextureImage(folder + "/textures/texture.jpg");
            Geometry geometry = new Geometry(new Mesh(vertices, indices), new UnshadedTexture(textureImage));
            geometry.getLocalTransform().setScale(400f);
            geometry.getLocalTransform().setTranslation(100, 100, 0);
            addGeometry(geometry, getGuiViewport());
        }
    }
}
