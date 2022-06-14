package com.wizzardo.vulkan;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryWatcher;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ResourcesListener extends Thread {
    protected final List<String> paths;
    protected List<Consumer<File>> listeners = new CopyOnWriteArrayList<>();

    public ResourcesListener(List<String> paths) {
        this.paths = paths;
        setDaemon(true);
        start();
    }

    @Override
    public void run() {
        try {
            Map<File, Long> modifications = new HashMap<>();
            DirectoryWatcher watcher = DirectoryWatcher.builder()
                    .paths(paths.stream().map(Paths::get).collect(Collectors.toList()))
                    .listener(event -> {
//                        System.out.println(event);
                        switch (event.eventType()) {
                            case CREATE:
                                processEvent(event, modifications);
                                break;
                            case MODIFY:
                                processEvent(event, modifications);
                                break;
                            case DELETE:
                                break;
                            case OVERFLOW:
                                break;
                            default:
                                throw new IllegalStateException("Unexpected value: " + event);
                        }
                    })
                    .fileHashing(false)
                    .build();

            watcher.watch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean addListener(Consumer<File> listener) {
        if (listener == null)
            return false;

        return listeners.add(listener);
    }

    public boolean removeListener(Consumer<File> listener) {
        if (listener == null)
            return false;

        return listeners.remove(listener);
    }

    protected void processEvent(DirectoryChangeEvent event, Map<File, Long> modifications) {
        File f = event.path().toFile();
        if (f.isFile() && modifications.getOrDefault(f, 0L) != f.lastModified()) {
            modifications.put(f, f.lastModified());
//            System.out.println("processEvent " + f);

            for (int i = 0; i < listeners.size(); i++) {
                Consumer<File> fileConsumer = listeners.get(i);
                try {
                    fileConsumer.accept(f);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
