package com.wizzardo.vulkan;

import static org.lwjgl.vulkan.VK10.vkDestroyFence;
import static org.lwjgl.vulkan.VK10.vkDestroySemaphore;

import org.lwjgl.vulkan.VkDevice;

class SyncObjects {
    private final Frame[] frames;
    private final Frame[] framesByImage;

    SyncObjects(Frame[] frames, int imagesCount) {
        this.frames = frames;
        framesByImage = new Frame[imagesCount];
    }

    public Frame getFrame(int index) {
        return frames[index];
    }

    public int getFramesCount() {
        return frames.length;
    }

    public Frame byImage(int index) {
        return framesByImage[index];
    }

    public void setByImage(int imageIndex, Frame frame) {
        framesByImage[imageIndex] = frame;
    }

    public void cleanup(VkDevice device) {
        for (Frame frame : frames) {
            vkDestroySemaphore(device, frame.renderFinishedSemaphore(), null);
            vkDestroySemaphore(device, frame.imageAvailableSemaphore(), null);
            vkDestroyFence(device, frame.fence(), null);
        }
    }
}
