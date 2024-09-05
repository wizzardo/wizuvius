package com.example.headless_compute;

import com.example.AbstractSampleApp;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.vulkan.*;
import com.wizzardo.vulkan.material.SpecializationConstantInfo;
import com.wizzardo.vulkan.material.Uniform;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class SampleApp extends AbstractSampleApp {

    long descriptorSetLayout;
    long descriptorSet;
    Uniform.IntArray uniform;
    Pipeline pipeline;

    public SampleApp() {
        super();
        headless = true;
    }

    public static void main(String[] args) {
        SampleApp sampleApp = new SampleApp();
        sampleApp.start();
    }

    @Override
    protected void initApp() {
        try (MemoryStack stack = stackPush()) {
            ByteBuffer shaderCode = Unchecked.call(() -> loadAssetAsByteBuffer(folder + "/shaders/fib.comp"));
            ByteBuffer entryPoint = stack.UTF8("main");

            long compShaderModule = ShaderLoader.createShaderModule(device, shaderCode);

            VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(1, stack);

            SpecializationConstantInfo.Int countConstant = new SpecializationConstantInfo.Int(0, 0, 16);

            VkPipelineShaderStageCreateInfo compShaderStageInfo = shaderStages.get(0);
            compShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            compShaderStageInfo.stage(VK_SHADER_STAGE_COMPUTE_BIT);
            compShaderStageInfo.module(compShaderModule);
            compShaderStageInfo.pName(entryPoint);
            compShaderStageInfo.pSpecializationInfo(prepareSpecializationInfo(0, Collections.singletonList(countConstant), stack));

            int[] data = new int[32];
            {
                for (int i = 0; i < data.length; i++) {
                    data[i] = i;
                }
            }

            UniformBuffer storageBufferObject = UniformBuffers.createStorageBufferObject(physicalDevice, device, data.length * 4);
            storageBufferObject.map(device, resourceCleaner);
            uniform = new Uniform.IntArray(this, VK_SHADER_STAGE_COMPUTE_BIT, 0, data, storageBufferObject);
            uniform.update();

            VulkanDescriptorSets.DescriptorSetLayoutBuilder layoutBuilder = new VulkanDescriptorSets.DescriptorSetLayoutBuilder();
            layoutBuilder.append(new VulkanDescriptorSets.DescriptorSetLayoutBindingStorageBuffer(
                    uniform.binding, uniform.stage, uniform.uniformBuffer
            ));
            descriptorSetLayout = layoutBuilder.build(device);
            long dsl = descriptorSetLayout;
            addCleanupTask(this, () -> {
                ResourceCleaner.printDebugInCleanupTask(Material.class);
                vkDestroyDescriptorSetLayout(device, dsl, null);
            });

            VulkanDescriptorSets.DescriptorSetsBuilder descriptorSetsBuilder = new VulkanDescriptorSets.DescriptorSetsBuilder(layoutBuilder.bindings)
                    .withUniformBuffers(Collections.singletonList(uniform.uniformBuffer));
            List<Long> descriptors = descriptorSetsBuilder.build(device, 1, descriptorSetLayout, descriptorPool);
            descriptorSet = descriptors.get(0);


            VkPipelineLayoutCreateInfo pipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            pipelineLayoutCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            pipelineLayoutCreateInfo.pSetLayouts(stack.longs(descriptorSetLayout));

            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);
            if (vkCreatePipelineLayout(device, pipelineLayoutCreateInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }

            long pipelineLayout = pPipelineLayout.get(0);

            VkComputePipelineCreateInfo.Buffer pipelineInfo = VkComputePipelineCreateInfo.calloc(1, stack);
            pipelineInfo.sType$Default();
            pipelineInfo.stage(compShaderStageInfo);
            pipelineInfo.layout(pipelineLayout);
            pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
            pipelineInfo.basePipelineIndex(-1);

            LongBuffer pipelineBuffer = stack.mallocLong(1);

            if (vkCreateComputePipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pipelineBuffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }

            long computePipeline = pipelineBuffer.get(0);
            Pipeline pipeline = new Pipeline(computePipeline, pipelineLayout);
            addCleanupTask(pipeline, pipeline.createCleanupTask(device));
            this.pipeline = pipeline;

            vkDestroyShaderModule(device, compShaderModule, null);
        }
    }

    @Override
    protected void mainLoop() {
        int[] ints = new int[uniform.uniformBuffer.size / 4];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = uniform.uniformBuffer.getBuffer().getInt(i * 4);
        }
        System.out.println("before: " + Arrays.toString(ints));

        try (MemoryStack stack = stackPush()) {
            DrawFrameTempData tempData = new DrawFrameTempData(stack);

            VkCommandBuffer commandBuffer = VulkanCommands.createEmptyCommandBuffers(device, commandPool, 1).get(0);


            VkCommandBufferBeginInfo beginInfo = tempData.commandBufferTempData.beginInfo;
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            if (vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
                throw new RuntimeException("Failed to begin recording command buffer");
            }

            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, pipeline.graphicsPipeline);

            vkCmdBindDescriptorSets(
                    commandBuffer,
                    VK_PIPELINE_BIND_POINT_COMPUTE,
                    pipeline.pipelineLayout,
                    0,
                    tempData.pLong.put(0, descriptorSet),
                    null
            );

            vkCmdDispatch(commandBuffer, 32, 1, 1);

            if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to record command buffer");
            }

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack);
            fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

            if (vkCreateFence(device, fenceInfo, null, tempData.pLong) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create fence object");
            }

            long fence = tempData.pLong.get();

            vkResetFences(device, fence);

            int vkResult;

            PointerBuffer pCommandBuffers = stack.mallocPointer(1);
            pCommandBuffers.put(0, commandBuffer);

            VkSubmitInfo submitInfo = tempData.submitInfo;
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pWaitDstStageMask(tempData.pInt.put(0, VK_PIPELINE_STAGE_TRANSFER_BIT));
            submitInfo.pCommandBuffers(pCommandBuffers);


            if ((vkResult = vkQueueSubmit(graphicsQueue, submitInfo, fence)) != VK_SUCCESS) {
                throw new RuntimeException("Failed to submit draw command buffer: " + vkResult);
            }

            vkWaitForFences(device, fence, true, UINT64_MAX);

            vkDestroyFence(device, fence, null);
        }

        for (int i = 0; i < ints.length; i++) {
            ints[i] = uniform.uniformBuffer.getBuffer().getInt(i * 4);
        }
        System.out.println("after: " + Arrays.toString(ints));
    }
}
