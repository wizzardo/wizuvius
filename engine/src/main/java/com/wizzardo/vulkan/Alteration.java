package com.wizzardo.vulkan;

public interface Alteration {
    /**
    *  @return true if app should keep this alteration in loop, false otherwise
    */
    boolean onUpdate(double tpf);
}
