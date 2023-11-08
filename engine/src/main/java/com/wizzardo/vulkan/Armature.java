package com.wizzardo.vulkan;

import org.joml.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Armature {
    protected Bone[] bones;
    protected Matrix4f localMatrix;
    protected Map<String, Animation> animations = new HashMap<>();

    public Armature(int size) {
        bones = new Bone[size];
    }

    protected void setBone(int i, Bone bone) {
        bones[i] = bone;
    }

    public int indexOf(String name) {
        for (int i = 0; i < bones.length; i++) {
            if (bones[i] != null && bones[i].name.equals(name))
                return i;
        }
        return -1;
    }

    public Matrix4f getLocalMatrix() {
        return localMatrix;
    }

    public Bone getBone(int i) {
        return bones[i];
    }

    public void addAnimation(Animation animation) {
        animations.put(animation.name, animation);
    }

    public Set<String> getAnimationNames() {
        return animations.keySet();
    }

    public Animation getAnimation(String name) {
        return animations.get(name);
    }

    public int size() {
        return bones.length;
    }

    public static class Animation {
        public String name;
        public double duration;
        public BoneKeys[] boneKeys;

        public static class Key {
            public double time;
            public Vector3f position;
            public Quaternionf rotation;
            public Vector3f scaling;
        }

        public static class BoneKeys {
            public int boneIndex;
            public Key[] keys;
        }
    }

    public static class Bone {
        public int parentIndex;
        public String name;
        public Matrix4f inverseBindMatrix;
        public Matrix4f localMatrix;
        public List<VertexWeight> weights;
    }

    public static class VertexWeight {
        public int vertexIndex;
        public float weight;
    }

    public static class BoneWeight {
        public int boneIndex;
        public float weight;

        public BoneWeight(int boneIndex, float weight) {
            this.boneIndex = boneIndex;
            this.weight = weight;
        }
    }

    public static class BonesWeights {
        public Vector4i boneIndex = new Vector4i().setComponent(3, 0);
        public Vector4f weight = new Vector4f().setComponent(3, 0);
    }

    public BoneWeight[][] calculateBoneWeightsPerVertex(int vertexIndexCount) {
        BoneWeight[][] boneWeightsByVertex = new BoneWeight[vertexIndexCount][];
        for (int i = 0; i < bones.length; i++) {
            outer:
            for (VertexWeight vertexWeight : bones[i].weights) {
                BoneWeight[] arr = boneWeightsByVertex[vertexWeight.vertexIndex];
                if (arr == null)
                    boneWeightsByVertex[vertexWeight.vertexIndex] = arr = new BoneWeight[4];

                int j = 0;
                while (j < 4) {
                    BoneWeight weight = arr[j];
                    if (weight == null)
                        break;
                    if (weight.boneIndex == i)
                        continue outer;
                    j++;
                }
                if (j == 4)
                    throw new IllegalArgumentException("More than 4 weights assigned to a bone per vertex!");

                arr[j] = new BoneWeight(i, vertexWeight.weight);
            }
        }
        return boneWeightsByVertex;
    }

    public BonesWeights[] calculateBonesWeightsPerVertex(int vertexIndexCount) {
        BonesWeights[] boneWeightsByVertex = new BonesWeights[vertexIndexCount];
        for (int i = 0; i < bones.length; i++) {
            outer:
            for (VertexWeight vertexWeight : bones[i].weights) {
                BonesWeights bonesWeights = boneWeightsByVertex[vertexWeight.vertexIndex];
                if (bonesWeights == null)
                    boneWeightsByVertex[vertexWeight.vertexIndex] = bonesWeights = new BonesWeights();

                int j = 0;
                while (j < 4) {
                    float weight = bonesWeights.weight.get(j);
                    if (weight == 0f)
                        break;
                    if (bonesWeights.boneIndex.get(j) == i)
                        continue outer;
                    j++;
                }
                if (j == 4)
                    throw new IllegalArgumentException("More than 4 weights assigned to a bone per vertex!");

                bonesWeights.boneIndex.setComponent(j, i);
                bonesWeights.weight.setComponent(j, vertexWeight.weight);
            }
        }
        return boneWeightsByVertex;
    }
}
