package com.example.flying_camera;

import com.example.AbstractSampleApp;
import com.wizzardo.vulkan.*;
import com.wizzardo.vulkan.input.GlfwKey;
import com.wizzardo.vulkan.material.predefined.UnshadedColor;
import com.wizzardo.vulkan.scene.Geometry;
import com.wizzardo.vulkan.scene.shape.Box;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.lwjgl.glfw.GLFW.*;

public class SampleApp extends AbstractSampleApp {

    static Robot robot;
    final static Double ZERO = 0.0;

    public static void main(String[] args) {
        System.setProperty("joml.format", "false");
        try {
            robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }

        new SampleApp().start();
    }

    AtomicReference<Double> mouseDiffX = new AtomicReference<>(0.0);
    AtomicReference<Double> mouseDiffY = new AtomicReference<>(0.0);

    @Override
    protected void initApp() {
        getMainViewport().getCamera().setLocation(new Vector3f(2, 2, 2));
        getMainViewport().getCamera().lookAt(Vectors.ZERO, Vectors.UNIT_Z);

        {
            Material material = new UnshadedColor(new Vector3f(1, 0, 0));
            Geometry geometry = new Box(material);
            addGeometry(geometry, getMainViewport());
        }


        int[] windowPositionX = new int[1];
        int[] windowPositionY = new int[1];
        glfwSetWindowPosCallback(getWindow(), (window1, xpos, ypos) -> {
            System.out.println("window.position: " + xpos + " : " + ypos);
            windowPositionX[0] = xpos;
            windowPositionY[0] = ypos;
        });
        int[] windowWidth = new int[1];
        int[] windowHeight = new int[1];
        glfwGetWindowSize(getWindow(), windowWidth, windowHeight);
        glfwGetWindowPos(getWindow(), windowPositionX, windowPositionY);

        AtomicBoolean mouseCaptured = new AtomicBoolean(true);

        {
            int centerX = windowPositionX[0] + windowWidth[0] / 2;
            int centerY = windowPositionY[0] + windowHeight[0] / 2;
            if (robot != null && mouseCaptured.get()) {
                robot.mouseMove(centerX, centerY);
            }
        }


        inputsManager.addMouseMoveListener((x, y) -> {
//            System.out.println("mouse: " + x + " " + y);
//            System.out.println("set mouse position to " + centerX + " " + centerY);
            int centerX = windowPositionX[0] + windowWidth[0] / 2;
            int centerY = windowPositionY[0] + windowHeight[0] / 2;

            while (true) {
                Double prev = mouseDiffX.get();
                if (mouseDiffX.compareAndSet(prev, prev + (x - windowWidth[0] / 2)))
                    break;
            }
            while (true) {
                Double prev = mouseDiffY.get();
                if (mouseDiffY.compareAndSet(prev, prev + (y - windowHeight[0] / 2)))
                    break;
            }

            if (robot != null && mouseCaptured.get()) {
                robot.mouseMove(centerX, centerY);
            }
//            glfwSetCursorPos(getWindow(), windowPositionX[0] + windowWidth[0] / 2, windowPositionY[0] + windowHeight[0] / 2);
        });

//        inputsManager.setCursorVisible(false);
        inputsManager.addKeyListener((key, pressed, repeat) -> {
//            if (GlfwKey.GLFW_KEY_ESCAPE == key)
//                shutdown();
//            else
                if (GlfwKey.GLFW_KEY_SPACE == key && pressed) {
                mouseCaptured.set(!mouseCaptured.get());
                    return false;
            }

//            System.out.println(key + " " + (pressed ? "pressed" : "released"));
            return true;
        });
    }


    @Override
    protected void simpleUpdate(double tpf) {

//        for (Geometry geometry : getMainViewport().getGeometries()) {
//            geometry.getLocalTransform().getRotation().setAngleAxis(this.getTime() * Math.toRadians(90) / 10f, 0.0f, 0.0f, 1.0f);
//        }

        double dx = mouseDiffX.getAndSet(ZERO);
        double dy = mouseDiffY.getAndSet(ZERO);
        if (dx != 0 || dy != 0) {
            Camera camera = getMainViewport().getCamera();
            Quaternionf rotation = camera.getRotation();
            rotation.mul((float) (dy / 1000), (float) (-dx / 1000), 0f, 1f);
            rotation.normalize();

            TempVars vars = TempVars.get();
            try {
                camera.lookAtDirection(camera.getDirection(vars.vect1), Vectors.UNIT_Z);
            } finally {
                vars.release();
            }
        }

        if (inputsManager.getKeyState().isKeyPressed(GlfwKey.GLFW_KEY_A)) {
            Camera camera = getMainViewport().getCamera();
            TempVars vars = TempVars.get();
            try {
                camera.getLocation().add(camera.getLeft(vars.vect1).mul((float) (tpf)));
            } finally {
                vars.release();
            }
        } else if (inputsManager.getKeyState().isKeyPressed(GlfwKey.GLFW_KEY_D)) {
            Camera camera = getMainViewport().getCamera();
            TempVars vars = TempVars.get();
            try {
                camera.getLocation().add(camera.getLeft(vars.vect1).mul((float) (-tpf)));
            } finally {
                vars.release();
            }
        }

        if (inputsManager.getKeyState().isKeyPressed(GlfwKey.GLFW_KEY_W)) {
            Camera camera = getMainViewport().getCamera();
            TempVars vars = TempVars.get();
            try {
                camera.getLocation().add(camera.getDirection(vars.vect1).mul((float) (tpf)));
            } finally {
                vars.release();
            }
        } else if (inputsManager.getKeyState().isKeyPressed(GlfwKey.GLFW_KEY_S)) {
            Camera camera = getMainViewport().getCamera();
            TempVars vars = TempVars.get();
            try {
                camera.getLocation().add(camera.getDirection(vars.vect1).mul((float) (-tpf)));
            } finally {
                vars.release();
            }
        }
    }
}
