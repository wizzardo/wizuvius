package com.wizzardo.vulkan;

import com.wizzardo.vulkan.material.predefined.UnshadedColor;
import com.wizzardo.vulkan.material.predefined.UnshadedTexture;
import com.wizzardo.vulkan.misc.IntArrayBuilder;
import com.wizzardo.vulkan.scene.Geometry;
import com.wizzardo.vulkan.scene.Node;
import com.wizzardo.vulkan.scene.Spatial;
import org.joml.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.IOException;
import java.lang.Math;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.Arrays;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.system.MemoryUtil.*;

public class ModelLoader {


    static {
        Configuration.DISABLE_FUNCTION_CHECKS.set(true);
        Assimp.aiGetVersionMajor();
        System.out.println("Assimp.aiGetVersionMajor() " + Assimp.aiGetVersionMajor());
        Configuration.DISABLE_FUNCTION_CHECKS.set(false);
//        for (int i = 0; i < aiGetImportFormatCount(); i++) {
//            AIImporterDesc importerDesc = aiGetImportFormatDescription(i);
//            System.out.println(importerDesc.mNameString()+": "+importerDesc.mFileExtensionsString());
//        }
        AILogStream logStream = AILogStream.create();
        logStream.callback((message, user) -> {
            String s = memUTF8(message);
            System.out.println("assimp: " + s);
        });
        Assimp.aiAttachLogStream(logStream);
    }

    public static Spatial loadModel(File file, int flags) {
        try (AIScene scene = aiImportFile(file.getAbsolutePath(), flags)) {
            if (scene == null || scene.mRootNode() == null) {
                throw new RuntimeException("Could not load model: " + aiGetErrorString());
            }

            Spatial s = new Spatial();
            List<AssimpMaterial> materials = processMaterials(scene);
            processNode(scene.mRootNode(), scene, s, convertMaterials(materials));
            return s;
        }
    }

    public interface AssetLoader {
        ByteBuffer load(String asset) throws IOException;
    }

    public static Spatial loadModel(ByteBuffer file, int flags, String hint) {
        try (AIScene scene = aiImportFileFromMemory(file, flags, hint)) {
            if (scene == null || scene.mRootNode() == null) {
                throw new RuntimeException("Could not load model: " + aiGetErrorString());
            }

            Spatial s = new Spatial();
            List<AssimpMaterial> materials = processMaterials(scene);
            processNode(scene.mRootNode(), scene, s, convertMaterials(materials));
            return s;
        }
    }

    public static Spatial loadModel(String path, int flags, AssetLoader assetLoader) {
        AIFileIO fileIO = AIFileIO.create();

        try {
            Map<String, ByteBuffer> loaded = new HashMap<>();
            Map<Long, AIFile> openFiles = new HashMap<>();
            fileIO.OpenProc((pFileIO, fileName, openMode) -> {
                        ByteBuffer data;
                        String fileNameUtf8 = memUTF8(fileName);
                        try {
                            ByteBuffer byteBuffer = loaded.get(fileNameUtf8);
                            if (byteBuffer != null) {
                                data = byteBuffer.slice();
                            } else {
                                System.out.println("trying to open file " + fileNameUtf8);
                                data = assetLoader.load(fileNameUtf8);
                                loaded.put(fileNameUtf8, data.slice());
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Could not open file: " + fileNameUtf8);
                        }

                        AIFile file = AIFile.create();
                        openFiles.put(file.address(), file);
                        return file
                                .ReadProc((pFile, pBuffer, size, count) -> {
                                    long max = Math.min(data.remaining(), size * count);
                                    if (max != size * count && max % size != 0) {
                                        max -= max % size;
                                    }
                                    System.out.println(fileNameUtf8 + ".read " + max + "  " + size + "  " + count);
                                    if (data.position() != 0 || max != 200 || fileNameUtf8.toLowerCase().endsWith("obj")) {
                                        memCopy(memAddress(data) + data.position(), pBuffer, max);
                                    }
                                    data.position((int) (data.position() + max));
                                    return max / size;
                                })
                                .TellProc(pFile -> data.position())
                                .SeekProc((pFile, offset, origin) -> {
                                    if (origin == aiOrigin_CUR) {
                                        data.position(data.position() + (int) offset);
                                    } else if (origin == aiOrigin_SET) {
                                        data.position((int) offset);
                                    } else if (origin == aiOrigin_END) {
                                        data.position(data.limit() + (int) offset);
                                    }
                                    System.out.println(fileNameUtf8 + ".seek " + data.position() + " " + origin);
                                    return 0;
                                })
                                .FileSizeProc(pFile -> data.limit())
                                .address();
                    })
                    .CloseProc((pFileIO, pFile) -> {
                        AIFile aiFile = openFiles.get(pFile);
                        if (aiFile == null)
                            return;

                        aiFile.ReadProc().free();
                        aiFile.SeekProc().free();
                        aiFile.FileSizeProc().free();
                        aiFile.TellProc().free();

                        openFiles.remove(pFile);
                    });

            try (AIScene scene = aiImportFileEx(path, flags, fileIO)) {
                if (scene == null || scene.mRootNode() == null) {
                    throw new RuntimeException("Could not load model: " + aiGetErrorString());
                }

                String folder = path.substring(0, path.lastIndexOf('/'));
                Spatial s = new Spatial();
                List<AssimpMaterial> assimpMaterials = processMaterials(scene);
                List<AssimpAnimation> assimpAnimations = processAnimations(scene);
                List<Material> materials = convertMaterials(assimpMaterials);
                for (Material material : materials) {
                    for (TextureImage texture : material.getTextures()) {
                        texture.setFilename(folder + "/" + texture.filename);
                    }
                }
                processNode(scene.mRootNode(), scene, s, materials);

                for (AssimpAnimation assimpAnimation : assimpAnimations) {
                    s.geometries()
                            .filter(geometry -> {
                                Armature armature = geometry.getMesh().armature;
                                if (armature != null && assimpAnimation.nodeAnimations.size() <= armature.bones.length) {
                                    for (NodeAnimation nodeAnimation : assimpAnimation.nodeAnimations) {
                                        if (armature.indexOf(nodeAnimation.nodeName) == -1)
                                            return false;
                                    }
                                    return true;
                                }
                                return false;
                            })
                            .forEach(geometry -> geometry.getMesh().armature.addAnimation(assimpAnimation.toAnimation(geometry.getMesh().armature)));
                }

                return s;
            }

        } finally {
            fileIO.OpenProc().free();
            fileIO.CloseProc().free();
        }
    }

    protected static List<Material> convertMaterials(List<AssimpMaterial> assimpMaterials) {
        return assimpMaterials.stream().map(assimpMaterial -> {
            if (!assimpMaterial.textures.isEmpty()) {
                assimpMaterial.textures.sort(Comparator.comparingInt(o -> o.type.ordinal()));
                UnshadedTexture material = new UnshadedTexture();
                for (TextureInfo texture : assimpMaterial.textures) {
                    if (texture.loadableTexture != null)
                        material.addTextureImage(texture.loadableTexture);
                    else
                        material.addTextureImage(new TextureImage(texture.fileName, texture.type.toTextureImageType()));
                }
                return material;
            }
            return new UnshadedColor(new Vector3f(assimpMaterial.diffuseColor.x, assimpMaterial.diffuseColor.y, assimpMaterial.diffuseColor.z));
        }).collect(Collectors.toList());
    }

    private static void processNode(AINode node, AIScene scene, Spatial s, List<Material> materials) {
        s.setName(node.mName().dataString());
        AIMatrix4x4 transformation = node.mTransformation();
        Matrix4f matrix4f = new Matrix4f(
                transformation.a1(), transformation.b1(), transformation.c1(), transformation.d1(),
                transformation.a2(), transformation.b2(), transformation.c2(), transformation.d2(),
                transformation.a3(), transformation.b3(), transformation.c3(), transformation.d3(),
                transformation.a4(), transformation.b4(), transformation.c4(), transformation.d4()
        );
        matrix4f.getTranslation(s.getLocalTransform().getTranslation());
        matrix4f.getScale(s.getLocalTransform().getScale());
        matrix4f.getUnnormalizedRotation(s.getLocalTransform().getRotation());

        if (node.mNumMeshes() != 0) {
            processNodeMeshes(scene, node, s, materials);
        }

        if (node.mChildren() != null) {
            PointerBuffer children = node.mChildren();
            for (int i = 0; i < node.mNumChildren(); i++) {
                AINode child = AINode.create(children.get(i));
                Spatial nodeList = new Spatial();
                processNode(child, scene, nodeList, materials);
                s.attachChild(nodeList);
            }
        }

    }

    private static void processNodeMeshes(AIScene scene, AINode node, Node n, List<Material> materials) {
        IntBuffer meshIndices = node.mMeshes();
        for (int i = 0; i < meshIndices.capacity(); i++) {
            processMesh(scene, meshIndices.get(i), n, materials);
        }
    }

    private static List<AssimpAnimation> processAnimations(AIScene scene) {
        int numAnimations = scene.mNumAnimations();
        ArrayList<AssimpAnimation> animations = new ArrayList<>(numAnimations);

        PointerBuffer mAnimations = scene.mAnimations();

        for (int i = 0; i < numAnimations; i++) {
            AIAnimation animation = AIAnimation.create(mAnimations.get(i));
            AssimpAnimation a = new AssimpAnimation();
            animations.add(a);
            a.duration = animation.mDuration();
            a.ticksPerSecond = animation.mTicksPerSecond();
            a.name = animation.mName().dataString();
            a.channelsNumber = animation.mNumChannels();
            a.meshChannelsNumber = animation.mNumMeshChannels();
            a.morphMeshChannelsNumber = animation.mNumMorphMeshChannels();

            ArrayList<NodeAnimation> nodeAnimations = new ArrayList<>(animation.mNumChannels());
            a.nodeAnimations = nodeAnimations;
            PointerBuffer mChannels = animation.mChannels();
            for (int j = 0; j < a.channelsNumber; j++) {
                AINodeAnim aiNodeAnim = AINodeAnim.create(mChannels.get(j));
                NodeAnimation na = new NodeAnimation();
                nodeAnimations.add(na);
                na.nodeName = aiNodeAnim.mNodeName().dataString();
                na.preState = aiNodeAnim.mPreState();
                na.postState = aiNodeAnim.mPostState();

                int keysNum = aiNodeAnim.mNumPositionKeys();
                na.keys = new ArrayList<>(keysNum);
                AIVectorKey.Buffer positionKeys = aiNodeAnim.mPositionKeys();
                AIVectorKey.Buffer scalingKeys = aiNodeAnim.mScalingKeys();
                AIQuatKey.Buffer rotationKeys = aiNodeAnim.mRotationKeys();
                for (int k = 0; k < keysNum; k++) {
                    AIVectorKey aiVectorKey = positionKeys.get(k);
                    AIVector3D scale = scalingKeys.get(k).mValue();
                    AIVector3D position = aiVectorKey.mValue();
                    AIQuaternion rotation = rotationKeys.get(k).mValue();
                    NodeAnimationKey key = new NodeAnimationKey();
                    key.time = aiVectorKey.mTime();
                    key.position = new Vector3f(position.x(), position.y(), position.z());
                    key.scaling = new Vector3f(scale.x(), scale.y(), scale.z());
                    key.rotation = new Quaternionf(rotation.x(), rotation.y(), rotation.z(), rotation.w());
                    na.keys.add(key);
                }
            }
        }
        return animations;
    }

    private static List<AssimpMaterial> processMaterials(AIScene scene) {
        PointerBuffer pMaterials = scene.mMaterials();
        int numMaterials = scene.mNumMaterials();
        ArrayList<AssimpMaterial> materials = new ArrayList<>(numMaterials);
        AIColor4D colour = AIColor4D.create();
        AIString string = AIString.create();
        float[] floats = new float[4];
        int[] ints = new int[4];
        int[] arraysLimits = new int[]{4};


        PointerBuffer mTextures = scene.mTextures();
        int numTextures = scene.mNumTextures();
        List<LoadableTexture> textures = new ArrayList<>(numTextures);

        for (int i = 0; i < numTextures; i++) {
            AITexture t = AITexture.create(mTextures.get(0));
            ByteBuffer src = t.pcDataCompressed();
            ByteBuffer data = memAlloc(src.capacity());
            MemoryUtil.memCopy(src, data);
            textures.add(new LoadableTexture(data, t.achFormatHintString(), t.mFilename().dataString(), TextureImage.Type.UNKNOWN));
        }

        for (int i = 0; i < numMaterials; i++) {
            AIMaterial material = AIMaterial.create(pMaterials.get(i));
            AssimpMaterial m = new AssimpMaterial();
            materials.add(m);
//            int numProperties = material.mNumProperties();
//            PointerBuffer pMaterialProperties = material.mProperties();
//            for (int j = 0; j < numProperties; j++) {
//                AIMaterialProperty materialProperty = AIMaterialProperty.create(pMaterialProperties.get(j));
//                String key = materialProperty.mKey().dataString();
//                Object value = null;
//
//                System.out.println(key + ": " + materialProperty.mType() + "    " + value);
//            }
//            System.out.println();


            for (int type = 1; type <= 21; type++) {
                int count = aiGetMaterialTextureCount(material, type);
                if (count != 0) {
                    if (m.textures.isEmpty())
                        m.textures = new ArrayList<>(count);
                    System.out.println("material has a texture of type: " + TextureType.valueOf(type) + "; count: " + count);
                    for (int j = 0; j < count; j++) {
                        aiGetMaterialTexture(material, type, j, string, (IntBuffer) null, null, null, null, null, null);
                        String value = string.dataString();
                        System.out.println("\t" + value);
                        TextureType textureType = TextureType.valueOf(type);
                        if (value.startsWith("*")) {
                            LoadableTexture loadableTexture = textures.get(Integer.parseInt(value.substring(1)));
                            if (loadableTexture.type == TextureImage.Type.UNKNOWN)
                                loadableTexture.setType(textureType.toTextureImageType());

                            m.textures.add(new TextureInfo(loadableTexture, textureType));
                        } else {
                            if (File.separatorChar == '/') {
                                value = value.replace("\\", "/");
                            } else {
                                value = value.replace("/", "\\");
                            }

                            m.textures.add(new TextureInfo(value, textureType));
                        }
                    }
                }
            }


            int result = aiGetMaterialColor(material, AI_MATKEY_COLOR_AMBIENT, aiTextureType_NONE, 0, colour);
            if (result == 0) {
                m.ambientColor = new Vector4f(colour.r(), colour.g(), colour.b(), colour.a());
            }
            result = aiGetMaterialColor(material, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, colour);
            if (result == 0) {
                m.diffuseColor = new Vector4f(colour.r(), colour.g(), colour.b(), colour.a());
            }
            result = aiGetMaterialColor(material, AI_MATKEY_COLOR_EMISSIVE, aiTextureType_NONE, 0, colour);
            if (result == 0) {
                m.emissiveColor = new Vector4f(colour.r(), colour.g(), colour.b(), colour.a());
            }
            result = aiGetMaterialColor(material, AI_MATKEY_COLOR_REFLECTIVE, aiTextureType_NONE, 0, colour);
            if (result == 0) {
                m.reflectiveColor = new Vector4f(colour.r(), colour.g(), colour.b(), colour.a());
            }
            result = aiGetMaterialColor(material, AI_MATKEY_COLOR_SPECULAR, aiTextureType_NONE, 0, colour);
            if (result == 0) {
                m.specularColor = new Vector4f(colour.r(), colour.g(), colour.b(), colour.a());
            }
            result = aiGetMaterialColor(material, AI_MATKEY_COLOR_TRANSPARENT, aiTextureType_NONE, 0, colour);
            if (result == 0) {
                m.transparentColor = new Vector4f(colour.r(), colour.g(), colour.b(), colour.a());
            }
            result = aiGetMaterialString(material, AI_MATKEY_NAME, aiTextureType_NONE, 0, string);
            if (result == 0) {
                m.name = string.dataString();
            }
            result = aiGetMaterialString(material, AI_MATKEY_GLTF_ALPHAMODE, aiTextureType_NONE, 0, string);
            if (result == 0) {
                m.alphamode = string.dataString();
            }

            arraysLimits[0] = floats.length;
            result = aiGetMaterialFloatArray(material, AI_MATKEY_SHININESS, aiTextureType_NONE, 0, floats, arraysLimits);
            if (result == 0) {
                m.shininess = floats[0];
            }
            arraysLimits[0] = floats.length;
            result = aiGetMaterialFloatArray(material, AI_MATKEY_METALLIC_FACTOR, aiTextureType_NONE, 0, floats, arraysLimits);
            if (result == 0) {
                m.metallicFactor = floats[0];
            }
            arraysLimits[0] = floats.length;
            result = aiGetMaterialFloatArray(material, AI_MATKEY_ROUGHNESS_FACTOR, aiTextureType_NONE, 0, floats, arraysLimits);
            if (result == 0) {
                m.roughnessFactor = floats[0];
            }
            arraysLimits[0] = floats.length;
            result = aiGetMaterialFloatArray(material, AI_MATKEY_OPACITY, aiTextureType_NONE, 0, floats, arraysLimits);
            if (result == 0) {
                m.opacity = floats[0];
            }
            arraysLimits[0] = floats.length;
            result = aiGetMaterialFloatArray(material, AI_MATKEY_GLTF_ALPHACUTOFF, aiTextureType_NONE, 0, floats, arraysLimits);
            if (result == 0) {
                m.alphacutoff = floats[0];
            }
            arraysLimits[0] = floats.length;
            result = aiGetMaterialIntegerArray(material, AI_MATKEY_SHADING_MODEL, aiTextureType_NONE, 0, ints, arraysLimits);
            if (result == 0) {
                m.shadingModel = ints[0];
            }

        }
        return materials;
    }

    private static void processMesh(AIScene scene, int meshIndex, Node model, List<Material> materials) {

        PointerBuffer pMeshes = scene.mMeshes();
        AIMesh mesh = AIMesh.create(pMeshes.get(meshIndex));

        Material material = materials.get(mesh.mMaterialIndex());

        Geometry geometry = new Geometry(
                mesh.mName().dataString(),
                readMesh(mesh),
                material
        );
        model.attachChild(geometry);
    }

    private static Armature readArmature(AIMesh mesh) {
        int numBones = mesh.mNumBones();
        PointerBuffer mBones = mesh.mBones();
        Armature armature = new Armature(numBones);
        for (int i = 0; i < numBones; i++) {
            AIBone aiBone = AIBone.create(mBones.get(i));
            Armature.Bone bone = new Armature.Bone();
            armature.setBone(i, bone);
            bone.name = aiBone.mName().dataString();
            AINode aiNode = aiBone.mNode();
            AINode mParent = aiNode.mParent();
            String parentName = mParent.mName().dataString();
            bone.parentIndex = armature.indexOf(parentName);
            if (i == 0) {
                AIMatrix4x4 transformation = mParent.mTransformation();
                Matrix4f nodeTransformation = new Matrix4f(
                        transformation.a1(), transformation.b1(), transformation.c1(), transformation.d1(),
                        transformation.a2(), transformation.b2(), transformation.c2(), transformation.d2(),
                        transformation.a3(), transformation.b3(), transformation.c3(), transformation.d3(),
                        transformation.a4(), transformation.b4(), transformation.c4(), transformation.d4()
                );
                armature.localMatrix = nodeTransformation;
            }
            AIMatrix4x4 transformation = aiNode.mTransformation();
            Matrix4f nodeTransformation = new Matrix4f(
                    transformation.a1(), transformation.b1(), transformation.c1(), transformation.d1(),
                    transformation.a2(), transformation.b2(), transformation.c2(), transformation.d2(),
                    transformation.a3(), transformation.b3(), transformation.c3(), transformation.d3(),
                    transformation.a4(), transformation.b4(), transformation.c4(), transformation.d4()
            );

            Matrix4f parentMatrix = bone.parentIndex == -1 ? armature.localMatrix : armature.getBone(bone.parentIndex).localMatrix;
            parentMatrix.mul(nodeTransformation, nodeTransformation);
//            if (bone.parentIndex != -1) {
//                armature.getBone(bone.parentIndex).localMatrix.mul(nodeTransformation, nodeTransformation);
//            }

            bone.localMatrix = nodeTransformation;

            AIMatrix4x4 aiMatrix4x4 = aiBone.mOffsetMatrix();
            bone.inverseBindMatrix = new Matrix4f(
                    aiMatrix4x4.a1(), aiMatrix4x4.b1(), aiMatrix4x4.c1(), aiMatrix4x4.d1(),
                    aiMatrix4x4.a2(), aiMatrix4x4.b2(), aiMatrix4x4.c2(), aiMatrix4x4.d2(),
                    aiMatrix4x4.a3(), aiMatrix4x4.b3(), aiMatrix4x4.c3(), aiMatrix4x4.d3(),
                    aiMatrix4x4.a4(), aiMatrix4x4.b4(), aiMatrix4x4.c4(), aiMatrix4x4.d4()
            );

            AIVertexWeight.Buffer weights = aiBone.mWeights();
            bone.weights = new ArrayList<>(weights.capacity());
            for (int j = 0; j < weights.capacity(); j++) {
                AIVertexWeight aiVertexWeight = weights.get(j);
                Armature.VertexWeight weight = new Armature.VertexWeight();
                bone.weights.add(weight);
                weight.vertexIndex = aiVertexWeight.mVertexId();
                weight.weight = aiVertexWeight.mWeight();
            }
        }

        return armature;
    }

    private static Mesh readMesh(AIMesh mesh) {
        AIVector3D.Buffer positions = requireNonNull(mesh.mVertices());
        AIVector3D.Buffer normals = mesh.mNormals();
        AIVector3D.Buffer tangents = mesh.mTangents();
        AIColor4D.Buffer colors = mesh.mColors(0);
        AIVector3D.Buffer textureCoords = mesh.mTextureCoords(0);

//        AIVector3D tempVector3D = AIVector3D.create();
//        AIColor4D tempColor = AIColor4D.create();

        Armature armature = readArmature(mesh);
        Armature.BonesWeights[] bonesWeightsPerVertex = armature.calculateBonesWeightsPerVertex(positions.capacity());

        Vertex[] vertices = new Vertex[positions.capacity()];
        for (int i = 0; i < vertices.length; i++) {
            AIVector3D position = positions.get(i);
            AIColor4D color = colors != null ? colors.get(i) : null;
            AIVector3D texCoords = textureCoords != null ? textureCoords.get(i) : null;
            AIVector3D normal = normals != null ? normals.get(i) : null;
            AIVector3D tangent = tangents != null ? tangents.get(i) : null;

            Armature.BonesWeights bonesWeights = bonesWeightsPerVertex[i];
            Vertex vertex = new Vertex(
                    new Vector3f(position.x(), position.y(), position.z()),
                    color != null ? new Vector4f(color.r(), color.g(), color.b(), color.a()) : null,
                    texCoords != null ? new Vector2f(texCoords.x(), texCoords.y()) : null,
                    normal != null ? new Vector3f(normal.x(), normal.y(), normal.z()) : null,
                    tangent != null ? new Vector3f(tangent.x(), tangent.y(), tangent.z()) : null,
                    bonesWeights != null ? bonesWeights.weight : null,
                    bonesWeights != null ? bonesWeights.boneIndex : null
            );
            vertices[i] = vertex;
        }

        IntArrayBuilder indexes = new IntArrayBuilder((int) (positions.capacity() * 1.5));
        AIFace.Buffer aiFaces = mesh.mFaces();
        for (int i = 0; i < mesh.mNumFaces(); i++) {
            AIFace face = aiFaces.get(i);
            IntBuffer pIndices = face.mIndices();
            int numIndices = face.mNumIndices();
            for (int j = 0; j < numIndices; j++) {
                indexes.append(pIndices.get(j));
            }
        }

        Mesh m = new Mesh(vertices, indexes.toIntArray(true));

        AIAABB aabb = mesh.mAABB();
        if (aabb != null) {
            AIVector3D min = aabb.mMin();
            AIVector3D max = aabb.mMax();
            m.setBoundingBox(new Mesh.BoundingBox(
                    new Vector3f(min.x(), min.y(), min.z()),
                    new Vector3f(max.x(), max.y(), max.z())
            ));
        }

        m.armature = armature;

        return m;
    }

    public static class AssimpAnimation {
        public double duration;
        public String name;
        public int channelsNumber;
        public int meshChannelsNumber;
        public int morphMeshChannelsNumber;
        public double ticksPerSecond;
        public List<NodeAnimation> nodeAnimations;

        public Armature.Animation toAnimation(Armature armature) {
            Armature.Animation animation = new Armature.Animation();
            animation.name = name;
            animation.duration = ticksPerSecond > 0 ? duration / ticksPerSecond : duration;
            animation.boneKeys = new Armature.Animation.BoneKeys[nodeAnimations.size()];
            for (int i = 0; i < nodeAnimations.size(); i++) {
                NodeAnimation nodeAnimation = nodeAnimations.get(i);
                Armature.Animation.BoneKeys boneKeys = new Armature.Animation.BoneKeys();
                animation.boneKeys[i] = boneKeys;
                boneKeys.boneIndex = armature.indexOf(nodeAnimation.nodeName);
                boneKeys.keys = new Armature.Animation.Key[nodeAnimation.keys.size()];
                List<NodeAnimationKey> keys = nodeAnimation.keys;
                for (int j = 0; j < keys.size(); j++) {
                    NodeAnimationKey animationKey = keys.get(j);
                    Armature.Animation.Key key = new Armature.Animation.Key();
                    boneKeys.keys[j] = key;
                    key.time = animationKey.time;
                    key.rotation = animationKey.rotation;
                    key.position = animationKey.position;
                    key.scaling = animationKey.scaling;
                }
            }
            Arrays.sort(animation.boneKeys, Comparator.comparingInt(value -> value.boneIndex));
            return animation;
        }
    }

    public static class NodeAnimation {
        public String nodeName;
        public int preState;
        public int postState;
        public List<NodeAnimationKey> keys;
    }

    public static class NodeAnimationKey {
        double time;
        public Vector3f position;
        public Quaternionf rotation;
        public Vector3f scaling;
    }

    public static class AssimpMaterial {
        public String name;
        public Vector4f ambientColor;
        public Vector4f diffuseColor;
        public Vector4f emissiveColor;
        public Vector4f reflectiveColor;
        public Vector4f specularColor;
        public Vector4f transparentColor;
        public String alphamode;
        public float shininess;
        public float metallicFactor;
        public float roughnessFactor;
        public float opacity;
        public float alphacutoff;
        public int shadingModel;
        public List<TextureInfo> textures = Collections.emptyList();
    }

    public static class TextureInfo {
        public final String fileName;
        public final TextureType type;
        public final LoadableTexture loadableTexture;

        public TextureInfo(String fileName, TextureType type) {
            this.fileName = fileName;
            this.type = type;
            this.loadableTexture = null;
        }

        public TextureInfo(LoadableTexture loadableTexture, TextureType type) {
            this.fileName = loadableTexture.filename;
            this.type = type;
            this.loadableTexture = loadableTexture;
        }
    }

    public static class LoadableTexture extends TextureImage {
        protected ByteBuffer data;
        protected String hint;

        public LoadableTexture(ByteBuffer buffer, String hint, String filename, Type type) {
            super(filename, type);
            this.data = buffer;
            this.hint = hint;
        }

        @Override
        public void load(VulkanApplication application) throws IOException {
            if (textureImage != 0)
                return;

            TextureImage image = TextureLoader.createTextureImage(
                    application.physicalDevice,
                    application.device,
                    application.transferQueue,
                    application.commandPool,
                    () -> data
            );
            this.textureImage = image.textureImage;
            this.textureImageMemory = image.textureImageMemory;
            this.textureImageView = image.textureImageView;
            data = null;
        }
    }

    public enum TextureType {
        NONE(0),
        DIFFUSE(1),
        SPECULAR(2),
        AMBIENT(3),
        EMISSIVE(4),
        HEIGHT(5),
        NORMALS(6),
        SHININESS(7),
        OPACITY(8),
        DISPLACEMENT(9),
        LIGHTMAP(10),
        REFLECTION(11),
        BASE_COLOR(12),
        NORMAL_CAMERA(13),
        EMISSION_COLOR(14),
        METALNESS(15),
        DIFFUSE_ROUGHNESS(16),
        AMBIENT_OCCLUSION(17),
        SHEEN(19),
        CLEARCOAT(20),
        TRANSMISSION(21),
        UNKNOWN(18);

        public final int assimpType;

        private static final TextureType[] byAssimpType = new TextureType[22];

        TextureType(int assimpType) {
            this.assimpType = assimpType;
        }

        static {
            for (TextureType type : values()) {
                byAssimpType[type.assimpType] = type;
            }
        }

        public static TextureType valueOf(int assimpType) {
            return byAssimpType[assimpType];
        }

        public TextureImage.Type toTextureImageType() {
            return TextureImage.Type.valueOf(name());
        }
    }

}
