package com.wizzardo.vulkan;

public class Spatial {
    protected Transform localTransform = new Transform();
    protected String name;

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
}
