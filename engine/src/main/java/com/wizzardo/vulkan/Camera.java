package com.wizzardo.vulkan;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Camera {

    protected Vector3f location = new Vector3f();
    protected Quaternionf rotation = new Quaternionf();

    public Vector3f getLocation() {
        return location;
    }

    public void setLocation(Vector3f location) {
        this.location = location;
    }

    public Quaternionf getRotation() {
        return rotation;
    }

    public void setRotation(Quaternionf rotation) {
        this.rotation = rotation;
    }
}
