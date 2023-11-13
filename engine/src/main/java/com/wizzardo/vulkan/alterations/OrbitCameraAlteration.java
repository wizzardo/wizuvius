package com.wizzardo.vulkan.alterations;

import com.wizzardo.vulkan.*;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class OrbitCameraAlteration implements Alteration {

    protected final Quaternionf rotation = new Quaternionf();
    protected float rotationSpeed = (float) (Math.PI / 2);
    protected final float[] angles = new float[2];
    protected float zoomDistance = -1;
    protected float zoomSpeed = 0.1f;
    protected float minZoom = 0.1f;
    protected float maxZoom = 50;
    protected Vector3fc focusPoint = new Vector3f(Vectors.ZERO);
    protected final VulkanApplication app;
    protected final Camera camera;
    protected boolean invertX = false;
    protected boolean invertY = false;
    protected volatile boolean enabled = true;

    public static final int GLFW_KEY_A = 65;
    public static final int GLFW_KEY_D = 68;
    public static final int GLFW_KEY_S = 83;
    public static final int GLFW_KEY_W = 87;

    public OrbitCameraAlteration(VulkanApplication app, Camera camera) {
        this.app = app;
        this.camera = camera;
    }

    private void init() {
        Vector3f location = camera.getLocation();
        float xyLength = (float) Math.sqrt(location.x * location.x + location.y * location.y);
        float xyAngle = (float) Math.asin(location.y / xyLength);
        float xzLength = (float) Math.sqrt(location.x * location.x + location.z * location.z);
        float xzAngle = (float) Math.asin(location.z / xzLength);
        angles[0] = xyAngle * (invertY ? -1 : 1);
        angles[1] = xzAngle;
        zoomDistance = location.length();

        lookAt(camera, focusPoint);
        float[] mousePosition = new float[2];

        app.getInputsManager().addMouseMoveListener((x, y) -> {
            if(!enabled)
                return;

            if (!app.getInputsManager().getKeyState().isMouseButtonPressed(0))
                return;

            float diffX = (float) (mousePosition[0] - x);
            float diffY = (float) (mousePosition[1] - y);

            mousePosition[0] = (float) x;
            mousePosition[1] = (float) y;

            if (diffX != 0)
                rotateCamera(camera, diffX / 100f, true, rotationSpeed);
            if (diffY != 0)
                rotateCamera(camera, -diffY / 100f, false, rotationSpeed);
        });
        app.getInputsManager().addMouseButtonListener((x, y, button, pressed) -> {
            if(!enabled)
                return true;

            if (button == 0 && pressed) {
                mousePosition[0] = (float) x;
                mousePosition[1] = (float) y;
                return false;
            }
            return true;
        });
        app.getInputsManager().addScrollListener((x, y, scrollX, scrollY) -> {
            if(!enabled)
                return true;

            zoomDistance = (float) Math.min(maxZoom, Math.max(minZoom, zoomDistance + zoomSpeed * scrollY));
            lookAt(camera, focusPoint);
            return false;
        });
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public float getRotationSpeed() {
        return rotationSpeed;
    }

    public void setRotationSpeed(float rotationSpeed) {
        this.rotationSpeed = rotationSpeed;
    }

    public float getZoomSpeed() {
        return zoomSpeed;
    }

    public void setZoomSpeed(float zoomSpeed) {
        this.zoomSpeed = zoomSpeed;
    }

    public float getMinZoom() {
        return minZoom;
    }

    public void setMinZoom(float minZoom) {
        this.minZoom = minZoom;
    }

    public float getMaxZoom() {
        return maxZoom;
    }

    public void setMaxZoom(float maxZoom) {
        this.maxZoom = maxZoom;
    }

    public Vector3fc getFocusPoint() {
        return focusPoint;
    }

    public void setFocusPoint(Vector3fc focusPoint) {
        this.focusPoint = focusPoint;
    }

    public boolean isInvertX() {
        return invertX;
    }

    public void setInvertX(boolean invertX) {
        this.invertX = invertX;
    }

    public boolean isInvertY() {
        return invertY;
    }

    public void setInvertY(boolean invertY) {
        this.invertY = invertY;
    }

    @Override
    public boolean onUpdate(double tpf) {
        if (zoomDistance == -1)
            init();
        if (app.getInputsManager().getKeyState().isKeyPressed(GLFW_KEY_A)) {
            rotateCamera(camera, (float) (tpf), true, rotationSpeed);
        } else if (app.getInputsManager().getKeyState().isKeyPressed(GLFW_KEY_D)) {
            rotateCamera(camera, (float) (-tpf), true, rotationSpeed);
        }
        if (app.getInputsManager().getKeyState().isKeyPressed(GLFW_KEY_W)) {
            rotateCamera(camera, (float) (tpf), false, rotationSpeed);
        } else if (app.getInputsManager().getKeyState().isKeyPressed(GLFW_KEY_S)) {
            rotateCamera(camera, (float) (-tpf), false, rotationSpeed);
        }
        return true;
    }


    protected void rotateCamera(Camera camera, float value, boolean horizontal, float rotationSpeed) {
        if (horizontal) {
            if (invertX) {
                value *= -1;
            }
            angles[1] += value * rotationSpeed;

            // stop the angles from becoming too big.
            if (angles[1] > Math.PI * 2) {
                angles[1] -= Math.PI * 2;
            } else if (angles[1] < -Math.PI * 2) {
                angles[1] += Math.PI * 2;
            }
        } else {
            if (invertY) {
                value *= -1;
            }

            angles[0] += value * rotationSpeed;

            // 89 degrees. Avoid the "flip" problem.
            float maxRotX = (float) (Math.PI / 2 - Math.PI / 180);

            // limit camera rotation.
            if (angles[0] < -maxRotX) {
                angles[0] = -maxRotX;
            }

            if (angles[0] > maxRotX) {
                angles[0] = maxRotX;
            }
        }
        lookAt(camera, focusPoint);
    }


    protected void lookAt(Camera cam, Vector3fc focusPoint) {
        MathTools.setAngles(rotation, angles[0], camera.getUpVector().equals(Vectors.UNIT_Y) ? angles[1] : 0f, camera.getUpVector().equals(Vectors.UNIT_Z) ? angles[1] : 0f);

        try (TempVars vars = TempVars.get()) {
            Vector3f direction = rotation.transformUnit(camera.getUpVector().equals(Vectors.UNIT_Z) ? Vectors.UNIT_Y : Vectors.UNIT_Z, vars.vect1);
            Vector3f loc = direction.mul(zoomDistance);
            cam.getLocation().set(loc);

            cam.lookAt(focusPoint, camera.getUpVector());
        }
    }
}
