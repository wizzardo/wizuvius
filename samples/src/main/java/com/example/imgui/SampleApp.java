package com.example.imgui;

import com.example.AbstractSampleApp;
import com.wizzardo.tools.io.FileTools;
import com.wizzardo.vulkan.*;
import com.wizzardo.vulkan.material.PushConstantInfo;
import com.wizzardo.vulkan.material.predefined.UnshadedColor;
import com.wizzardo.vulkan.scene.Geometry;
import com.wizzardo.vulkan.scene.Node;
import com.wizzardo.vulkan.scene.shape.Box;
import imgui.*;
//import imgui.backends.glfw.ImGuiImplGlfw;
import imgui.callback.ImGuiInputTextCallback;
import imgui.flag.ImGuiInputTextFlags;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImInt;
import imgui.type.ImString;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.util.List;
import java.util.Properties;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class SampleApp extends AbstractSampleApp {

    public static void main(String[] args) {
        new SampleApp().start();
    }

    ImguiMesh imguiMesh;
    Material imguiMaterial;
    ImGuiImplGlfw imGuiImplGlfw;

    static class ImguiMesh extends Mesh {
        protected final ImDrawData drawData = ImGui.getDrawData();
        protected final ImGuiIO io = ImGui.getIO();
        protected ImVec4 clipRect = new ImVec4();
        protected int vertexCount;
        protected int indexCount;
        protected VulkanApplication app;
        protected Material.VertexLayout vertexLayout;
        protected ByteBuffer constants;
        protected MethodHandle nGetCmdListVtxBufferData;
        protected MethodHandle nGetCmdListIdxBufferData;

        public ImguiMesh(VulkanApplication app) {
            super(new Vertex[0], new int[0]);
            this.app = app;

            if (ImDrawData.sizeOfImDrawIdx() == 2)
                indexBufferType = VK_INDEX_TYPE_UINT16;

            constants = ByteBuffer.allocateDirect(Float.BYTES * 4).order(ByteOrder.nativeOrder());
            constants.putFloat(-1);
            constants.putFloat(-1);
            constants.putFloat(-1);
            constants.putFloat(-1);
            constants.position(0);

            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                {
                    Method method = ImDrawData.class.getDeclaredMethod("nGetCmdListVtxBufferData", int.class, ByteBuffer.class, int.class);
                    method.setAccessible(true);
                    nGetCmdListVtxBufferData = lookup.unreflect(method);
                }
                {
                    Method method = ImDrawData.class.getDeclaredMethod("nGetCmdListIdxBufferData", int.class, ByteBuffer.class, int.class);
                    method.setAccessible(true);
                    nGetCmdListIdxBufferData = lookup.unreflect(method);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void draw(VkCommandBuffer commandBuffer, Material material, CommandBufferTempData tempData) {

            VkViewport.Buffer viewport = tempData.viewport;
            viewport.width(io.getDisplaySizeX());
            viewport.height(io.getDisplaySizeY());
            viewport.minDepth(0);
            viewport.maxDepth(1);
            vkCmdSetViewport(commandBuffer, 0, viewport);

            constants.putFloat(0, 2.0f / io.getDisplaySizeX());
            constants.putFloat(4, 2.0f / io.getDisplaySizeY());

            vkCmdPushConstants(commandBuffer, material.pipeline.pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0, constants);


            int cmdListsCount = drawData.getCmdListsCount();
            for (int i = 0; i < cmdListsCount; i++) {

                int commands = drawData.getCmdListCmdBufferSize(i);
                for (int j = 0; j < commands; j++) {
                    drawData.getCmdListCmdBufferClipRect(clipRect, i, j);
                    tempData.scissors.offset(tempData.offset2D.set(Math.max(0, (int) clipRect.x), Math.max(0, (int) clipRect.y)));
                    tempData.scissors.extent(tempData.extent2D.set((int) (clipRect.z - clipRect.x), (int) (clipRect.w - clipRect.y)));
                    vkCmdSetScissor(commandBuffer, 0, tempData.scissors);

                    int elements = drawData.getCmdListCmdBufferElemCount(i, j);
                    int indexOffset = drawData.getCmdListCmdBufferIdxOffset(i, j);
                    int vertexOffset = drawData.getCmdListCmdBufferVtxOffset(i, j);
                    vkCmdDrawIndexed(commandBuffer, elements, 1, indexOffset, vertexOffset, 0);
                }

            }
        }

        @Override
        public int getIndicesLength() {
            return indexCount;
        }

        @Override
        public void prepare(VulkanApplication app, Material.VertexLayout vertexLayout) {
            this.vertexLayout = vertexLayout;
        }

        public void update() {
            int totalVtxCount = drawData.getTotalVtxCount();
            if (totalVtxCount == 0)
                return;

            int totalIdxCount = drawData.getTotalIdxCount();
            if (totalIdxCount == 0)
                return;

            if (vertexBuffer == null || vertexCount < totalVtxCount) {
                vertexBuffer = createBuffer(totalVtxCount * vertexLayout.sizeof, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, vertexLayout.sizeof);
                app.getResourceCleaner().addTask(vertexBuffer, vertexBuffer.createCleanupTask(app.getDevice()));
                vertexCount = totalVtxCount;
            }

            if (indexBuffer == null || indexCount < totalIdxCount) {
                indexBuffer = createBuffer(totalIdxCount * ImDrawData.sizeOfImDrawIdx(), VK_BUFFER_USAGE_INDEX_BUFFER_BIT, ImDrawData.sizeOfImDrawIdx());
                app.getResourceCleaner().addTask(indexBuffer, indexBuffer.createCleanupTask(app.getDevice()));
                indexCount = totalIdxCount;
            }

            int cmdListsCount = drawData.getCmdListsCount();
            if (cmdListsCount == 1) {
                try {
                    nGetCmdListVtxBufferData.invoke(drawData, 0, vertexBuffer.getMappedBuffer(), vertexCount * vertexLayout.sizeof);
                    nGetCmdListIdxBufferData.invoke(drawData, 0, indexBuffer.getMappedBuffer(), indexCount * ImDrawData.sizeOfImDrawIdx());
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            } else {
                int vertexBufferOffset = 0;
                int indexBufferOffset = 0;
                for (int i = 0; i < cmdListsCount; i++) {
                    ByteBuffer vtxBufferData = drawData.getCmdListVtxBufferData(i);
                    vertexBuffer.getMappedBuffer().position(vertexBufferOffset);
                    vertexBuffer.getMappedBuffer().put(vtxBufferData);
                    vertexBufferOffset += drawData.getCmdListVtxBufferSize(i) * vertexLayout.sizeof;

                    ByteBuffer idxBufferData = drawData.getCmdListIdxBufferData(i);
                    indexBuffer.getMappedBuffer().position(indexBufferOffset);
                    indexBuffer.getMappedBuffer().put(idxBufferData);
                    indexBufferOffset += drawData.getCmdListIdxBufferSize(i) * ImDrawData.sizeOfImDrawIdx();
                }
            }
        }

        private BufferHolder createBuffer(int size, int usage, int sizeof) {
            BufferHolder bufferHolder;
            try (MemoryStack stack = stackPush()) {
                LongBuffer pBuffer = stack.mallocLong(1);
                LongBuffer pBufferMemory = stack.mallocLong(1);
                VulkanBuffers.createBuffer(
                        app.getPhysicalDevice(),
                        app.getDevice(),
                        size,
                        usage,
                        VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                        pBuffer,
                        pBufferMemory
                );

                bufferHolder = new BufferHolder(pBuffer.get(0), pBufferMemory.get(0), size, sizeof);

                PointerBuffer data = stack.mallocPointer(1);
                vkMapMemory(app.getDevice(), bufferHolder.bufferMemory, 0, size, 0, data);
                bufferHolder.setMappedBuffer(data.getByteBuffer(0, size));
            }
            return bufferHolder;
        }
    }

    static File findFont(String targetFontName) {
        String[] fontDirs = {
                "C:\\Windows\\Fonts\\",                       // Windows
                "/usr/share/fonts/",                          // Linux
                "/usr/local/share/fonts/",                    // Linux
                System.getProperty("user.home") + "/.fonts/", // Linux
                "/System/Library/Fonts/",                     // macOS
                "/Library/Fonts/",                            // macOS
                System.getProperty("user.home") + "/Library/Fonts/" // macOS
        };

        for (String dir : fontDirs) {
            File dirFile = new File(dir);
            if (!dirFile.isDirectory())
                continue;

            File[] files = dirFile.listFiles();
            if (files == null)
                continue;

            for (File file : files) {
                if (file.getName().toLowerCase().contains(targetFontName.toLowerCase()) && file.getName().endsWith(".ttf")) {
                    return file;
                }
            }
        }
        return null;
    }

    @Override
    protected void initApp() {
        Properties properties = System.getProperties();
        boolean shouldScale = properties.getProperty("os.name", "").contains("Mac");

        // need to run it as async task to set glfw callback after GlfwInputsManager, that does it by same addTask()
        addTask(() -> {

            ImGui.createContext();
            imGuiImplGlfw = new ImGuiImplGlfw();
            imGuiImplGlfw.init(window, true);

            ImGuiIO io = ImGui.getIO();
            io.setIniFilename(null);

            io.setConfigViewportsNoTaskBarIcon(true);
            ImFont imFont = io.getFonts().addFontFromFileTTF(
                    findFont("Arial").getAbsolutePath(),
                    20 * (shouldScale ? inputsManager.getWindowScaleX() : 1)
            );
            io.setFontDefault(imFont);
            io.getFonts().build();

            if (shouldScale) {
                ImGui.getIO().setDisplaySize(width * inputsManager.getWindowScaleX(), height * inputsManager.getWindowScaleY());
            }


            guiViewport.enableDynamicState(VK_DYNAMIC_STATE_VIEWPORT);
            guiViewport.enableDynamicState(VK_DYNAMIC_STATE_SCISSOR);

            {
                ImInt fontTextureWidth = new ImInt();
                ImInt fontTextureHeight = new ImInt();
                ByteBuffer texDataAsRGBA32 = io.getFonts().getTexDataAsRGBA32(fontTextureWidth, fontTextureHeight);

                TextureImage fontTexture = TextureLoader.createTextureImage(
                        this,
                        texDataAsRGBA32,
                        fontTextureWidth.get(),
                        fontTextureHeight.get(),
                        texDataAsRGBA32.limit(),
                        VK_FORMAT_R8G8B8A8_UNORM,
                        1
                );
                imguiMaterial = new Material() {
                    {
                        withUBO = false;
                        vertexLayout = new VertexLayout(
                                VertexLayout.BindingDescription.F2, // Position
                                VertexLayout.BindingDescription.TEXTURE_COORDINATES, // UV
                                VertexLayout.BindingDescription.B4UNORM // Color
                        );

                        setVertexShader(folder + "/imgui.vert");
                        setFragmentShader(folder + "/imgui.frag");

                        addTextureImage(fontTexture);
                        setTextureSampler(createTextureSampler(1));

                        addPushConstant(new PushConstantInfo(VK_SHADER_STAGE_VERTEX_BIT, Float.BYTES * 4) {
                            @Override
                            public void accept(ByteBuffer byteBuffer) {
                            }
                        });

                    }
                };

                updateImguiFrame();

                imguiMesh = new ImguiMesh(this);
                Geometry imguiGeometry = new Geometry("imgui", imguiMesh, imguiMaterial) {
                    @Override
                    public boolean isPrepared() {
                        return super.isPrepared() && mesh.getVertexBuffer() != null;
                    }
                };
                addGeometry(imguiGeometry, getGuiViewport());
//                updateImguiMesh();
            }

            getMainViewport().getCamera().setLocation(new Vector3f(2, 2, 2));
            getMainViewport().getCamera().lookAt(Vectors.ZERO, Vectors.UNIT_Z);

            {
                Material material = new UnshadedColor(new Vector3f(1, 1, 0));
                Geometry geometry = new Box(material);
                addGeometry(geometry, getMainViewport());
            }
        });
    }


    private void updateImguiFrame() {
        imGuiImplGlfw.newFrame();

        ImGui.newFrame();
        processImgui();
        ImGui.render();
    }

    private int count;
    private ImString str = new ImString(5);
    private float[] flt = new float[1];
    private IntMemo<String> countToString = new IntMemo<>(String::valueOf);
    private Memo<String, String> resultMemo = new Memo<>(arg -> "Result: " + arg);
    ImGuiInputTextCallback imGuiInputTextCallback = new ImGuiInputTextCallback() {
        @Override
        public void accept(ImGuiInputTextCallbackData imGuiInputTextCallbackData) {
            System.out.println((char) imGuiInputTextCallbackData.getEventChar());
        }
    };

    public void processImgui() {
//        ImGui.text("Hello, World! " + FontAwesomeIcons.Smile);
        ImGui.setWindowSize(800, 600);
//        ImGui.setWindowSize(800, 600, ImGuiCond.Once);
        ImGui.text("Hello, World! ");
        if (ImGui.button(" Click")) {
            count++;
        }
        ImGui.sameLine();
        ImGui.text(countToString.get(count));
        ImGui.inputText("string", str, ImGuiInputTextFlags.CallbackResize | ImGuiInputTextFlags.CallbackCharFilter, imGuiInputTextCallback);
        ImGui.text(resultMemo.get(str.get()));

        ImGui.sliderFloat("float", flt, 0, 1);
        ImGui.separator();
        ImGui.text("Extra");
//        Extra.show(this);
    }

    static class IntMemo<T> {
        protected T value;
        protected int arg = Integer.MIN_VALUE;
        protected final Mapper<T> mapper;

        IntMemo(Mapper<T> mapper) {
            this.mapper = mapper;
        }

        interface Mapper<T> {
            T map(int arg);
        }

        public T get(int arg) {
            if (arg != this.arg) {
                this.arg = arg;
                value = mapper.map(arg);
            }

            return value;
        }
    }

    static class Memo<T, A> {
        protected T value;
        protected A arg;
        protected final Mapper<T, A> mapper;

        Memo(Mapper<T, A> mapper) {
            this.mapper = mapper;
        }

        interface Mapper<T, A> {
            T map(A arg);
        }

        public T get(A arg) {
            if (!arg.equals(this.arg)) {
                this.arg = arg;
                value = mapper.map(arg);
                System.out.println("memo updated: " + arg + " != " + this.arg);
            }

            return value;
        }
    }

    @Override
    protected void simpleUpdate(double tpf) {
        List<Geometry> geometries = getMainViewport().getGeometries();
        for (int i = 0; i < geometries.size(); i++) {
            Geometry geometry = geometries.get(i);
            geometry.getLocalTransform().getRotation().setAngleAxis(this.getTime() * Math.toRadians(90) / 10f, 0.0f, 0.0f, 1.0f);
            if (geometry.getMaterial() instanceof UnshadedColor) {
                UnshadedColor material = (UnshadedColor) geometry.getMaterial();
                material.getColor().set((float) (Math.sin(this.getTime() / 3)), (float) (Math.sin(this.getTime() / 2)), (float) Math.sin(this.getTime()));
                material.updateUniforms();
            }
        }

        if (this.imGuiImplGlfw != null) {
            updateImguiFrame();
            imguiMesh.update();
        }
    }
}
