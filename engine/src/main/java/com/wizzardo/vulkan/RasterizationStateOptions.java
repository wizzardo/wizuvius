package com.wizzardo.vulkan;

public class RasterizationStateOptions {

    public boolean depthClamp = false;
    public boolean rasterizerDiscard = false;
    public PolygonMode polygonMode = PolygonMode.FILL;
    public CullMode cullMode = CullMode.INHERIT_FROM_VIEWPORT;
    public float lineWidth = 1f;
    public FrontFace frontFace = FrontFace.COUNTER_CLOCKWISE;


    public enum PolygonMode {
        FILL, LINE, POINT,
    }

    public enum CullMode {
        INHERIT_FROM_VIEWPORT,
        NONE,
        FRONT_BIT,
        BACK_BIT,
        FRONT_AND_BACK;
    }

    public enum FrontFace {
        COUNTER_CLOCKWISE, CLOCKWISE
    }

}
