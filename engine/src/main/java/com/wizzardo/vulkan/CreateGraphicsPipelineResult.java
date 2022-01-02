package com.wizzardo.vulkan;

class CreateGraphicsPipelineResult {
    public final long pipelineLayout;
    public final long graphicsPipeline;

    CreateGraphicsPipelineResult(long pipelineLayout, long graphicsPipeline) {
        this.pipelineLayout = pipelineLayout;
        this.graphicsPipeline = graphicsPipeline;
    }
}
