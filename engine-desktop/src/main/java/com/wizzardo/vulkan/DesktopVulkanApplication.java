package com.wizzardo.vulkan;

import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.io.IOTools;
import com.wizzardo.vulkan.input.GlfwInputsManager;
import com.wizzardo.vulkan.input.InputsManager;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkInstance;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public class DesktopVulkanApplication extends VulkanApplication {

    protected long window;
    protected long prevAllocation = 0;
    protected com.sun.management.ThreadMXBean threadMXBean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    protected boolean allocationTrackingEnabled = threadMXBean.isThreadAllocatedMemorySupported() && threadMXBean.isThreadAllocatedMemoryEnabled();
    protected long threadId = -1;
    protected ResourcesListener resourcesListener;
    protected File devResourcesFolder = new File("src/main/resources");
    protected int allocationGetCost;

    protected void printAllocation(String mark) {
        if (threadId == -1) {
            threadId = Thread.currentThread().getId();
            allocationGetCost = Math.abs((int) (threadMXBean.getThreadAllocatedBytes(threadId) - threadMXBean.getThreadAllocatedBytes(threadId)));
        }

        if (allocationTrackingEnabled) {
            long allocatedBytes = threadMXBean.getThreadAllocatedBytes(threadId);
            prevAllocation += allocationGetCost;
            if (allocatedBytes - prevAllocation > 0) {
                System.out.println(mark + " allocatedBytes: " + (allocatedBytes - prevAllocation));
                prevAllocation = threadMXBean.getThreadAllocatedBytes(threadId);
            }
        }
    }

    @Override
    public void run() {
        if (development && resourcesListener == null) {
            resourcesListener = createResourcesListener();
        }
        super.run();
    }

    protected ResourcesListener createResourcesListener() {
        return new ResourcesListener(Arrays.asList(devResourcesFolder.getAbsolutePath()));
    }

    @Override
    public void logV(Supplier<String> data) {
        System.out.println("V " + data.get());
    }

    @Override
    public void logE(Supplier<String> data, Throwable e) {
        System.out.println("E " + data.get());
        e.printStackTrace();
    }

    @Override
    public void logD(Supplier<String> data) {
        System.out.println("D " + data.get());
    }

    @Override
    protected void doInLoop() {
        if (glfwWindowShouldClose(window)) {
            shutdown();
            return;
        }

        glfwPollEvents();
        super.doInLoop();
    }

    public long getWindow() {
        return window;
    }

    @Override
    protected void initWindow() {
        if (!glfwInit()) {
            throw new RuntimeException("Cannot initialize GLFW");
        }

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
//        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        String title = getClass().getSimpleName();

        window = glfwCreateWindow(width, height, title, NULL, NULL);

        if (window == NULL) {
            throw new RuntimeException("Cannot create window");
        }
    }

    @Override
    protected InputsManager initInputsManager() {
        if (inputsManager != null)
            throw new IllegalStateException("InputsManager is already initialised");

        return new GlfwInputsManager(this);
    }

    @Override
    protected long createSurface(VkInstance instance) {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pSurface = stack.longs(VK_NULL_HANDLE);

            if (glfwCreateWindowSurface(instance, window, null, pSurface) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create window surface");
            }

            return pSurface.get(0);
        }
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    @Override
    protected VulkanInstances getVulkanInstanceFactory() {
        return new VulkanInstances() {
            @Override
            public PointerBuffer getRequiredExtensions() {
                PointerBuffer glfwExtensions = glfwGetRequiredInstanceExtensions();
                if (ENABLE_VALIDATION_LAYERS) {
                    MemoryStack stack = stackGet();

                    PointerBuffer extensions;
                    if (glfwExtensions != null) {
                        extensions = stack.mallocPointer(glfwExtensions.capacity() + 1);
                        extensions.put(glfwExtensions);
                    } else
                        extensions = stack.mallocPointer(1);

                    extensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));

                    // Rewind the buffer before returning it to reset its position back to 0
                    return extensions.rewind();
                }

                return glfwExtensions;
            }
        };
    }

    @Override
    public InputStream loadAsset(String asset) throws IOException {
        File f = new File(asset);
        if (f.exists())
            return Files.newInputStream(f.toPath());

        {
            String name = asset;
            String nameLoweCase = asset.toLowerCase();
            if (nameLoweCase.endsWith(".spv")) {
                name = asset.substring(0, asset.length() - 4);
                nameLoweCase = nameLoweCase.substring(0, nameLoweCase.length() - 4);
            }

            ShaderSPIRVUtils.ShaderKind shaderKind = null;
            if (nameLoweCase.endsWith(".frag"))
                shaderKind = ShaderSPIRVUtils.ShaderKind.FRAGMENT_SHADER;
            else if (nameLoweCase.endsWith(".vert"))
                shaderKind = ShaderSPIRVUtils.ShaderKind.VERTEX_SHADER;

            if (shaderKind != null) {
                String source;
                File file = new File(devResourcesFolder, name);
                if (file.exists())
                    source = new String(FileTools.bytes(file), StandardCharsets.UTF_8);
                else
                    source = new String(IOTools.bytes(this.getClass().getResourceAsStream("/" + name)), StandardCharsets.UTF_8);

                ShaderSPIRVUtils.SPIRV spirv = ShaderSPIRVUtils.compileShader(name, source, shaderKind);
                byte[] bytes = new byte[spirv.bytecode().limit()];
                spirv.bytecode().get(bytes);
                spirv.free();

                return new ByteArrayInputStream(bytes);
            }
        }
        return this.getClass().getResourceAsStream("/" + asset);
    }

    @Override
    protected File getAssetFile(String asset) throws IOException {
        File f = new File(devResourcesFolder, asset);
        if (f.exists())
            return f;

        return super.getAssetFile(asset);
    }

    public ByteBuffer loadAssetAsByteBuffer(String asset) throws IOException {
        byte[] bytes = IOTools.bytes(loadAsset(asset));
        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        return buffer;
    }

    @Override
    public boolean addResourceChangeListener(Consumer<File> listener) {
        if (resourcesListener != null)
            return resourcesListener.addListener(listener);
        return false;
    }
}
