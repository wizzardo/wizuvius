package com.wizzardo.vulkan.ui.imgui;

import com.wizzardo.vulkan.*;
import imgui.ImDrawData;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImVec4;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkViewport;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

class ImGuiMesh extends Mesh {
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

    public ImGuiMesh(VulkanApplication app) {
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
    public void draw(VkCommandBuffer commandBuffer, Material material, VulkanApplication.CommandBufferTempData tempData) {

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
