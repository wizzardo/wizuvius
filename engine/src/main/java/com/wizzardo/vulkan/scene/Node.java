package com.wizzardo.vulkan.scene;

import java.util.ArrayList;
import java.util.List;

public class Node extends Spatial {
    private List<Spatial> children = new ArrayList<>();

    public List<Spatial> getChildren() {
        return children;
    }

    public void attachChild(Spatial spatial) {
        children.add(spatial);
    }

    public boolean detachChild(Spatial spatial) {
        return children.remove(spatial);
    }
}
