package com.example;

import com.wizzardo.vulkan.DesktopVulkanApplication;

import java.io.File;

public abstract class AbstractSampleApp extends DesktopVulkanApplication {

    protected final String folder = this.getClass().getPackageName().substring(this.getClass().getPackageName().lastIndexOf(".") + 1);

    public AbstractSampleApp() {
        width = 1280;
        height = 720;
        development = true;
        super.folder = new File("samples/src/main/resources");
    }
}
