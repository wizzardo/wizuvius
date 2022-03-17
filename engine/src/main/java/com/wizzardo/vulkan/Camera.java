package com.wizzardo.vulkan;

import org.joml.*;
import org.lwjgl.vulkan.VkExtent2D;

import java.lang.Math;

public class Camera {

    protected Vector3f location = new Vector3f();
    protected Quaternionf rotation = new Quaternionf();
    protected Matrix4f view = new Matrix4f();
    protected Matrix4f projection = new Matrix4f();
    protected Viewport viewport;
    protected int screenWidth;
    protected int screenHeight;

    public void setProjection(float fieldOfViewDegrees, float aspectRation, float nearPlane, float farPlane) {
        projection.setPerspective((float) Math.toRadians(fieldOfViewDegrees), aspectRation, nearPlane, farPlane);
        projection.m11(projection.m11() * -1);
    }

    public void setProjection(Matrix4f projection) {
        this.projection.set(projection);
    }

    public Vector3f getLocation() {
        return location;
    }

    public void setLocation(Vector3f location) {
        this.location = location;
    }

    public Quaternionf getRotation() {
        return rotation;
    }

    public void setRotation(Quaternionf rotation) {
        this.rotation = rotation;
    }

    public Vector3f getDirection() {
        return getRotationColumn(rotation, 2, null);
    }

    public Vector3f getUp() {
        return getRotationColumn(rotation, 1, null);
    }

    /**
     * Converts the given position from world space to screen space.
     *
     * @param worldPosition a location in world coordinates (not null, unaffected)
     * @param store         storage for the result (modified if not null)
     * @return a (3-D) location vector (in screen coordinates, either
     * <code>store</code> or a new vector)
     */
    public Vector3f getScreenCoordinates(Vector3f worldPosition, Vector3f store, int width, int height) {
        if (store == null)
            store = new Vector3f();

        float w = multProj(projection, store, store);
        store.div(w);

        store.x = ((store.x + 1f) / 2f) * width;
        store.y = ((store.y + 1f) / 2f) * height;
        store.z = (store.z + 1f) / 2f;

        return store;
    }

    /**
     * Apply this perspective transform to the specified Vector3f. Return the W
     * value, calculated by dotting the vector with the last row.
     *
     * @param vec   the vector to transform (not null, unaffected)
     * @param store storage for the result (not null, modified)
     * @return the W value
     */
    public static float multProj(Matrix4f projection, Vector3f vec, Vector3f store) {
        float vx = vec.x, vy = vec.y, vz = vec.z;
        store.x = projection.m00() * vx + projection.m10() * vy + projection.m20() * vz + projection.m30();
        store.y = projection.m01() * vx + projection.m11() * vy + projection.m21() * vz + projection.m31();
        store.z = projection.m02() * vx + projection.m12() * vy + projection.m22() * vz + projection.m32();
        return projection.m03() * vx + projection.m13() * vy + projection.m23() * vz + projection.m33();
    }

    protected Vector3f getRotationColumn(Quaternionf q, int i, Vector3f store) {
        if (store == null)
            store = new Vector3f();

        float norm = q.x * q.x + q.y * q.y + q.z * q.z + q.w * q.w;
        if (norm != 1.0f) {
            norm = invSqrt(norm);
        }


        if (i == 0) {
            float xy = q.x * q.y * norm;
            float xz = q.x * q.z * norm;
            float yy = q.y * q.y * norm;
            float yw = q.y * q.w * norm;
            float zz = q.z * q.z * norm;
            float zw = q.z * q.w * norm;

            store.x = 1 - 2 * (yy + zz);
            store.y = 2 * (xy + zw);
            store.z = 2 * (xz - yw);
        } else if (i == 1) {
            float xx = q.x * q.x * norm;
            float xy = q.x * q.y * norm;
            float xw = q.x * q.w * norm;
            float yz = q.y * q.z * norm;
            float zz = q.z * q.z * norm;
            float zw = q.z * q.w * norm;

            store.x = 2 * (xy - zw);
            store.y = 1 - 2 * (xx + zz);
            store.z = 2 * (yz + xw);
        } else if (i == 2) {
            float xx = q.x * q.x * norm;
            float xz = q.x * q.z * norm;
            float xw = q.x * q.w * norm;
            float yy = q.y * q.y * norm;
            float yz = q.y * q.z * norm;
            float yw = q.y * q.w * norm;

            store.x = 2 * (xz + yw);
            store.y = 2 * (yz - xw);
            store.z = 1 - 2 * (xx + yy);
        } else {
            throw new IllegalArgumentException("Invalid column index. " + i);
        }

        return store;
    }

    public void lookAt(Vector3fc pos, Vector3fc worldUpVector) {
        TempVars vars = TempVars.get();
        Vector3f newDirection = vars.vect1;
        Vector3f newUp = vars.vect2;
        Vector3f newLeft = vars.vect3;

        newDirection.set(pos).sub(location).normalize();

        newUp.set(worldUpVector).normalize();
        if (newUp.equals(Vectors.ZERO)) {
            newUp.set(Vectors.UNIT_Y);
        }

        newLeft.set(newUp).cross(newDirection).normalize();
        if (newLeft.equals(Vectors.ZERO)) {
            if (newDirection.x != 0) {
                newLeft.set(newDirection.y, -newDirection.x, 0f);
            } else {
                newLeft.set(0f, newDirection.z, -newDirection.y);
            }
        }

        newUp.set(newDirection).cross(newLeft).normalize();

        fromAxes(newLeft, newUp, newDirection, rotation).normalize();
        vars.release();
    }

    public static float invSqrt(float fValue) {
        return (float) (1.0f / Math.sqrt(fValue));
    }

    public static Quaternionf fromAxes(Vector3f xAxis, Vector3f yAxis, Vector3f zAxis, Quaternionf source) {
        return fromRotationMatrix(xAxis.x, yAxis.x, zAxis.x, xAxis.y, yAxis.y,
                zAxis.y, xAxis.z, yAxis.z, zAxis.z, source);
    }


    public static Matrix4f fromFrame(Vector3fc location, Vector3fc direction, Vector3fc up, Matrix4f source) {
        if (source == null)
            source = new Matrix4f();

        TempVars vars = TempVars.get();
        try {
            Vector3f fwdVector = vars.vect1.set(direction);
            Vector3f leftVector = vars.vect2.set(fwdVector).cross(up);
            Vector3f upVector = vars.vect3.set(leftVector).cross(fwdVector);

            source.m00(leftVector.x);
            source.m10(leftVector.y);
            source.m20(leftVector.z);
            source.m30(-leftVector.dot(location));

            source.m01(upVector.x);
            source.m11(upVector.y);
            source.m21(upVector.z);
            source.m31(-upVector.dot(location));

            source.m02(-fwdVector.x);
            source.m12(-fwdVector.y);
            source.m22(-fwdVector.z);
            source.m32(fwdVector.dot(location));

            source.m03(0f);
            source.m13(0f);
            source.m23(0f);
            source.m33(1f);
        } finally {
            vars.release();
        }
        return source;
    }

    public static Quaternionf fromRotationMatrix(
            float m00,
            float m01,
            float m02,
            float m10,
            float m11,
            float m12,
            float m20,
            float m21,
            float m22,
            Quaternionf source
    ) {
        if (source == null)
            source = new Quaternionf();

        // first normalize the forward (F), up (U) and side (S) vectors of the rotation matrix
        // so that the scale does not affect the rotation
        float lengthSquared = m00 * m00 + m10 * m10 + m20 * m20;
        if (lengthSquared != 1f && lengthSquared != 0f) {
            lengthSquared = (float) (1.0f / Math.sqrt(lengthSquared));
            m00 *= lengthSquared;
            m10 *= lengthSquared;
            m20 *= lengthSquared;
        }
        lengthSquared = m01 * m01 + m11 * m11 + m21 * m21;
        if (lengthSquared != 1f && lengthSquared != 0f) {
            lengthSquared = (float) (1.0f / Math.sqrt(lengthSquared));
            m01 *= lengthSquared;
            m11 *= lengthSquared;
            m21 *= lengthSquared;
        }
        lengthSquared = m02 * m02 + m12 * m12 + m22 * m22;
        if (lengthSquared != 1f && lengthSquared != 0f) {
            lengthSquared = (float) (1.0f / Math.sqrt(lengthSquared));
            m02 *= lengthSquared;
            m12 *= lengthSquared;
            m22 *= lengthSquared;
        }

        // Use the Graphics Gems code, from
        // ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z
        // *NOT* the "Matrix and Quaternions FAQ", which has errors!

        // the trace is the sum of the diagonal elements; see
        // http://mathworld.wolfram.com/MatrixTrace.html
        float t = m00 + m11 + m22;

        // we protect the division by s by ensuring that s>=1
        if (t >= 0) { // |w| >= .5
            float s = (float) Math.sqrt(t + 1); // |s|>=1 ...
            source.w = 0.5f * s;
            s = 0.5f / s;                 // so this division isn't bad
            source.x = (m21 - m12) * s;
            source.y = (m02 - m20) * s;
            source.z = (m10 - m01) * s;
        } else if ((m00 > m11) && (m00 > m22)) {
            float s = (float) Math.sqrt(1.0f + m00 - m11 - m22); // |s|>=1
            source.x = s * 0.5f; // |x| >= .5
            s = 0.5f / s;
            source.y = (m10 + m01) * s;
            source.z = (m02 + m20) * s;
            source.w = (m21 - m12) * s;
        } else if (m11 > m22) {
            float s = (float) Math.sqrt(1.0f + m11 - m00 - m22); // |s|>=1
            source.y = s * 0.5f; // |y| >= .5
            s = 0.5f / s;
            source.x = (m10 + m01) * s;
            source.z = (m21 + m12) * s;
            source.w = (m02 - m20) * s;
        } else {
            float s = (float) Math.sqrt(1.0f + m22 - m00 - m11); // |s|>=1
            source.z = s * 0.5f; // |z| >= .5
            s = 0.5f / s;
            source.x = (m02 + m20) * s;
            source.y = (m21 + m12) * s;
            source.w = (m10 - m01) * s;
        }

        return source;
    }

    public Vector3f getWorldCoordinates(Vector2f screenPos, float projectionZPos) {
        return getWorldCoordinates(screenPos.x, screenPos.y, projectionZPos, null);
    }

    public Vector3f getWorldCoordinates(float x, float y, float projectionZPos, Vector3f store) {
        return getWorldCoordinates(x, y, projectionZPos, store, new Matrix4f());
    }

    public Vector3f getWorldCoordinates(float x, float y, float projectionZPos, Vector3f store, Matrix4f tempMatrix) {
        if (store == null)
            store = new Vector3f();
        if (tempMatrix == null)
            tempMatrix = new Matrix4f();

        Matrix4f inverseMat = tempMatrix;
        inverseMat.set(projection)
                .mul(view)
                .invert();

//        float viewPortLeft = 0;
//        float viewPortRight = 1;
//        float viewPortBottom = 0;
//        float viewPortTop = 1;
//        store.set(
//                (x / screenWidth - viewPortLeft) / (viewPortRight - viewPortLeft) * 2 - 1,
//                (y / screenHeight - viewPortBottom) / (viewPortTop - viewPortBottom) * 2 - 1,
//                projectionZPos * 2 - 1);

        store.set(
                (x / screenWidth) * 2 - 1,
                (y / screenHeight) * 2 - 1,
                projectionZPos * 2 - 1);

        float w = multProj(inverseMat, store, store);
        store.mul(1f / w);

        return store;
    }
}
