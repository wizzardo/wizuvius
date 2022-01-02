package com.wizzardo.vulkan;

import static org.lwjgl.system.MemoryStack.stackGet;

import org.lwjgl.system.MemoryStack;

import java.nio.LongBuffer;

public class Frame {

    private final long imageAvailableSemaphore;
    private final long renderFinishedSemaphore;
    private final long fence;

    public Frame(long imageAvailableSemaphore, long renderFinishedSemaphore, long fence) {
        this.imageAvailableSemaphore = imageAvailableSemaphore;
        this.renderFinishedSemaphore = renderFinishedSemaphore;
        this.fence = fence;
    }

    public long imageAvailableSemaphore() {
        return imageAvailableSemaphore;
    }

    public LongBuffer pImageAvailableSemaphore() {
        return pImageAvailableSemaphore(stackGet());
    }

    public long renderFinishedSemaphore() {
        return renderFinishedSemaphore;
    }

    public LongBuffer pRenderFinishedSemaphore() {
        return pRenderFinishedSemaphore(stackGet());
    }

    public long fence() {
        return fence;
    }

    public LongBuffer pFence() {
        return pFence(stackGet());
    }

    public LongBuffer pImageAvailableSemaphore(MemoryStack stack) {
        return stack.longs(imageAvailableSemaphore);
    }

    public LongBuffer pRenderFinishedSemaphore(MemoryStack stack) {
        return stack.longs(renderFinishedSemaphore);
    }

    public LongBuffer pFence(MemoryStack stack) {
        return stack.longs(fence);
    }
}
