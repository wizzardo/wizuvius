package com.wizzardo.vulkan.misc;

import com.wizzardo.tools.io.IOTools;

import java.io.*;
import java.util.concurrent.*;

public class RuntimeTools {

    private static ExecutorService threadPool = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((t, e) -> e.printStackTrace());
        return thread;
    });

    public static String execute(String[] command, int timeoutSeconds, File workDir) throws IOException, InterruptedException, ExecutionException {
        System.out.println("executing command: " + String.join(" ", command));
        Process process = Runtime.getRuntime().exec(command, null, workDir);

        CountDownLatch latch = new CountDownLatch(1);

        Future<String> messageFuture = threadPool.submit(() -> {
            StringBuilder sb = new StringBuilder();
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String line;
                while (true) {
                    line = br.readLine();
                    if (line != null) {
                        System.out.println(line);
                        sb.append(line).append("\n");
                    } else
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
            return sb.toString();
        });

        boolean exited = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!exited) {
            process.destroy();
            throw new IllegalStateException("Timeout");
        }
        String message = messageFuture.get();
//        System.out.println(message);
        if (!latch.await(1, TimeUnit.SECONDS)) {
            throw new IllegalStateException("latch wasn't released");
        }
        return message;
    }

    public static class ExecResult {
        public final byte[] result;
        public final byte[] error;

        public ExecResult(byte[] result, byte[] error) {
            this.result = result;
            this.error = error;
        }
    }
    public static ExecResult executeToStdout(String[] command, int timeoutSeconds, File workDir) throws IOException, InterruptedException, ExecutionException {
        System.out.println("executing command: " + String.join(" ", command));
        Process process = Runtime.getRuntime().exec(command, null, workDir);
        CountDownLatch latch = new CountDownLatch(1);

        Future<byte[]> standardOutput = threadPool.submit(() -> {
            try {
                return IOTools.bytes(process.getInputStream());
            } catch (Exception e) {
                return null;
            } finally {
                latch.countDown();
            }
        });
        Future<byte[]> errorOutput = threadPool.submit(() -> {
            try {
                return IOTools.bytes(process.getErrorStream());
            } catch (Exception e) {
                return null;
            } finally {
                latch.countDown();
            }
        });

        boolean exited = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!exited) {
            process.destroy();
            throw new IllegalStateException("Timeout");
        }
        byte[] result = standardOutput.get();
        byte[] error = errorOutput.get();
//        System.out.println(message);
        if (!latch.await(1, TimeUnit.SECONDS)) {
            throw new IllegalStateException("latch wasn't released");
        }
        return new ExecResult(result, error);
    }
}
