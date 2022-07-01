package com.wizzardo.vulkan;

import java.util.stream.IntStream;

public class QueueFamilyIndices {
    private Integer graphicsFamily;
    private Integer transferFamily;
    private Integer computeFamily;

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

    public int getTransferFamily() {
        if (transferFamily != null)
            return transferFamily;

        return graphicsFamily;
    }

    public int getComputeFamily() {
        if (computeFamily != null)
            return computeFamily;

        return graphicsFamily;
    }

    public void setGraphicsFamily(int i) {
        graphicsFamily = i;
    }

    public void setTransferFamily(int i) {
        transferFamily = i;
    }

    public void setComputeFamily(int i) {
        computeFamily = i;
    }

    @Override
    public String toString() {
        return "QueueFamilyIndices{" +
                "graphicsFamily=" + graphicsFamily +
                ", transferFamily=" + transferFamily +
                ", computeFamily=" + computeFamily +
                '}';
    }
}
