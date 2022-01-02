package com.wizzardo.vulkan;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Transform {

    public static final Quaternionf ROT_IDENTITY = new Quaternionf(0, 0, 0, 1);
    public static final Vector3f VECTOR_ZERO = new Vector3f();
    public static final Transform IDENTITY = new Transform();

    private Quaternionf rot = new Quaternionf();
    private Vector3f translation = new Vector3f();
    private Vector3f scale = new Vector3f(1, 1, 1);


    public Transform(Vector3f translation, Quaternionf rot) {
        this.translation.set(translation);
        this.rot.set(rot);
    }

    public Transform(Vector3f translation, Quaternionf rot, Vector3f scale) {
        this(translation, rot);
        this.scale.set(scale);
    }

    public Transform(Vector3f translation) {
        this(translation, ROT_IDENTITY);
    }

    public Transform(Quaternionf rot) {
        this(VECTOR_ZERO, rot);
    }

    public Transform() {
        this(VECTOR_ZERO, ROT_IDENTITY);
    }

    /**
     * Sets this rotation to the given Quaternion value.
     *
     * @param rot The new rotation for this Transform.
     * @return this
     */
    public Transform setRotation(Quaternionf rot) {
        this.rot.set(rot);
        return this;
    }

    /**
     * Sets this translation to the given value.
     *
     * @param trans The new translation for this Transform.
     * @return this
     */
    public Transform setTranslation(Vector3f trans) {
        this.translation.set(trans);
        return this;
    }

    /**
     * Return the translation vector in this Transform.
     *
     * @return translation vector.
     */
    public Vector3f getTranslation() {
        return translation;
    }

    /**
     * Sets this scale to the given value.
     *
     * @param scale The new scale for this Transform.
     * @return this
     */
    public Transform setScale(Vector3f scale) {
        this.scale.set(scale);
        return this;
    }

    /**
     * Sets this scale to the given value.
     *
     * @param scale The new scale for this Transform.
     * @return this
     */
    public Transform setScale(float scale) {
        this.scale.set(scale, scale, scale);
        return this;
    }

    /**
     * Return the scale vector in this Transform.
     *
     * @return scale vector.
     */
    public Vector3f getScale() {
        return scale;
    }

    /**
     * Stores this translation value into the given vector3f. If trans is null,
     * a new vector3f is created to hold the value. The value, once stored, is
     * returned.
     *
     * @param trans The store location for this transform's translation.
     * @return The value of this transform's translation.
     */
    public Vector3f getTranslation(Vector3f trans) {
        if (trans == null) {
            trans = new Vector3f();
        }
        trans.set(this.translation);
        return trans;
    }

    /**
     * Stores this rotation value into the given Quaternion. If quat is null, a
     * new Quaternion is created to hold the value. The value, once stored, is
     * returned.
     *
     * @param quat The store location for this transform's rotation.
     * @return The value of this transform's rotation.
     */
    public Quaternionf getRotation(Quaternionf quat) {
        if (quat == null) {
            quat = new Quaternionf();
        }
        quat.set(rot);
        return quat;
    }

    /**
     * Return the rotation quaternion in this Transform.
     *
     * @return rotation quaternion.
     */
    public Quaternionf getRotation() {
        return rot;
    }

    /**
     * Stores this scale value into the given vector3f. If scale is null, a new
     * vector3f is created to hold the value. The value, once stored, is
     * returned.
     *
     * @param scale The store location for this transform's scale.
     * @return The value of this transform's scale.
     */
    public Vector3f getScale(Vector3f scale) {
        if (scale == null) {
            scale = new Vector3f();
        }
        scale.set(this.scale);
        return scale;
    }

//    /**
//     * Sets this transform to the interpolation between the first transform and
//     * the second by delta amount.
//     *
//     * @param t1 The beginning transform.
//     * @param t2 The ending transform.
//     * @param delta An amount between 0 and 1 representing how far to
//     * interpolate from t1 to t2.
//     */
//    public void interpolateTransforms(Transform t1, Transform t2, float delta) {
//        this.rot.set(t1.rot);
//        this.rot.nlerp(t2.rot, delta);
//        this.translation.interpolateLocal(t1.translation, t2.translation, delta);
//        this.scale.interpolateLocal(t1.scale, t2.scale, delta);
//    }
//
//    /**
//     * Changes the values of this Transform according to its parent. Very similar
//     * to the concept of Node/Spatial transforms.
//     *
//     * @param parent The parent Transform.
//     * @return This Transform, after combining.
//     */
//    public Transform combineWithParent(Transform parent) {
//        //applying parent scale to local scale
//        scale.multLocal(parent.scale);
//        //applying parent rotation to local rotation.
//        parent.rot.mult(rot, rot);
//        //applying parent scale to local translation.
//        translation.multLocal(parent.scale);
//        //applying parent rotation to local translation, then applying parent translation to local translation.
//        //Note that parent.rot.multLocal(translation) doesn't modify "parent.rot" but "translation"
//        parent.rot
//                .multLocal(translation)
//                .addLocal(parent.translation);
//
//        return this;
//    }

    /**
     * Sets this transform's translation to the given x,y,z values.
     *
     * @param x This transform's new x translation.
     * @param y This transform's new y translation.
     * @param z This transform's new z translation.
     * @return this
     */
    public Transform setTranslation(float x, float y, float z) {
        translation.set(x, y, z);
        return this;
    }

    /**
     * Sets this transform's scale to the given x,y,z values.
     *
     * @param x This transform's new x scale.
     * @param y This transform's new y scale.
     * @param z This transform's new z scale.
     * @return this
     */
    public Transform setScale(float x, float y, float z) {
        scale.set(x, y, z);
        return this;
    }

//    public Vector3f transformVector(final Vector3f in, Vector3f store) {
//        if (store == null) {
//            store = new Vector3f();
//        }
//
//        // multiply with scale first, then rotate, finally translate (cf.
//        // Eberly)
//        return rot.mult(store.set(in).multLocal(scale), store).addLocal(translation);
//    }
//
//    public Vector3f transformInverseVector(final Vector3f in, Vector3f store) {
//        if (store == null) {
//            store = new Vector3f();
//        }
//
//        // The author of this code should look above and take the inverse of that
//        // But for some reason, they didn't ..
////        in.subtract(translation, store).divideLocal(scale);
////        rot.inverse().mult(store, store);
//        in.subtract(translation, store);
//        rot.inverse().mult(store, store);
//        store.divideLocal(scale);
//
//        return store;
//    }
//
//    public Matrix4f toTransformMatrix() {
//        return toTransformMatrix(null);
//    }
//
//    public Matrix4f toTransformMatrix(Matrix4f store) {
//        if (store == null) {
//            store = new Matrix4f();
//        }
//        store.setTranslation(translation);
//        rot.toTransformMatrix(store);
//        store.setScale(scale);
//        return store;
//    }
//
//    public void fromTransformMatrix(Matrix4f mat) {
//        TempVars vars = TempVars.get();
//        translation.set(mat.toTranslationVector(vars.vect1));
//        rot.set(mat.toRotationQuat(vars.quat1));
//        scale.set(mat.toScaleVector(vars.vect2));
//        vars.release();
//    }
//
//    public Transform invert() {
//        Transform t = new Transform();
//        t.fromTransformMatrix(toTransformMatrix().invertLocal());
//        return t;
//    }

}
