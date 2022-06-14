package com.example;

import com.wizzardo.vulkan.DesktopVulkanApplication;

public abstract class AbstractSampleApp extends DesktopVulkanApplication {

    protected final String folder = this.getClass().getPackageName().substring(this.getClass().getPackageName().lastIndexOf(".") + 1);

    public AbstractSampleApp() {
        width = 1280;
        height = 720;
    }
}
