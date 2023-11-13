package com.wizzardo.vulkan.alterations;

import com.wizzardo.vulkan.*;
import com.wizzardo.vulkan.input.GlfwKey;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class FreeCameraAlteration implements Alteration {

    protected final DesktopVulkanApplication app;
    protected final Camera camera;
    protected float rotationSpeed = 0.001f;
    protected float movementSpeed = 1f;

    public FreeCameraAlteration(DesktopVulkanApplication app, Camera camera) {
        this.app = app;
        this.camera = camera;
        init();
    }


    protected void init() {
        float[] mousePosition = new float[2];
        app.getInputsManager().addMouseMoveListener((x, y) -> {
            if (!app.getInputsManager().getKeyState().isMouseButtonPressed(0))
                return;

            float diffX = (float) (mousePosition[0] - x);
            float diffY = (float) (mousePosition[1] - y);

            mousePosition[0] = (float) x;
            mousePosition[1] = (float) y;

            if (diffX != 0 || diffY != 0) {
                rotateCamera(diffX, diffY);
            }
        });
        app.getInputsManager().addMouseButtonListener((x, y, button, pressed) -> {
            if (button == 0 && pressed) {
                mousePosition[0] = (float) x;
                mousePosition[1] = (float) y;
                System.out.println("mouse press " + x + " " + y);
                System.out.println("set mouse position to " + mousePosition[0] + " " + mousePosition[1]);
            }
            return true;
        });


//        inputsManager.setCursorVisible(false);
        app.getInputsManager().addKeyListener((key, pressed, repeat) -> {
            if (GlfwKey.GLFW_KEY_C == key && pressed) {
                Quaternionf r = camera.getRotation();
                Vector3f l = camera.getLocation();
                System.out.println("camera.getLocation().set(" + l.x + "f, " + l.y + "f, " + l.z + "f);");
                System.out.println("camera.getRotation().set(" + r.x + "f, " + r.y + "f, " + r.z + "f, " + r.w + "f);");
            }

//            System.out.println(key + " " + (pressed ? "pressed" : "released"));
            return true;
        });
    }

    private void rotateCamera(double dx, double dy) {
        Quaternionf rotation = camera.getRotation();
        rotation.mul((float) (dy * rotationSpeed), (float) (-dx * rotationSpeed), 0f, 1f);
        rotation.normalize();

        try (TempVars vars = TempVars.get()) {
            camera.lookAtDirection(camera.getDirection(vars.vect1), camera.getUpVector());
        }
    }


    @Override
    public boolean onUpdate(double tpf) {
        if (app.getInputsManager().getKeyState().isKeyPressed(GlfwKey.GLFW_KEY_A)) {
            try (TempVars vars = TempVars.get()) {
                camera.getLocation().add(camera.getLeft(vars.vect1).mul((float) (tpf * movementSpeed)));
            }
        } else if (app.getInputsManager().getKeyState().isKeyPressed(GlfwKey.GLFW_KEY_D)) {
            try (TempVars vars = TempVars.get()) {
                camera.getLocation().add(camera.getLeft(vars.vect1).mul((float) (-tpf * movementSpeed)));
            }
        }

        if (app.getInputsManager().getKeyState().isKeyPressed(GlfwKey.GLFW_KEY_W)) {
            try (TempVars vars = TempVars.get()) {
                camera.getLocation().add(camera.getDirection(vars.vect1).mul((float) (tpf * movementSpeed)));
            }
        } else if (app.getInputsManager().getKeyState().isKeyPressed(GlfwKey.GLFW_KEY_S)) {
            try (TempVars vars = TempVars.get()) {
                camera.getLocation().add(camera.getDirection(vars.vect1).mul((float) (-tpf * movementSpeed)));
            }
        }
        return true;
    }

    public float getRotationSpeed() {
        return rotationSpeed;
    }

    public void setRotationSpeed(float rotationSpeed) {
        this.rotationSpeed = rotationSpeed;
    }

    public float getMovementSpeed() {
        return movementSpeed;
    }

    public void setMovementSpeed(float movementSpeed) {
        this.movementSpeed = movementSpeed;
    }
}
