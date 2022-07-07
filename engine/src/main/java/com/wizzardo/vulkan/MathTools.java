package com.wizzardo.vulkan;

import org.joml.Quaternionf;

public class MathTools {

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

}
