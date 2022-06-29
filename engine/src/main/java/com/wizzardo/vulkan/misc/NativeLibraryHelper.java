package com.wizzardo.vulkan.misc;

import com.wizzardo.tools.io.FileTools;
import com.wizzardo.vulkan.VulkanApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class NativeLibraryHelper {

    public static void load(String name) throws IOException {
//        String arch = System.getProperty("os.arch");
//        name = name + (arch.contains("64") ? (arch.contains("aarch64") ? "_aarch64":"_x64") : "_x32") + ".so";
        name += ".dylib";

        InputStream in = VulkanApplication.class.getResourceAsStream("/" + name);

        File fileOut;
        if (in == null) {
            File file = new File(name);
            if (file.exists())
                in = new FileInputStream(file);
            else
                in = new FileInputStream("build/" + name);
        }
        fileOut = File.createTempFile(name, "lib");
        FileTools.bytes(fileOut, in);
        System.load(fileOut.toString());
        fileOut.delete();
    }
}
