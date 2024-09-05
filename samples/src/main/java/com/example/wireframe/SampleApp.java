package com.example.wireframe;

import com.example.AbstractSampleApp;
import com.wizzardo.vulkan.*;
import com.wizzardo.vulkan.material.predefined.UnshadedWireframe;
import com.wizzardo.vulkan.scene.Geometry;
import com.wizzardo.vulkan.scene.shape.Capsule;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.List;

public class SampleApp extends AbstractSampleApp {

    public static void main(String[] args) {
        new SampleApp().start();
    }

    {
        enabledDeviceFeatures = EnumSet.of(DeviceFeature.SAMPLER_ANISOTROPY, DeviceFeature.FILL_MODE_NON_SOLID);
    }

    @Override
    protected void initApp() {
        getMainViewport().getCamera().setLocation(new Vector3f(2, 2, 2));
        getMainViewport().getCamera().lookAt(Vectors.ZERO, Vectors.UNIT_Z);

        {
            Material material = new UnshadedWireframe(new Vector3f(1,1,0));
            Geometry geometry = new Capsule(material);
            addGeometry(geometry, getMainViewport());
        }
    }

    @Override
    protected void simpleUpdate(double tpf) {
        List<Geometry> geometries = getMainViewport().getGeometries();
        for (int i = 0; i < geometries.size(); i++) {
            Geometry geometry = geometries.get(i);
            geometry.getLocalTransform().getRotation().setAngleAxis(this.getTime() * Math.toRadians(90) / 10f, 0.0f, 0.0f, 1.0f);
        }
    }
}
