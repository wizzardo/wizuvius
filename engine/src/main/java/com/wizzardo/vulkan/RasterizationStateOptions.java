package com.wizzardo.vulkan;

public class RasterizationStateOptions {

    public boolean depthClamp = false;
    public boolean rasterizerDiscard = false;
    public PolygonMode polygonMode = PolygonMode.FILL;
    public float lineWidth = 1f;
    public FrontFace frontFace = FrontFace.COUNTER_CLOCKWISE;


    public enum PolygonMode {
        FILL, LINE, POINT,
    }

    public enum FrontFace {
        COUNTER_CLOCKWISE, CLOCKWISE
    }

}
