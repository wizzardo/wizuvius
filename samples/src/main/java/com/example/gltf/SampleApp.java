package com.example.gltf;

import com.example.AbstractSampleApp;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.vulkan.*;
import com.wizzardo.vulkan.scene.Spatial;
import org.joml.Vector3f;


import static org.lwjgl.assimp.Assimp.aiProcess_JoinIdenticalVertices;

public class SampleApp extends AbstractSampleApp {

    public static void main(String[] args) {
        new SampleApp().start();
    }

    Spatial scene;

    @Override
    protected void initApp() {
        int cameraOffset = 5;
        getMainViewport().getCamera().setLocation(new Vector3f(cameraOffset, cameraOffset, cameraOffset));
        getMainViewport().getCamera().lookAt(Vectors.ZERO, Vectors.UNIT_Z);

        {
            scene = loadScene();
            scene.getLocalTransform().setScale(0.1f);
            getMainViewport().getScene().attachChild(scene);
        }
    }

    @Override
    protected void simpleUpdate(double tpf) {
        scene.getLocalTransform().getRotation().setAngleAxis(this.getTime() * Math.toRadians(90) / 10f, 0.0f, 0.0f, 1.0f);
    }

    private Spatial loadScene() {
        Spatial scene = Unchecked.call(() -> ModelLoader.loadModel(
                folder + "/models/well.gltf", aiProcess_JoinIdenticalVertices, this::loadAssetAsByteBuffer
        ));
        return scene;
    }

}
