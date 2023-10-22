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

import com.wizzardo.tools.image.ImageTools;
import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.vulkan.input.InputsManager;
import com.wizzardo.vulkan.material.SpecializationConstantInfo;
import com.wizzardo.vulkan.misc.AtomicArrayList;
import com.wizzardo.vulkan.scene.Geometry;

import com.wizzardo.vulkan.scene.Node;
import com.wizzardo.vulkan.scene.Spatial;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryHelpers;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
        System.setProperty("joml.format", "false");
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

    protected int graphicsQueueTimestampValidBits;
    protected float timestampPeriod;
    protected long timestampQueryPool;
    protected VkQueue graphicsQueue;
    protected VkQueue transferQueue;
    protected VkQueue presentQueue;

    protected int swapChainImageFormat;
    protected long swapChain;
    protected List<Long> swapChainImages;
    protected List<Long> swapChainImageViews;
    protected Viewport mainViewport;
    protected Viewport guiViewport;
    protected InputsManager inputsManager;

    protected long descriptorPool;
    protected long commandPool;
    protected DepthResources depthResources;

    protected int bindlessTexturesCount = 4096;
    protected boolean bindlessTexturesEnabled = true;
    protected BindlessTexturePool bindlessTexturePool;

    protected SyncObjects syncObjects;
    protected long waitForSemaphore = 0;
    protected int currentFrame;
    protected volatile int width;
    protected volatile int height;
    protected volatile int extentWidth;
    protected volatile int extentHeight;
    protected boolean framebufferResize;
    protected Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    protected AtomicArrayList<Alteration> alterations = new AtomicArrayList<>(16);


    protected long previousPrintFps = System.nanoTime();
    protected long previousFrame = System.nanoTime();
    protected long fpsLimit = -1;
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

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getExtentWidth() {
        return extentWidth;
    }

    public int getExtentHeight() {
        return extentHeight;
    }

    public void setFpsLimit(int limit) {
        if (limit > 0) {
            fpsLimit = 1000 / limit;
        } else {
            fpsLimit = -1;
        }
    }

    public int getFpsLimit() {
        if (fpsLimit == -1)
            return -1;
        else
            return (int) (1000 / fpsLimit);
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

    public TextureImage createTextureImage(String asset) throws IOException {
        String assetLowerCase = asset.toLowerCase();
        if (assetLowerCase.endsWith(".ktx")) {
            File assetFile = Unchecked.ignore(() -> getAssetFile(asset), null);
            if (assetFile == null)
                return createTextureImage(asset + "2");

            return TextureLoader.createTextureImageKtx(physicalDevice, device, transferQueue, commandPool, assetFile);
        } else if (assetLowerCase.endsWith(".ktx2")) {
            return TextureLoader.createTextureImageKtx2(physicalDevice, device, transferQueue, commandPool, getAssetFile(asset));
        }

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

    protected File getAssetFile(String asset) throws IOException {
        File f = new File(asset);
        if (f.exists())
            return f;

        InputStream in = this.getClass().getResourceAsStream("/" + asset);
        if (in == null)
            throw new IllegalArgumentException("Asset " + asset + " not found");

        f = File.createTempFile(asset, "tmp");
        FileTools.bytes(f, in);
        return f;
    }

    ;

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
        physicalDevice = VulkanDevices.pickPhysicalDevice(instance, surface, bindlessTexturesEnabled);

        QueueFamilyIndices indices = VulkanQueues.findQueueFamilies(physicalDevice, surface);
        device = VulkanDevices.createLogicalDevice(physicalDevice, indices, bindlessTexturesEnabled);
        graphicsQueue = VulkanQueues.createQueue(device, indices.getGraphicsFamily());

        //https://stackoverflow.com/questions/67358235/how-to-measure-execution-time-of-vulkan-pipeline
        graphicsQueueTimestampValidBits = indices.getGraphicsQueueTimestampValidBits();
        if (graphicsQueueTimestampValidBits > 0) {
            try (MemoryStack stack = stackPush()) {
                VkPhysicalDeviceProperties properties = VulkanDevices.getPhysicalDeviceProperties(stack, physicalDevice);
                timestampPeriod = properties.limits().timestampPeriod();

                VkQueryPoolCreateInfo poolCreateInfo = VkQueryPoolCreateInfo.calloc(stack);
                poolCreateInfo.queryCount(100);
                poolCreateInfo.queryType(VK_QUERY_TYPE_TIMESTAMP);

                LongBuffer longs = stack.longs(0);
                vkCreateQueryPool(device, poolCreateInfo, null, longs);
                timestampQueryPool = longs.get(0);
            }
        }

        transferQueue = VulkanQueues.createQueue(device, indices.getTransferFamily());
//        System.out.println(indices);
//        presentQueue = createPresentQueue(device, indices.presentFamily);
        presentQueue = VulkanQueues.createQueue(device, indices.getGraphicsFamily());
        commandPool = VulkanCommands.createCommandPool(device, indices);

        mainViewport = new Viewport();
        guiViewport = new Viewport();

        inputsManager = initInputsManager();

        createSwapChainObjects();
        createBindlessTexturesDescriptorSet();

        syncObjects = SwapChainTools.createSyncObjects(device, swapChainImages, MAX_FRAMES_IN_FLIGHT);
        mainViewport.setCommandBuffers(VulkanCommands.createEmptyCommandBuffers(device, commandPool, swapChainImages.size()));
        guiViewport.setCommandBuffers(VulkanCommands.createEmptyCommandBuffers(device, commandPool, swapChainImages.size()));
    }

    protected void createBindlessTexturesDescriptorSet() {
        if (!bindlessTexturesEnabled)
            return;

        if (System.getProperties().getProperty("os.name", "").contains("Mac")) {
            if (!System.getenv().getOrDefault("MVK_CONFIG_USE_METAL_ARGUMENT_BUFFERS", "").equals("1")) {
                throw new IllegalArgumentException("Environment variables should contain MVK_CONFIG_USE_METAL_ARGUMENT_BUFFERS=1 to use bindless textures on mac");
            }
        }
        bindlessTexturePool = new BindlessTexturePool(device, physicalDevice, bindlessTexturesCount);
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
        swapChainImageFormat = result.swapChainImageFormat;
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

    protected static class DrawFrameTempData {
        public final IntBuffer pImageIndex;
        public final LongBuffer pLong;
        public final LongBuffer pLong2;
        public final LongBuffer pLong_2;
        public final IntBuffer pInt;
        public final PointerBuffer pCommandBuffers;
        public final VkSubmitInfo submitInfo;
        public final VkPresentInfoKHR presentInfo;
        public final CommandBufferTempData commandBufferTempData;
        public final PointerBuffer pPointerBuffer;

        public DrawFrameTempData(MemoryStack stack) {
            pImageIndex = stack.mallocInt(1);
            pLong = stack.mallocLong(1);
            pLong2 = stack.mallocLong(1);
            pLong_2 = stack.mallocLong(2);
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

        if (bindlessTexturePool != null)
            bindlessTexturePool.cleanup();

        mainViewport.cleanup(device, commandPool);
        guiViewport.cleanup(device, commandPool);

        syncObjects.cleanup(device);

        vkDestroyQueryPool(device, timestampQueryPool, null);
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
            Viewport viewport,
            Material.VertexLayout vertexLayout,
            List<SpecializationConstantInfo> constants,
            long... descriptorSetLayouts
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
            vertShaderStageInfo.pSpecializationInfo(prepareSpecializationInfo(0, constants, stack));

            VkPipelineShaderStageCreateInfo fragShaderStageInfo = shaderStages.get(1);
            fragShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            fragShaderStageInfo.stage(VK_SHADER_STAGE_FRAGMENT_BIT);
            fragShaderStageInfo.module(fragShaderModule);
            fragShaderStageInfo.pName(entryPoint);
            fragShaderStageInfo.pSpecializationInfo(prepareSpecializationInfo(1, constants, stack));

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

            VkViewport.Buffer vkViewports = VkViewport.calloc(1, stack);
            vkViewports.x(0.0f);
            vkViewports.y(0.0f);
            vkViewports.width(viewport.extent.width());
            vkViewports.height(viewport.extent.height());
            vkViewports.minDepth(0.0f);
            vkViewports.maxDepth(1.0f);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            scissor.offset(VkOffset2D.calloc(stack).set(0, 0));
            scissor.extent(viewport.extent);

            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack);
            viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
            viewportState.pViewports(vkViewports);
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
//            depthStencil.depthCompareOp(VK_COMPARE_OP_LESS_OR_EQUAL); // todo check the difference
            depthStencil.depthBoundsTestEnable(false);
            depthStencil.minDepthBounds(0.0f); // Optional
            depthStencil.maxDepthBounds(1.0f); // Optional
            depthStencil.stencilTestEnable(false);

            // ===> COLOR BLENDING <===
            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachments = VkPipelineColorBlendAttachmentState.calloc(viewport.getColorAttachmentsCount(), stack);
            for (int i = 0; i < colorBlendAttachments.capacity(); i++) {
                VkPipelineColorBlendAttachmentState attachmentState = colorBlendAttachments.get(i);
                attachmentState.colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
                if (viewport.isColorBlendingEnabled()) {
                    attachmentState.blendEnable(true);
                    attachmentState.srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA);
                    attachmentState.dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
                    attachmentState.colorBlendOp(VK_BLEND_OP_ADD);
                    attachmentState.srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE);
                    attachmentState.dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO);
                    attachmentState.alphaBlendOp(VK_BLEND_OP_ADD);
                } else {
                    attachmentState.blendEnable(false);
                }
            }

            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack);
            colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
            colorBlending.logicOpEnable(false);
            colorBlending.logicOp(VK_LOGIC_OP_COPY);
            colorBlending.pAttachments(colorBlendAttachments);
            colorBlending.blendConstants(stack.floats(0.0f, 0.0f, 0.0f, 0.0f));

            // ===> PIPELINE LAYOUT CREATION <===

            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            pipelineLayoutInfo.pSetLayouts(stack.longs(descriptorSetLayouts));

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
            pipelineInfo.renderPass(viewport.getRenderPass());
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

    protected static VkSpecializationInfo prepareSpecializationInfo(int stage, List<SpecializationConstantInfo> constants, MemoryStack stack) {
        List<SpecializationConstantInfo> list = constants.stream()
                .filter(it -> it.stage == stage)
                .sorted(Comparator.comparingInt(value -> value.constantId))
                .collect(Collectors.toList());
        if (!list.isEmpty()) {
            VkSpecializationInfo specializationInfo = VkSpecializationInfo.calloc(stack);
            VkSpecializationMapEntry.Buffer entries = VkSpecializationMapEntry.calloc(list.size(), stack);
            int offset = 0;
            for (int i = 0; i < list.size(); i++) {
                VkSpecializationMapEntry entry = entries.get(i);
                SpecializationConstantInfo info = list.get(i);
                entry.set(info.constantId, offset, info.size);
                offset += info.size;
            }
            ByteBuffer buffer = ByteBuffer.allocateDirect(offset).order(ByteOrder.nativeOrder());
            for (int i = 0; i < list.size(); i++) {
                list.get(i).accept(buffer);
            }
            buffer.rewind();
            specializationInfo.pMapEntries(entries);
            specializationInfo.pData(buffer);
            return specializationInfo;
        }
        return null;
    }


    public double getTime() {
        return System.nanoTime() / 1_000_000_000.0;
    }

    protected  abstract void printAllocation(String mark);

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

        waitForSemaphore = thisFrame.imageAvailableSemaphore();

        syncObjects.setByImage(imageIndex, thisFrame);

        long time = System.nanoTime();
        if (fpsLimit > 0 && (time - previousFrame) / 1000 / 1000 < fpsLimit) {
            long wait = fpsLimit - (time - previousFrame) / 1000 / 1000 - 1;
            try {
                Thread.sleep(wait);
            } catch (InterruptedException ignored) {
            }
            time = System.nanoTime();
        }

        double tpf = (time - previousFrame) / 1_000_000_000.0;
        previousFrame = time;

        printAllocation("drawFrame before simpleUpdate");
        simpleUpdate(tpf);
        processAlterations(tpf);
        printAllocation("drawFrame after simpleUpdate");

        updateModelUniformBuffers(imageIndex);
        printAllocation("drawFrame after updateModelUniformBuffers");

        recordCommands(tempData, imageIndex);
        printAllocation("drawFrame after recordCommands");

        vkResetFences(device, thisFrame.pFence(tempData.pLong));

        VkSubmitInfo submitInfo = tempData.submitInfo;
        submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

        submitInfo.waitSemaphoreCount(1);
        submitInfo.pWaitSemaphores(tempData.pLong.put(0, waitForSemaphore));
        submitInfo.pWaitDstStageMask(tempData.pInt.put(0, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
        submitInfo.pSignalSemaphores(thisFrame.pRenderFinishedSemaphore(tempData.pLong2));

        VkCommandBuffer commandBuffer = mainViewport.getCommandBuffers().get(imageIndex);
        VkCommandBuffer guiCommandBuffer = guiViewport.getCommandBuffers().get(imageIndex);
        submitInfo.pCommandBuffers(tempData.pCommandBuffers.put(0, commandBuffer).put(1, guiCommandBuffer));

        if ((vkResult = vkQueueSubmit(graphicsQueue, submitInfo, thisFrame.fence())) != VK_SUCCESS) {
//        if ((vkResult = vkQueueSubmit(graphicsQueue, submitInfo, 0)) != VK_SUCCESS) {
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

        fpsCounter++;
        if (time - previousPrintFps >= 1_000_000_000) {
            System.out.println("fps: " + fpsCounter);
            fpsCounter = 0;
            previousPrintFps = time;
        }
        printAllocation("drawFrame end");
    }

    private void processAlterations(double tpf) {
        int shiftLeftTo = -1;
        for (int i = 0; i < alterations.size(); i++) {
            Alteration alteration = alterations.get(i);
            if (!alteration.onUpdate(tpf)) {
                if (shiftLeftTo == -1)
                    shiftLeftTo = i;
            } else {
                if (shiftLeftTo != -1) {
                    alterations.set(shiftLeftTo, alteration);
                    shiftLeftTo++;
                }
            }
        }
        while (shiftLeftTo > 0 && shiftLeftTo < alterations.size()) {
            alterations.remove(alterations.size() - 1);
        }
    }

    public void addAlteration(Alteration alteration) {
        alterations.add(alteration);
    }

    protected void recordCommands(DrawFrameTempData tempData, int imageIndex) {
        recordCommands(mainViewport, imageIndex, tempData.commandBufferTempData);
        recordCommands(guiViewport, imageIndex, tempData.commandBufferTempData);
    }

    protected void updateModelUniformBuffers(int imageIndex) {
        mainViewport.updateModelUniformBuffers(this, imageIndex);
        guiViewport.updateModelUniformBuffers(this, imageIndex);
    }

    public Frame getCurrentFrame() {
        return syncObjects.getFrame(currentFrame);
    }

    protected void simpleUpdate(double tpf) {
    }

    protected static class CommandBufferTempData {
        public final VkCommandBufferBeginInfo beginInfo;
        public final VkRenderPassBeginInfo renderPassInfo;
        public final VkRect2D renderArea;
        public final VkClearValue.Buffer[] clearValues;
        public final LongBuffer pLong_1;
        public final LongBuffer pLong_2;
        public final LongBuffer pLongs2_1;

        public CommandBufferTempData(MemoryStack stack) {
            beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            renderPassInfo = VkRenderPassBeginInfo.calloc(stack);
            renderArea = VkRect2D.calloc(stack);

            clearValues = new VkClearValue.Buffer[4];
            for (int i = 0; i < clearValues.length; i++) {
                VkClearValue.Buffer clearValues = this.clearValues[i] = VkClearValue.calloc(i + 1, stack);
                for (int j = 0; j < i; j++) {
                    clearValues.get(j).color().float32(stack.floats(0.0f, 0.0f, 0.0f, 0.0f));
                }
                clearValues.get(clearValues.capacity() - 1).depthStencil().set(1.0f, 0);
            }

            pLong_1 = stack.longs(0);
            pLong_2 = stack.longs(0);
            pLongs2_1 = stack.longs(0, 0);
        }
    }

    protected Mesh recordCommandPreviousMesh = null;
    protected long recordCommandPreviousGraphicsPipeline = 0L;

    protected VkCommandBuffer recordCommands(Viewport viewport, int imageIndex, CommandBufferTempData tempData) {
        VkCommandBuffer commandBuffer = viewport.getCommandBuffers().get(imageIndex);

        VkCommandBufferBeginInfo beginInfo = tempData.beginInfo;
        beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
        beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

        if (vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
            throw new RuntimeException("Failed to begin recording command buffer");
        }

        recordCommands(viewport, imageIndex, tempData, commandBuffer);

        if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
            throw new RuntimeException("Failed to record command buffer");
        }
        return commandBuffer;
    }

    protected void recordCommands(Viewport viewport, int imageIndex, CommandBufferTempData tempData, VkCommandBuffer commandBuffer) {
        VkRenderPassBeginInfo renderPassInfo = tempData.renderPassInfo;
        renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);
        renderPassInfo.renderPass(viewport.getRenderPass());

        VkRect2D renderArea = tempData.renderArea;
        renderArea.offset(viewport.getOffset());
        renderArea.extent(viewport.getExtent());
        renderPassInfo.renderArea(renderArea);
        renderPassInfo.pClearValues(tempData.clearValues[viewport.colorAttachmentsCount]);
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
    }

    protected void recordGeometryDraw(Node node, VkCommandBuffer commandBuffer, CommandBufferTempData tempData, int imageIndex) {
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

        recordDraw(geometry.getMesh(), geometry.getMaterial(), geometry.getDescriptorSet(geometry.getMaterial(), imageIndex), commandBuffer, tempData);
    }

    protected void recordDraw(Mesh mesh, Material material, long descriptorSet, VkCommandBuffer commandBuffer, CommandBufferTempData tempData) {
        long graphicsPipeline = material.graphicsPipeline;
        if (graphicsPipeline != recordCommandPreviousGraphicsPipeline) {
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);
            recordCommandPreviousGraphicsPipeline = graphicsPipeline;
        }

        if (mesh != recordCommandPreviousMesh) {
            LongBuffer vertexBuffers = tempData.pLong_1.put(0, mesh.getVertexBuffer().buffer);
            LongBuffer offsets = tempData.pLong_2.put(0, 0l);
            vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);
            vkCmdBindIndexBuffer(commandBuffer, mesh.getIndexBuffer().buffer, 0, VK_INDEX_TYPE_UINT32); // todo: use VK_INDEX_TYPE_UINT16 if short is enough
            recordCommandPreviousMesh = mesh;
        }

        if (bindlessTexturesEnabled) {
            vkCmdBindDescriptorSets(
                    commandBuffer,
                    VK_PIPELINE_BIND_POINT_GRAPHICS,
                    material.pipelineLayout,
                    0,
                    tempData.pLongs2_1.put(0, descriptorSet).put(1, bindlessTexturePool.bindlessTexturesDescriptorSet),
                    null
            );
        } else {
            vkCmdBindDescriptorSets(
                    commandBuffer,
                    VK_PIPELINE_BIND_POINT_GRAPHICS,
                    material.pipelineLayout,
                    0,
                    tempData.pLong_1.put(0, descriptorSet),
                    null
            );
        }

        vkCmdDrawIndexed(commandBuffer, mesh.getIndicesLength(), 1, 0, 0, 0);
    }

    public boolean addResourceChangeListener(Consumer<File> listener) {
        return false;
    }

    public boolean removeResourceChangeListener(Consumer<File> listener) {
        return false;
    }

    public void saveScreenshot(String name) {
        try (MemoryStack stack = stackPush()) {
            boolean supportsBlit = true;
            int width = this.extentWidth;
            int height = this.extentHeight;

            // Check blit support for source and destination
            VkFormatProperties formatProps = VkFormatProperties.calloc(stack);
            // Check if the device supports blitting from optimal images (the swapchain images are in optimal format)
            vkGetPhysicalDeviceFormatProperties(physicalDevice, swapChainImageFormat, formatProps);
            if ((formatProps.optimalTilingFeatures() & VK_FORMAT_FEATURE_BLIT_SRC_BIT) == 0) {
                System.out.println("Device does not support blitting from optimal tiled images, using copy instead of blit!");
                supportsBlit = false;
            }

            // Check if the device supports blitting to linear images
            vkGetPhysicalDeviceFormatProperties(physicalDevice, VK_FORMAT_R8G8B8A8_UNORM, formatProps);
            if ((formatProps.linearTilingFeatures() & VK_FORMAT_FEATURE_BLIT_DST_BIT) == 0) {
                System.out.println("Device does not support blitting to linear tiled images, using copy instead of blit!");
                supportsBlit = false;
            }
            System.out.println("supportsBlit: " + supportsBlit);

            LongBuffer srcImage = stack.longs(swapChainImages.get(currentFrame));

            VkImageCreateInfo imageCreateCI = VkImageCreateInfo.calloc(stack);
            imageCreateCI.imageType(VK_IMAGE_TYPE_2D);
            // Note that vkCmdBlitImage (if supported) will also do format conversions if the swapchain color format would differ
            imageCreateCI.format(VK_FORMAT_R8G8B8A8_SRGB);
            imageCreateCI.extent().width(width);
            imageCreateCI.extent().height(height);
            imageCreateCI.extent().depth(1);
            imageCreateCI.arrayLayers(1);
            imageCreateCI.mipLevels(1);
            imageCreateCI.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageCreateCI.samples(VK_SAMPLE_COUNT_1_BIT);
            imageCreateCI.tiling(VK_IMAGE_TILING_LINEAR);
            imageCreateCI.usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT);

            LongBuffer dstImage = stack.mallocLong(1);
            if (vkCreateImage(device, imageCreateCI, null, dstImage) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create image");
            }

            // Create memory to back up the image
            VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc(stack);
            vkGetImageMemoryRequirements(device, dstImage.get(0), memRequirements);
            VkMemoryAllocateInfo memAllocInfo = VkMemoryAllocateInfo.calloc(stack);

            LongBuffer dstImageMemory = stack.mallocLong(1);
            memAllocInfo.allocationSize(memRequirements.size());
            memAllocInfo.memoryTypeIndex(VulkanBuffers.findMemoryTypeIndex(physicalDevice, memRequirements.memoryTypeBits(), VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT));

            if (vkAllocateMemory(device, memAllocInfo, null, dstImageMemory) != VK_SUCCESS) {
                throw new RuntimeException("Failed to AllocateMemory");
            }
            if (vkBindImageMemory(device, dstImage.get(0), dstImageMemory.get(0), 0) != VK_SUCCESS) {
                throw new RuntimeException("Failed to BindImageMemory");
            }

            VkCommandBuffer copyCmd = VulkanCommands.beginSingleTimeCommands(device, commandPool);

            // Transition destination image to transfer destination layout
            VulkanImages.insertImageMemoryBarrier(
                    stack,
                    copyCmd,
                    dstImage,
                    0,
                    VK_ACCESS_TRANSFER_WRITE_BIT,
                    VK_IMAGE_LAYOUT_UNDEFINED,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VkImageSubresourceRange.calloc(stack).set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1)
            );

            // Transition swapchain image from present to transfer source layout
            VulkanImages.insertImageMemoryBarrier(
                    stack,
                    copyCmd,
                    srcImage,
                    VK_ACCESS_MEMORY_READ_BIT,
                    VK_ACCESS_TRANSFER_READ_BIT,
                    KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
                    VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VkImageSubresourceRange.calloc(stack).set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1)
            );
            if (supportsBlit) {
                // Define the region to blit (we will blit the whole swapchain image)
                VkOffset3D blitSize = VkOffset3D.calloc(stack);
                blitSize.set(width, height, 1);
                VkImageBlit.Buffer imageBlitRegion = VkImageBlit.calloc(1, stack);
                imageBlitRegion.srcSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                imageBlitRegion.srcSubresource().layerCount(1);
                imageBlitRegion.srcOffsets(1).set(blitSize);
                imageBlitRegion.dstSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                imageBlitRegion.dstSubresource().layerCount(1);
                imageBlitRegion.dstOffsets(1).set(blitSize);

                // Issue the blit command
                vkCmdBlitImage(
                        copyCmd,
                        srcImage.get(0), VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                        dstImage.get(0), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        imageBlitRegion,
                        VK_FILTER_NEAREST);
            } else {
                // Otherwise use image copy (requires us to manually flip components)
                VkImageCopy.Buffer imageCopyRegion = VkImageCopy.calloc(1, stack);
                imageCopyRegion.srcSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                imageCopyRegion.srcSubresource().layerCount(1);
                imageCopyRegion.dstSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                imageCopyRegion.dstSubresource().layerCount(1);
                imageCopyRegion.extent().width(width);
                imageCopyRegion.extent().height(height);
                imageCopyRegion.extent().depth(1);

                // Issue the copy command
                vkCmdCopyImage(
                        copyCmd,
                        srcImage.get(0), VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                        dstImage.get(0), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        imageCopyRegion);
            }

            // Transition destination image to general layout, which is the required layout for mapping the image memory later on
            VulkanImages.insertImageMemoryBarrier(
                    stack,
                    copyCmd,
                    dstImage,
                    VK_ACCESS_TRANSFER_WRITE_BIT,
                    VK_ACCESS_MEMORY_READ_BIT,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    VK_IMAGE_LAYOUT_GENERAL,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VkImageSubresourceRange.calloc(stack).set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1)
            );


            // Transition back the swap chain image after the blit is done
            VulkanImages.insertImageMemoryBarrier(
                    stack,
                    copyCmd,
                    srcImage,
                    VK_ACCESS_TRANSFER_READ_BIT,
                    VK_ACCESS_MEMORY_READ_BIT,
                    VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VkImageSubresourceRange.calloc(stack).set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1));

            VulkanCommands.endSingleTimeCommands(device, graphicsQueue, commandPool, copyCmd);

//            // Get layout of the image (including row pitch)
//            VkImageSubresource subResource = VkImageSubresource.calloc(stack).set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0);
//
//            VkSubresourceLayout subResourceLayout = VkSubresourceLayout.calloc(stack);
//            vkGetImageSubresourceLayout(device, dstImage.get(0), subResource, subResourceLayout);

            ByteBuffer byteBufferPointer = MemoryUtil.memByteBufferSafe(-1, 0);
            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(device, dstImageMemory.get(0), 0, VK_WHOLE_SIZE, 0, data);
            MemoryHelpers.remapByteBuffer(byteBufferPointer, data.get(0), (int) memRequirements.size());
//                        byteBufferPointer.position((int) subResourceLayout.offset());

            // If source is BGR (destination is always RGB) and we can't use blit (which does automatic conversion), we'll have to manually swizzle color components
            boolean colorSwizzle = false;
            // Check if source is BGR
            // Note: Not complete, only contains most common and basic BGR surface formats for demonstration purposes
            if (!supportsBlit) {
                if (swapChainImageFormat == VK_FORMAT_B8G8R8A8_SRGB)
                    colorSwizzle = true;
            }

            int[] row = new int[width];

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            WritableRaster raster = image.getRaster();
            IntBuffer ints = byteBufferPointer.asIntBuffer();
            int[] pixel = new int[3];
            for (int y = 0; y < height; y++) {
                ints.get(row);
                for (int x = 0; x < width; x++) {
                    int value = row[x];
                    if (colorSwizzle) {
                        pixel[0] = ((value >> 16) & 0xFF);
                        pixel[1] = ((value >> 8) & 0xFF);
                        pixel[2] = (value & 0xFF);
                    } else {
                        pixel[2] = ((value >> 16) & 0xFF);
                        pixel[1] = ((value >> 8) & 0xFF);
                        pixel[0] = (value & 0xFF);
                    }
                    raster.setPixel(x, y, pixel);
                }
            }

            FileTools.bytes(name + ".png", ImageTools.savePNGtoBytes(image));

            vkUnmapMemory(device, dstImageMemory.get(0));
            vkFreeMemory(device, dstImageMemory.get(0), null);
            vkDestroyImage(device, dstImage.get(0), null);

            System.out.println("screenshot saved");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
