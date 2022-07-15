package com.example;

import com.wizzardo.vulkan.DesktopVulkanApplication;
import com.wizzardo.vulkan.input.GlfwKey;

import java.io.File;

public abstract class AbstractSampleApp extends DesktopVulkanApplication {

    protected final String folder = this.getClass().getPackageName().substring(this.getClass().getPackageName().lastIndexOf(".") + 1);

    public AbstractSampleApp() {
        width = 1280;
        height = 720;
        development = true;
        setFpsLimit(60);
        super.devResourcesFolder = new File("samples/src/main/resources");
    }

    @Override
    protected void initVulkan() {
        super.initVulkan();

        inputsManager.addKeyListener((key, pressed, repeat) -> {
            if (GlfwKey.GLFW_KEY_ESCAPE == key)
                shutdown();
            else if (GlfwKey.GLFW_KEY_MINUS == key && pressed) {
                setFpsLimit(getFpsLimit() / 2);
                System.out.println("set fps limit: " + getFpsLimit());
            } else if (GlfwKey.GLFW_KEY_EQUAL == key && pressed) {
                setFpsLimit(getFpsLimit() * 2);
                System.out.println("set fps limit: " + getFpsLimit());
            }
        });
    }
}
