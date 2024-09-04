package com.example.light_phong;

import com.example.AbstractSampleApp;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.vulkan.Material;
import com.wizzardo.vulkan.TextureImage;
import com.wizzardo.vulkan.Vectors;
import com.wizzardo.vulkan.material.predefined.UnshadedColor;
import com.wizzardo.vulkan.scene.Geometry;
import com.wizzardo.vulkan.scene.shape.Box;
import org.joml.Vector3f;

import java.util.List;

public class SampleApp extends AbstractSampleApp {

    public static void main(String[] args) {
        new SampleApp().start();
    }

    @Override
    protected void initApp() {
        getMainViewport().getCamera().setLocation(new Vector3f(2, 2, 2));
        getMainViewport().getCamera().lookAt(Vectors.ZERO, Vectors.UNIT_Z);

        {
            TextureImage textureImage = Unchecked.call(() -> createTextureImage(folder + "/textures/texture.jpg"));
//            Material material = new UnshadedTexture(textureImage);
//            Material material = new UnshadedColor(new Vector3f(1, 0, 0));

            Material material = new Material() {
                {
                    vertexLayout = new VertexLayout(
                            VertexLayout.BindingDescription.POSITION,
                            VertexLayout.BindingDescription.TEXTURE_COORDINATES,
                            VertexLayout.BindingDescription.NORMAL
                    );
                }
            };
            material.setVertexShader(folder + "/shaders/tri.vert");
            material.setFragmentShader(folder + "/shaders/tri.frag");
            material.addTextureImage(textureImage);
            material.setTextureSampler(createTextureSampler(textureImage.getMipLevels()));


            Geometry geometry = new Box(material);
            addGeometry(geometry, getMainViewport());
        }
    }

    @Override
    protected void simpleUpdate(double tpf) {
        List<Geometry> geometries = getMainViewport().getGeometries();
        for (int i = 0; i < geometries.size(); i++) {
            Geometry geometry = geometries.get(i);
            geometry.getLocalTransform().getRotation().setAngleAxis(this.getTime() * Math.toRadians(90) / 10f, 0.0f, 0.0f, 1.0f);
            if (geometry.getMaterial() instanceof UnshadedColor) {
                UnshadedColor material = (UnshadedColor) geometry.getMaterial();
                material.setColor(new Vector3f((float) (Math.sin(this.getTime() / 3)), (float) (Math.sin(this.getTime() / 2)), (float) Math.sin(this.getTime())));
            }
        }
    }
}
