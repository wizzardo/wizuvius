package com.wizzardo.vulkan.editor;

import com.wizzardo.vulkan.DesktopVulkanApplication;
import com.wizzardo.vulkan.ui.javafx.JavaFxToTextureBridge;

public class Main extends DesktopVulkanApplication {

    static {
        JavaFxToTextureBridge.init();
    }

    public Main() {
        width = 1280;
        height = 720;
    }

    public static void main(String[] args) {

    }
}
