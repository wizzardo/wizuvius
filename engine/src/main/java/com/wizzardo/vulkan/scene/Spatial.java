package com.wizzardo.vulkan.scene;

import com.wizzardo.vulkan.Transform;

public class Spatial {
    protected Transform localTransform = new Transform();
    protected String name;
    protected Node parent;

    public Spatial() {
    }

    public Spatial(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Transform getLocalTransform() {
        return localTransform;
    }

    public void setLocalTransform(Transform transform) {
        localTransform = transform;
    }

    protected void setParent(Node parent) {
        this.parent = parent;
    }

    public Node getParent() {
        return parent;
    }
}
