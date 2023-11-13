package com.wizzardo.vulkan.scene;

import com.wizzardo.vulkan.Transform;

public class Spatial extends Node {
    protected Transform localTransform = new Transform();

    public Transform getLocalTransform() {
        return localTransform;
    }

    public void setLocalTransform(Transform transform) {
        localTransform = transform;
    }
}
