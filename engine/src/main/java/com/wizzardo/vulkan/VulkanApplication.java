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
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_A_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_B_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_G_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_R_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMPARE_OP_LESS;
import static org.lwjgl.vulkan.VK10.VK_CULL_MODE_BACK_BIT;
import static org.lwjgl.vulkan.VK10.VK_CULL_MODE_FRONT_AND_BACK;
import static org.lwjgl.vulkan.VK10.VK_CULL_MODE_NONE;
import static org.lwjgl.vulkan.VK10.VK_FRONT_FACE_COUNTER_CLOCKWISE;
import static org.lwjgl.vulkan.VK10.VK_INDEX_TYPE_UINT32;
import static org.lwjgl.vulkan.VK10.VK_LOGIC_OP_COPY;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
import static org.lwjgl.vulkan.VK10.VK_POLYGON_MODE_FILL;
import static org.lwjgl.vulkan.VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
import static org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUBPASS_CONTENTS_INLINE;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkBeginCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkCmdBeginRenderPass;
import static org.lwjgl.vulkan.VK10.vkCmdBindDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkCmdBindIndexBuffer;
import static org.lwjgl.vulkan.VK10.vkCmdBindPipeline;
import static org.lwjgl.vulkan.VK10.vkCmdBindVertexBuffers;
import static org.lwjgl.vulkan.VK10.vkCmdDrawIndexed;
import static org.lwjgl.vulkan.VK10.vkCmdEndRenderPass;
import static org.lwjgl.vulkan.VK10.vkCreateGraphicsPipelines;
import static org.lwjgl.vulkan.VK10.vkCreatePipelineLayout;
import static org.lwjgl.vulkan.VK10.vkDestroyCommandPool;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorPool;
import static org.lwjgl.vulkan.VK10.vkDestroyDevice;
import static org.lwjgl.vulkan.VK10.vkDestroyImage;
import static org.lwjgl.vulkan.VK10.vkDestroyImageView;
import static org.lwjgl.vulkan.VK10.vkDestroyInstance;
import static org.lwjgl.vulkan.VK10.vkDestroyShaderModule;
import static org.lwjgl.vulkan.VK10.vkDeviceWaitIdle;
import static org.lwjgl.vulkan.VK10.vkEndCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;
import static org.lwjgl.vulkan.VK10.vkGetInstanceProcAddr;
import static org.lwjgl.vulkan.VK10.vkQueueSubmit;
import static org.lwjgl.vulkan.VK10.vkResetFences;
import static org.lwjgl.vulkan.VK10.vkWaitForFences;

import static java.util.stream.Collectors.toSet;

import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.vulkan.scene.Geometry;

import org.joml.Matrix4f;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkOffset2D;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkViewport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class VulkanApplication extends Thread {

    private static final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    static final Set<String> VALIDATION_LAYERS;
    public static final boolean ENABLE_VALIDATION_LAYERS = DEBUG.get(false);
    public static final int MAX_FRAMES_IN_FLIGHT = 2;
    public static final String applicationName = "Hello Triangle";
    public static final String engineName = "No Engine";
    static final Set<String> DEVICE_EXTENSIONS = Stream.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME).collect(toSet());

    static {
        Configuration.VULKAN_LIBRARY_NAME.set("libvulkan.so");
        Configuration.ASSIMP_LIBRARY_NAME.set("libassimp.so");

        if (ENABLE_VALIDATION_LAYERS) {
            VALIDATION_LAYERS = new HashSet<>();
            VALIDATION_LAYERS.add("VK_LAYER_KHRONOS_validation");
        } else {
            VALIDATION_LAYERS = null;
        }
    }

    private volatile boolean running = true;

    private VkInstance instance;
    private long debugMessenger;
    private long surface;

    private VkPhysicalDevice physicalDevice;
    private VkDevice device;

    private VkQueue graphicsQueue;
    private VkQueue presentQueue;

    private long swapChain;
    private List<Long> swapChainImages;
    private List<Long> swapChainImageViews;
    private Viewport mainViewport;
    private Viewport guiViewport;

    private long descriptorPool;
    private long commandPool;
    private DepthResources depthResources;

    private SyncObjects syncObjects;

    private int currentFrame;
    private volatile int width;
    private volatile int height;
    private boolean framebufferResize;
    private Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();


    private long previousPrintFps = System.nanoTime();
    private long previousFrame = System.nanoTime();
    private int fpsCounter = 0;


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

    public TextureImage createTextureImage(String asset) {
        Supplier<ByteBuffer> loader = () -> Unchecked.call(() -> {
            byte[] bytes = IOTools.bytes(loadAsset(asset));
            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return buffer;
        });
        return TextureLoader.createTextureImage(physicalDevice, device, graphicsQueue, commandPool, loader);
    }

    public long createTextureSampler(int mipLevels) {
        return TextureLoader.createTextureSampler(device, mipLevels);
    }

    protected abstract void logV(Supplier<String> data);

    protected abstract void logE(Supplier<String> data, Throwable e);

    protected abstract void logD(Supplier<String> data);

    protected abstract long createSurface(VkInstance instance);

    protected abstract InputStream loadAsset(String asset) throws IOException;


    public VkDevice getDevice() {
        return device;
    }

    public Viewport getMainViewport() {
        return mainViewport;
    }

    public Viewport getGuiViewport() {
        return guiViewport;
    }

    protected void initVulkan() {
        instance = VulkanInstances.createInstance();
        debugMessenger = DebugTools.setupDebugMessenger(instance);
        surface = createSurface(instance);
        physicalDevice = VulkanDevices.pickPhysicalDevice(instance, surface);

        QueueFamilyIndices indices = VulkanQueues.findQueueFamilies(physicalDevice);
        device = VulkanDevices.createLogicalDevice(physicalDevice, indices);
        graphicsQueue = VulkanQueues.createQueue(device, indices.getGraphicsFamily());
//        presentQueue = createPresentQueue(device, indices.presentFamily);
        presentQueue = VulkanQueues.createQueue(device, indices.getGraphicsFamily());
        commandPool = VulkanCommands.createCommandPool(device, indices);

        mainViewport = new Viewport();
        guiViewport = new Viewport();

        createSwapChainObjects();

        mainViewport.camera.setProjection(45, (float) mainViewport.extent.width() / (float) mainViewport.extent.height(), 0.1f, 10f);
        guiViewport.camera.setProjection(new Matrix4f(
                2.0f, 0.0f, 0.0f, -0.0f,
                0.0f, 2.0f, 0.0f, -0.0f,
                0.0f, 0.0f, -2.0f, 1.0f,
                0.0f, 0.0f, 0.0f, 1.0f).transpose());

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

        prepareGeometries(mainViewport);
        prepareGeometries(guiViewport);
    }

    protected void prepareGeometries(Viewport viewport) {
        List<PreparedGeometry> preparedGeometries = viewport.getPreparedGeometries();
        for (int i = 0; i < preparedGeometries.size(); i++) {
            PreparedGeometry geometry = preparedGeometries.get(i);
            Material material = geometry.geometry.getMaterial();
            if (material.pipelineLayout == 0L) {
                ByteBuffer vertShaderSPIRV = Unchecked.call(() -> {
                    byte[] bytes = IOTools.bytes(loadAsset(material.vertexShader));
                    ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
                    buffer.put(bytes);
                    buffer.flip();
                    return buffer;
                });
                ByteBuffer fragShaderSPIRV = Unchecked.call(() -> {
                    byte[] bytes = IOTools.bytes(loadAsset(material.fragmentShader));
                    ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
                    buffer.put(bytes);
                    buffer.flip();
                    return buffer;
                });
                CreateGraphicsPipelineResult pipeline = createGraphicsPipeline(
                        device,
                        vertShaderSPIRV,
                        fragShaderSPIRV,
                        viewport.getExtent(),
                        viewport.getRenderPass(),
                        geometry.descriptorSetLayout
                );
                material.pipelineLayout = pipeline.pipelineLayout;
                material.graphicsPipeline = pipeline.graphicsPipeline;
            }

            UniformBuffers uniformBuffers = UniformBuffers.createUniformBuffers(physicalDevice, device, swapChainImages);

            geometry.descriptorSets = VulkanDescriptorSets.createDescriptorSets(device,
                    swapChainImages,
                    geometry.descriptorSetLayout,
                    descriptorPool,
                    material.textureImage.textureImageView,
                    material.textureSampler,
                    uniformBuffers.uniformBuffers
            );
            geometry.uniformBuffers = uniformBuffers;
        }

    }

    protected void mainLoop() {
        while (running) {
            drawFrame();

            while (!tasks.isEmpty()) {
                try {
                    tasks.poll().run();
                } catch (Exception e) {
                    logE(e::getMessage, e);
                }
            }
        }

        // Wait for the device to complete all operations before release resources
        vkDeviceWaitIdle(device);
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
        PreparedGeometry preparedGeometry = new PreparedGeometry(geometry);
        Mesh mesh = geometry.getMesh();

        if (mesh.getVertexBuffer() == null || mesh.getIndexBuffer() == null) {
            mesh.setVertexBuffer(Utils.createVertexBuffer(physicalDevice, device, graphicsQueue, commandPool, mesh.getVertices()));
            mesh.setIndexBuffer(Utils.createIndexBuffer(physicalDevice, device, graphicsQueue, commandPool, mesh.getIndices()));
            mesh.setIndicesLength(mesh.getIndices().length);
        }

        preparedGeometry.descriptorSetLayout = VulkanDescriptorSets.createDescriptorSetLayout(device);

        Material material = geometry.getMaterial();
        if (material.pipelineLayout == 0L) {
            ByteBuffer vertShaderSPIRV = Unchecked.call(() -> {
                byte[] bytes = IOTools.bytes(loadAsset(material.vertexShader));
                ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
                buffer.put(bytes);
                buffer.flip();
                return buffer;
            });
            ByteBuffer fragShaderSPIRV = Unchecked.call(() -> {
                byte[] bytes = IOTools.bytes(loadAsset(material.fragmentShader));
                ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
                buffer.put(bytes);
                buffer.flip();
                return buffer;
            });
            CreateGraphicsPipelineResult pipeline = createGraphicsPipeline(
                    device,
                    vertShaderSPIRV,
                    fragShaderSPIRV,
                    viewport.getExtent(),
                    viewport.getRenderPass(),
                    preparedGeometry.descriptorSetLayout
            );
            material.pipelineLayout = pipeline.pipelineLayout;
            material.graphicsPipeline = pipeline.graphicsPipeline;
        }

        UniformBuffers uniformBuffers = UniformBuffers.createUniformBuffers(physicalDevice, device, swapChainImages);

        preparedGeometry.descriptorSets = VulkanDescriptorSets.createDescriptorSets(device,
                swapChainImages,
                preparedGeometry.descriptorSetLayout,
                descriptorPool,
                material.textureImage.textureImageView,
                material.textureSampler,
                uniformBuffers.uniformBuffers
        );
        preparedGeometry.uniformBuffers = uniformBuffers;

        viewport.getPreparedGeometries().add(preparedGeometry);
    }

    protected static CreateGraphicsPipelineResult createGraphicsPipeline(
            VkDevice device,
            ByteBuffer vertShaderSPIRV,
            ByteBuffer fragShaderSPIRV,
            VkExtent2D swapChainExtent,
            long renderPass,
            long descriptorSetLayout
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
            vertexInputInfo.pVertexBindingDescriptions(Vertex.getBindingDescription());
            vertexInputInfo.pVertexAttributeDescriptions(Vertex.getAttributeDescriptions());


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
            colorBlendAttachment.blendEnable(false);

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

    private void drawFrame() {
        try (MemoryStack stack = stackPush()) {
            Frame thisFrame = syncObjects.getFrame(currentFrame);
            vkWaitForFences(device, thisFrame.pFence(stack), true, UINT64_MAX);

            IntBuffer pImageIndex = stack.mallocInt(1);
            int vkResult = vkAcquireNextImageKHR(device, swapChain, UINT64_MAX, thisFrame.imageAvailableSemaphore(), VK_NULL_HANDLE, pImageIndex);
            if (vkResult == VK_ERROR_OUT_OF_DATE_KHR) {
                logV(() -> "VK_ERROR_OUT_OF_DATE_KHR");
                recreateSwapChain();
                return;
            } else if (vkResult != VK_SUCCESS) {
                throw new RuntimeException("Cannot get image");
            }

            final int imageIndex = pImageIndex.get(0);

            Frame prev = syncObjects.byImage(imageIndex);
            if (prev != null) {
                vkWaitForFences(device, prev.pFence(stack), true, UINT64_MAX);
            }

            syncObjects.setByImage(imageIndex, thisFrame);

            long time = System.nanoTime();
            double tpf = (previousFrame - time) / 1_000_000_000.0;
            previousFrame = time;

            simpleUpdate(tpf);

            mainViewport.updateModelUniformBuffers(this, imageIndex);
            guiViewport.updateModelUniformBuffers(this, imageIndex);

            VkCommandBuffer commandBuffer = recordCommands(mainViewport, imageIndex);
            VkCommandBuffer guiCommandBuffer = recordCommands(guiViewport, imageIndex);

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

            submitInfo.waitSemaphoreCount(1);
            submitInfo.pWaitSemaphores(thisFrame.pImageAvailableSemaphore(stack));
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
            submitInfo.pSignalSemaphores(thisFrame.pRenderFinishedSemaphore(stack));
            submitInfo.pCommandBuffers(stack.pointers(commandBuffer, guiCommandBuffer));

            vkResetFences(device, thisFrame.pFence(stack));

            if ((vkResult = vkQueueSubmit(graphicsQueue, submitInfo, thisFrame.fence())) != VK_SUCCESS) {
                vkResetFences(device, thisFrame.pFence(stack));
                throw new RuntimeException("Failed to submit draw command buffer: " + vkResult);
            }

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
            presentInfo.pWaitSemaphores(thisFrame.pRenderFinishedSemaphore(stack));
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(swapChain));
            presentInfo.pImageIndices(pImageIndex);

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
        }

        fpsCounter++;
        long time = System.nanoTime();
        if (time - previousPrintFps >= 1_000_000_000) {
            System.out.println("fps: " + fpsCounter);
            fpsCounter = 0;
            previousPrintFps = time;
        }
    }

    protected void simpleUpdate(double tpf) {
    }

    protected VkCommandBuffer recordCommands(Viewport viewport, int imageIndex) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBuffer commandBuffer = viewport.getCommandBuffers().get(imageIndex);

//            if (vkResetCommandBuffer(commandBuffer, 0) != VK_SUCCESS) {
//                throw new RuntimeException("Failed to reset command buffer");
//            }

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);

            renderPassInfo.renderPass(viewport.getRenderPass());

            VkRect2D renderArea = VkRect2D.calloc(stack);
            renderArea.offset(viewport.getOffset());
            renderArea.extent(viewport.getExtent());
            renderPassInfo.renderArea(renderArea);

            VkClearValue.Buffer clearValues = VkClearValue.calloc(2, stack);
            clearValues.get(0).color().float32(stack.floats(0.0f, 0.0f, 0.0f, 1.0f));
            clearValues.get(1).depthStencil().set(1.0f, 0);
            renderPassInfo.pClearValues(clearValues);

            if (vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
                throw new RuntimeException("Failed to begin recording command buffer");
            }

            renderPassInfo.framebuffer(viewport.getSwapChainFramebuffers().get(imageIndex));

            vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

            long previousGraphicsPipeline = 0;
            Mesh previousMesh = null;
            List<PreparedGeometry> preparedGeometries = viewport.getPreparedGeometries();
            for (int j = 0; j < preparedGeometries.size(); j++) {
                PreparedGeometry geometry = preparedGeometries.get(j);

                long graphicsPipeline = geometry.geometry.getMaterial().graphicsPipeline;
                if (graphicsPipeline != previousGraphicsPipeline) {
                    vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);
                    previousGraphicsPipeline = graphicsPipeline;
                }

                Mesh mesh = geometry.geometry.getMesh();
                if (mesh != previousMesh) {
                    LongBuffer vertexBuffers = stack.longs(mesh.getVertexBuffer().buffer);
                    LongBuffer offsets = stack.longs(0);
                    vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);
                    vkCmdBindIndexBuffer(commandBuffer, mesh.getIndexBuffer().buffer, 0, VK_INDEX_TYPE_UINT32); // todo: use VK_INDEX_TYPE_UINT16 if short is enough
                    previousMesh = mesh;
                }

                vkCmdBindDescriptorSets(
                        commandBuffer,
                        VK_PIPELINE_BIND_POINT_GRAPHICS,
                        geometry.geometry.getMaterial().pipelineLayout,
                        0,
                        stack.longs(geometry.descriptorSets.get(imageIndex)),
                        null
                );

                vkCmdDrawIndexed(commandBuffer, mesh.getIndicesLength(), 1, 0, 0, 0);
            }

            vkCmdEndRenderPass(commandBuffer);
            if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to record command buffer");
            }
            return commandBuffer;
        }
    }

}
