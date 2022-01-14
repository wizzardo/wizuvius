package com.wizzardo.vulkan;

import com.wizzardo.tools.io.IOTools;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkInstance;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
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

    @Override
    protected void logV(Supplier<String> data) {
        System.out.println("V " + data.get());
    }

    @Override
    protected void logE(Supplier<String> data, Throwable e) {
        System.out.println("E " + data.get());
        e.printStackTrace();
    }

    @Override
    protected void logD(Supplier<String> data) {
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

    @Override
    protected void initWindow() {
        if (!glfwInit()) {
            throw new RuntimeException("Cannot initialize GLFW");
        }

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        String title = getClass().getSimpleName();

        window = glfwCreateWindow(width, height, title, NULL, NULL);

        if (window == NULL) {
            throw new RuntimeException("Cannot create window");
        }
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

                    PointerBuffer extensions = stack.mallocPointer(glfwExtensions.capacity() + 1);

                    extensions.put(glfwExtensions);
                    extensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));

                    // Rewind the buffer before returning it to reset its position back to 0
                    return extensions.rewind();
                }

                return glfwExtensions;
            }
        };
    }

    @Override
    protected InputStream loadAsset(String asset) throws IOException {
        if (asset.toLowerCase().endsWith(".spv")) {
            String name = asset.substring(0, asset.length() - 4);
            ShaderSPIRVUtils.ShaderKind kind;
            if (name.toLowerCase().endsWith(".frag"))
                kind = ShaderSPIRVUtils.ShaderKind.FRAGMENT_SHADER;
            else if (name.toLowerCase().endsWith(".vert"))
                kind = ShaderSPIRVUtils.ShaderKind.VERTEX_SHADER;
            else
                throw new IllegalArgumentException();

            String source = new String(IOTools.bytes(loadAsset(name)), StandardCharsets.UTF_8);
            ShaderSPIRVUtils.SPIRV spirv = ShaderSPIRVUtils.compileShader(name, source, kind);
            byte[] bytes = new byte[spirv.bytecode().limit()];
            spirv.bytecode().get(bytes);
            spirv.free();
            return new ByteArrayInputStream(bytes);
        }
        return this.getClass().getResourceAsStream("/" + asset);
    }

}
