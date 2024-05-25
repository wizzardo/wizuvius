package com.wizzardo.vulkan.misc;

import java.io.File;

public interface ResourceChangeListener {
    /**
     *  @return true if app should keep this listener, false otherwise
     */
    boolean onChange(File file);
}
