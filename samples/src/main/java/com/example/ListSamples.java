package com.example;

import java.io.File;
import java.util.Arrays;

public class ListSamples {
    public static void main(String[] args) {
        System.out.println("Please specify a sample");
        System.out.println("Available samples:");
        File[] files = new File("src/main/java/com/example").listFiles();
        Arrays.sort(files);
        for (int i = 0; i < files.length; i++) {
            File dir = files[i];
            if (!dir.isDirectory())
                continue;

            if (new File(dir, "SampleApp.java").exists())
                System.out.println("\t" + dir.getName());
        }

    }
}
