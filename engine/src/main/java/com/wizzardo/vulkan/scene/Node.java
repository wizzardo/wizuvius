package com.wizzardo.vulkan.scene;

import java.util.ArrayList;
import java.util.List;

public class Node extends Spatial {
    protected List<Spatial> children = new ArrayList<>();

    public List<Spatial> getChildren() {
        return children;
    }

    public void attachChild(Spatial spatial) {
        children.add(spatial);
        spatial.setParent(this);
    }

    public boolean detachChild(Spatial spatial) {
        boolean removed = children.remove(spatial);
        if (removed) {
            spatial.setParent(null);
        }
        return removed;
    }
}
