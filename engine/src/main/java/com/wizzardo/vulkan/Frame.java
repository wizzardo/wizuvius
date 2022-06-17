package com.wizzardo.vulkan;

import static org.lwjgl.system.MemoryStack.stackGet;

import org.lwjgl.system.MemoryStack;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

public class Frame {

    private final long imageAvailableSemaphore;
    private final long renderFinishedSemaphore;
    private final long fence;
    protected final List<FrameListener> listeners = new ArrayList<>();

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

    public LongBuffer pImageAvailableSemaphore(LongBuffer buffer) {
        return buffer.put(0, imageAvailableSemaphore);
    }

    public LongBuffer pRenderFinishedSemaphore(MemoryStack stack) {
        return stack.longs(renderFinishedSemaphore);
    }

    public LongBuffer pRenderFinishedSemaphore(LongBuffer buffer) {
        return buffer.put(0, renderFinishedSemaphore);
    }

    public LongBuffer pFence(MemoryStack stack) {
        return stack.longs(fence);
    }

    public LongBuffer pFence(LongBuffer buffer) {
        return buffer.put(0, fence);
    }

    public void onFinish() {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onFinish();
        }
    }

    public void resetListeners() {
        listeners.clear();
    }

    public void addFrameListener(FrameListener listener) {
        listeners.add(listener);
    }

    public interface FrameListener {
        void onFinish();
    }
}
