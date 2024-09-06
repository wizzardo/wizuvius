package com.wizzardo.vulkan;

import java.util.stream.IntStream;

public class QueueFamilyIndices {
    private Integer graphicsFamily;
    private Integer transferFamily;
    private Integer computeFamily;
    private int graphicsQueueTimestampValidBits;

    public boolean isComplete() {
        return graphicsFamily != null && transferFamily != null && computeFamily != null;
    }

    public int[] unique() {
        return IntStream.of(graphicsFamily).toArray();
    }

    public int[] array() {
        return new int[]{graphicsFamily};
    }

    public Integer getGraphicsFamily() {
        return graphicsFamily;
    }

    public Integer getTransferFamily() {
        return transferFamily;
    }

    public Integer getComputeFamily() {
        return computeFamily;
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

    public void setGraphicsQueueTimestampValidBits(int timestampValidBits) {
        graphicsQueueTimestampValidBits = timestampValidBits;
    }

    public int getGraphicsQueueTimestampValidBits() {
        return graphicsQueueTimestampValidBits;
    }
}
