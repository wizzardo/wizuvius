package com.wizzardo.vulkan;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceCleaner extends Thread {
    protected static boolean printDebug = true;
    protected Map<Reference<?>, Runnable> cleanupTasks = new ConcurrentHashMap<>();
    protected ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
    protected volatile boolean running = true;

    public ResourceCleaner() {
        super(ResourceCleaner.class.getSimpleName());
        this.setDaemon(true);
    }

    @Override
    public void run() {
        while (running) {
            Reference<?> reference = null;
            try {
                reference = referenceQueue.remove();
            } catch (InterruptedException ignored) {
            }
            if (reference != null) {
                Runnable task = cleanupTasks.remove(reference);
                if (task != null) {
                    try {
                        task.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        cleanupTasks.forEach((reference, task) -> {
            try {
                task.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        cleanupTasks.clear();
    }

    public void addTask(Object object, Runnable task) {
        if (!running)
            throw new IllegalStateException(ResourceCleaner.class.getSimpleName() + " has stopped");

//        WeakReference<Object> ref = new WeakReference<>(object, referenceQueue);
        PhantomReference<Object> ref = new PhantomReference<>(object, referenceQueue);
        cleanupTasks.put(ref, task);
    }

    public void shutdown() {
        running = false;
        this.interrupt();
        try {
            this.join();
        } catch (InterruptedException ignored) {
        }
    }

    public static void printDebugInCleanupTask(Class<?> clazz) {
        if (printDebug)
            System.out.println("CleanupTask " + clazz.getSimpleName());
    }

    public static void printDebugInCleanupTask(Class<?> clazz, String additionalInfo) {
        if (printDebug)
            System.out.println("CleanupTask " + clazz.getSimpleName() + " " + additionalInfo);
    }
}
