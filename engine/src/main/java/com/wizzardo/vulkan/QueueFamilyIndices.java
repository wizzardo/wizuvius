package com.wizzardo.vulkan;

import java.util.stream.IntStream;

class QueueFamilyIndices {
    private Integer graphicsFamily;

    public boolean isComplete() {
        return graphicsFamily != null;
    }

    public int[] unique() {
        return IntStream.of(graphicsFamily).toArray();
    }

    public int[] array() {
        return new int[]{graphicsFamily};
    }

    public int getGraphicsFamily() {
        return graphicsFamily;
    }

    public void setGraphicsFamily(int i) {
        graphicsFamily = i;
    }
}
