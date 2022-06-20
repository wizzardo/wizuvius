package com.wizzardo.vulkan;

import static org.lwjgl.system.Configuration.DEBUG;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkAcquireNextImageKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkDestroySwapchainKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR;

import static java.util.stream.Collectors.toSet;
import static org.lwjgl.vulkan.VK10.*;

import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.vulkan.input.InputsManager;
import com.wizzardo.vulkan.scene.Geometry;

import com.wizzardo.vulkan.scene.Node;
import com.wizzardo.vulkan.scene.Spatial;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class VulkanApplication extends Thread {

    private static final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;
    static final Set<String> VALIDATION_LAYERS;
    public static final boolean ENABLE_VALIDATION_LAYERS = DEBUG.get(false);
    public static final int MAX_FRAMES_IN_FLIGHT = 2;
    public static final String applicationName = "Hello Triangle";
    public static final String engineName = "No Engine";
    static final Set<String> DEVICE_EXTENSIONS = Stream.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME).collect(toSet());

    static {
        Properties properties = System.getProperties();
        if (properties.getProperty("os.name", "").contains("Mac")) {
            Configuration.GLFW_LIBRARY_NAME.set("glfw_async");
            String arch = properties.getProperty("os.arch", "");
            if (arch.equals("aarch64")) {
                Configuration.VULKAN_LIBRARY_NAME.set("macos/arm64/org/lwjgl/vulkan/libMoltenVK.dylib");
                Configuration.ASSIMP_LIBRARY_NAME.set("macos/arm64/org/lwjgl/assimp/libassimp.dylib");
            } else if (arch.contains("64")) {
                Configuration.VULKAN_LIBRARY_NAME.set("macos/x64/org/lwjgl/vulkan/libMoltenVK.dylib");
                Configuration.ASSIMP_LIBRARY_NAME.set("macos/x64/org/lwjgl/assimp/libassimp.dylib");
            }
        } else {
            Configuration.VULKAN_LIBRARY_NAME.set("libvulkan.so");
            Configuration.ASSIMP_LIBRARY_NAME.set("libassimp.so");
        }

        if (ENABLE_VALIDATION_LAYERS) {
            VALIDATION_LAYERS = new HashSet<>();
            VALIDATION_LAYERS.add("VK_LAYER_KHRONOS_validation");
        } else {
            VALIDATION_LAYERS = null;
        }
    }

    protected volatile boolean running = true;

    protected VkInstance instance;
    protected long debugMessenger;
    protected long surface;

    protected VkPhysicalDevice physicalDevice;
    protected VkDevice device;

    protected VkQueue graphicsQueue;
    protected VkQueue transferQueue;
    protected VkQueue presentQueue;

    protected long swapChain;
    protected List<Long> swapChainImages;
    protected List<Long> swapChainImageViews;
    protected Viewport mainViewport;
    protected Viewport guiViewport;
    protected InputsManager inputsManager;

    protected long descriptorPool;
    protected long commandPool;
    protected DepthResources depthResources;

    protected SyncObjects syncObjects;

    protected int currentFrame;
    protected volatile int width;
    protected volatile int height;
    protected volatile int extentWidth;
    protected volatile int extentHeight;
    protected boolean framebufferResize;
    protected Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();


    protected long previousPrintFps = System.nanoTime();
    protected long previousFrame = System.nanoTime();
    protected int fpsCounter = 0;
    protected boolean development = new File("build.gradle").exists();


    private static void destroyDebugUtilsMessengerEXT(VkInstance instance, long debugMessenger, VkAllocationCallbacks allocationCallbacks) {
        if (vkGetInstanceProcAddr(instance, "vkDestroyDebugUtilsMessengerEXT") != NULL) {
            vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, allocationCallbacks);
        }
    }

    public void run() {
        initWindow();
        initVulkan();
        initApp();
        mainLoop();
        cleanup();
    }

    public void shutdown() {
        running = false;
    }


    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        logV(() -> "resize " + width + "x" + height);
//        tasks.add(() -> {
//            recreateSwapChain();
//        });
    }


    protected void initWindow() {
    }

    protected void initApp() {
    }

    protected abstract InputsManager initInputsManager();

    public TextureImage createTextureImage(String asset) {
        Supplier<ByteBuffer> loader = () -> Unchecked.call(() -> {
            byte[] bytes = IOTools.bytes(loadAsset(asset));
            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return buffer;
        });
        return TextureLoader.createTextureImage(physicalDevice, device, transferQueue, commandPool, loader);
    }

    public long createTextureSampler(int mipLevels) {
        return TextureLoader.createTextureSampler(device, mipLevels);
    }

    protected abstract void logV(Supplier<String> data);

    protected abstract void logE(Supplier<String> data, Throwable e);

    protected abstract void logD(Supplier<String> data);

    protected abstract long createSurface(VkInstance instance);

    protected abstract InputStream loadAsset(String asset) throws IOException;

    public boolean isDevelopmentEnvironment() {
        return development;
    }

    public VkDevice getDevice() {
        return device;
    }

    public VkPhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }

    public long getCommandPool() {
        return commandPool;
    }

    public long getDescriptorPool() {
        return descriptorPool;
    }

    public List<Long> getSwapChainImages() {
        return swapChainImages;
    }

    public VkQueue getGraphicsQueue() {
        return graphicsQueue;
    }

    public VkQueue getTransferQueue() {
        return transferQueue;
    }

    public Viewport getMainViewport() {
        return mainViewport;
    }

    public Viewport getGuiViewport() {
        return guiViewport;
    }

    protected VulkanInstances getVulkanInstanceFactory() {
        return new VulkanInstances();
    }

    public InputsManager getInputsManager() {
        return inputsManager;
    }

    public void addTask(Runnable runnable) {
        tasks.add(runnable);
    }

    protected void initVulkan() {
        int vkVersion = VK.getInstanceVersionSupported();
        System.out.println("Vulkan version: "
                + VK10.VK_VERSION_MAJOR(vkVersion)
                + "." + VK10.VK_VERSION_MINOR(vkVersion)
                + "." + VK10.VK_VERSION_PATCH(vkVersion)
        );
        instance = getVulkanInstanceFactory().createInstance();
        debugMessenger = DebugTools.setupDebugMessenger(instance);
        surface = createSurface(instance);
        physicalDevice = VulkanDevices.pickPhysicalDevice(instance, surface);

        QueueFamilyIndices indices = VulkanQueues.findQueueFamilies(physicalDevice, surface);
        device = VulkanDevices.createLogicalDevice(physicalDevice, indices);
        graphicsQueue = VulkanQueues.createQueue(device, indices.getGraphicsFamily());
        transferQueue = VulkanQueues.createQueue(device, indices.getTransferFamily());
//        System.out.println(indices);
//        presentQueue = createPresentQueue(device, indices.presentFamily);
        presentQueue = VulkanQueues.createQueue(device, indices.getGraphicsFamily());
        commandPool = VulkanCommands.createCommandPool(device, indices);

        mainViewport = new Viewport();
        guiViewport = new Viewport();

        inputsManager = initInputsManager();

        createSwapChainObjects();

        syncObjects = SwapChainTools.createSyncObjects(device, swapChainImages, MAX_FRAMES_IN_FLIGHT);
        mainViewport.setCommandBuffers(VulkanCommands.createEmptyCommandBuffers(device, commandPool, swapChainImages.size()));
        guiViewport.setCommandBuffers(VulkanCommands.createEmptyCommandBuffers(device, commandPool, swapChainImages.size()));
    }


    protected void recreateSwapChain() {
//        try(MemoryStack stack = stackPush()) {
//
//            IntBuffer width = stack.ints(0);
//            IntBuffer height = stack.ints(0);
//
//            while(width.get(0) == 0 && height.get(0) == 0) {
//                glfwGetFramebufferSize(window, width, height);
//                glfwWaitEvents();
//            }
//        }

//        logV(() -> "recreateSwapChain " + width + "x" + height);
//        logV(() -> "recreateSwapChain.getSurfaceFrame: " + surfaceHolder.getSurfaceFrame().width() + "x" + surfaceHolder.getSurfaceFrame().height());

        vkDeviceWaitIdle(device);
        cleanupSwapChain();
        createSwapChainObjects();
    }

    protected void createSwapChainObjects() {
        SwapChainTools.CreateSwapChainResult result = SwapChainTools.createSwapChain(physicalDevice, device, surface, width, height);
        int swapChainImageFormat = result.swapChainImageFormat;
        swapChain = result.swapChain;
        swapChainImages = result.swapChainImages;
//        swapChainExtent = result.swapChainExtent;

        mainViewport.setExtent(result.swapChainExtent);
        guiViewport.setExtent(result.swapChainExtent);

        swapChainImageViews = SwapChainTools.createImageViews(device, swapChainImages, swapChainImageFormat);
        mainViewport.setRenderPass(VulkanRenderPass.createRenderPass(physicalDevice, device, swapChainImageFormat));
        guiViewport.setRenderPass(VulkanRenderPass.createGuiRenderPass(device, swapChainImageFormat));

        depthResources = VulkanImages.createDepthResources(physicalDevice, device, result.swapChainExtent, graphicsQueue, commandPool);
        mainViewport.setSwapChainFramebuffers(SwapChainTools.createFramebuffers(device, swapChainImageViews, depthResources.depthImageView, mainViewport.getRenderPass(), mainViewport.getExtent()));
        guiViewport.setSwapChainFramebuffers(SwapChainTools.createFramebuffers(device, swapChainImageViews, guiViewport.getRenderPass(), guiViewport.getExtent()));
        descriptorPool = VulkanDescriptorSets.createDescriptorPool(device, swapChainImages.size() * 100);

        mainViewport.camera.setProjection(
                45,
                (float) mainViewport.extent.width() / (float) mainViewport.extent.height(),
                0.1f,
                100f
        );
        System.out.println(width + "x" + height);

        extentWidth = result.swapChainExtent.width();
        extentHeight = result.swapChainExtent.height();
        System.out.println("Extent: " + extentWidth + "x" + extentHeight);
//        guiViewport.camera.setProjection(new Matrix4f(
//                2.0f / width, 0.0f, 0.0f, -0.0f,
//                0.0f, 2.0f / height, 0.0f, -0.0f,
//                0.0f, 0.0f, -2.0f, 1.0f,
//                0.0f, 0.0f, 0.0f, 1.0f
//                ).transpose()
//        );

        float n = 0.5f;
        float f = 1.5f;
        float r = 0.5f;
        float l = -0.5f;
        float t = 0.5f;
        float b = -0.5f;
        guiViewport.camera.setProjection(new Matrix4f(
                        2.0f / (r - l) / extentWidth, 0.0f, 0.0f, -(r + l) / (r - l),
                        0.0f, -2.0f / (t - b) / extentHeight, 0.0f, -(t + b) / (t - b),
                        0.0f, 0.0f, -2.0f / (f - n), 0 /*-(f + n) / (f - n)*/,
                        0.0f, 0.0f, 0.0f, 1.0f
                )
                        .transpose()
        );

//        System.out.println(guiViewport.camera.projection);

//        guiViewport.camera.lookAt(new Vector3f(0, 0, 1), new Vector3f(0, 1, 0));
        guiViewport.camera.lookAt(new Vector3f(0, 0, 1), new Vector3f(0, -1, 0));
        guiViewport.camera.getLocation().x = extentWidth / 2f;
        guiViewport.camera.getLocation().y = extentHeight / 2f;

        prepareGeometries(mainViewport);
        prepareGeometries(guiViewport);
    }

    protected void prepareGeometries(Viewport viewport) {
        List<Geometry> preparedGeometries = viewport.getGeometries();
        for (int i = 0; i < preparedGeometries.size(); i++) {
            Geometry geometry = preparedGeometries.get(i);
            geometry.getMaterial().prepare(this, viewport);
            geometry.prepare(this);
        }

    }

    static class DrawFrameTempData {
        final IntBuffer pImageIndex;
        final LongBuffer pLong;
        final LongBuffer pLong2;
        final IntBuffer pInt;
        final PointerBuffer pCommandBuffers;
        final VkSubmitInfo submitInfo;
        final VkPresentInfoKHR presentInfo;
        final CommandBufferTempData commandBufferTempData;
        final PointerBuffer pPointerBuffer;

        DrawFrameTempData(MemoryStack stack) {
            pImageIndex = stack.mallocInt(1);
            pLong = stack.mallocLong(1);
            pLong2 = stack.mallocLong(1);
            pInt = stack.mallocInt(1);
            pCommandBuffers = stack.mallocPointer(2);
            submitInfo = VkSubmitInfo.calloc(stack);
            presentInfo = VkPresentInfoKHR.calloc(stack);
            commandBufferTempData = new CommandBufferTempData(stack);
            pPointerBuffer = stack.mallocPointer(1);
        }
    }

    protected void mainLoop() {
        try (MemoryStack stack = stackPush()) {
            DrawFrameTempData tempData = new DrawFrameTempData(stack);
            while (running) {
                drawFrame(tempData);
                doInLoop();
            }
        }

        // Wait for the device to complete all operations before release resources
        vkDeviceWaitIdle(device);
    }

    protected void doInLoop() {
        while (!tasks.isEmpty()) {
            try {
                tasks.poll().run();
            } catch (Exception e) {
                logE(e::getMessage, e);
            }
        }
    }

    protected void cleanupSwapChain() {
        vkDestroyImageView(device, depthResources.depthImageView, null);
        vkDestroyImage(device, depthResources.depthImage, null);
        vkFreeMemory(device, depthResources.depthImageMemory, null);

        vkDestroyDescriptorPool(device, descriptorPool, null);

        mainViewport.cleanupSwapChain(device);
        guiViewport.cleanupSwapChain(device);

        swapChainImageViews.forEach(imageView -> vkDestroyImageView(device, imageView, null));

        vkDestroySwapchainKHR(device, swapChain, null);
    }

    protected void cleanup() {
        cleanupSwapChain();

        mainViewport.cleanup(device, commandPool);
        guiViewport.cleanup(device, commandPool);

        syncObjects.cleanup(device);

        vkDestroyCommandPool(device, commandPool, null);
        vkDestroyDevice(device, null);

        if (ENABLE_VALIDATION_LAYERS) {
            destroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
        }

        vkDestroySurfaceKHR(instance, surface, null);
        vkDestroyInstance(instance, null);
    }

    public void addGeometry(Geometry geometry, Viewport viewport) {
        geometry.getMaterial().prepare(this, viewport);
        geometry.getMesh().prepare(this, geometry.getMaterial().vertexLayout);
        geometry.prepare(this);

        viewport.getGeometries().add(geometry);
    }

    protected static CreateGraphicsPipelineResult createGraphicsPipeline(
            VkDevice device,
            ByteBuffer vertShaderSPIRV,
            ByteBuffer fragShaderSPIRV,
            VkExtent2D swapChainExtent,
            long renderPass,
            long descriptorSetLayout,
            Material.VertexLayout vertexLayout
    ) {
        try (MemoryStack stack = stackPush()) {
            long vertShaderModule = ShaderLoader.createShaderModule(device, vertShaderSPIRV);
            long fragShaderModule = ShaderLoader.createShaderModule(device, fragShaderSPIRV);

            ByteBuffer entryPoint = stack.UTF8("main");

            VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2, stack);

            VkPipelineShaderStageCreateInfo vertShaderStageInfo = shaderStages.get(0);
            vertShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            vertShaderStageInfo.stage(VK_SHADER_STAGE_VERTEX_BIT);
            vertShaderStageInfo.module(vertShaderModule);
            vertShaderStageInfo.pName(entryPoint);

            VkPipelineShaderStageCreateInfo fragShaderStageInfo = shaderStages.get(1);
            fragShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            fragShaderStageInfo.stage(VK_SHADER_STAGE_FRAGMENT_BIT);
            fragShaderStageInfo.module(fragShaderModule);
            fragShaderStageInfo.pName(entryPoint);

            // ===> VERTEX STAGE <===

            VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack);
            vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            vertexInputInfo.pVertexBindingDescriptions(Vertex.getBindingDescription(stack, vertexLayout));
            vertexInputInfo.pVertexAttributeDescriptions(Vertex.getAttributeDescriptions(stack, vertexLayout));


            // ===> ASSEMBLY STAGE <===

            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
            inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
            inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
            inputAssembly.primitiveRestartEnable(false);

            // ===> VIEWPORT & SCISSOR

            VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
            viewport.x(0.0f);
            viewport.y(0.0f);
            viewport.width(swapChainExtent.width());
            viewport.height(swapChainExtent.height());
            viewport.minDepth(0.0f);
            viewport.maxDepth(1.0f);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            scissor.offset(VkOffset2D.calloc(stack).set(0, 0));
            scissor.extent(swapChainExtent);

            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack);
            viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
            viewportState.pViewports(viewport);
            viewportState.pScissors(scissor);

            // ===> RASTERIZATION STAGE <===

            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack);
            rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
            rasterizer.depthClampEnable(false);
            rasterizer.rasterizerDiscardEnable(false);
            rasterizer.polygonMode(VK_POLYGON_MODE_FILL);
            rasterizer.lineWidth(1.0f);
//            rasterizer.cullMode(VK_CULL_MODE_BACK_BIT);
            rasterizer.cullMode(VK_CULL_MODE_NONE);
            rasterizer.frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE);
            rasterizer.depthBiasEnable(false);

            // ===> MULTISAMPLING <===

            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
            multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
            multisampling.sampleShadingEnable(false);
            multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

            VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack);
            depthStencil.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
            depthStencil.depthTestEnable(true);
            depthStencil.depthWriteEnable(true);
            depthStencil.depthCompareOp(VK_COMPARE_OP_LESS);
            depthStencil.depthBoundsTestEnable(false);
            depthStencil.minDepthBounds(0.0f); // Optional
            depthStencil.maxDepthBounds(1.0f); // Optional
            depthStencil.stencilTestEnable(false);

            // ===> COLOR BLENDING <===

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack);
            colorBlendAttachment.colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
            colorBlendAttachment.blendEnable(true);
            colorBlendAttachment.srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA);
            colorBlendAttachment.dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
            colorBlendAttachment.colorBlendOp(VK_BLEND_OP_ADD);
            colorBlendAttachment.srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE);
            colorBlendAttachment.dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO);
            colorBlendAttachment.alphaBlendOp(VK_BLEND_OP_ADD);

            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack);
            colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
            colorBlending.logicOpEnable(false);
            colorBlending.logicOp(VK_LOGIC_OP_COPY);
            colorBlending.pAttachments(colorBlendAttachment);
            colorBlending.blendConstants(stack.floats(0.0f, 0.0f, 0.0f, 0.0f));

            // ===> PIPELINE LAYOUT CREATION <===

            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            pipelineLayoutInfo.pSetLayouts(stack.longs(descriptorSetLayout));

            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);

            if (vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }

            long pipelineLayout = pPipelineLayout.get(0);

            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
            pipelineInfo.pStages(shaderStages);
            pipelineInfo.pVertexInputState(vertexInputInfo);
            pipelineInfo.pInputAssemblyState(inputAssembly);
            pipelineInfo.pViewportState(viewportState);
            pipelineInfo.pRasterizationState(rasterizer);
            pipelineInfo.pMultisampleState(multisampling);
            pipelineInfo.pDepthStencilState(depthStencil);
            pipelineInfo.pColorBlendState(colorBlending);
            pipelineInfo.layout(pipelineLayout);
            pipelineInfo.renderPass(renderPass);
            pipelineInfo.subpass(0);
            pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
            pipelineInfo.basePipelineIndex(-1);

            LongBuffer pGraphicsPipeline = stack.mallocLong(1);

            if (vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }

            long graphicsPipeline = pGraphicsPipeline.get(0);

            // ===> RELEASE RESOURCES <===

            vkDestroyShaderModule(device, vertShaderModule, null);
            vkDestroyShaderModule(device, fragShaderModule, null);

//            vertShaderSPIRV.free();
//            fragShaderSPIRV.free();
            return new CreateGraphicsPipelineResult(pipelineLayout, graphicsPipeline);
        }
    }


    public double getTime() {
        return System.nanoTime() / 1_000_000_000.0;
    }

    protected long prevAllocation = 0;
    protected com.sun.management.ThreadMXBean threadMXBean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    protected boolean allocationTrackingEnabled = threadMXBean.isThreadAllocatedMemorySupported() && threadMXBean.isThreadAllocatedMemoryEnabled();
    protected long threadId = -1;

    protected void printAllocation(String mark) {
        if (threadId == -1)
            threadId = Thread.currentThread().getId();

        if (allocationTrackingEnabled) {
            long allocatedBytes = threadMXBean.getThreadAllocatedBytes(threadId);
            if (allocatedBytes - prevAllocation > 0) {
                System.out.println(mark + " allocatedBytes: " + (allocatedBytes - prevAllocation));

                allocatedBytes = threadMXBean.getThreadAllocatedBytes(threadId);
                prevAllocation = allocatedBytes;
            }
        }
    }

    protected void drawFrame(DrawFrameTempData tempData) {
        printAllocation("drawFrame start");
        Frame thisFrame = getCurrentFrame();
        vkWaitForFences(device, thisFrame.pFence(tempData.pLong), true, UINT64_MAX);

        int vkResult = vkAcquireNextImageKHR(device, swapChain, UINT64_MAX, thisFrame.imageAvailableSemaphore(), VK_NULL_HANDLE, tempData.pImageIndex);
        if (vkResult == VK_ERROR_OUT_OF_DATE_KHR || vkResult == VK_SUBOPTIMAL_KHR) {
            if (vkResult == VK_ERROR_OUT_OF_DATE_KHR)
                logV(() -> "VK_ERROR_OUT_OF_DATE_KHR");
            if (vkResult == VK_SUBOPTIMAL_KHR)
                logV(() -> "VK_SUBOPTIMAL_KHR");
            recreateSwapChain();
            return;
        } else if (vkResult != VK_SUCCESS) {
            throw new RuntimeException("Cannot get image " + vkResult);
        }

        final int imageIndex = tempData.pImageIndex.get(0);

        Frame prev = syncObjects.byImage(imageIndex);
        if (prev != null) {
            vkWaitForFences(device, prev.pFence(tempData.pLong), true, UINT64_MAX);
            prev.onFinish();
            prev.resetListeners();
        }

        syncObjects.setByImage(imageIndex, thisFrame);

        long time = System.nanoTime();
        double tpf = (time - previousFrame) / 1_000_000_000.0;
        previousFrame = time;

        printAllocation("drawFrame before simpleUpdate");
        simpleUpdate(tpf);
        printAllocation("drawFrame after simpleUpdate");

        mainViewport.updateModelUniformBuffers(this, imageIndex, tempData.pPointerBuffer);
        guiViewport.updateModelUniformBuffers(this, imageIndex, tempData.pPointerBuffer);
        printAllocation("drawFrame after updateModelUniformBuffers");

        VkCommandBuffer commandBuffer = recordCommands(mainViewport, imageIndex, tempData.commandBufferTempData);
        VkCommandBuffer guiCommandBuffer = recordCommands(guiViewport, imageIndex, tempData.commandBufferTempData);
        printAllocation("drawFrame after recordCommands");

        vkResetFences(device, thisFrame.pFence(tempData.pLong));

        VkSubmitInfo submitInfo = tempData.submitInfo;
        submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

        submitInfo.waitSemaphoreCount(1);
        submitInfo.pWaitSemaphores(thisFrame.pImageAvailableSemaphore(tempData.pLong));
        submitInfo.pWaitDstStageMask(tempData.pInt.put(0, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
        submitInfo.pSignalSemaphores(thisFrame.pRenderFinishedSemaphore(tempData.pLong2));
        submitInfo.pCommandBuffers(tempData.pCommandBuffers.put(0, commandBuffer).put(1, guiCommandBuffer));

        if ((vkResult = vkQueueSubmit(graphicsQueue, submitInfo, thisFrame.fence())) != VK_SUCCESS) {
            vkResetFences(device, thisFrame.pFence(tempData.pLong));
            throw new RuntimeException("Failed to submit draw command buffer: " + vkResult);
        }

        VkPresentInfoKHR presentInfo = tempData.presentInfo;
        presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
        presentInfo.pWaitSemaphores(thisFrame.pRenderFinishedSemaphore(tempData.pLong));
        presentInfo.swapchainCount(1);
        presentInfo.pSwapchains(tempData.pLong2.put(0, swapChain));
        presentInfo.pImageIndices(tempData.pImageIndex);

        vkResult = vkQueuePresentKHR(presentQueue, presentInfo);
        if (vkResult == VK_ERROR_OUT_OF_DATE_KHR || vkResult == VK_SUBOPTIMAL_KHR || framebufferResize) {
            framebufferResize = false;
            if (vkResult == VK_ERROR_OUT_OF_DATE_KHR)
                logV(() -> "VK_ERROR_OUT_OF_DATE_KHR");
            if (vkResult == VK_SUBOPTIMAL_KHR)
                logV(() -> "VK_SUBOPTIMAL_KHR");
            if (framebufferResize)
                logV(() -> "framebufferResize");
            recreateSwapChain();
        } else if (vkResult != VK_SUCCESS) {
            throw new RuntimeException("Failed to present swap chain image");
        }

        currentFrame = (currentFrame + 1) % syncObjects.getFramesCount();

//        fpsCounter++;
//        if (time - previousPrintFps >= 1_000_000_000) {
//            System.out.println("fps: " + fpsCounter);
//            fpsCounter = 0;
//            previousPrintFps = time;
//        }
        printAllocation("drawFrame end");
    }

    public Frame getCurrentFrame() {
        return syncObjects.getFrame(currentFrame);
    }

    protected void simpleUpdate(double tpf) {
    }

    static class CommandBufferTempData {
        final VkCommandBufferBeginInfo beginInfo;
        final VkRenderPassBeginInfo renderPassInfo;
        final VkRect2D renderArea;
        final VkClearValue.Buffer clearValues;
        //        final         FloatBuffer clearColor;
        final LongBuffer pLong_1;
        final LongBuffer pLong_2;

        CommandBufferTempData(MemoryStack stack) {
            beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            renderPassInfo = VkRenderPassBeginInfo.calloc(stack);
            renderArea = VkRect2D.calloc(stack);
            clearValues = VkClearValue.calloc(2, stack);

//            clearColor = stack.floats(0.0f, 0.0f, 0.0f, 1.0f);
            clearValues.get(0).color().float32(stack.floats(0.0f, 0.0f, 0.0f, 1.0f));
            clearValues.get(1).depthStencil().set(1.0f, 0);
            pLong_1 = stack.longs(0);
            pLong_2 = stack.longs(0);
        }
    }

    protected Mesh recordCommandPreviousMesh = null;
    protected long recordCommandPreviousGraphicsPipeline = 0L;

    protected VkCommandBuffer recordCommands(Viewport viewport, int imageIndex, CommandBufferTempData tempData) {
        VkCommandBuffer commandBuffer = viewport.getCommandBuffers().get(imageIndex);

//            if (vkResetCommandBuffer(commandBuffer, 0) != VK_SUCCESS) {
//                throw new RuntimeException("Failed to reset command buffer");
//            }

        VkCommandBufferBeginInfo beginInfo = tempData.beginInfo;
        beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
        beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

        VkRenderPassBeginInfo renderPassInfo = tempData.renderPassInfo;
        renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);

        renderPassInfo.renderPass(viewport.getRenderPass());

        VkRect2D renderArea = tempData.renderArea;
        renderArea.offset(viewport.getOffset());
        renderArea.extent(viewport.getExtent());
        renderPassInfo.renderArea(renderArea);

        VkClearValue.Buffer clearValues = tempData.clearValues;
//            clearValues.get(0).color().float32(stack.floats(0.0f, 0.0f, 0.0f, 1.0f));
//            clearValues.get(1).depthStencil().set(1.0f, 0);
        renderPassInfo.pClearValues(clearValues);

        if (vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
            throw new RuntimeException("Failed to begin recording command buffer");
        }

        renderPassInfo.framebuffer(viewport.getSwapChainFramebuffers().get(imageIndex));

        vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

        List<Geometry> geometries = viewport.getGeometries();
        for (int j = 0; j < geometries.size(); j++) {
            Geometry geometry = geometries.get(j);
            recordGeometryDraw(geometry, commandBuffer, tempData, imageIndex);
        }

        recordGeometryDraw(viewport.getScene(), commandBuffer, tempData, imageIndex);
        recordCommandPreviousMesh = null;
        recordCommandPreviousGraphicsPipeline = 0L;

        vkCmdEndRenderPass(commandBuffer);
        if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
            throw new RuntimeException("Failed to record command buffer");
        }
        return commandBuffer;
    }

    protected void recordGeometryDraw(Node node, VkCommandBuffer commandBuffer, CommandBufferTempData tempData, int imageIndex){
        List<Spatial> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Spatial spatial = children.get(i);
            if (spatial instanceof Geometry) {
                recordGeometryDraw((Geometry) spatial, commandBuffer, tempData, imageIndex);
            } else if (spatial instanceof Node) {
                recordGeometryDraw((Node) spatial, commandBuffer, tempData, imageIndex);
            }
        }
    }
    protected void recordGeometryDraw(Geometry geometry, VkCommandBuffer commandBuffer, CommandBufferTempData tempData, int imageIndex) {
        if (!geometry.isPrepared())
            return;

        long graphicsPipeline = geometry.getMaterial().graphicsPipeline;
        if (graphicsPipeline != recordCommandPreviousGraphicsPipeline) {
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);
            recordCommandPreviousGraphicsPipeline = graphicsPipeline;
        }

        Mesh mesh = geometry.getMesh();
        if (mesh != recordCommandPreviousMesh) {
            LongBuffer vertexBuffers = tempData.pLong_1.put(0, mesh.getVertexBuffer().buffer);
            LongBuffer offsets = tempData.pLong_2.put(0, 0l);
            vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);
            vkCmdBindIndexBuffer(commandBuffer, mesh.getIndexBuffer().buffer, 0, VK_INDEX_TYPE_UINT32); // todo: use VK_INDEX_TYPE_UINT16 if short is enough
            recordCommandPreviousMesh = mesh;
        }

        vkCmdBindDescriptorSets(
                commandBuffer,
                VK_PIPELINE_BIND_POINT_GRAPHICS,
                geometry.getMaterial().pipelineLayout,
                0,
                tempData.pLong_1.put(0, geometry.getDescriptorSet(imageIndex)),
                null
        );

        vkCmdDrawIndexed(commandBuffer, mesh.getIndicesLength(), 1, 0, 0, 0);
    }

    public boolean addResourceChangeListener(Consumer<File> listener) {
        return false;
    }

    public boolean removeResourceChangeListener(Consumer<File> listener) {
        return false;
    }
}
