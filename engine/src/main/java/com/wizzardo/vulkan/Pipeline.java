package com.wizzardo.vulkan;

public class Pipeline {
    public final long graphicsPipeline;
    public final long pipelineLayout;

    public Pipeline(long graphicsPipeline, long pipelineLayout) {
        this.graphicsPipeline = graphicsPipeline;
        this.pipelineLayout = pipelineLayout;
    }
}
