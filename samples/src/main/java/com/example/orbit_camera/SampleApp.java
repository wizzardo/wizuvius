package com.example.orbit_camera;

import com.example.AbstractSampleApp;
import com.wizzardo.vulkan.Camera;
import com.wizzardo.vulkan.Material;
import com.wizzardo.vulkan.TempVars;
import com.wizzardo.vulkan.Vectors;
import com.wizzardo.vulkan.input.GlfwKey;
import com.wizzardo.vulkan.material.predefined.UnshadedColor;
import com.wizzardo.vulkan.scene.Geometry;
import com.wizzardo.vulkan.scene.shape.Box;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.lwjgl.glfw.GLFW.*;

public class SampleApp extends AbstractSampleApp {

    public static void main(String[] args) {
        new SampleApp().start();
    }

    private final Quaternionf rotation = new Quaternionf();
    private float[] mousePosition = new float[2];
    private float rotationSpeed = (float) (Math.PI / 2);
    private final float[] angles = new float[2];
    private float zoomDistance = 0;
    private float zoomSpeed = 0.1f;
    private float minZoom = 0.1f;
    private float maxZoom = 50;
    private Vector3fc focusPoint = new Vector3f(Vectors.ZERO);

    @Override
    protected void initApp() {
        getMainViewport().getCamera().setLocation(new Vector3f(2, 2, 2));
        getMainViewport().getCamera().lookAt(Vectors.ZERO, Vectors.UNIT_Z);
        {
            Vector3f location = getMainViewport().getCamera().getLocation();
            float xyLength = (float) Math.sqrt(location.x * location.x + location.y * location.y);
            float xyAngle = (float) Math.asin(location.y / xyLength);
            float xzLength = (float) Math.sqrt(location.x * location.x + location.z * location.z);
            float xzAngle = (float) Math.asin(location.z / xzLength);
            angles[0] = xyAngle;
            angles[1] = xzAngle;
            zoomDistance = location.length();

            lookAt(getMainViewport().getCamera(), focusPoint);
        }

        {
            Material material = new UnshadedColor(new Vector3f(1, 0, 0));
            Geometry geometry = new Box(material);
            addGeometry(geometry, getMainViewport());
        }


        float extentScale = this.getExtentWidth() * 1f / this.getWidth();
        inputsManager.addMouseMoveListener((x, y) -> {
            if (!inputsManager.getKeyState().isMouseButtonPressed(GLFW_MOUSE_BUTTON_1))
                return;

            float diffX = (float) (mousePosition[0] - x);
            float diffY = (float) (mousePosition[1] - y);

            mousePosition[0] = (float) x;
            mousePosition[1] = (float) y;
            Camera camera = getMainViewport().getCamera();

            if (diffX != 0)
                rotateCamera(camera, diffX / 100f, true, rotationSpeed);
            if (diffY != 0)
                rotateCamera(camera, -diffY / 100f, false, rotationSpeed);
        });
        inputsManager.addMouseButtonListener((x, y, button, pressed) -> {
            if (button == GLFW_MOUSE_BUTTON_1 && pressed) {
                mousePosition[0] = (float) x / extentScale;
                mousePosition[1] = (float) y / extentScale;
            }
        });
        inputsManager.addScrollListener((x, y, scrollX, scrollY) -> {
            zoomDistance = (float) Math.min(maxZoom, Math.max(minZoom, zoomDistance + zoomSpeed * scrollY));
            lookAt(getMainViewport().getCamera(), focusPoint);
        });

        inputsManager.addKeyListener((key, pressed, repeat) -> {
            if (GlfwKey.GLFW_KEY_ESCAPE == key)
                shutdown();
        });
    }

    private void rotateCamera(Camera camera, float value, boolean horizontal, float rotationSpeed) {
        if (horizontal) {
            angles[1] += value * rotationSpeed;

            // stop the angles from becoming too big.
            if (angles[1] > Math.PI * 2) {
                angles[1] -= Math.PI * 2;
            } else if (angles[1] < -Math.PI * 2) {
                angles[1] += Math.PI * 2;
            }
        } else {
//            if (invertY) {
//                value *= -1;
//            }

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


    private void lookAt(Camera cam, Vector3fc focusPoint) {
        setAngles(rotation, angles[0], 0f, angles[1]);

        TempVars vars = TempVars.get();
        try {

            Vector3f direction = rotation.transformUnit(Vectors.UNIT_Y, vars.vect1);
            Vector3f loc = direction.mul(zoomDistance);
            cam.getLocation().set(loc);

            cam.lookAt(focusPoint, Vectors.UNIT_Z);
        } finally {
            vars.release();
        }
    }

    /**
     * Sets the quaternion from the specified Tait-Bryan angles, applying the
     * rotations in x-z-y extrinsic order or y-z'-x" intrinsic order.
     *
     * @param q      the Quaternion to modify
     * @param xAngle the X angle (in radians)
     * @param yAngle the Y angle (in radians)
     * @param zAngle the Z angle (in radians)
     * @return the (modified) Quaternion
     * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/conversions/eulerToQuaternion/index.htm">http://www.euclideanspace.com/maths/geometry/rotations/conversions/eulerToQuaternion/index.htm</a>
     */
    public static Quaternionf setAngles(Quaternionf q, float xAngle, float yAngle, float zAngle) {
        float angle;
        double sinY, sinZ, sinX, cosY, cosZ, cosX;
        angle = zAngle * 0.5f;
        sinZ = Math.sin(angle);
        cosZ = Math.cos(angle);
        angle = yAngle * 0.5f;
        sinY = Math.sin(angle);
        cosY = Math.cos(angle);
        angle = xAngle * 0.5f;
        sinX = Math.sin(angle);
        cosX = Math.cos(angle);

        double cosYXcosZ = cosY * cosZ;
        double sinYXsinZ = sinY * sinZ;
        double cosYXsinZ = cosY * sinZ;
        double sinYXcosZ = sinY * cosZ;

        q.w = (float) (cosYXcosZ * cosX - sinYXsinZ * sinX);
        q.x = (float) (cosYXcosZ * sinX + sinYXsinZ * cosX);
        q.y = (float) (sinYXcosZ * cosX + cosYXsinZ * sinX);
        q.z = (float) (cosYXsinZ * cosX - sinYXcosZ * sinX);

        q.normalize();
        return q;
    }

    @Override
    protected void simpleUpdate(double tpf) {
        if (inputsManager.getKeyState().isKeyPressed(GlfwKey.GLFW_KEY_A)) {
            Camera camera = getMainViewport().getCamera();
            rotateCamera(camera, (float) (tpf), true, rotationSpeed);
        } else if (inputsManager.getKeyState().isKeyPressed(GlfwKey.GLFW_KEY_D)) {
            Camera camera = getMainViewport().getCamera();
            rotateCamera(camera, (float) (-tpf), true, rotationSpeed);
        }
        if (inputsManager.getKeyState().isKeyPressed(GlfwKey.GLFW_KEY_W)) {
            Camera camera = getMainViewport().getCamera();
            rotateCamera(camera, (float) (tpf), false, rotationSpeed);
        } else if (inputsManager.getKeyState().isKeyPressed(GlfwKey.GLFW_KEY_S)) {
            Camera camera = getMainViewport().getCamera();
            rotateCamera(camera, (float) (-tpf), false, rotationSpeed);
        }
    }
}
