package com.wizzardo.vulkan.misc;

import com.wizzardo.tools.io.FileTools;
import com.wizzardo.vulkan.VulkanApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class NativeLibraryHelper {

    public static void load(String name) throws IOException {
//        String arch = System.getProperty("os.arch");
//        name = name + (arch.contains("64") ? (arch.contains("aarch64") ? "_aarch64":"_x64") : "_x32") + ".so";

        Properties properties = System.getProperties();
        if (properties.getProperty("os.name", "").contains("Mac")) {
            name += ".dylib";
        } else if (properties.getProperty("os.name", "").toLowerCase().contains("linux")) {
            int versionStart = name.indexOf('.');
            name = name.substring(0, versionStart) + ".so" + name.substring(versionStart);
        } else {
            throw new IllegalArgumentException("OS " + properties.getProperty("os.name", "") + " not supportd yet");
        }


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
