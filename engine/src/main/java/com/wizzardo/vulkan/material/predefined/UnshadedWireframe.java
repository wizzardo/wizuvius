package com.wizzardo.vulkan.material.predefined;

import com.wizzardo.vulkan.RasterizationStateOptions;
import org.joml.Vector3f;

public class UnshadedWireframe extends UnshadedColor {

    public UnshadedWireframe(Vector3f color) {
        super(color);
    }

    @Override
    protected RasterizationStateOptions createRasterizationStateOptions() {
        RasterizationStateOptions options = super.createRasterizationStateOptions();
        options.polygonMode = RasterizationStateOptions.PolygonMode.LINE;
        return options;
    }
}
